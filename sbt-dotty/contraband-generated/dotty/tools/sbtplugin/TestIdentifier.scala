/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin
final class TestIdentifier private (
  val build: dotty.tools.sbtplugin.BuildIdentifier,
  val path: Vector[String],
  val hasChildrenTests: Boolean) extends Serializable {
  
  
  
  override def equals(o: Any): Boolean = o match {
    case x: TestIdentifier => (this.build == x.build) && (this.path == x.path) && (this.hasChildrenTests == x.hasChildrenTests)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (37 * (37 * (17 + "dotty.tools.sbtplugin.TestIdentifier".##) + build.##) + path.##) + hasChildrenTests.##)
  }
  override def toString: String = {
    "TestIdentifier(" + build + ", " + path + ", " + hasChildrenTests + ")"
  }
  private[this] def copy(build: dotty.tools.sbtplugin.BuildIdentifier = build, path: Vector[String] = path, hasChildrenTests: Boolean = hasChildrenTests): TestIdentifier = {
    new TestIdentifier(build, path, hasChildrenTests)
  }
  def withBuild(build: dotty.tools.sbtplugin.BuildIdentifier): TestIdentifier = {
    copy(build = build)
  }
  def withPath(path: Vector[String]): TestIdentifier = {
    copy(path = path)
  }
  def withHasChildrenTests(hasChildrenTests: Boolean): TestIdentifier = {
    copy(hasChildrenTests = hasChildrenTests)
  }
}
object TestIdentifier {
  
  def apply(build: dotty.tools.sbtplugin.BuildIdentifier, path: Vector[String], hasChildrenTests: Boolean): TestIdentifier = new TestIdentifier(build, path, hasChildrenTests)
}
