/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin
final class TestStatus private (
  val id: dotty.tools.sbtplugin.TestIdentifier,
  val kind: dotty.tools.sbtplugin.TestStatusKind,
  val details: String) extends Serializable {
  
  
  
  override def equals(o: Any): Boolean = o match {
    case x: TestStatus => (this.id == x.id) && (this.kind == x.kind) && (this.details == x.details)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (37 * (37 * (17 + "dotty.tools.sbtplugin.TestStatus".##) + id.##) + kind.##) + details.##)
  }
  override def toString: String = {
    "TestStatus(" + id + ", " + kind + ", " + details + ")"
  }
  private[this] def copy(id: dotty.tools.sbtplugin.TestIdentifier = id, kind: dotty.tools.sbtplugin.TestStatusKind = kind, details: String = details): TestStatus = {
    new TestStatus(id, kind, details)
  }
  def withId(id: dotty.tools.sbtplugin.TestIdentifier): TestStatus = {
    copy(id = id)
  }
  def withKind(kind: dotty.tools.sbtplugin.TestStatusKind): TestStatus = {
    copy(kind = kind)
  }
  def withDetails(details: String): TestStatus = {
    copy(details = details)
  }
}
object TestStatus {
  
  def apply(id: dotty.tools.sbtplugin.TestIdentifier, kind: dotty.tools.sbtplugin.TestStatusKind, details: String): TestStatus = new TestStatus(id, kind, details)
}
