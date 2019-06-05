package dotty.tools.dotc
package transform

import core._
import Names._
import StdNames.{nme, tpnme}
import Types._
import dotty.tools.dotc.transform.MegaPhase._
import Flags._
import Contexts.Context
import Symbols._
import Constants._
import Decorators._
import DenotTransformers._

object CompleteJavaEnums {
  val name: String = "completeJavaEnums"

  private val nameParamName: TermName = "$name".toTermName
  private val ordinalParamName: TermName = "$ordinal".toTermName
}

/** For Scala enums that inherit from java.lang.Enum:
 *  Add constructor parameters for `name` and `ordinal` to pass from each
 *  case to the java.lang.Enum class.
 */
class CompleteJavaEnums extends MiniPhase with InfoTransformer { thisPhase =>
  import CompleteJavaEnums._
  import ast.tpd._

  override def phaseName: String = CompleteJavaEnums.name

  override def relaxedTypingInGroup: Boolean = true
    // Because it adds additional parameters to some constructors

  def transformInfo(tp: Type, sym: Symbol)(implicit ctx: Context): Type =
    if (sym.isConstructor && derivesFromJEnum(sym.owner)) addConstrParams(sym.info)
    else tp

  /** Is `sym` a Scala enum class that derives (directly) from `java.lang.Enum`?
   */
  private def derivesFromJEnum(sym: Symbol)(implicit ctx: Context) =
    sym.is(Enum, butNot = Case) &&
    sym.info.parents.exists(p => p.typeSymbol == defn.JEnumClass)

  /** Add constructor parameters `$name: String` and `$ordinal: Int` to the end of
   *  the last parameter list of (method- or poly-) type `tp`.
   */
  private def addConstrParams(tp: Type)(implicit ctx: Context): Type = tp match {
    case tp: PolyType =>
      tp.derivedLambdaType(resType = addConstrParams(tp.resType))
    case tp: MethodType =>
      tp.resType match {
        case restpe: MethodType =>
          tp.derivedLambdaType(resType = addConstrParams(restpe))
        case _ =>
          tp.derivedLambdaType(
            paramNames = tp.paramNames ++ List(nameParamName, ordinalParamName),
            paramInfos = tp.paramInfos ++ List(defn.StringType, defn.IntType))
      }
  }

  /** The list of parameter definitions `$name: String, $ordinal: Int`, in given `owner`
   *  with given flags (either `Param` or `ParamAccessor`)
   */
  private def addedParams(owner: Symbol, flag: FlagSet)(implicit ctx: Context): List[ValDef] = {
    val nameParam = ctx.newSymbol(owner, nameParamName, flag | Synthetic, defn.StringType, coord = owner.span)
    val ordinalParam = ctx.newSymbol(owner, ordinalParamName, flag | Synthetic, defn.IntType, coord = owner.span)
    List(ValDef(nameParam), ValDef(ordinalParam))
  }

  /** Add arguments `args` to the parent constructor application in `parents` that invokes
   *  a constructor of `targetCls`,
   */
  private def addEnumConstrArgs(targetCls: Symbol, parents: List[Tree], args: List[Tree])(implicit ctx: Context): List[Tree] =
    parents.map {
      case app @ Apply(fn, args0) if fn.symbol.owner == targetCls => cpy.Apply(app)(fn, args0 ++ args)
      case p => p
    }

  /** 1. If this is a constructor of a enum class that extends, add $name and $ordinal parameters to it.
   *
   *  2. If this is a $new method that creates simple cases, pass $name and $ordinal parameters
   *     to the enum superclass. The $new method looks like this:
   *
   *       def $new(..., enumTag: Int, name: String) = {
   *         class $anon extends E(...) { ... }
   *         new $anon
   *       }
   *
   *     After the transform it is expanded to
   *
   *       def $new(..., enumTag: Int, name: String) = {
   *         class $anon extends E(..., name, enumTag) { ... }
   *         new $anon
   *       }
   */
  override def transformDefDef(tree: DefDef)(implicit ctx: Context): DefDef = {
    val sym = tree.symbol
    if (sym.isConstructor && derivesFromJEnum(sym.owner))
      cpy.DefDef(tree)(
        vparamss = tree.vparamss.init :+ (tree.vparamss.last ++ addedParams(sym, Param)))
    else if (sym.name == nme.DOLLAR_NEW && derivesFromJEnum(sym.owner.linkedClass)) {
      val Block((tdef @ TypeDef(tpnme.ANON_CLASS, templ: Template)) :: Nil, call) = tree.rhs
      val args = tree.vparamss.last.takeRight(2).map(param => ref(param.symbol)).reverse
      val templ1 = cpy.Template(templ)(
        parents = addEnumConstrArgs(sym.owner.linkedClass, templ.parents, args))
      cpy.DefDef(tree)(
        rhs = cpy.Block(tree.rhs)(cpy.TypeDef(tdef)(tdef.name, templ1) :: Nil, call))
    }
    else tree
  }

  /** 1. If this is an enum class, add $name and $ordinal parameters to its
   *     parameter accessors and pass them on to the java.lang.Enum constructor,
   *     replacing the dummy arguments that were passed before.
   *
   *  2. If this is an anonymous class that implement a value enum case,
   *     pass $name and $ordinal parameters to the enum superclass. The class
   *     looks like this:
   *
   *       class $anon extends E(...) {
   *          ...
   *          def enumTag = N
   *          def toString = S
   *          ...
   *       }
   *
   *     After the transform it is expanded to
   *
   *       class $anon extends E(..., N, S) {
   *         "same as before"
   *       }
   */
  override def transformTemplate(templ: Template)(implicit ctx: Context): Template = {
    val cls = templ.symbol.owner
    if (derivesFromJEnum(cls)) {
      val (params, rest) = decomposeTemplateBody(templ.body)
      val addedDefs = addedParams(cls, ParamAccessor)
      val addedSyms = addedDefs.map(_.symbol.entered)
      val parents1 = templ.parents.map {
        case app @ Apply(fn, _) if fn.symbol.owner == defn.JEnumClass =>
          cpy.Apply(app)(fn, addedSyms.map(ref))
        case p => p
      }
      cpy.Template(templ)(
        parents = parents1,
        body = params ++ addedDefs ++ rest)
    }
    else if (cls.isAnonymousClass && cls.owner.is(EnumCase) && derivesFromJEnum(cls.owner.owner.linkedClass)) {
      def rhsOf(name: TermName) =
        templ.body.collect {
          case mdef: DefDef if mdef.name == name => mdef.rhs
        }.head
      val args = List(rhsOf(nme.toString_), rhsOf(nme.enumTag))
      cpy.Template(templ)(
        parents = addEnumConstrArgs(cls.owner.owner.linkedClass, templ.parents, args))
    }
    else templ
  }
}
