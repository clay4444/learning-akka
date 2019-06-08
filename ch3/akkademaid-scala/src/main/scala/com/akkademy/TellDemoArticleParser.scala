package com.akkademy

import java.util.concurrent.TimeoutException

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorRef, Props}
import akka.util.Timeout
import com.akkademy.messages.{GetRequest, SetRequest}

/**
  * tell: fire-and-forget
  * 向Actor发送一条消息，所有发送至sender()的响应都会返回给消息的Actor，
  *
  * 不需要提供超时参数，
  *
  * 通过在Actor中存储一些状态，可以解决之前ask模式访问多个actor无法知道是哪个actor超时的问题，
  *
  * 不需要创建临时的actor，减小了额外的开销，
  *
  * 如果在Actor的系统的外部调用tell，而不使用ask，那么没有明显的方法用来接收并处理响应，此时的解决方案
  * 新建一个临时的actor，用于处理消息的响应（ask其实就是用这种方式实现的）
  *
  */
class TellDemoArticleParser(cacheActorPath: String,
                            httpClientActorPath: String,
                            articleParserActorPath: String,
                            implicit val timeout: Timeout
                           ) extends Actor {
  val cacheActor = context.actorSelection(cacheActorPath)
  val httpClientActor = context.actorSelection(httpClientActorPath)
  val articleParserActor = context.actorSelection(articleParserActorPath)

  implicit val ec = context.dispatcher

  /**
    * While this example is a bit harder to understand than the ask demo,
    * for extremely performance critical applications, this has an advantage over ask.
    * The creation of 5 objects are saved - only one extra actor is created.
    * Functionally it's similar.
    * It will make the request to the HTTP actor w/o waiting for the cache response though (can be solved).
    *
    * @return
    */

  override def receive: Receive = {
    case ParseArticle(uri) =>

      //构建额外的Actor，用于处理响应；
      val extraActor = buildExtraActor(sender(), uri)

      cacheActor.tell(GetRequest(uri), extraActor)  //extraActor 作为sender
      //类似于设置自己的返回收件箱
      //第一步,发送url
      httpClientActor.tell(uri, extraActor)

      context.system.scheduler.scheduleOnce(timeout.duration, extraActor, "timeout")
  }

  /**
    * The extra actor will collect responses from the assorted actors it interacts with.
    * The cache actor reply, the http actor reply, and the article parser reply are all handled.
    * Then the actor will shut itself down once the work is complete.
    * A great use case for the use of tell here (aka extra pattern) is aggregating data from several sources.
    */
  private def buildExtraActor(senderRef: ActorRef, uri: String): ActorRef = {
    return context.actorOf(Props(new Actor {
      override def receive = {

        //在ask的例子中：有三个不同的超时会导致错误，而且我们不知道到底是哪个超时了
        //而在这里，只有一个我们可以控制的超时：要么发生超时，要么运行成功；
        case "timeout" => //if we get timeout, then fail
          senderRef ! Failure(new TimeoutException("timeout!"))
          context.stop(self)
        //第二步,收到body,解析body
        case HttpResponse(body) => //If we get the http response first, we pass it to be parsed.
          //传递给解析器
          articleParserActor ! ParseHtmlArticle(uri, body)
        //TODO 接收到缓存??
        case body: String => //If we get the cache response first, then we handle it and shut down.
          //The cache response will come back before the HTTP response so we never parse in this case.
          senderRef ! body
          context.stop(self)
        //第四步,收到解析后的结果
        case ArticleBody(uri, body) => //If we get the parsed article back, then we've just parsed it
          //设置缓存
          cacheActor ! SetRequest(uri, body) //Cache it as we just parsed it
          //发送body
          senderRef ! body
          context.stop(self)

        case t => //We can get a cache miss
          println("ignoring msg: " + t.getClass)
      }
    }))
  }

}
