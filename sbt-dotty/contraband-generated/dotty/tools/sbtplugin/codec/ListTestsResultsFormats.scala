/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin.codec
import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
trait ListTestsResultsFormats { self: dotty.tools.sbtplugin.codec.TestIdentifierFormats with sjsonnew.BasicJsonProtocol =>
implicit lazy val ListTestsResultsFormat: JsonFormat[dotty.tools.sbtplugin.ListTestsResults] = new JsonFormat[dotty.tools.sbtplugin.ListTestsResults] {
  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): dotty.tools.sbtplugin.ListTestsResults = {
    jsOpt match {
      case Some(js) =>
      unbuilder.beginObject(js)
      val tests = unbuilder.readField[Vector[dotty.tools.sbtplugin.TestIdentifier]]("tests")
      unbuilder.endObject()
      dotty.tools.sbtplugin.ListTestsResults(tests)
      case None =>
      deserializationError("Expected JsObject but found None")
    }
  }
  override def write[J](obj: dotty.tools.sbtplugin.ListTestsResults, builder: Builder[J]): Unit = {
    builder.beginObject()
    builder.addField("tests", obj.tests)
    builder.endObject()
  }
}
}
