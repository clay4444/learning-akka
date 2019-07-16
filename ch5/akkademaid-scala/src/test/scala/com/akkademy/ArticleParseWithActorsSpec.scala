package com.akkademy

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import com.akkademy.TestHelper.TestCameoActor
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}

/**
  * Router 是用于负载均衡和路由的抽象
  *
  * 路由策略的种类
  * Round Robin 轮流往各个Actor发，
  * Random 随机发
  * ....
  * Consistent Hashing 给router一个key，router生成哈希值，根据哈希值决定发到哪个节点，适用于将特定的数据发送到特定的位置
  * BalancingPool 只可以用于本地Actor，多个Actor共享同一个邮箱，所有Actor一有空闲就从邮箱中取任务，可以确保所有actor都处理繁忙状态；
  */
class ArticleParseWithActorsSpec extends FlatSpec with Matchers {
  val system = ActorSystem()

  /**
    * 第一种用法：由Router来创建一个Actor pool
    * 使用这种方式，所有pool中的Actor都是Router的子节点，所以router可以定义监督策略
    *
    * 第二种用法：把一个 Actor 列表传递给 Router
    * 使用这种方式router不能定义监督策略
    */
  val workerRouter: ActorRef =
    system.actorOf(
      Props.create(classOf[ArticleParseActor]).
        withDispatcher("my-dispatcher").
        withRouter(new RoundRobinPool(8)), "workerRouter") //使用RoundRobin路由策略，8个actor的pool
    //new RoundRobinPool(8).withSupervisorStrategy() 指定pool中actor的监督策略


  //akka document
  val escalator = OneForOneStrategy() {
    case e ⇒ testActor ! e; SupervisorStrategy.Escalate
  }
  val router = system.actorOf(RoundRobinPool(1,supervisorStrategy = escalator).props(routeeProps = Props[TestActor]))


  //反例，不应该在主线程池中执行；
  //val ec:ExecutionContext = system.dispatchers.lookup("dispatcherid")  //获得一个额外定义的 Dispatcher
  val future: Future[Int] = Future {
    1
  }(system.dispatcher)  //system.dispatcher 获得默认的主线程池  / ec

  "ArticleParseActor" should "do work concurrently" in {

    val p = Promise[String]()
    //作为一个线索,传递进actor
    val cameoActor: ActorRef =
      system.actorOf(Props(new TestCameoActor(p)))

    (0 to 2000).foreach(x => {
      workerRouter.tell(
        new ParseArticle(TestHelper.file)
        , cameoActor);
    })

    //等20s,查看结果
    TestHelper.profile(() => Await.ready(p.future, 20 seconds), "Actors")
  }
}
