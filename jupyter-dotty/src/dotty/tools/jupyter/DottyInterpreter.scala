package dotty.tools
package jupyter

import java.io.{ByteArrayOutputStream, PrintStream}

import dotty.tools.repl.{State, ReplDriver}

import almond.interpreter.{ExecuteResult, Interpreter}
import almond.interpreter.api.{DisplayData, OutputHandler}
import almond.interpreter.input.InputManager
import almond.protocol.KernelInfo

class DottyInterpreter(classLoader: Option[ClassLoader]) extends Interpreter {

  // To implement
  def kernelInfo(): KernelInfo = KernelInfo(
    implementation="dotty",
    implementation_version="0.1",
    language_info=KernelInfo.LanguageInfo(
      name="dotty",
      version="2.14.0", // TODO ?
      mimetype="text/scala",
      file_extension=".scala",
      nbconvert_exporter="", // TODO ?
      pygments_lexer=None, // TODO ?
      codemirror_mode=None, // TODO ?
    ),
    banner="Dotty kernel"
    // helper_links=None,
  )

  def currentLine: Int = count
  @volatile private var count = 0
  private val buf = new ByteArrayOutputStream
  private val replDriver = new ReplDriver(settings=Array[String]("-usejavacp"), // TODO What are the settings ?
                                          out=new PrintStream(buf),
                                          classLoader=classLoader) // TODO

  private var currState = replDriver.initialState

  def execute(
    code: String,
    storeHistory: Boolean,
    inputManager: Option[InputManager],
    outputHandler: Option[OutputHandler]
  ): ExecuteResult = {
    currState = replDriver.run(code)(currState)

    count += 1
    val out = buf.toString("utf-8")
    buf.reset

    ExecuteResult.Success(
      DisplayData.text(out)
    )
  }
}
