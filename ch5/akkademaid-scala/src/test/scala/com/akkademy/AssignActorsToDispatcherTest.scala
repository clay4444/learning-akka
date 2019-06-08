package com.akkademy

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.routing._
import com.akkademy.TestHelper.TestCameoActor
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}

class AssignActorsToDispatcherTest extends FlatSpec with Matchers {
  val system = ActorSystem()

  "ActorsAssignedToDispatcher" should "do work concurrently" in {

    val p = Promise[String]()

    //如何将actor分配给自定义的 dispatcher：直接调用 withDispatcher，指定 dispatcher-id
    // 创建一系列actor，并把这些actor分配给我们自己定义的dispatcher，
    val actors: IndexedSeq[ActorRef] = (0 to 7).map(x => {
      system.actorOf(Props(classOf[ArticleParseActor]).
        withDispatcher("article-parsing-dispatcher"))
    })

    //然后再创建一个使用这些actor的Router，这样就可以轻松的使用这些actor执行并行操作了；
    //而且这些actor都在 article-parsing-dispatcher 这个dispatcher 中执行，不会阻塞默认dispatcher；
    val workerRouter = system.actorOf(RoundRobinGroup(
      actors.map(x => x.path.toStringWithoutAddress).toList).props(),
      "workerRouter")

    val cameoActor: ActorRef =
      system.actorOf(Props(new TestCameoActor(p)))

    (0 to 2000).foreach(x => {
      workerRouter.tell(
        new ParseArticle(TestHelper.file)
        , cameoActor);
    })

    TestHelper.profile(() => Await.ready(p.future, 20 seconds), "ActorsAssignedToDispatcher")
  }

}
