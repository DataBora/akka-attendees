package actors

import akka.actor.typed.ActorSystem
import akka.actor.typed.{Behavior,ActorRef}
import akka.actor.typed.scaladsl.Behaviors

object Main {
  def main(args: Array[String]): Unit = {

    val replyHandler: Behavior[String] = Behaviors.receiveMessage[String] { message =>
      println(s"Reply received: $message")
      Behaviors.same
    }
   
    val replyActorSystem: ActorSystem[String] = ActorSystem(replyHandler, "replyHandler")
    val greeter: ActorSystem[Greeter.GreetCommand] = ActorSystem(Greeter(), "greeter")

 
    greeter ! Greeter.Greet("Bora", "001", replyActorSystem)
    greeter ! Greeter.Greet("Ficko", "002", replyActorSystem)
    greeter ! Greeter.Greet("Bocko", "003", replyActorSystem)

  
    Thread.sleep(5000) 
    greeter ! Greeter.Goodbye("Bora", "001", replyActorSystem)

    Thread.sleep(3000)
    greeter ! Greeter.Goodbye("Ficko", "002", replyActorSystem)

    Thread.sleep(70000)
    greeter ! Greeter.Goodbye("Bocko", "003", replyActorSystem)

  
    greeter.terminate()
    replyActorSystem.terminate()
  }
}
