package tasty4scalac

import dotty.tools.dotc.core.Comments.{ContextDoc, ContextDocstrings}
import dotty.tools.dotc.core.Contexts
import dotty.tools.dotc.core.Contexts.ContextBase
import dotty.tools.dotc.reporting.Reporter.NoReporter
import dotty.tools.io.VirtualDirectory
import tasty4scalac.Compiler.Factory

final class Dotty(val ctx: Contexts.Context) extends dotty.tools.dotc.Compiler with Compiler {
  override def compile(code: String): Unit = newRun(ctx.fresh).compile(code)
}

object Dotty extends Factory {
  private val classpath = System.getProperty("dotty.classpath")

  override def apply(): Compiler = {
    implicit val ctx: Contexts.FreshContext = new ContextBase().initialCtx.fresh
    ctx.setSetting(ctx.settings.classpath, classpath)
    ctx.setSetting(ctx.settings.YtestPickler, true)
    ctx.setSetting(ctx.settings.encoding, "UTF8")
    ctx.setSetting(ctx.settings.outputDir, new VirtualDirectory("<quote compilation output>"))

    ctx.setReporter(NoReporter)
    ctx.setProperty(ContextDoc, new ContextDocstrings)
    new Dotty(ctx)
  }
}