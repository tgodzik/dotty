/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin
final class ListBuildsResult private (
  val builds: Vector[dotty.tools.sbtplugin.BuildIdentifier]) extends Serializable {
  
  
  
  override def equals(o: Any): Boolean = o match {
    case x: ListBuildsResult => (this.builds == x.builds)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (17 + "dotty.tools.sbtplugin.ListBuildsResult".##) + builds.##)
  }
  override def toString: String = {
    "ListBuildsResult(" + builds + ")"
  }
  private[this] def copy(builds: Vector[dotty.tools.sbtplugin.BuildIdentifier] = builds): ListBuildsResult = {
    new ListBuildsResult(builds)
  }
  def withBuilds(builds: Vector[dotty.tools.sbtplugin.BuildIdentifier]): ListBuildsResult = {
    copy(builds = builds)
  }
}
object ListBuildsResult {
  
  def apply(builds: Vector[dotty.tools.sbtplugin.BuildIdentifier]): ListBuildsResult = new ListBuildsResult(builds)
}
