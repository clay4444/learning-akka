package com.akkademy

import akka.actor.Actor
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{UnreachableMember, MemberEvent}
import akka.event.Logging

/**
  * 订阅集群事件，将集群环形拓扑中发生的所有变化都记录到日志
  */
class ClusterController extends Actor {

  //创建logger，
  val log = Logging(context.system, this)

  //创建集群对象，/ 获取指向 Cluster 对象的引用
  val cluster = Cluster(context.system)

  override def preStart() {
    //订阅两个事件，
    // MemberEvent：该事件会在集群状态发生变化时发出通知
    // UnreachableMember：该事件会在某个节点被标记为不可用时发出通知。
    cluster.subscribe(self, classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop() {
    //取消订阅，防止内存泄露
    cluster.unsubscribe(self)
  }

  //直接打印事件
  override def receive = {
    case x: MemberEvent => log.info("MemberEvent: {}", x)
    case x: UnreachableMember => log.info("UnreachableMember {}: ", x)
  }
}

