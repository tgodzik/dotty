package scala.tasty

trait Context {

  val impl: Tasty

  def owner: impl.Definition

  def toTasty[T](expr: quoted.Expr[T]): impl.Term
  def toTasty[T](expr: quoted.Type[T]): impl.TypeTree

}

object Context {
  // TODO move to some other place
  /** Compiler context available in a ~ at inline site */
  def compilationContext: Context = throw new Exception("Not in inline macro")
}
