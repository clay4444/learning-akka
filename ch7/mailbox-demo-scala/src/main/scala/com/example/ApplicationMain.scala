package com.example

import akka.actor.ActorSystem
import akka.pattern.CircuitBreaker
import akka.pattern.ask
import akka.util.Timeout
import com.example.PingActor.PingMessage
import scala.concurrent.duration._

/**
 * Modified version of the new project from typesafe demonstrating mailbox config
 * and circuitbreaker
  *
  * 这个例子写的有点太扯了把。。。
 */
object ApplicationMain extends App {
  val system = ActorSystem("MyActorSystem")
  implicit val ec = system.dispatcher//used by circuit breaker
  implicit val timeout = Timeout(2 seconds)//used by ask

  //创建使用 akka.actor.boundedmailbox 的邮箱
  val pingActorWithMailbox = system.actorOf(PingActor.props.withMailbox("akka.actor.boundedmailbox"), "pingActor")

  //先发送一条初始化的消息，让其开始ping-pong 自己发送消息
  pingActorWithMailbox ! PingActor.Initialize

  val pongActor = system.actorOf(PongActor.props, "pongactor2")

  //熔断器
  val breaker =
    new CircuitBreaker(system.scheduler,
      maxFailures = 1, //熔断器熔断之前发生错误的最大次数（无论是超时还是 Future 返回失败）；
      callTimeout = 1 seconds, //调用超时（延时超过多长时间就熔断）；
      resetTimeout = 1 seconds).//重置超时（等待多久以后将状态改为“半开”，并尝试发送一个请求）。
      onOpen(println("circuit breaker opened!")). //注册日志事件
      onClose(println("circuit breaker closed!")).
      onHalfOpen(println("circuit breaker half-open"))


  val future1 = breaker.withCircuitBreaker{
    pongActor ? PingMessage("ping")
  }
  val future2 = breaker.withCircuitBreaker{
    pongActor ? PingMessage("ping")
  }

  future1.map{x => println("response1 : " + x)}
  future2.map{x => println("response2 : " + x)}//断路器将在这里半开

  //查看断路器如何响应
  Thread.sleep(1000)
  system.awaitTermination()
}