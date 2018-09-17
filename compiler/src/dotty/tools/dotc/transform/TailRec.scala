package dotty.tools.dotc
package transform

import ast.Trees._
import ast.{TreeTypeMap, tpd}
import core._
import Flags._
import Constants.Constant
import Contexts.Context
import Decorators._
import Symbols._
import StdNames.nme
import Types._
import config.Printers.tailrec
import NameKinds.{TailLabelName, TailLocalName, TailTempName}
import MegaPhase.MiniPhase
import reporting.diagnostic.messages.TailrecNotApplicable

/**
 * A Tail Rec Transformer
 * @author     Erik Stenman, Iulian Dragos,
 *             ported and heavily modified for dotty by Dmitry Petrashko
 *             moved after erasure and adapted to emit `Labeled` blocks
 *             by Sébastien Doeraene
 * @version    1.1
 *
 *             What it does:
 *             <p>
 *             Finds method calls in tail-position and replaces them with jumps.
 *             A call is in a tail-position if it is the last instruction to be
 *             executed in the body of a method. This includes being in
 *             tail-position of a `return` from a `Labeled` block which is itself
 *             in tail-position (which is critical for tail-recursive calls in the
 *             cases of a `match`). To identify tail positions, we recurse over
 *             the trees that may contain calls in tail-position (trees that can't
 *             contain such calls are not transformed).
 *             </p>
 *             <p>
 *             When a method contains at least one tail-recursive call, its rhs
 *             is wrapped in the following structure:
 *             </p>
 *             <pre>
 *             var localForParam1: T1 = param1
 *             ...
 *             while (true) {
 *               tailResult[ResultType]: {
 *                 return {
 *                   // original rhs
 *                 }
 *               }
 *             }
 *             </pre>
 *             <p>
 *             Self-recursive calls in tail-position are then replaced by (a)
 *             reassigning the local `var`s substituting formal parameters and
 *             (b) a `return` from the `tailResult` labeled block, which has the
 *             net effect of looping back to the beginning of the method.
 *             </p>
 *             <p>
 *             As the JVM provides no way to jump from a method to another one,
 *             non-recursive calls in tail-position are not optimized.
 *             </p>
 *             <p>
 *             A method call is self-recursive if it calls the current method and
 *             the method is final (otherwise, it could
 *             be a call to an overridden method in a subclass).
 *             Recursive calls on a different instance are optimized. Since 'this'
 *             is not a local variable it is added as a label parameter.
 *             </p>
 *             <p>
 *             This phase has been moved after erasure to allow the use of vars
 *             for the parameters combined with a `WhileDo`. This is also
 *             beneficial to support polymorphic tail-recursive calls.
 *             </p>
 *             <p>
 *             In scalac, if the method had type parameters, the call must contain
 *             the same parameters as type arguments. This is no longer the case in
 *             dotc thanks to being located after erasure.
 *             In scalac, this is named tailCall but it does only provide optimization for
 *             self recursive functions, that's why it's renamed to tailrec
 *             </p>
 */
class TailRec extends MiniPhase {
  import tpd._

  override def phaseName: String = TailRec.name

  override def runsAfter: Set[String] = Set(Erasure.name) // tailrec assumes erased types

  override def transformDefDef(tree: tpd.DefDef)(implicit ctx: Context): Tree = {
    val method = tree.symbol
    val mandatory = method.hasAnnotation(defn.TailrecAnnot)
    def noTailTransform = {
      // FIXME: want to report this error on `tree.namePos`, but
      // because of extension method getting a weird pos, it is
      // better to report on methodbol so there's no overlap
      if (mandatory)
        ctx.error(TailrecNotApplicable(method), method.pos)
      tree
    }

    val isCandidate = method.isEffectivelyFinal &&
      !((method is Accessor) || (tree.rhs eq EmptyTree) || (method is Label))

    if (isCandidate) {
      val DefDef(name, Nil, vparams :: Nil, _, _) = tree
      val enclosingClass = method.enclosingClass.asClass
      // Note: this can be split in two separate transforms(in different groups),
      // than first one will collect info about which transformations and rewritings should be applied
      // and second one will actually apply,
      // now this speculatively transforms tree and throws away result in many cases
      val transformer = new TailRecElimination(method, enclosingClass, vparams.map(_.symbol), mandatory)
      val rhsSemiTransformed = transformer.transform(tree.rhs)

      if (transformer.rewrote) {
        val varForRewrittenThis = transformer.varForRewrittenThis
        val rewrittenParamSyms = transformer.rewrittenParamSyms
        val varsForRewrittenParamSyms = transformer.varsForRewrittenParamSyms

        val initialValDefs = {
          val initialParamValDefs = for ((param, local) <- rewrittenParamSyms.zip(varsForRewrittenParamSyms)) yield {
            ValDef(local.asTerm, ref(param))
          }
          varForRewrittenThis match {
            case Some(local) => ValDef(local.asTerm, This(tree.symbol.owner.asClass)) :: initialParamValDefs
            case none => initialParamValDefs
          }
        }

        val rhsFullyTransformed = varForRewrittenThis match {
          case Some(localThisSym) =>
            val classSym = tree.symbol.owner.asClass
            val thisRef = localThisSym.termRef
            new TreeTypeMap(
              typeMap = _.substThisUnlessStatic(classSym, thisRef)
                .subst(rewrittenParamSyms, varsForRewrittenParamSyms.map(_.termRef)),
              treeMap = {
                case tree: This if tree.symbol == classSym => Ident(thisRef)
                case tree => tree
              }
            ).transform(rhsSemiTransformed)

          case none =>
            new TreeTypeMap(
              typeMap = _.subst(rewrittenParamSyms, varsForRewrittenParamSyms.map(_.termRef))
            ).transform(rhsSemiTransformed)
        }

        cpy.DefDef(tree)(rhs =
          Block(
            initialValDefs :::
            WhileDo(Literal(Constant(true)), {
              Labeled(transformer.continueLabel.get.asTerm, {
                Return(rhsFullyTransformed, ref(method))
              })
            }) :: Nil,
            Throw(Literal(Constant(null))) // unreachable code
          )
        )
      } else noTailTransform
    }
    else noTailTransform
  }

  private class TailRecElimination(method: Symbol, enclosingClass: ClassSymbol, paramSyms: List[Symbol], isMandatory: Boolean) extends TreeMap {
    import tpd._

    var rewrote: Boolean = false

    var continueLabel: Option[Symbol] = None
    var varForRewrittenThis: Option[Symbol] = None
    var rewrittenParamSyms: List[Symbol] = Nil
    var varsForRewrittenParamSyms: List[Symbol] = Nil

    private def getContinueLabel()(implicit ctx: Context): Symbol = {
      continueLabel match {
        case Some(sym) => sym
        case none =>
          val sym = ctx.newSymbol(method, TailLabelName.fresh(), Label, defn.UnitType)
          continueLabel = Some(sym)
          sym
      }
    }

    private def getVarForRewrittenThis()(implicit ctx: Context): Symbol = {
      varForRewrittenThis match {
        case Some(sym) => sym
        case none =>
          val tpe =
            if (enclosingClass.is(Module)) enclosingClass.thisType
            else enclosingClass.asClass.classInfo.selfType
          val sym = ctx.newSymbol(method, nme.SELF, Synthetic | Mutable, tpe)
          varForRewrittenThis = Some(sym)
          sym
      }
    }

    private def getVarForRewrittenParam(param: Symbol)(implicit ctx: Context): Symbol = {
      rewrittenParamSyms.indexOf(param) match {
        case -1 =>
          val sym = ctx.newSymbol(method, TailLocalName.fresh(param.name.toTermName), Synthetic | Mutable, param.info)
          rewrittenParamSyms ::= param
          varsForRewrittenParamSyms ::= sym
          sym
        case index => varsForRewrittenParamSyms(index)
      }
    }

    /** Symbols of Labeled blocks that are in tail position. */
    private val tailPositionLabeledSyms = new collection.mutable.HashSet[Symbol]()

    private[this] var inTailPosition = true

    /** Rewrite this tree to contain no tail recursive calls */
    def transform(tree: Tree, tailPosition: Boolean)(implicit ctx: Context): Tree = {
      if (inTailPosition == tailPosition) transform(tree)
      else {
        val saved = inTailPosition
        inTailPosition = tailPosition
        try transform(tree)
        finally inTailPosition = saved
      }
    }

    def yesTailTransform(tree: Tree)(implicit ctx: Context): Tree =
      transform(tree, tailPosition = true)

    def noTailTransform(tree: Tree)(implicit ctx: Context): Tree =
      transform(tree, tailPosition = false)

    def noTailTransforms[Tr <: Tree](trees: List[Tr])(implicit ctx: Context): List[Tr] =
      trees.mapConserve(noTailTransform).asInstanceOf[List[Tr]]

    override def transform(tree: Tree)(implicit ctx: Context): Tree = {
      /* Rewrite an Apply to be considered for tail call transformation. */
      def rewriteApply(tree: Apply): Tree = {
        val arguments = noTailTransforms(tree.args)

        def continue =
          cpy.Apply(tree)(noTailTransform(tree.fun), arguments)

        def fail(reason: String) = {
          if (isMandatory) ctx.error(s"Cannot rewrite recursive call: $reason", tree.pos)
          else tailrec.println("Cannot rewrite recursive call at: " + tree.pos + " because: " + reason)
          continue
        }

        val call = tree.fun.symbol
        val prefix = tree.fun match {
          case Select(qual, _) => qual
          case x: Ident if x.symbol eq method => EmptyTree
          case x => x
        }

        val isRecursiveCall = call eq method
        def isRecursiveSuperCall = (method.name eq call.name) &&
          method.matches(call) &&
          enclosingClass.appliedRef.widen <:< prefix.tpe.widenDealias

        if (isRecursiveCall) {
          if (inTailPosition) {
            tailrec.println("Rewriting tail recursive call: " + tree.pos)
            rewrote = true

            val assignParamPairs = for {
              (param, arg) <- paramSyms.zip(arguments)
              if (arg match {
                case arg: Ident => arg.symbol != param
                case _ => true
              })
            } yield {
              (getVarForRewrittenParam(param), arg)
            }

            val assignThisAndParamPairs = {
              if (prefix eq EmptyTree) assignParamPairs
              else {
                // TODO Opt: also avoid assigning `this` if the prefix is `this.`
                (getVarForRewrittenThis(), noTailTransform(prefix)) :: assignParamPairs
              }
            }

            val assignments = assignThisAndParamPairs match {
              case (lhs, rhs) :: Nil =>
                Assign(ref(lhs), rhs) :: Nil
              case _ :: _ =>
                val (tempValDefs, assigns) = (for ((lhs, rhs) <- assignThisAndParamPairs) yield {
                  val temp = ctx.newSymbol(method, TailTempName.fresh(lhs.name.toTermName), Flags.Synthetic, lhs.info)
                  (ValDef(temp, rhs), Assign(ref(lhs), ref(temp)).withPos(tree.pos))
                }).unzip
                tempValDefs ::: assigns
              case nil =>
                Nil
            }

            val tpt = TypeTree(method.info.resultType)
            Block(assignments, Typed(Return(Literal(Constant(())).withPos(tree.pos), ref(getContinueLabel())), tpt))
          }
          else fail("it is not in tail position")
        }
        else if (isRecursiveSuperCall)
          fail("it contains a recursive call targeting a supertype")
        else
          continue
      }

      def rewriteTry(tree: Try): Try = {
        val expr = noTailTransform(tree.expr)
        val hasFinalizer = tree.finalizer ne EmptyTree
        // SI-1672 Catches are in tail position when there is no finalizer
        val cases =
          if (hasFinalizer) noTailTransforms(tree.cases)
          else transformSub(tree.cases)
        val finalizer =
          if (hasFinalizer) noTailTransform(tree.finalizer)
          else EmptyTree
        cpy.Try(tree)(expr, cases, finalizer)
      }

      tree match {
        case tree@Apply(fun, args) =>
          val meth = fun.symbol
          if (meth == defn.Boolean_|| || meth == defn.Boolean_&&)
            cpy.Apply(tree)(noTailTransform(fun), transform(args))
          else
            rewriteApply(tree)

        case tree: Select =>
          cpy.Select(tree)(noTailTransform(tree.qualifier), tree.name)

        case tree@Block(stats, expr) =>
          cpy.Block(tree)(
            noTailTransforms(stats),
            transform(expr)
          )

        case tree@If(cond, thenp, elsep) =>
          cpy.If(tree)(
            noTailTransform(cond),
            transform(thenp),
            transform(elsep)
          )

        case tree@CaseDef(_, _, body) =>
          cpy.CaseDef(tree)(body = transform(body))

        case tree@Match(selector, cases) =>
          cpy.Match(tree)(
            noTailTransform(selector),
            transformSub(cases)
          )

        case tree: Try =>
          rewriteTry(tree)

        case Alternative(_) | Bind(_, _) =>
          assert(false, "We should never have gotten inside a pattern")
          tree

        case t @ DefDef(_, _, _, _, _) =>
          t // todo: could improve to handle DefDef's with a label flag calls to which are in tail position

        case ValDef(_, _, _) | EmptyTree | Super(_, _) | This(_) |
             Literal(_) | TypeTree() | TypeDef(_, _) =>
          tree

        case Labeled(bind, expr) =>
          if (inTailPosition)
            tailPositionLabeledSyms += bind.symbol
          cpy.Labeled(tree)(bind, transform(expr))

        case Return(expr, from) =>
          val fromSym = from.symbol
          val inTailPosition = fromSym.is(Label) && tailPositionLabeledSyms.contains(fromSym)
          cpy.Return(tree)(transform(expr, inTailPosition), from)

        case _ =>
          super.transform(tree)
      }
    }
  }
}

object TailRec {
  val name: String = "tailrec"
}
