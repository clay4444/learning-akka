package pong;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Status;
import akka.japi.pf.ReceiveBuilder;
import scala.PartialFunction;

public class JavaPongActor extends AbstractActor {
    public PartialFunction receive() {

        //。receive 方法返回的类型是 PartialFunction，这个类型来自 Scala API。在 Java 中，
        // 并没有提供任何原生方法来构造 Scala 的 PartialFunction（并不对所有可能输入进行处理的函数），
        // 因此 Akka 为我们提供了一个抽象的构造方法类 ReceiveBuilder，用于生成 PartialFunction 作为返回值。
        return ReceiveBuilder.
                matchEquals("Ping", s ->
                        //第一个参数是我们想要发送至对方信箱的消息。 第二个参数则是希望对方 Actor 看到的发送者。
                        sender().tell("Pong", ActorRef.noSender())).
                matchAny(x ->
                        sender().tell(
                                //向发送方报告错误信息
                                new Status.Failure(new Exception("unknown message")), self()
                        )).
                build();
    }
}
