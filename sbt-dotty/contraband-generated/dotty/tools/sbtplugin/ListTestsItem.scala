/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin
final class ListTestsItem private (
  val id: dotty.tools.sbtplugin.TestIdentifier,
  val subTests: Vector[dotty.tools.sbtplugin.ListTestsItem]) extends Serializable {
  
  
  
  override def equals(o: Any): Boolean = o match {
    case x: ListTestsItem => (this.id == x.id) && (this.subTests == x.subTests)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (37 * (17 + "dotty.tools.sbtplugin.ListTestsItem".##) + id.##) + subTests.##)
  }
  override def toString: String = {
    "ListTestsItem(" + id + ", " + subTests + ")"
  }
  private[this] def copy(id: dotty.tools.sbtplugin.TestIdentifier = id, subTests: Vector[dotty.tools.sbtplugin.ListTestsItem] = subTests): ListTestsItem = {
    new ListTestsItem(id, subTests)
  }
  def withId(id: dotty.tools.sbtplugin.TestIdentifier): ListTestsItem = {
    copy(id = id)
  }
  def withSubTests(subTests: Vector[dotty.tools.sbtplugin.ListTestsItem]): ListTestsItem = {
    copy(subTests = subTests)
  }
}
object ListTestsItem {
  
  def apply(id: dotty.tools.sbtplugin.TestIdentifier, subTests: Vector[dotty.tools.sbtplugin.ListTestsItem]): ListTestsItem = new ListTestsItem(id, subTests)
}
