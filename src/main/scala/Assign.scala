
import Model.Child.{ClassifyDataChild}
import Model.Parent.{ClassifyData, GetData}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import org.apache.poi.ss.usermodel.{CellType, DateUtil, WorkbookFactory}

import java.io.File
import scala.collection.mutable.ListBuffer

object Assign extends App {

  val actorSystem = ActorSystem("actorSystem")

  class Parent extends Actor {
    override def receive: Receive = {
      case GetData(filePath: String) => {
        val childRef: ActorRef = actorSystem.actorOf(Props[Child])
        childRef ! GetData(filePath)
      }
      case ClassifyData(fileContent: ListBuffer[ListBuffer[String]]) => {
        val childPool = actorSystem.actorOf(RoundRobinPool(10).props(Props[Child]), "Child")
        fileContent.foreach({data=>childPool ! ClassifyDataChild(data)})
      }
    }
  }

  class Child extends Actor {
    override def receive: Receive = {
      case GetData(filePath: String) => {

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
                  rowContent +=DateUtil.getExcelDate(cell.getDateCellValue).toString
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
      case ClassifyDataChild(data: ListBuffer[String]) => {
        println(data)
        Thread.sleep(10000)
      }
    }
  }
  val parentActor: ActorRef = actorSystem.actorOf(Props[Parent])
  val filPath:String="C:\\Users\\simchhabra\\Desktop\\SCALA\\LEARN\\AKKA\\AkkaAssignment\\src\\main\\scala\\Resources\\Superstore_purchases.xlsx"
  parentActor ! GetData(filPath)

}