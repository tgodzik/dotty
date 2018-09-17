/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin.codec
import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
trait ListTestsParamsFormats { self: dotty.tools.sbtplugin.codec.TestIdentifierFormats with sjsonnew.BasicJsonProtocol =>
implicit lazy val ListTestsParamsFormat: JsonFormat[dotty.tools.sbtplugin.ListTestsParams] = new JsonFormat[dotty.tools.sbtplugin.ListTestsParams] {
  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): dotty.tools.sbtplugin.ListTestsParams = {
    jsOpt match {
      case Some(js) =>
      unbuilder.beginObject(js)
      val parents = unbuilder.readField[Vector[dotty.tools.sbtplugin.TestIdentifier]]("parents")
      unbuilder.endObject()
      dotty.tools.sbtplugin.ListTestsParams(parents)
      case None =>
      deserializationError("Expected JsObject but found None")
    }
  }
  override def write[J](obj: dotty.tools.sbtplugin.ListTestsParams, builder: Builder[J]): Unit = {
    builder.beginObject()
    builder.addField("parents", obj.parents)
    builder.endObject()
  }
}
}
