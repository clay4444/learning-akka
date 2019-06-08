package com.akkademy

import akka.actor._
import akka.event.Logging
import com.akkademy.messages.{KeyNotFoundException, GetRequest, SetRequest}
import scala.collection.mutable.HashMap

/**
  * 监督策略：
  * 1.继续 resume
  * 2.停止 stop
  * 3.重启 restart
  * 4.向上反映 escalate
  *
  * 默认的监督策略：
  * 运行过程中抛出异常：restart
  * 运行过程中发生错误：escalate
  * 初始化时发生异常：stop
  *
  * 生命周期
  * prestart() 开始之前，构造函数之后，也就是重启完之后，已经创建完了新的Actor
  * postStop() stop之后，也就是重启之前，还没创建新的Actor，
  * preRestart() 重启之前，默认调用postStop()
  * postRestart() 重启之后，默认调用prestart()
  */
class AkkademyDb extends Actor {
  val map = new HashMap[String, Object]
  val log = Logging(context.system, this)

  override def receive = {
    case x: messages.Connected =>
      //发来啥,返回啥
      sender() ! x
    case x: List[_] =>
      x.foreach{
        case SetRequest(key, value, senderRef) =>
          handleSetRequest(key, value, senderRef)
        case GetRequest(key, senderRef) =>
          handleGetRequest(key, senderRef)
      }
    case SetRequest(key, value, senderRef) =>
      handleSetRequest(key, value, senderRef)
    case GetRequest(key, senderRef) =>
      handleGetRequest(key, senderRef)
    case o =>
      log.info("unknown message")
      sender() ! Status.Failure(new ClassNotFoundException)
  }

  def handleSetRequest(key: String, value: Object, senderRef: ActorRef): Unit = {
    log.info("received SetRequest - key: {} value: {}", key, value)
    map.put(key, value)
    senderRef ! Status.Success
  }

  def handleGetRequest(key: String, senderRef: ActorRef): Unit = {
    log.info("received GetRequest - key: {}", key)
    val response: Option[Object] = map.get(key)
    response match {
      case Some(x) => senderRef ! x
      case None => senderRef ! Status.Failure(new KeyNotFoundException(key))
    }
  }
}

object Main extends App {
  val system = ActorSystem("akkademy")
  val helloActor = system.actorOf(Props[AkkademyDb], name = "akkademy-db")
}
