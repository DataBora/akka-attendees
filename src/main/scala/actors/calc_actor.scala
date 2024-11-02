package actors

import akka.actor.typed.{Behavior,ActorRef}
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import scala.io.StdIn.readLine
import java.io.IOException

object Greeter {

  private case class State(currentAttendance: Int = 0, totalIncome: Double = 0.0)

  sealed trait GreetCommand

  final case class Greet(whom: String, replyTo: ActorRef[String]) extends GreetCommand
  final case class Goodbye(whom: String, replyTo: ActorRef[String]) extends GreetCommand

  class GreeterBehavior(context: ActorContext[GreetCommand], state: State) extends AbstractBehavior[GreetCommand](context) {

  private val ticketPrice = 10.0

    override def onMessage(msg: GreetCommand): Behavior[GreetCommand] = {
      msg match {
        
        case Greet(whom, replyTo) =>
          val newState = state.copy(
            currentAttendance = state.currentAttendance + 1,
            totalIncome = state.totalIncome + ticketPrice
          )
          context.log.info(s"Hello $whom! Current attendance: ${newState.currentAttendance}, Total income: ${newState.totalIncome}")
          replyTo ! s"Hello $whom! Current attendance: ${newState.currentAttendance}, Total income: ${newState.totalIncome}"
          new GreeterBehavior(context, newState) 

        case Goodbye(whom, replyTo) =>
     
          val newState = state.copy(
            currentAttendance = math.max(state.currentAttendance - 1, 0)
          )
          context.log.info(s"Goodbye $whom! Current attendance: ${newState.currentAttendance}, Total income: ${newState.totalIncome}")
          replyTo ! s"Goodbye $whom! Current attendance: ${newState.currentAttendance}, Total income: ${newState.totalIncome}"
          new GreeterBehavior(context, newState) // Return new behavior with updated state
      }
    }
  }

  // Factory method to create the actor behavior
  def apply(): Behavior[GreetCommand] = 
    Behaviors.setup(context => new GreeterBehavior(context, State())) 
  
}

// object Calculator {
  
//   sealed trait CalculatorCommand

//   final case class Multiply(num1: Double, num2: Double) extends CalculatorCommand
//   final case class Divide(num1: Double, num2: Double) extends CalculatorCommand
//   final case class Subtract(num1: Double, num2: Double) extends CalculatorCommand
//   final case class Add(num1: Double, num2: Double) extends CalculatorCommand
//   final case class Mod(num1: Double, num2: Double) extends CalculatorCommand

//   class CalculatorBehavior(context: ActorContext[CalculatorCommand]) extends AbstractBehavior[CalculatorCommand](context) {
//     override def onMessage(msg: CalculatorCommand): Behavior[CalculatorCommand] = {
//       msg match {
//         case Multiply(num1, num2) =>
//           context.log.info(s"Multiplication result is: ${num1 * num2}")
//           this 

//         case Divide(num1, num2) =>
//           context.log.info(s"Division result is: ${num1 / num2}")
//           this 

//         case Subtract(num1,num2) =>
//           context.log.info(s"Subtraction result is: ${num1 - num2}")
//           this
        
//         case Add(num1, num2)=>
//           context.log.info(s"Addition result is: ${num1 + num2}")
//           this

//         case Mod(num1, num2) =>
//           context.log.info(s"Mod result is: ${num1 % num2}")
//           this
//       }
//     }
//   }
  
//   def apply(): Behavior[CalculatorCommand] = 
//     Behaviors.setup(context => new CalculatorBehavior(context))
// }

object Main {
  def main(args: Array[String]): Unit = {
    // kreiranje Actor System-a

    //definisanje prostog actora da hendluje reply iz Greeter-a
    val replyHandler = Behaviors.receiveMessage[String]{
      message => println(s"Reply received: $message")
      Behaviors.same
    }


    val replyActorSystem: ActorSystem[String] = ActorSystem(replyHandler, "replyHandler")
    val greeter: ActorSystem[Greeter.GreetCommand] = ActorSystem(Greeter(), "greeter")

    // messages to the actor
    greeter ! Greeter.Greet("Bora", replyActorSystem)
    greeter ! Greeter.Goodbye("Soka", replyActorSystem)
    greeter ! Greeter.Greet("Ficko", replyActorSystem)
    greeter ! Greeter.Greet("Bocko", replyActorSystem)


  
    greeter.terminate()
  }
}


