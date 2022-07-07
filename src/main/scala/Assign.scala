
import Model.Child.{ClassifyDataChild, Mile1}
import Model.Parent.{ClassifiedObjectData, ClassifyData, ErrorHandling, GetData}
import Model.PurchaseDetail
import Util.fileUtil
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.typesafe.config.ConfigFactory
import org.apache.poi.ss.usermodel.{CellType, DateUtil, WorkbookFactory}

import java.io.File
import scala.collection.mutable.ListBuffer

object Assign extends App{
  val conf=ConfigFactory.load("project.json")
  val actorSystem = ActorSystem("actorSystem")

  class Parent extends Actor {
    val orderList: ListBuffer[PurchaseDetail] = new ListBuffer[PurchaseDetail]()

    override def receive: Receive = {
      case GetData(filePath: String) => {
        val childRef: ActorRef = actorSystem.actorOf(Props[Child])
        childRef ! GetData(filePath)
      }
      case ClassifyData(fileContent: ListBuffer[ListBuffer[String]]) => {
        val childPool = actorSystem.actorOf(RoundRobinPool(10).props(Props[Child]), "Child")
        fileContent.foreach({ data => childPool ! ClassifyDataChild(data) })
      }
      case ErrorHandling(data:Any,error:Exception)=>{
        fileUtil(List(data,error),conf.getString("errorHandlingFileName"))
      }
    }
  }

  class Child extends Actor {

    implicit val materializer= ActorMaterializer()
    override def receive: Receive = {

      //val orderList:ListBuffer[PurchaseDetail]=new ListBuffer[PurchaseDetail]()
      case GetData(filePath: String) => {
        try{

          val fileContent = ListBuffer[ListBuffer[String]]()
          val f = new File(filePath)

          val workbook = WorkbookFactory.create(f)
          val mySheet = workbook.getSheetAt(0)
          val rowIterator = mySheet.iterator()
          rowIterator.next()
          while (rowIterator.hasNext) {
            val rowContent = ListBuffer[String]()
            val row = rowIterator.next()
            val cellIterator = row.cellIterator()

            while (cellIterator.hasNext) {
              val cell = cellIterator.next()

              cell.getCellType match {
                case CellType.STRING => {
                  rowContent += cell.getStringCellValue
                }
                case CellType.NUMERIC => {
                  if (DateUtil.isCellDateFormatted(cell)) {
                    rowContent += cell.getDateCellValue.toString
                  }else {
                    rowContent += cell.getNumericCellValue.toString
                  }
                }
                case CellType.BLANK => {
                  rowContent += cell.getCellType.toString
                }
              }
            }
            fileContent += rowContent
          }
          sender() ! ClassifyData(fileContent)
        }
        catch {
          case e:Exception=>{
            sender() ! ErrorHandling(filePath,e)
          }
        }
      }
      case ClassifyDataChild(data: ListBuffer[String]) => {
        try{
          val order:PurchaseDetail= PurchaseDetail(orderDate = data.head,
            shipDate = data(1),
            shipMode = data(2),
            customerName = data(3),
            segment = data(4),
            country = data(5),
            city = data(6),
            state = data(7),
            region = data(8),
            category = data(9),
            subRegion = data(10),
            name= data(11),
            Sales= data(12).toFloat,
            quantity= data(13).toFloat,
            discount= data(14).toFloat,
            profit= data(15).toFloat,
          )
          context.self ! Mile1(order)
        }
        catch{
          case e:Exception=> {
            sender() ! ErrorHandling(data, e)
          }
        }

      }
      case Mile1(data:PurchaseDetail)=>{
        val source=Source.single(data)
        val flow1=Flow[PurchaseDetail].filter(_.category==conf.getString("category"))
        val flow2=Flow[PurchaseDetail].filter(_.orderDate.split(" ").last==conf.getString("year"))

        val sink=Sink.foreach(println)
        source.async.via(flow1).via(flow2).to(sink).run
      }
    }
  }
  val parentActor: ActorRef = actorSystem.actorOf(Props[Parent])
  val filPath:String=conf.getString("filePath")
  parentActor ! GetData(filPath)
}