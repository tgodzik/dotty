/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin
final class TestStatus private (
  val id: dotty.tools.sbtplugin.TestIdentifier,
  val kind: dotty.tools.sbtplugin.TestStatusKind,
  val shortDescription: String,
  val longDescription: String) extends Serializable {
  
  
  
  override def equals(o: Any): Boolean = o match {
    case x: TestStatus => (this.id == x.id) && (this.kind == x.kind) && (this.shortDescription == x.shortDescription) && (this.longDescription == x.longDescription)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (37 * (37 * (37 * (17 + "dotty.tools.sbtplugin.TestStatus".##) + id.##) + kind.##) + shortDescription.##) + longDescription.##)
  }
  override def toString: String = {
    "TestStatus(" + id + ", " + kind + ", " + shortDescription + ", " + longDescription + ")"
  }
  private[this] def copy(id: dotty.tools.sbtplugin.TestIdentifier = id, kind: dotty.tools.sbtplugin.TestStatusKind = kind, shortDescription: String = shortDescription, longDescription: String = longDescription): TestStatus = {
    new TestStatus(id, kind, shortDescription, longDescription)
  }
  def withId(id: dotty.tools.sbtplugin.TestIdentifier): TestStatus = {
    copy(id = id)
  }
  def withKind(kind: dotty.tools.sbtplugin.TestStatusKind): TestStatus = {
    copy(kind = kind)
  }
  def withShortDescription(shortDescription: String): TestStatus = {
    copy(shortDescription = shortDescription)
  }
  def withLongDescription(longDescription: String): TestStatus = {
    copy(longDescription = longDescription)
  }
}
object TestStatus {
  
  def apply(id: dotty.tools.sbtplugin.TestIdentifier, kind: dotty.tools.sbtplugin.TestStatusKind, shortDescription: String, longDescription: String): TestStatus = new TestStatus(id, kind, shortDescription, longDescription)
}
