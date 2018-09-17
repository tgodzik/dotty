/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin
final class CompileBuildsParams private (
  val builds: Vector[dotty.tools.sbtplugin.BuildIdentifier]) extends Serializable {
  
  
  
  override def equals(o: Any): Boolean = o match {
    case x: CompileBuildsParams => (this.builds == x.builds)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (17 + "dotty.tools.sbtplugin.CompileBuildsParams".##) + builds.##)
  }
  override def toString: String = {
    "CompileBuildsParams(" + builds + ")"
  }
  private[this] def copy(builds: Vector[dotty.tools.sbtplugin.BuildIdentifier] = builds): CompileBuildsParams = {
    new CompileBuildsParams(builds)
  }
  def withBuilds(builds: Vector[dotty.tools.sbtplugin.BuildIdentifier]): CompileBuildsParams = {
    copy(builds = builds)
  }
}
object CompileBuildsParams {
  
  def apply(builds: Vector[dotty.tools.sbtplugin.BuildIdentifier]): CompileBuildsParams = new CompileBuildsParams(builds)
}
