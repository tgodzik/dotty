/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin.codec
import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
trait TestIdentifierFormats { self: dotty.tools.sbtplugin.codec.BuildIdentifierFormats with sjsonnew.BasicJsonProtocol =>
implicit lazy val TestIdentifierFormat: JsonFormat[dotty.tools.sbtplugin.TestIdentifier] = new JsonFormat[dotty.tools.sbtplugin.TestIdentifier] {
  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): dotty.tools.sbtplugin.TestIdentifier = {
    jsOpt match {
      case Some(js) =>
      unbuilder.beginObject(js)
      val build = unbuilder.readField[dotty.tools.sbtplugin.BuildIdentifier]("build")
      val path = unbuilder.readField[Vector[String]]("path")
      val hasChildrenTests = unbuilder.readField[Boolean]("hasChildrenTests")
      unbuilder.endObject()
      dotty.tools.sbtplugin.TestIdentifier(build, path, hasChildrenTests)
      case None =>
      deserializationError("Expected JsObject but found None")
    }
  }
  override def write[J](obj: dotty.tools.sbtplugin.TestIdentifier, builder: Builder[J]): Unit = {
    builder.beginObject()
    builder.addField("build", obj.build)
    builder.addField("path", obj.path)
    builder.addField("hasChildrenTests", obj.hasChildrenTests)
    builder.endObject()
  }
}
}
