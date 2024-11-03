package actors

import akka.actor.typed.{Behavior,ActorRef}
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import scala.io.StdIn.readLine
import java.io.IOException
import com.github.nscala_time.time.Imports._
import java.io.{File, PrintWriter}
import scala.io.Source
import scala.util.Try

object Greeter {

  // val attendees = List(("Bora","001") , ("Soka", "002"), ("Ficko","003"),("Oblo","004") ,("Kocko","005"))

  private case class AttendeeTime(startTime: DateTime, endTime: Option[DateTime] = None)

  private case class State(
    currentAttendance: Int = 0,
    totalIncome: Double = 0.0
  )

  sealed trait GreetCommand
  case class Greet(whom: String, id: String, replyTo: ActorRef[String]) extends GreetCommand
  case class Goodbye(whom: String, id: String, replyTo: ActorRef[String]) extends GreetCommand

  class GreeterBehavior(context: ActorContext[GreetCommand], state: State) extends AbstractBehavior[GreetCommand](context) {

  private val ticketPrice = 10.0
  private val filePath = "attendance_log.txt"

    override def onMessage(msg: GreetCommand): Behavior[GreetCommand] = {
      msg match {
        
        case Greet(whom, id, replyTo) =>
            val newAttendance = state.currentAttendance + 1
            val newIncome = state.totalIncome + ticketPrice
            val startTime = DateTime.now()

          writeToFile(s"$id, $whom, arrival, ${startTime.toString()}\n")
          
          context.log.info(s"Hello $whom (id: $id)! Current attendance: $newAttendance, Total income: $newIncome")
          replyTo ! s"Hello $whom (id: $id)! Current attendance: $newAttendance, Total income: $newIncome, Arrival time: $startTime"
          new GreeterBehavior(context, state.copy(currentAttendance = newAttendance, totalIncome = newIncome))

        case Goodbye(whom, id, replyTo) =>
            val endTime = DateTime.now()

          
            val arrivalTime = try {
              val lines = Source.fromFile(filePath).getLines().toList
               val arrivalEntries = lines
                 .filter(line => line.startsWith(id + ", " + whom + ", arrival"))
                 .map { line =>
                val Array(_, _, _, arrivalTimeString) = line.split(",", 4)
                DateTime.parse(arrivalTimeString.trim)
              }

              // Get the most recent arrival time
            arrivalEntries.lastOption
              } catch {
                case e: Exception =>
                  context.log.error("Error reading arrival time for {}: {}", whom, e.getMessage)
                  None
              }
            
             writeToFile(s"$id, $whom, departure, ${endTime.toString()}\n")

            // Calculate the time spent
            val timeSpent = (arrivalTime, endTime) match {
              case (Some(start), _) =>
                val durationInSeconds = new Duration(start, endTime).getStandardSeconds
                durationInSeconds / 60.0
              case _ => 0.0
            }

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
  
  def apply(): Behavior[GreetCommand] = 
    Behaviors.setup(context => new GreeterBehavior(context, State())) 
  
}


