package com.akkademy

import akka.actor.{Actor, ActorSystem}

import scala.concurrent.Future

class ArticleParseActor extends Actor{
  override def receive: Receive = {
    case ParseArticle(htmlString) =>
      val body: String = ArticleParser(htmlString)
      sender() ! body
  }

  /**
    * 这是个反例
    */
  val system = ActorSystem()

  implicit val ec = system.dispatcher //获得默认的dispatcher(相当于主线程池)
  val future = Future(() => println("run in ec"))  //不建议这么做，因为有可能会阻塞主线程池，导致整个程序阻塞
}

object ArticleParser {
  def apply(html: String) : String =
    de.l3s.boilerpipe.extractors.ArticleExtractor.INSTANCE.getText(html)
}