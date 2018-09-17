/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin
final class RunTestsParams private (
  val tests: Vector[dotty.tools.sbtplugin.TestIdentifier]) extends Serializable {
  
  
  
  override def equals(o: Any): Boolean = o match {
    case x: RunTestsParams => (this.tests == x.tests)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (17 + "dotty.tools.sbtplugin.RunTestsParams".##) + tests.##)
  }
  override def toString: String = {
    "RunTestsParams(" + tests + ")"
  }
  private[this] def copy(tests: Vector[dotty.tools.sbtplugin.TestIdentifier] = tests): RunTestsParams = {
    new RunTestsParams(tests)
  }
  def withTests(tests: Vector[dotty.tools.sbtplugin.TestIdentifier]): RunTestsParams = {
    copy(tests = tests)
  }
}
object RunTestsParams {
  
  def apply(tests: Vector[dotty.tools.sbtplugin.TestIdentifier]): RunTestsParams = new RunTestsParams(tests)
}
