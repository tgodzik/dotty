/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin.codec
import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
trait CompileBuildsParamsFormats { self: dotty.tools.sbtplugin.codec.BuildIdentifierFormats with sjsonnew.BasicJsonProtocol =>
implicit lazy val CompileBuildsParamsFormat: JsonFormat[dotty.tools.sbtplugin.CompileBuildsParams] = new JsonFormat[dotty.tools.sbtplugin.CompileBuildsParams] {
  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): dotty.tools.sbtplugin.CompileBuildsParams = {
    jsOpt match {
      case Some(js) =>
      unbuilder.beginObject(js)
      val builds = unbuilder.readField[Vector[dotty.tools.sbtplugin.BuildIdentifier]]("builds")
      unbuilder.endObject()
      dotty.tools.sbtplugin.CompileBuildsParams(builds)
      case None =>
      deserializationError("Expected JsObject but found None")
    }
  }
  override def write[J](obj: dotty.tools.sbtplugin.CompileBuildsParams, builder: Builder[J]): Unit = {
    builder.beginObject()
    builder.addField("builds", obj.builds)
    builder.endObject()
  }
}
}
