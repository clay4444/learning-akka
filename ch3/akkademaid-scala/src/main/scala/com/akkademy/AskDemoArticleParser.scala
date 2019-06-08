package com.akkademy

import akka.actor.Actor
import akka.pattern.ask
import akka.util.Timeout
import com.akkademy.messages.{GetRequest, SetRequest}

import scala.concurrent.Future

/**
  * ask：向acker发送一条消息，返回一个Future，当Actor返回响应时，会完成Future，但是不会向消息发送者的
  * 邮箱返回任何的消息；
  *
  * 每次发送一个消息都会创建一个临时的Actor 用于完成之前定义的Future，而且每ask一次，就生成一个临时的actor
  *
  * 必须设置超时参数
  *
  * 当代码中发生错误时，一定要返回失败消息，如果一个Actor抛出了异常，那么它是不会返回消息的，就会导致等待响应的
  * Future 发生超时，如果一个actor ask了多个其他的Actor，那么此时就不知道是谁超时了；
  */
class AskDemoArticleParser(cacheActorPath: String,
                           httpClientActorPath: String,
                           acticleParserActorPath: String,
                           implicit val timeout: Timeout  //必须设置超时参数
                          ) extends Actor {
  val cacheActor = context.actorSelection(cacheActorPath)
  val httpClientActor = context.actorSelection(httpClientActorPath)
  val articleParserActor = context.actorSelection(acticleParserActorPath)

  import scala.concurrent.ExecutionContext.Implicits.global


  /**
    * Note there are 3 asks so this potentially creates 6 extra objects:
    * - 3 Promises
    * - 3 Extra actors
    * It's a bit simpler than the tell example.
    */
  override def receive: Receive = {
    case ParseArticle(uri) =>
      //这里的 senderRef 非常重要，必须存在，因为后续对sender()的调用是在匿名函数中，
      //很可能在不同的线程中执行，它们有着不同的上下文，在主线程保存这个变量，才能安全的传递到匿名函数的闭包中；
      val senderRef = sender() //sender ref needed for use in callback (see Pipe pattern for better solution)

      val cacheResult = cacheActor ? GetRequest(uri) //ask cache actor

      val result = cacheResult.recoverWith { //if request fails, then ask the articleParseActor
        //异常时的处理
        case _: Exception =>
          val fRawResult = httpClientActor ? uri

          fRawResult flatMap {
            case HttpResponse(rawArticle) =>
              articleParserActor ? ParseHtmlArticle(uri, rawArticle)
            case x =>
              Future.failed(new Exception("unknown response"))
          }
      }

      // take the result and pipe it back to the actor
      // (see Pipe pattern for improved implementation)
      result onComplete {
        case scala.util.Success(x: String) =>
          println("cached result!")
          //返回缓存结果
          senderRef ! x //cached result
        case scala.util.Success(x: ArticleBody) =>
          //设置缓存
          cacheActor ! SetRequest(uri, x.body)
          senderRef ! x
        case scala.util.Failure(t) =>
          senderRef ! akka.actor.Status.Failure(t)
        case x =>
          println("unknown message! " + x)
      }
  }
}
