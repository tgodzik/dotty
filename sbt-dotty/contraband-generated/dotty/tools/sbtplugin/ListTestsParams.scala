/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin
final class ListTestsParams private (
  val parents: Vector[dotty.tools.sbtplugin.TestIdentifier]) extends Serializable {
  
  
  
  override def equals(o: Any): Boolean = o match {
    case x: ListTestsParams => (this.parents == x.parents)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (17 + "dotty.tools.sbtplugin.ListTestsParams".##) + parents.##)
  }
  override def toString: String = {
    "ListTestsParams(" + parents + ")"
  }
  private[this] def copy(parents: Vector[dotty.tools.sbtplugin.TestIdentifier] = parents): ListTestsParams = {
    new ListTestsParams(parents)
  }
  def withParents(parents: Vector[dotty.tools.sbtplugin.TestIdentifier]): ListTestsParams = {
    copy(parents = parents)
  }
}
object ListTestsParams {
  
  def apply(parents: Vector[dotty.tools.sbtplugin.TestIdentifier]): ListTestsParams = new ListTestsParams(parents)
}
