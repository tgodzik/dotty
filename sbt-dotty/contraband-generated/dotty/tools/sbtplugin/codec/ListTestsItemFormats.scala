/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin.codec
import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
trait ListTestsItemFormats { self: dotty.tools.sbtplugin.codec.TestIdentifierFormats with dotty.tools.sbtplugin.codec.ListTestsItemFormats with sjsonnew.BasicJsonProtocol =>
implicit lazy val ListTestsItemFormat: JsonFormat[dotty.tools.sbtplugin.ListTestsItem] = new JsonFormat[dotty.tools.sbtplugin.ListTestsItem] {
  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): dotty.tools.sbtplugin.ListTestsItem = {
    jsOpt match {
      case Some(js) =>
      unbuilder.beginObject(js)
      val id = unbuilder.readField[dotty.tools.sbtplugin.TestIdentifier]("id")
      val subTests = unbuilder.readField[Vector[dotty.tools.sbtplugin.ListTestsItem]]("subTests")
      unbuilder.endObject()
      dotty.tools.sbtplugin.ListTestsItem(id, subTests)
      case None =>
      deserializationError("Expected JsObject but found None")
    }
  }
  override def write[J](obj: dotty.tools.sbtplugin.ListTestsItem, builder: Builder[J]): Unit = {
    builder.beginObject()
    builder.addField("id", obj.id)
    builder.addField("subTests", obj.subTests)
    builder.endObject()
  }
}
}
