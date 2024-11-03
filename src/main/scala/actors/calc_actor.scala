package actors

import akka.actor.typed.{Behavior,ActorRef}
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import scala.io.StdIn.readLine
import java.io.IOException
import com.github.nscala_time.time.Imports._
import java.io.{File, PrintWriter}
import scala.io.Source
import scala.util.Try

object Greeter {

  val attendees = List(("Bora","001") , ("Soka", "002"), ("Ficko","003"),("Oblo","004") ,("Kocko","005"))

  // private case class State(attendee: String = "",
  //                         currentAttendance: Int = 0, 
  //                         totalIncome: Double = 0.0, 
  //                         startTime: DateTime = DateTime.now(), 
  //                         endTime: DateTime = DateTime.now(), 
  //                         totalTimeSpent: Double= 0.0)
  private case class AttendeeTime(startTime: DateTime, endTime: Option[DateTime] = None)

  private case class State(
    currentAttendance: Int = 0,
    totalIncome: Double = 0.0
  )

  sealed trait GreetCommand

  final case class Greet(whom: String, id: String, replyTo: ActorRef[String]) extends GreetCommand
  final case class Goodbye(whom: String, id: String, replyTo: ActorRef[String]) extends GreetCommand

  class GreeterBehavior(context: ActorContext[GreetCommand], state: State) extends AbstractBehavior[GreetCommand](context) {

  private val ticketPrice = 10.0
  private val filePath = "attendance_log.txt"

    override def onMessage(msg: GreetCommand): Behavior[GreetCommand] = {
      msg match {
        
        case Greet(whom, id, replyTo) =>
            val newAttendance = state.currentAttendance + 1
            val newIncome = state.totalIncome + ticketPrice
            val startTime = DateTime.now()

          writeToFile(s"$id, $whom, ${startTime.toString()}\n")
          
          context.log.info(s"Hello $whom (id: $id)! Current attendance: $newAttendance, Total income: $newIncome")
          replyTo ! s"Hello $whom (id: $id)! Current attendance: $newAttendance, Total income: $newIncome, Arrival time: $startTime"
          new GreeterBehavior(context, state.copy(currentAttendance = newAttendance, totalIncome = newIncome))

        case Goodbye(whom, id, replyTo) =>

          val arrivalTime = try {
            val lines = Source.fromFile("attendance_log.txt").getLines()
            
            val arrivalEntry = lines.find { line =>
              val Array(entryId, entryName, _) = line.split(",", 3) 
              entryId.trim == id && entryName.trim == whom
            }
          
            arrivalEntry.map { entry =>
              val Array(_, _, arrivalTimeString) = entry.split(",", 3)
              DateTime.parse(arrivalTimeString.trim)
            }
          } catch {
            case e: Exception =>
              context.log.error("Error reading arrival time for {}: {}", whom, e.getMessage)
              None
          }

          val endTime = DateTime.now()
          val timeSpent = arrivalTime.map { start =>
            val durationInSeconds = new Duration(start, endTime).getStandardSeconds
            durationInSeconds / 60.0 
          }.getOrElse(0.0)

          // Log and respond
          val newState = state.copy(
            currentAttendance = math.max(state.currentAttendance - 1, 0)
          )
          context.log.info(f"Goodbye $whom (id: $id)! Time spent: $timeSpent%.2f minutes.")
          replyTo ! f"Goodbye $whom (id: $id)! Time spent: $timeSpent%.2f minutes."

          new GreeterBehavior(context, newState)

      }
    }
    
  private def writeToFile(data: String): Unit ={
    val writer = new PrintWriter(new java.io.FileOutputStream(new File(filePath), true))
    try{
      writer.write(data)
    } finally {
      writer.close()
    }

  }

  }



  // Factory method to create the actor behavior
  def apply(): Behavior[GreetCommand] = 
    Behaviors.setup(context => new GreeterBehavior(context, State())) 
  
}

object Main {
  def main(args: Array[String]): Unit = {
    // Define a simple actor to handle replies from Greeter
    val replyHandler: Behavior[String] = Behaviors.receiveMessage[String] { message =>
      println(s"Reply received: $message")
      Behaviors.same
    }

    // Create Actor Systems for reply handler and Greeter
    val replyActorSystem: ActorSystem[String] = ActorSystem(replyHandler, "replyHandler")
    val greeter: ActorSystem[Greeter.GreetCommand] = ActorSystem(Greeter(), "greeter")

    // Send Greet messages to Greeter for each attendee
    greeter ! Greeter.Greet("Bora", "001", replyActorSystem)
    greeter ! Greeter.Greet("Ficko", "002", replyActorSystem)
    greeter ! Greeter.Greet("Bocko", "003", replyActorSystem)

    // Wait to simulate time spent at the event, then send Goodbye messages
    Thread.sleep(30000) // Simulate time passing for the attendees

    greeter ! Greeter.Goodbye("Bora", "001", replyActorSystem)
    greeter ! Greeter.Goodbye("Ficko", "002", replyActorSystem)
    greeter ! Greeter.Goodbye("Bocko", "003", replyActorSystem)

    // Terminate the actor system after some delay to allow processing of all messages
    Thread.sleep(5000)
    greeter.terminate()
    replyActorSystem.terminate()
  }
}

