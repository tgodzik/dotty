/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin
/** response to dotty/test */
final class TestResult private (
  val res: String) extends Serializable {
  
  
  
  override def equals(o: Any): Boolean = o match {
    case x: TestResult => (this.res == x.res)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (17 + "dotty.tools.sbtplugin.TestResult".##) + res.##)
  }
  override def toString: String = {
    "TestResult(" + res + ")"
  }
  private[this] def copy(res: String = res): TestResult = {
    new TestResult(res)
  }
  def withRes(res: String): TestResult = {
    copy(res = res)
  }
}
object TestResult {
  
  def apply(res: String): TestResult = new TestResult(res)
}
