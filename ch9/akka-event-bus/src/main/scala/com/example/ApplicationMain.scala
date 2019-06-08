package com.example

import akka.actor.{Actor, ActorSystem, Props}

object ApplicationMain extends App {
  val system = ActorSystem("MyActorSystem")
  val greetingActor = system.actorOf(Props(new GreetingActor))

  val jLookupBus = new JavaLookupClassifier
  jLookupBus.subscribe(greetingActor, "java-greetings")
  jLookupBus.publish(new EventBusMessage("time", System.currentTimeMillis().toString))
  jLookupBus.publish(new EventBusMessage("java-greetings", "java event bus greeting"))

  val sLookupBus = new ScalaLookupClassifier
  sLookupBus.subscribe(greetingActor, "scala-greetings")  //订阅 scala-greetings 这个话题
  sLookupBus.publish(new EventBusMessage("time", System.currentTimeMillis().toString))

  //只收到下面这个消息
  sLookupBus.publish(new EventBusMessage("scala-greetings", "scala event bus greeting"))
}

class GreetingActor extends Actor {
  override def receive: Receive = {
    case x => println("guess we received a greeting! msg: " + x)
  }
}