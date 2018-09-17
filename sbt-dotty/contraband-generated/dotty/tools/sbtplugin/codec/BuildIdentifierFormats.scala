/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin.codec
import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
trait BuildIdentifierFormats { self: sjsonnew.BasicJsonProtocol =>
implicit lazy val BuildIdentifierFormat: JsonFormat[dotty.tools.sbtplugin.BuildIdentifier] = new JsonFormat[dotty.tools.sbtplugin.BuildIdentifier] {
  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): dotty.tools.sbtplugin.BuildIdentifier = {
    jsOpt match {
      case Some(js) =>
      unbuilder.beginObject(js)
      val name = unbuilder.readField[String]("name")
      val hasTests = unbuilder.readField[Boolean]("hasTests")
      unbuilder.endObject()
      dotty.tools.sbtplugin.BuildIdentifier(name, hasTests)
      case None =>
      deserializationError("Expected JsObject but found None")
    }
  }
  override def write[J](obj: dotty.tools.sbtplugin.BuildIdentifier, builder: Builder[J]): Unit = {
    builder.beginObject()
    builder.addField("name", obj.name)
    builder.addField("hasTests", obj.hasTests)
    builder.endObject()
  }
}
}
