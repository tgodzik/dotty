package dotty.tools
package jupyter

import java.net.{URLClassLoader, URL}

import almond.util.ThreadUtil.singleThreadedExecutionContext
import almond.channels.zeromq.ZeromqThreads
import almond.kernel.install.{Install, Options}
import almond.kernel.{Kernel, KernelThreads}
import almond.logger.{Level, LoggerContext}

// TODO Could not use CaseApp[Options]:
// [error] 13 |object DottyKernel extends CaseApp[Options] {
// [error]    |                                           ^
// [error]    |no implicit argument of type caseapp.core.parser.Parser[dotty.tools.jupyter.Options] was found for parameter parser of constructor CaseApp in class CaseApp.
// [error]    |I found:
// [error]    |
// [error]    |    caseapp.core.parser.Parser.generic[dotty.tools.jupyter.Options, L, D, N, V, M, H
// [error]    |      ,
// [error]    |    R, P](caseapp.util.LowPriority.materialize,
// [error]    |      shapeless.LabelledGeneric.materializeProduct[dotty.tools.jupyter.Options,
// [error]    |        shapeless.DefaultSymbolicLabelling[dotty.tools.jupyter.Options]#Out
// [error]    |      , V, L](
// [error]    |        shapeless.DefaultSymbolicLabelling.mkDefaultSymbolicLabelling[
// [error]    |          dotty.tools.jupyter.Options
// [error]    |        ]
// [error]    |      , shapeless.Generic.materialize[dotty.tools.jupyter.Options, V],
// [error]    |        /* missing */implicitly[<notype>]
// [error]    |      , ???)
// [error]    |    , ???, ???, ???, ???, ???, ???, ???)
// [error]    |
// [error]    |But no implicit values were found that match expected type.
// [error] one error found
// [error] (jupyter-dotty / Compile / compileIncremental) Compilation failed

// import caseapp._

object DottyKernel /* extends CaseApp[Options] */ {
  // def run(options: Options, args: RemainingArgs): Unit = {

  // }

  def main(args: Array[String]): Unit = {
    val argsStr = args.mkString(",")
    println(s"Arguments: $argsStr")

    if (args.length >= 1 && args(0) == "--install")
      Install.installOrError(
        defaultId = "dotty",
        defaultDisplayName = "Dotty",
        language = "scala",
        options = Options(force = true)
      ) match {
        case Left(e) =>
          Console.err.println(s"Error: $e")
          sys.exit(1)
        case Right(dir) =>
          println(s"Installed dotty kernel under $dir")
          sys.exit(0)
      }

    // TODO better args
    val connectionFile = args(1)

    val logCtx = LoggerContext.stderr(Level.Warning)
    val zeromqThreads = ZeromqThreads.create("dotty-kernel")
    val kernelThreads = KernelThreads.create("dotty-kernel")
    val interpreterDotty = singleThreadedExecutionContext("dotty-interpreter")

    // TODO Solve classloader problem
    lazy val ctxURLs = java.lang.Thread.currentThread.getContextClassLoader match {
      case cl: java.net.URLClassLoader => cl.getURLs.toList
      case _ => List[Nothing]()
    }
    // lazy val classpath = urlsz map {_.toString}
    // println(s"Classpath is $classpath")
    val libURLs = List("file:/home/cranium/Documents/EPFL/MA3/dotty-jupyter/dotty/dist-bootstrapped/target/pack/lib/dotty-library_0.10-0.10.0-bin-SNAPSHOT.jar", "file:/home/cranium/Documents/EPFL/MA3/dotty-jupyter/dotty/dist-bootstrapped/target/pack/lib/scala-library-2.12.6.jar", "file:/home/cranium/Documents/EPFL/MA3/dotty-jupyter/dotty/")

    val defaultLoader = new URLClassLoader((ctxURLs ++ libURLs.map(new URL(_))).toArray)
    // val defaultLoader = Thread.currentThread().getContextClassLoader

    Kernel.create(new DottyInterpreter(None), interpreterDotty, kernelThreads, logCtx)
      .flatMap(_.runOnConnectionFile(connectionFile, "dotty", zeromqThreads))
      .unsafeRunSync()

  }
}
