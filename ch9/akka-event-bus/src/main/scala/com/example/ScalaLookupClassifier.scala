package com.example

import akka.actor.ActorRef
import akka.event.{LookupClassification, EventBus}

class ScalaLookupClassifier extends EventBus with LookupClassification {
    type Event = EventBusMessage  //分类器类型：消息的类型
    type Classifier = String  //事件类型：发布的事件中使用的数据类型
    type Subscriber = ActorRef  //订阅者类型

    //按照topic分类
    override protected def classify(event: Event): Classifier = event.topic

    override protected def publish(event: Event, subscriber: Subscriber): Unit = {
      subscriber ! event.msg
    }

    override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int =
      a.compareTo(b)

    //initial size of the index data structure
    override protected def mapSize: Int = 128
}
