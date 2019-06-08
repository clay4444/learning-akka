package com.akkademy

import akka.actor.{ActorSystem, Props}
import akka.cluster.client.ClusterClientReceptionist
import akka.routing.BalancingPool

/**
  * Akka cluster
  * 两个基本功能：失败发现(心跳机制) 和最终一致性(gossip协议)
  *
  *
  */
object Main{

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Akkademy")

    //此actor 是做集群事件监听的；
    val clusterController = system.actorOf(Props[ClusterController], "clusterController")

    //添加 akka cluster 依赖后，直接启动actor，就自动启动集群服务了；每台服务器上创建一个ActorPool
    //路径是 /user/workers
    val workers = system.actorOf(BalancingPool(5).props(Props[ArticleParseActor]), "workers")

    //在 ClusterReceptionist(用于和客户端通信的) 中注册 worker Actor。
    ClusterClientReceptionist(system).registerService(workers)
  }
}

