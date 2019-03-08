package tasty4scalac

object DebugIt extends App{

  val scalac = Scalac()

  val code1 =
    """
      | package a.c
      |
      | class A {
      |   def b() = true
      |
      |   def c = b ()
      | }
    """.stripMargin

  val res = scalac.compile(code1)
  println(res)


  val dotty = Dotty()

  val resDotty = dotty.compile(code1)

  println(resDotty)
  // TODO move from simple code pieces to more and more complex ones

}
