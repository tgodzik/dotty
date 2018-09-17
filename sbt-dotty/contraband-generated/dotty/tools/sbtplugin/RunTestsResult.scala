/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin
final class RunTestsResult private () extends Serializable {



override def equals(o: Any): Boolean = o match {
  case _: RunTestsResult => true
  case _ => false
}
override def hashCode: Int = {
  37 * (17 + "dotty.tools.sbtplugin.RunTestsResult".##)
}
override def toString: String = {
  "RunTestsResult()"
}
private[this] def copy(): RunTestsResult = {
  new RunTestsResult()
}

}
object RunTestsResult {
  
  def apply(): RunTestsResult = new RunTestsResult()
}
