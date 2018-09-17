/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin
final class BuildTargetIdentifier private (
  val name: String,
  val hasTests: Boolean) extends Serializable {
  
  
  
  override def equals(o: Any): Boolean = o match {
    case x: BuildTargetIdentifier => (this.name == x.name) && (this.hasTests == x.hasTests)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (37 * (17 + "dotty.tools.sbtplugin.BuildTargetIdentifier".##) + name.##) + hasTests.##)
  }
  override def toString: String = {
    "BuildTargetIdentifier(" + name + ", " + hasTests + ")"
  }
  private[this] def copy(name: String = name, hasTests: Boolean = hasTests): BuildTargetIdentifier = {
    new BuildTargetIdentifier(name, hasTests)
  }
  def withName(name: String): BuildTargetIdentifier = {
    copy(name = name)
  }
  def withHasTests(hasTests: Boolean): BuildTargetIdentifier = {
    copy(hasTests = hasTests)
  }
}
object BuildTargetIdentifier {
  
  def apply(name: String, hasTests: Boolean): BuildTargetIdentifier = new BuildTargetIdentifier(name, hasTests)
}
