object HelloWorld {
  def main(args: Array[String]): Unit = {
    println("hello world")
    val list = List(1, 2, 3)
    list match {
      case head :: tail => println(head)
      case Nil => println("empty")
    }
    val s = list match {
      case head :: tail => head.toString
      case Nil => "empty"
    }
    println(s)
  }
}
