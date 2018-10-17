package dotty.tools.dotc.quoted

import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.Driver
import dotty.tools.dotc.core.Contexts.{Context, ContextBase, FreshContext}
import dotty.tools.dotc.tastyreflect.TastyImpl
import dotty.tools.io.{AbstractFile, Directory, PlainDirectory, VirtualDirectory}
import dotty.tools.repl.AbstractFileClassLoader

import scala.quoted.{Expr, QuoteContext, Type}
import scala.quoted.Toolbox
import java.net.URLClassLoader

import dotty.tools.dotc.core.quoted.PickledQuotes

class QuoteDriver extends Driver {

  private[this] val contextBase: ContextBase = new ContextBase

  def run[T](code: QuoteContext => Expr[T], settings: Toolbox.Settings): T = {
    val outDir: AbstractFile = settings.outDir match {
      case Some(out) =>
        val dir = Directory(out)
        dir.createDirectory()
        new PlainDirectory(Directory(out))
      case None =>
        new VirtualDirectory("<quote compilation output>")
    }

    val (_, ctx0: Context) = setup(settings.compilerArgs.toArray :+ "dummy.scala", initCtx.fresh)
    val ctx = setQuoteSettings(ctx0.fresh.setSetting(ctx0.settings.outputDir, outDir), settings)

    val driver = new QuoteCompiler
    driver.newRun(ctx).compileExpr(code)

    val classLoader = new AbstractFileClassLoader(outDir, this.getClass.getClassLoader)

    val clazz = classLoader.loadClass(driver.outputClassName.toString)
    val method = clazz.getMethod("apply")
    val instance = clazz.getConstructor().newInstance()

    method.invoke(instance).asInstanceOf[T]
  }

  def show(expr: Expr[_], settings: Toolbox.Settings): String = {
    val (_, ctx: Context) = setup(settings.compilerArgs.toArray :+ "dummy.scala", initCtx.fresh)
    val tree = PickledQuotes.quotedExprToTree(expr)(ctx)
    val tree1 = if (settings.showRawTree) tree else (new TreeCleaner).transform(tree)(ctx)
    new TastyImpl(ctx).showSourceCode.showTree(tree1)(ctx)
  }

  def show(tpe: Type[_], settings: Toolbox.Settings): String = {
    val (_, ctx: Context) = setup(settings.compilerArgs.toArray :+ "dummy.scala", initCtx.fresh)
    val tree = PickledQuotes.quotedTypeToTree(tpe)(ctx)
    val tree1 = if (settings.showRawTree) tree else (new TreeCleaner).transform(tree)(ctx)
    new TastyImpl(ctx).showSourceCode.showTypeOrBoundsTree(tree1)(ctx)
  }

  override def initCtx: Context = {
    val ictx = contextBase.initialCtx
    ictx.settings.classpath.update(QuoteDriver.currentClasspath)(ictx)
    ictx
  }

  private def setQuoteSettings(ctx: FreshContext, settings: Toolbox.Settings): ctx.type = {
    ctx.setSetting(ctx.settings.color, if (settings.color) "always" else "never")
    ctx.setSetting(ctx.settings.YshowRawTree, settings.showRawTree)
  }

}

object QuoteDriver {

  def currentClasspath: String = {
    val classpath0 = System.getProperty("java.class.path")
    this.getClass.getClassLoader match {
      case cl: URLClassLoader =>
        // Loads the classes loaded by this class loader
        // When executing `run` or `test` in sbt the classpath is not in the property java.class.path
        val newClasspath = cl.getURLs.map(_.getFile())
        newClasspath.mkString("", java.io.File.pathSeparator, if (classpath0 == "") "" else java.io.File.pathSeparator + classpath0)
      case _ => classpath0
    }
  }

}
