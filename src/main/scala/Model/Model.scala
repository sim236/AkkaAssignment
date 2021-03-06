package Model

import scala.collection.mutable.ListBuffer

object Parent {
  case class GetData(filePath: String)
  case class ClassifyData(data:ListBuffer[ListBuffer[String]])
  case class ClassifiedObjectData(order:PurchaseDetail)
  case class ErrorHandling(data:Any,Error:Exception)
  case class Combine(data:PurchaseDetail)
}
object Child{
  case class ClassifyDataChild(data: ListBuffer[String])
  case class StreamDataChannel(data:ListBuffer[String])
  case class CategoryFilterFlow(data :ListBuffer[PurchaseDetail])
  case class CategoryWiseFinancialYear(data:ListBuffer[PurchaseDetail])
  case class BulkProductInsights(data:ListBuffer[PurchaseDetail])
}