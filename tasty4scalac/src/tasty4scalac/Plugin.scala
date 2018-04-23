package tasty4scalac

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin => NscPlugin, PluginComponent => NscPluginComponent}
import scala.collection.mutable
import scala.tools.nsc.SubComponent
import scala.tools.nsc.Phase
import scala.tools.nsc.backend.jvm.GenBCode
import scala.tools.nsc.typechecker.Analyzer
import scala.tools.nsc.transform.Erasure

import dotty.tools.dotc.core.tasty.TastyPrinter

import java.io._
import java.nio.file._

class Plugin(val global: Global) extends NscPlugin { self =>
  val name = "tasty"
  val description = "Pickles Scala trees (tasty format)."
  val components = List[NscPluginComponent](TastyComponent)

  def changePhasesOrder(runsAfterPhase: String, phaseClass: Class[_], fieldToModify: Object) = {
    val newRunsAfter = List(runsAfterPhase)
    val runsAfterField = phaseClass.getDeclaredField("runsAfter")
    runsAfterField.setAccessible(true)
    runsAfterField.set(fieldToModify, newRunsAfter)
  }

  //update the order of phases
  //typer, superaccessors
  changePhasesOrder("typer", classOf[scala.tools.nsc.Global$superAccessors$], global.superAccessors)

  //superaccessors, patmat
  changePhasesOrder("superaccessors", classOf[scala.tools.nsc.Global$patmat$], global.patmat)

  //patmat, extensionMethods
  changePhasesOrder("patmat", classOf[scala.tools.nsc.Global$extensionMethods$], global.extensionMethods)


  object TastyComponent extends {
    val global: self.global.type = self.global
  } with NscPluginComponent {
    override val runsAfter = List("superaccessors")
    override val runsRightAfter = Some("superaccessors")
    val phaseName = "tasty"
    override def description = "pickle tasty trees"

    import global._

    override def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
      override def apply(unit: CompilationUnit): Unit = {
        val tree = unit.body
        assert(!unit.isJava)

        println("Hello: " + tree)
        val pickler = new ScalacTastyPickler(global)
        val treePkl = pickler.treePkl
        treePkl.pickle(List(tree.asInstanceOf[treePkl.g.Tree]))

        treePkl.compactify()
        val pickled = pickler.assembleParts()

        val ctx = (new dotty.tools.dotc.core.Contexts.ContextBase).initialCtx
        new TastyPrinter(pickled)(ctx).printContents()

        val dir = Paths.get("mine/collection/immutable")
        Files.createDirectories(dir)
        val path = dir.resolve("Vector.tasty")
        Files.write(path, pickled)

        // dotty.tools.dotc.core.Main.process("-from-tasty"
      }
    }
  }
}
