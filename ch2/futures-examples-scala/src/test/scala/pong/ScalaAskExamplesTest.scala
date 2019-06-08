package pong

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import org.scalatest.{FunSpecLike, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Promise, Await, Future}

class ScalaAskExamplesTest extends FunSpecLike with Matchers {
  val system = ActorSystem()
  implicit val timeout = Timeout(5 seconds) //隐式参数，
  val pongActor = system.actorOf(Props(classOf[ScalaPongActor]))

  describe("Pong actor") {
    it("should respond with Pong") {
      val future = pongActor ? "Ping"  //? 就是发送消息，注意必须要引入 akka.pattern.ask
      val result = Await.result(future.mapTo[String], 1 second)
      assert(result == "Pong")
    }
    it("should fail on unknown message") {
      val future = pongActor ? "unknown"
      intercept[Exception]{
        Await.result(future.mapTo[String], 1 second)
      }
    }
  }

  describe("FutureExamples"){
    import scala.concurrent.ExecutionContext.Implicits.global  //运行在多线程上的异步操作，因此需要引入隐式的 ExecutionContext。
    it("should print to console"){
      askPong("Ping").onSuccess({
        case x: String => println("replied with: " + x)
      })
      Thread.sleep(100)
    }

    //结果转换
    it("should transform"){
      val f: Future[Char] = askPong("Ping").map(x => x.charAt(0))
      val c = Await.result(f, 1 second)
      c should equal('P')
    }

    //链式异步操作
    /**
     * Sends "Ping". Gets back "Pong"
     * Sends "Ping" again when it gets "Pong"
     */
    it("should transform async"){
      val f: Future[String] = askPong("Ping").flatMap(x => {
        assert(x == "Pong")
        askPong("Ping")
      })
      val c = Await.result(f, 1 second)
      c should equal("Pong")
    }

    //doesn't actually test anything - demonstrates an effect. next test shows assertion.

    //处理失败的情况
    it("should effect on failure"){
      askPong("causeError").onFailure{
        case e: Exception => println("Got exception")
      }
    }

    /**
     * similar example to previous test, but w/ assertion
     */

    it("should effect on failure (with assertion)"){
      val res = Promise()
      askPong("causeError").onFailure{
        case e: Exception =>
          res.failure(new Exception("failed!"))
      }

      intercept[Exception]{
        Await.result(res.future, 1 second)
      }
    }

    //从失败中恢复，返回一个默认值
    it("should recover on failure"){
      val f = askPong("causeError").recover({
        case t: Exception => "default"
      })

      val result = Await.result(f, 1 second)
      result should equal("default")
    }

    //异步的从失败中恢复，类似于专门用于错误情况的flatmap
    it("should recover on failure async"){
      val f = askPong("causeError").recoverWith({
        case t: Exception => askPong("Ping")
      })

      val result = Await.result(f, 1 second)
      result should equal("Pong")
    }


    /**
      * 链式操作
      * 在第一个操作完成时异步地发起另一个调用。接着，在发生错误时，我们使用一个 String 值来 恢复错误，保证 Future 能够返回成功。
      * 在执行操作链中的任一操作时发生的错误都可以作为链的末端发生的错误来处理。
      * 这样就形成了一个很有效的操作管道，无论是哪个操作导致了错误，都可以在最后来处理异常。
       */
    it("should chain together multiple operations"){
      val f = askPong("Ping").flatMap(x => askPong("Ping" + x)).recover({
        case _: Exception => "There was an error"
      })

      val result = Await.result(f, 1 second)
      result should equal("There was an error")
    }

    /**
      * 组合Future
      * 在 Scala 中，也可以使用 for 推导式将多个 Future 组合起来。我们能够像处理任何其他集合一样，解析出两个 Future 的结果并对它们进行处理
      * 要注意的是，这只不过是 flatMap 的一个“语法糖”
      *
      * 这个例子展示了一种处理多个不同类型 Future 的机制。通过这种方法，
      * 可以并行地执行任务，同时处理多个请求，更快地将响应返回给用户。这种对并行的使用可以帮助我们提高系统的响应速度。
      */
    it("should be handled with for comprehension"){
      val f1 = Future{4}
      val f2 = Future{5}

      val futureAddition =
        for{
          res1 <- f1
          res2 <- f2
        } yield res1 + res2
      val additionResult = Await.result(futureAddition, 1 second)
      assert(additionResult == 9)
    }

    //处理Future列表
    it("should handle a list of futures"){
      //防止失败
      val listOfFutures: List[Future[String]] = List("Pong", "Pong", "failure").map(x => askPong(x)).map(_.recover({case t: Exception => ""}))
      // 把 List[Future]转换成 Future[List]。
      val futureOfList: Future[List[String]] = Future.sequence(listOfFutures)
    }

  }

  def askPong(message: String): Future[String] = (pongActor ? message).mapTo[String]
}
