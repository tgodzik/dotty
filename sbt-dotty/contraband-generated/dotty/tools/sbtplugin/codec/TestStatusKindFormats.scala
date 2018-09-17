/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin.codec
import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
trait TestStatusKindFormats { self: sjsonnew.BasicJsonProtocol =>
implicit lazy val TestStatusKindFormat: JsonFormat[dotty.tools.sbtplugin.TestStatusKind] = new JsonFormat[dotty.tools.sbtplugin.TestStatusKind] {
  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): dotty.tools.sbtplugin.TestStatusKind = {
    jsOpt match {
      case Some(js) =>
      unbuilder.readString(js) match {
        case "Ignored" => dotty.tools.sbtplugin.TestStatusKind.Ignored
        case "Running" => dotty.tools.sbtplugin.TestStatusKind.Running
        case "Success" => dotty.tools.sbtplugin.TestStatusKind.Success
        case "Failure" => dotty.tools.sbtplugin.TestStatusKind.Failure
      }
      case None =>
      deserializationError("Expected JsString but found None")
    }
  }
  override def write[J](obj: dotty.tools.sbtplugin.TestStatusKind, builder: Builder[J]): Unit = {
    val str = obj match {
      case dotty.tools.sbtplugin.TestStatusKind.Ignored => "Ignored"
      case dotty.tools.sbtplugin.TestStatusKind.Running => "Running"
      case dotty.tools.sbtplugin.TestStatusKind.Success => "Success"
      case dotty.tools.sbtplugin.TestStatusKind.Failure => "Failure"
    }
    builder.writeString(str)
  }
}
}
