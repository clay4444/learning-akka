package com.akkademy

import akka.actor.{Actor, Stash}
import com.akkademy.messages.{Connected, Request}

/**
 * Use mailbox with stash-capacity
 * or build some sort of timeout to avoid memory leak.
 */

/**
  * 热交换
  * 有可能造成stash泄露的问题，一直链接不上服务端，导致stash的消息太多；
  */
class HotswapClientActor(address: String) extends Actor with Stash {
  private val remoteDb = context.system.actorSelection(address)


  //remoteDb离线时如何处理
  override def receive = {
    case x: Request =>  //can't handle until we know remote system is responding
      remoteDb ! new Connected //see if the remote actor is up，发送一个链接请求，如果remoteDb活跃，会返回一个Connected类型消息
      stash() //stash message for later，存储起来，因为还没创建链接
    case _: Connected => // Okay to start processing messages.
      //收到了响应,就说明连接上了
      unstashAll()
      context.become(online)//将 receive 块中定义的行为修改为一个新的PartialFunction
  }

  //remoteDb在线时如何处理
  def online: Receive = {
    //收到下线指令
    case x: Disconnected =>
      context.unbecome()
    case x: Request =>
      //本地转发
      remoteDb forward x //forward is used to preserve sender
  }
}

/**
 * Disconnect msg is unimplemented in this example.
 * Because we're not dealing w/ sockets directly,
 * we could instead implement an occasional ping/heartbeat that restarts
 * this Actor if the remote actor isn't responding.
 * After restarting, the actor will revert to the
 * default state and stash messages
 */

class Disconnected