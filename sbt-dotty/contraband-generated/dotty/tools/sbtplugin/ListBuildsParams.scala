/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin
final class ListBuildsParams private () extends Serializable {



override def equals(o: Any): Boolean = o match {
  case _: ListBuildsParams => true
  case _ => false
}
override def hashCode: Int = {
  37 * (17 + "dotty.tools.sbtplugin.ListBuildsParams".##)
}
override def toString: String = {
  "ListBuildsParams()"
}
private[this] def copy(): ListBuildsParams = {
  new ListBuildsParams()
}

}
object ListBuildsParams {
  
  def apply(): ListBuildsParams = new ListBuildsParams()
}
