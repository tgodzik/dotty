/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin
final class ListTestsResult private (
  val tests: Vector[dotty.tools.sbtplugin.TestIdentifier]) extends Serializable {
  
  
  
  override def equals(o: Any): Boolean = o match {
    case x: ListTestsResult => (this.tests == x.tests)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (17 + "dotty.tools.sbtplugin.ListTestsResult".##) + tests.##)
  }
  override def toString: String = {
    "ListTestsResult(" + tests + ")"
  }
  private[this] def copy(tests: Vector[dotty.tools.sbtplugin.TestIdentifier] = tests): ListTestsResult = {
    new ListTestsResult(tests)
  }
  def withTests(tests: Vector[dotty.tools.sbtplugin.TestIdentifier]): ListTestsResult = {
    copy(tests = tests)
  }
}
object ListTestsResult {
  
  def apply(tests: Vector[dotty.tools.sbtplugin.TestIdentifier]): ListTestsResult = new ListTestsResult(tests)
}
