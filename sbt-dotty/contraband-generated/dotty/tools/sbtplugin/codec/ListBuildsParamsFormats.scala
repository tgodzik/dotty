/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin.codec
import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
trait ListBuildsParamsFormats { self: sjsonnew.BasicJsonProtocol =>
implicit lazy val ListBuildsParamsFormat: JsonFormat[dotty.tools.sbtplugin.ListBuildsParams] = new JsonFormat[dotty.tools.sbtplugin.ListBuildsParams] {
  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): dotty.tools.sbtplugin.ListBuildsParams = {
    jsOpt match {
      case Some(js) =>
      unbuilder.beginObject(js)
      
      unbuilder.endObject()
      dotty.tools.sbtplugin.ListBuildsParams()
      case None =>
      deserializationError("Expected JsObject but found None")
    }
  }
  override def write[J](obj: dotty.tools.sbtplugin.ListBuildsParams, builder: Builder[J]): Unit = {
    builder.beginObject()
    
    builder.endObject()
  }
}
}
