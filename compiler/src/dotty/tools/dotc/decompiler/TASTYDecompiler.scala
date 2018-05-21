package dotty.tools.dotc.decompiler

import java.io.{OutputStream, PrintStream}

import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Contexts
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.tasty.TastyPrinter
import dotty.tools.dotc.printing.DecompilerPrinter
import dotty.tools.dotc.tasty.{TASTYReflector, TastyImpl}
import dotty.tools.io.File

/** Compiler from tasty to user readable high text representation
 *  of the compiled scala code.
 *
 * @author Nicolas Stucki
 */
class TASTYDecompiler(pageWidth: Int, printLines: Boolean, printTasty: Boolean, outputDir: String) extends TASTYReflector {

  override def reflect(tree: tpd.Tree)(implicit ctx: Contexts.Context): Unit = {
    if (outputDir == ".") printToOutput(System.out, tree)
    else {
      var os: OutputStream = null
      var ps: PrintStream = null
      try {
        os = File(outputDir + "/decompiled.scala").outputStream(append = true)
        ps = new PrintStream(os)
        printToOutput(ps, tree)
      } finally {
        if (os ne null) os.close()
        if (ps ne null) ps.close()
      }
    }
  }

  private def printToOutput(out: PrintStream, tree: tpd.Tree)(implicit ctx: Context): Unit = {
    val unit = ctx.compilationUnit

    out.println(s"/** Decompiled from $unit */")
    out.print(TastyImpl.showSourceCode.showTree(tree))

    if (printTasty) { // TODO split decompiler from -print-tasty
      out.println("/*")
      new TastyPrinter(unit.pickled.head._2).printContents()
      out.println("*/")
    }
  }
}
