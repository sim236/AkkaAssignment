package Model

import scala.collection.mutable.ListBuffer

object Parent {
  case class GetData(filePath: String)
  case class ClassifyData(data:ListBuffer[ListBuffer[String]])
}
object Child{
  case class ClassifyDataChild(data: ListBuffer[String])
  case class StreamDataChannel(data:ListBuffer[String])
}