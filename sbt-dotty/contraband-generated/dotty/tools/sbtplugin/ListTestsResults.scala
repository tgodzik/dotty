/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin
final class ListTestsResults private (
  val tests: Vector[dotty.tools.sbtplugin.TestIdentifier]) extends Serializable {
  
  
  
  override def equals(o: Any): Boolean = o match {
    case x: ListTestsResults => (this.tests == x.tests)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (17 + "dotty.tools.sbtplugin.ListTestsResults".##) + tests.##)
  }
  override def toString: String = {
    "ListTestsResults(" + tests + ")"
  }
  private[this] def copy(tests: Vector[dotty.tools.sbtplugin.TestIdentifier] = tests): ListTestsResults = {
    new ListTestsResults(tests)
  }
  def withTests(tests: Vector[dotty.tools.sbtplugin.TestIdentifier]): ListTestsResults = {
    copy(tests = tests)
  }
}
object ListTestsResults {
  
  def apply(tests: Vector[dotty.tools.sbtplugin.TestIdentifier]): ListTestsResults = new ListTestsResults(tests)
}
