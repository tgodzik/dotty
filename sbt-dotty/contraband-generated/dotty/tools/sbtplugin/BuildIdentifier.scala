/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin
final class BuildIdentifier private (
  val name: String,
  val hasTests: Boolean) extends Serializable {
  
  
  
  override def equals(o: Any): Boolean = o match {
    case x: BuildIdentifier => (this.name == x.name) && (this.hasTests == x.hasTests)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (37 * (17 + "dotty.tools.sbtplugin.BuildIdentifier".##) + name.##) + hasTests.##)
  }
  override def toString: String = {
    "BuildIdentifier(" + name + ", " + hasTests + ")"
  }
  private[this] def copy(name: String = name, hasTests: Boolean = hasTests): BuildIdentifier = {
    new BuildIdentifier(name, hasTests)
  }
  def withName(name: String): BuildIdentifier = {
    copy(name = name)
  }
  def withHasTests(hasTests: Boolean): BuildIdentifier = {
    copy(hasTests = hasTests)
  }
}
object BuildIdentifier {
  
  def apply(name: String, hasTests: Boolean): BuildIdentifier = new BuildIdentifier(name, hasTests)
}
