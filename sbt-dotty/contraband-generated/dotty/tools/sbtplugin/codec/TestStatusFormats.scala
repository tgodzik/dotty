/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin.codec
import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
trait TestStatusFormats { self: dotty.tools.sbtplugin.codec.TestIdentifierFormats with dotty.tools.sbtplugin.codec.TestStatusKindFormats with sjsonnew.BasicJsonProtocol =>
implicit lazy val TestStatusFormat: JsonFormat[dotty.tools.sbtplugin.TestStatus] = new JsonFormat[dotty.tools.sbtplugin.TestStatus] {
  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): dotty.tools.sbtplugin.TestStatus = {
    jsOpt match {
      case Some(js) =>
      unbuilder.beginObject(js)
      val id = unbuilder.readField[dotty.tools.sbtplugin.TestIdentifier]("id")
      val kind = unbuilder.readField[dotty.tools.sbtplugin.TestStatusKind]("kind")
      val details = unbuilder.readField[String]("details")
      unbuilder.endObject()
      dotty.tools.sbtplugin.TestStatus(id, kind, details)
      case None =>
      deserializationError("Expected JsObject but found None")
    }
  }
  override def write[J](obj: dotty.tools.sbtplugin.TestStatus, builder: Builder[J]): Unit = {
    builder.beginObject()
    builder.addField("id", obj.id)
    builder.addField("kind", obj.kind)
    builder.addField("details", obj.details)
    builder.endObject()
  }
}
}
