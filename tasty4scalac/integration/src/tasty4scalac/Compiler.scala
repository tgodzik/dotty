package tasty4scalac


trait Compiler {
  def compile(code: String): Unit

  final override def toString: String = getClass.getSimpleName
}

object Compiler {
  def dotty(): Compiler = Dotty()
  def scalac(): Compiler = Scalac()

  trait Factory {
    def apply(): Compiler
  }
}
