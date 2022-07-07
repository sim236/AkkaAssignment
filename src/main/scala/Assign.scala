
import Model.Child.{CategoryFilterFlow, CategoryWiseFinancialYear, ClassifyDataChild, Combine}
import Model.Parent.{ClassifiedObjectData, ClassifyData, ErrorHandling, GetData}
import Model.PurchaseDetail
import Util.{exceptionFileUtil, fileUtil}
import akka.NotUsed
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source, Zip, ZipN, ZipWith, ZipWithN}
import com.typesafe.config.ConfigFactory
import org.apache.poi.ss.usermodel.{CellType, DateUtil, WorkbookFactory}

import java.io.File
import scala.collection.mutable.ListBuffer

object Assign extends App{
  val conf=ConfigFactory.load("project.json")
  val actorSystem = ActorSystem("actorSystem")

  class Parent extends Actor {
    val orderList: ListBuffer[PurchaseDetail] = new ListBuffer[PurchaseDetail]()
    var size:Int=0
    override def receive: Receive = {
      case GetData(filePath: String) => {
        val childRef: ActorRef = actorSystem.actorOf(Props[Child])
        childRef ! GetData(filePath)
      }
      case ClassifyData(fileContent: ListBuffer[ListBuffer[String]]) => {
        size=fileContent.length
        val childPool = actorSystem.actorOf(RoundRobinPool(10).props(Props[Child]), "Child")
        fileContent.foreach({ data => childPool ! ClassifyDataChild(data) })
      }
      case Combine(data:PurchaseDetail)=>{
        orderList+=data
        if(orderList.length==size){
          val childRef: ActorRef = actorSystem.actorOf(Props[Child])
          childRef !  CategoryFilterFlow(orderList)
          childRef ! CategoryWiseFinancialYear(orderList)
        }
      }
      case ErrorHandling(data:Any,error:Exception)=>{
        exceptionFileUtil(data,error,conf.getString("errorHandlingFileName"))
      }
    }
  }

  class Child extends Actor {

    implicit val materializer= ActorMaterializer()
    override def receive: Receive = {

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
            sales= data(12).toFloat,
            quantity= data(13).toFloat,
            discount= data(14).toFloat,
            profit= data(15).toFloat,
          )
          sender() ! Combine(order)
        }
        catch{
          case e:Exception=> {
            sender() ! ErrorHandling(data, e)
          }
        }
      }


      case CategoryFilterFlow(data:ListBuffer[PurchaseDetail])=>{
        val source=Source(data.toList)
        val flowForCategory=Flow[PurchaseDetail].filter(_.category==conf.getString("category"))
        val sink=Sink.foreach(data=>{
          fileUtil(data,conf.getString("categorySinkFile"))
        })

        source.async.via(flowForCategory).to(sink).run
      }


      case CategoryWiseFinancialYear(data:ListBuffer[PurchaseDetail])=>{
        val source=Source(data.toList)
        val flow1=Flow[PurchaseDetail].filter(_.category==conf.getString("category"))
        val flow=Flow[PurchaseDetail].filter(_.customerName==conf.getString("naame"))
        val flow2=Flow[PurchaseDetail].filter(_.orderDate.split(" ").last==conf.getString("year"))
        val sumOfSalesFlow=Flow[PurchaseDetail].map(_.sales).fold(0.0)(_+_)
        val sumOfQuantityFlow=Flow[PurchaseDetail].map(_.quantity).fold(0.0)(_ + _)
        val sumOfDiscountFlow=Flow[PurchaseDetail].map(_.discount).fold(0.0)(_ + _)
        val sumOfProfitFlow=Flow[PurchaseDetail].map(_.profit).fold(0.0)(_ + _)
        val sink=Sink.foreach(data=>{
          fileUtil(data,conf.getString("categoryWiseFinancialYearSinkFile"))
        })


        val graphForSalesQuantityDiscountAndProfit=RunnableGraph.fromGraph(
          GraphDSL.create(){
            implicit builder:GraphDSL.Builder[NotUsed]=>
              import GraphDSL.Implicits._

              val broadcast= builder.add(Broadcast [PurchaseDetail] (4))

              val zip= builder.add(ZipN[Any](4))
              source.via(flow1).via(flow2) ~> broadcast

              broadcast.out(0) ~> sumOfSalesFlow ~> zip.in(0)
              broadcast.out(1) ~> sumOfQuantityFlow ~> zip.in(1)
              broadcast.out(2) ~> sumOfDiscountFlow~> zip.in(2)
              broadcast.out(3) ~> sumOfProfitFlow~> zip.in(3)
              zip.out ~> sink
              ClosedShape
          }
        )
        graphForSalesQuantityDiscountAndProfit.run
      }
    }
  }
  val parentActor: ActorRef = actorSystem.actorOf(Props[Parent])
  val filPath:String=conf.getString("filePath")
  parentActor ! GetData(filPath)
}