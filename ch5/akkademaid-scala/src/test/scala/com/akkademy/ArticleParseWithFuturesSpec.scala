package com.akkademy

import org.scalatest.{Matchers, FlatSpec}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ArticleParseWithFuturesSpec extends FlatSpec with Matchers {
  import scala.concurrent.ExecutionContext.Implicits.global

  //主线程池（默认Dispatcher）中执行Future，会阻塞主线程执行，不建议这样做
  "ArticleParser" should "do work concurrently with futures" in {
    val futures = (1 to 2000).map(x => {
      //简单明了
      Future(ArticleParser.apply(TestHelper.file))
    })

    TestHelper.profile(() => Await.ready(Future.sequence(futures), 30 seconds), "Futures")
  }
}
