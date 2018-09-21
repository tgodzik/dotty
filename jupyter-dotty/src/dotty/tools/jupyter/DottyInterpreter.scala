package dotty.tools
package jupyter

import almond.interpreter.{ExecuteResult, Interpreter}
import almond.interpreter.api.{DisplayData, OutputHandler}
import almond.interpreter.input.InputManager
import almond.protocol.KernelInfo

class DottyInterpreter extends Interpreter {
  // To implement
  def kernelInfo: KernelInfo = ???
  def currentLine: Int = ???
  def execute(
    code: String,
    storeHistory: Boolean,
    inputManager: Option[InputManager],
    outputHandler: Option[OutputHandler]
  ): ExecuteResult = ???
}
