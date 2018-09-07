/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin.codec
trait JsonProtocol extends sjsonnew.BasicJsonProtocol
  with dotty.tools.sbtplugin.codec.BuildTargetIdentifierFormats
  with dotty.tools.sbtplugin.codec.TestIdentifierFormats
  with dotty.tools.sbtplugin.codec.ListTestsParamsFormats
  with dotty.tools.sbtplugin.codec.ListTestsItemFormats
  with dotty.tools.sbtplugin.codec.ListTestsResultsFormats
  with dotty.tools.sbtplugin.codec.RunTestsParamsFormats
  with dotty.tools.sbtplugin.codec.RunTestsResultFormats
object JsonProtocol extends JsonProtocol