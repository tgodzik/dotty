import Macro._

object Test {


  val sym: Symantics = new Symantics {
    def __if[T](cond: Boolean, thenp: => T, elsep: => T): T = {
      println("__if " + cond)
      if (cond) thenp
      else elsep
    }
  }

  def main(args: Array[String]): Unit = {
    println(virtualize(sym)(42))

    println(virtualize(sym)("string of 42"))

    val b: Boolean = true
    println(virtualize(sym)(if(b) 1 else 2))
  }


}
