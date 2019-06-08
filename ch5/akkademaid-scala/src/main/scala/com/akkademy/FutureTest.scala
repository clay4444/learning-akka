package com.akkademy

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * 并行编程的两种方式：Future和Actor
  */

object FutureTest {

  val articleList:List[String] = List("titleA","titleB","titleC")

  val future:List[Future[String]] = articleList.map(article => {
    Future(ArticleParser.apply(article))
  })

  //sequence 把List[Future[String]] 转化为 Future[List[String]]，容易操作
  val articleFuture: Future[List[String]] = Future.sequence(future)
}
