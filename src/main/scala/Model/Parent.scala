package Model

import scala.collection.mutable.ListBuffer

object Parent {
  case class GetData(filePath: String)
  case class ClassifyData(data:ListBuffer[ListBuffer[String]])
  case class ClassifiedObjectData(order:PurchaseDetail)
  case class ErrorHandling(data:Any,Error:Exception)
}
object Child{
  case class ClassifyDataChild(data: ListBuffer[String])
  case class StreamDataChannel(data:ListBuffer[String])
  case class Mile1(data:PurchaseDetail)

}