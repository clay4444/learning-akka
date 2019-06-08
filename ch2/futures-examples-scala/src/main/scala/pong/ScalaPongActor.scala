package pong

import akka.actor.{Actor, Status}

class ScalaPongActor extends Actor {
  override def receive: Receive = {
    case "Ping" => sender() ! "Pong"
    //actor 不会自己主动返回失败信息，而是要我们手动发送错误消息
    case _ => sender() ! Status.Failure(new Exception("unknown message"))
  }
}
