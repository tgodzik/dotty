/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin.codec
trait JsonProtocol extends sjsonnew.BasicJsonProtocol
  with dotty.tools.sbtplugin.codec.BuildIdentifierFormats
  with dotty.tools.sbtplugin.codec.ListBuildsParamsFormats
  with dotty.tools.sbtplugin.codec.ListBuildsResultFormats
  with dotty.tools.sbtplugin.codec.TestIdentifierFormats
  with dotty.tools.sbtplugin.codec.ListTestsParamsFormats
  with dotty.tools.sbtplugin.codec.ListTestsResultFormats
  with dotty.tools.sbtplugin.codec.RunTestsParamsFormats
  with dotty.tools.sbtplugin.codec.RunTestsResultFormats
  with dotty.tools.sbtplugin.codec.TestStatusKindFormats
  with dotty.tools.sbtplugin.codec.TestStatusFormats
object JsonProtocol extends JsonProtocol