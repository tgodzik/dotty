/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin
sealed abstract class TestStatusKind extends Serializable
object TestStatusKind {
  
  
  case object Ignored extends TestStatusKind
  case object Running extends TestStatusKind
  case object Success extends TestStatusKind
  case object Failure extends TestStatusKind
}
