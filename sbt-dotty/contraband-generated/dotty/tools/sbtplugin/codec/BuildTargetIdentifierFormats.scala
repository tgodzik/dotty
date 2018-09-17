/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin.codec
import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
trait BuildTargetIdentifierFormats { self: sjsonnew.BasicJsonProtocol =>
implicit lazy val BuildTargetIdentifierFormat: JsonFormat[dotty.tools.sbtplugin.BuildTargetIdentifier] = new JsonFormat[dotty.tools.sbtplugin.BuildTargetIdentifier] {
  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): dotty.tools.sbtplugin.BuildTargetIdentifier = {
    jsOpt match {
      case Some(js) =>
      unbuilder.beginObject(js)
      val name = unbuilder.readField[String]("name")
      val hasTests = unbuilder.readField[Boolean]("hasTests")
      unbuilder.endObject()
      dotty.tools.sbtplugin.BuildTargetIdentifier(name, hasTests)
      case None =>
      deserializationError("Expected JsObject but found None")
    }
  }
  override def write[J](obj: dotty.tools.sbtplugin.BuildTargetIdentifier, builder: Builder[J]): Unit = {
    builder.beginObject()
    builder.addField("name", obj.name)
    builder.addField("hasTests", obj.hasTests)
    builder.endObject()
  }
}
}
