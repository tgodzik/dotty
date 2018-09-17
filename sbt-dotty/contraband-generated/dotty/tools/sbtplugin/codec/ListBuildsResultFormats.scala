/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin.codec
import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
trait ListBuildsResultFormats { self: dotty.tools.sbtplugin.codec.BuildIdentifierFormats with sjsonnew.BasicJsonProtocol =>
implicit lazy val ListBuildsResultFormat: JsonFormat[dotty.tools.sbtplugin.ListBuildsResult] = new JsonFormat[dotty.tools.sbtplugin.ListBuildsResult] {
  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): dotty.tools.sbtplugin.ListBuildsResult = {
    jsOpt match {
      case Some(js) =>
      unbuilder.beginObject(js)
      val builds = unbuilder.readField[Vector[dotty.tools.sbtplugin.BuildIdentifier]]("builds")
      unbuilder.endObject()
      dotty.tools.sbtplugin.ListBuildsResult(builds)
      case None =>
      deserializationError("Expected JsObject but found None")
    }
  }
  override def write[J](obj: dotty.tools.sbtplugin.ListBuildsResult, builder: Builder[J]): Unit = {
    builder.beginObject()
    builder.addField("builds", obj.builds)
    builder.endObject()
  }
}
}
