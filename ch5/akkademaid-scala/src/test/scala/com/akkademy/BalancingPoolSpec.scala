package com.akkademy

import akka.actor.{Props, ActorRef, ActorSystem}
import akka.routing.{BalancingPool, RoundRobinGroup}
import com.akkademy.TestHelper.TestCameoActor
import org.scalatest.{Matchers, FlatSpec}
import scala.concurrent.duration._

import scala.concurrent.{Await, Promise}

/**
  * 这章的主要目的是纵向扩展
  *
  * 由于 Actor 是本地的，所以我们有比前面使用的 RoundRobinGroup 更好的选择。
  * 对于本地 Actor 来说，可以使用 BalancingPool 来创建一个 Router，本章早些时候曾经简要地介绍过。
  * 使用 BalancingPool 时，Pool 中的所有 Actor 会共享同一个邮箱，
  * 然后通过高效的工作窃取机制将任务重新分配给任何空闲的 Actor。由于共享同一个邮箱，因此使用 BalancingPool 有助于确保在有工作的时候降低 Actor 的空闲率。
  * 由于 Router 并没有像 ForkJoinPool那样重新分配工作，只是由空闲的Actor从共享邮箱中抽取下一条消息处理，
  * 所以从技术上来讲并不是工作窃取。最后的效果是一样的：不可能发生某个 Actor 的工 作队列中有多条消息而另一个 Actor 处于空闲状态的情况。
  * 因为我们能够确保更多的 Actor 处于工作状态，所以这种做法通常都能获得比其他负载均衡策略更高的资源利用率。
  */
class BalancingPoolSpec extends FlatSpec with Matchers {
  val system = ActorSystem()

  "BalancingPool" should "do work concurrently" in {
    val p = Promise[String]()

    //使用BalancingPool 创建router，
    val workerRouter = system.actorOf(BalancingPool(8).props(Props(classOf[ArticleParseActor])),
      "balancing-pool-router")

    //sender actor ref
    val cameoActor: ActorRef =
      system.actorOf(Props(new TestCameoActor(p)))

    (0 to 2000).foreach(x => {
      workerRouter.tell(
        new ParseArticle(TestHelper.file)
        , cameoActor);
    })

    TestHelper.profile(() => Await.ready(p.future, 20 seconds), "ActorsInBalacingPool")
  }
}
