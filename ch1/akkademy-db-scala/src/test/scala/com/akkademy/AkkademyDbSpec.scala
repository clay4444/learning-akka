package com.akkademy

import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.util.Timeout
import com.akkademy.messages.{GetRequest, SetRequest}
import org.scalatest.{FunSpecLike, Matchers}

import scala.concurrent.duration._

class AkkademyDbSpec extends FunSpecLike with Matchers {

  //获取 Actor 系统的引用
  implicit val system = ActorSystem()
  implicit val timeout = Timeout(5 seconds)

  describe("akkademyDb") {
    describe("given SetRequest") {
      it("should place key/value into map") {

        //使用 Akka Testkit 来创建一个 TestActorRef，提供同步 API，并且允许我们访问其指向的 Actor
        //由于真正的Actor实例被隐藏了，因此在我们的Actor系统中创建 Actor返回的是一个 ActorRef
        val actorRef = TestActorRef(new AkkademyDb)
        //tell
        actorRef ! SetRequest("key", "value")

        // 因为我们使用的是 TestActorRef，所以只有在 tell 调用请求处理完成后，才会继续执行后面的代码。
        // 但是：通常情况下，tell 是 一个异步操作，调用后会立即返回。

        // 得到指向背后 Actor 实例的引用
        val akkademyDb = actorRef.underlyingActor
        akkademyDb.map.get("key") should equal(Some("value"))
      }

      describe("no given SetRequest") {
        it("should no key/value in map") {
          val actorRef = TestActorRef(new AkkademyDb)
          //tell
          actorRef ! SetRequest("key", "value")

          val akkademyDb = actorRef.underlyingActor
          akkademyDb.map.get("key2") should equal(None)
        }
      }

      describe(" my own test") {
        it("a") {
          val actorRef = TestActorRef(new AkkademyDb)
          //tell
          actorRef ! SetRequest("key", "hehe")
          actorRef ! "UFO"

          actorRef ! GetRequest("key")

          actorRef ! GetRequest("key2")
        }
      }
    }
  }
}

