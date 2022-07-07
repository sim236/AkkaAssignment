
import Model.Parent.GetData
import akka.actor.{ActorRef, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object Assign extends App{
  val conf=ConfigFactory.load("project.json")
  val actorSystem = ActorSystem("actorSystem")
  val parentActor: ActorRef = actorSystem.actorOf(Props[Parent])
  val filPath:String=conf.getString("filePath")
  parentActor ! GetData(filPath)
}