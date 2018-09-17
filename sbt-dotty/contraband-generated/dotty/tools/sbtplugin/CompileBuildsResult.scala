/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin
final class CompileBuildsResult private (
  val compilationSucceeded: Boolean) extends Serializable {
  
  
  
  override def equals(o: Any): Boolean = o match {
    case x: CompileBuildsResult => (this.compilationSucceeded == x.compilationSucceeded)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (17 + "dotty.tools.sbtplugin.CompileBuildsResult".##) + compilationSucceeded.##)
  }
  override def toString: String = {
    "CompileBuildsResult(" + compilationSucceeded + ")"
  }
  private[this] def copy(compilationSucceeded: Boolean = compilationSucceeded): CompileBuildsResult = {
    new CompileBuildsResult(compilationSucceeded)
  }
  def withCompilationSucceeded(compilationSucceeded: Boolean): CompileBuildsResult = {
    copy(compilationSucceeded = compilationSucceeded)
  }
}
object CompileBuildsResult {
  
  def apply(compilationSucceeded: Boolean): CompileBuildsResult = new CompileBuildsResult(compilationSucceeded)
}
