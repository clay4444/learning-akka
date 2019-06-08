package com.akkademy.clientactor

import akka.actor.FSM
import com.akkademy.clientactor.StateContainerTypes.RequestQueue
import com.akkademy.messages
import com.akkademy.messages._


//============未实现的功能==========
//1.根据心跳判断remotedb是否能提供服务
//2.两次心跳没有响应，重启此actor
//2.实现监督策略，在Actor每次重启时将相关信息输出到日志，
//==========================
//有限自动机（主要包含 状态 和 容器：存储消息的地方）

//sealed: 不能在类定义的文件之外定义任何新的子类,case如果少处理了,编译时会提示,厉害!
sealed trait State

/**
  * 定义状态
  */
//离线，队列中没有任何消息
case object Disconnected extends State

//在线，队列中没有任何消息
case object Connected extends State

//在线，队列中包含消息
case object ConnectedAndPending extends State

case object Flush

case object ConnectedMsg

object StateContainerTypes {
  //容器：保存一个请求列表作为状态容器，接收到Flush消息的时候处理这个列表中的请求；
  type RequestQueue = List[Request]
}

class FSMClientActor(address: String) extends FSM[State, RequestQueue] {
  private val remoteDb = context.system.actorSelection(address)

  //定义起始状态
  startWith(Disconnected, List.empty[Request])

  //定义不同状态对不同消息的的响应方法，以及如何根据接收到的消息切换状态；最简单的方式是用when，
  //必须要返回对状态的描述，要么是停留在FSM中的某个状态，要么是转移到另外一个状态
  when(Disconnected) {
    case Event(_: messages.Connected, container: RequestQueue) => //If we get back a ping from db, change state
      //默认就会转在线
      if (container.headOption.isEmpty)
        //转为在线
        goto(Connected)
      else
        goto(ConnectedAndPending)
    case Event(x: Request, container: RequestQueue) =>
      remoteDb ! new messages.Connected //Ping remote db to see if we're connected if not yet marked online.
      stay using (container :+ x) //Stash the msg
    case x =>
      println("uhh didn't quite get that: " + x)
      stay()
  }

  when(Connected) {
    case Event(x: Request, container: RequestQueue) =>
      //1.转在线+有内容状态
      //2.添加到队列中
      goto(ConnectedAndPending) using (container :+ x)
  }

  when(ConnectedAndPending) {
    case Event(Flush, container) =>
      //接受Flush后,会发送 List请求,包括了get和set
      remoteDb ! container;
      //发送完内容,再次转为离线,并且将容器内容清空
      goto(Connected) using Nil
    case Event(x: Request, container: RequestQueue) =>
      //保持ConnectedAndPending状态
      stay using (container :+ x)
  }

  //开始启动
  initialize()
}
