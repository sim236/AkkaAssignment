package Model

case class PurchaseDetail(
                           orderDate: String,
                           shipDate: String,
                           shipMode: String,
                           customerName: String,
                           segment: String,
                           country: String,
                           city: String,
                           state:String,
                           region:String,
                           category:String,
                           subRegion:String,
                           name:String,
                           sales:Float,
                           quantity:Float,
                           discount:Float,
                           profit:Float
                          )
