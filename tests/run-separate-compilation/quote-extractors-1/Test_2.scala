
object Test {
  import Macros._

  def main(args: Array[String]): Unit = {
    println(lift(StringLang)(null))
    println(lift(StringLang)(42))
    println(lift(StringLang)(true))
    println(lift(StringLang)(if (true) 1 else 2))
    println(lift(StringLang)(if (true) 1 else ""))
    println(lift(StringLang)(if (true) 1: Any else "": AnyRef))
  }
}
