package tasty4scalac.ast


trait ASTConstants[Constant] {

  def isUnit(constant: Constant): Boolean

  def isBoolean(constant: Constant): Boolean

  def getBoolean(constant: Constant): Boolean

  def isByte(constant: Constant): Boolean

  def getByte(constant: Constant): Byte
}

trait ASTNames[Name, TermName <: Name, TypeName <: Name] {

  def copyFromChrs(start: Int, length: Int): Array[Byte]

  def isNotWildcardName(name: Name): Boolean

  def toTermName(name: Name): TermName

  def toTermName(name: String): TermName

  def start(name: Name): Int

  def length(name: Name): Int

  def isNotEmpty(name: Name): Boolean

  def getTermKindTag(termName: TermName): Int

  protected def signedName(termName: TermName): (TermName, List[TypeName], TypeName)

  protected def isSignedName(termName: TermName): Boolean

  def isSimpleName(termName: TermName) : Boolean

  object SignedName {
    def unapply(termName: TermName): Option[(TermName, List[TypeName], TypeName)] = if (isSignedName(termName)) Some(signedName(termName)) else None
  }

  protected def anyQualifiedName(termName: TermName): (TermName, Name)

  protected def isAnyQualifiedName(termName: TermName): Boolean

  object AnyQualifiedName {
    def unapply(termName: TermName): Option[(TermName, Name)] = if (isAnyQualifiedName(termName)) Some(anyQualifiedName(termName)) else None
  }

  protected def anyUniqueName(termName: TermName): (TermName, String, Int)

  protected def isAnyUniqueName(termName: TermName): Boolean

  object AnyUniqueName {
    def unapply(termName: TermName): Option[(TermName, String, Int)] = if (isAnyUniqueName(termName)) Some(anyUniqueName(termName)) else None
  }

  protected def derivedName(termName: TermName): TermName

  protected def isDerivedName(termName: TermName): Boolean

  object DerivedName {
    def unapply(termName: TermName): Option[TermName] = if (isDerivedName(termName)) Some(derivedName(termName)) else None
  }

  protected def anyNumberedName(termName: TermName): (TermName, Int)

  protected def isAnyNumberedName(termName: TermName): Boolean

  object AnyNumberedName {
    def unapply(termName: TermName): Option[(TermName, Int)] = if (isAnyNumberedName(termName)) Some(anyNumberedName(termName)) else None
  }

}

trait ASTSymbols[Symbol, Context, Name] {

  def isPackage(symbol: Symbol)(implicit context: Context): Boolean

  def fullName(symbol: Symbol)(implicit context: Context): Name

  def name(symbol: Symbol)(implicit context: Context): Name

  def isEffectiveRoot(symbol: Symbol)(implicit context: Context): Boolean
}

trait ASTTypes[Type, Context, Constant, Symbol, Annotation, ParamRef] {

  def stripTypeVar(tpe: Type)(implicit ctx: Context): Type

  def binder(tpe: ParamRef): Type

  def paramNum(tpe: ParamRef): Int

  protected def isConstant(tpe: Type): Boolean

  protected def getConstant(tpe: Type): Constant

  object ConstantType {
    def unapply(arg: Type): Option[Constant] = if (isConstant(arg)) Some(getConstant(arg)) else None
  }

  protected def isThisType(tpe: Type): Boolean

  protected def getThisType(tpe: Type): (Type, Symbol)

  object ThisType {
    def unapply(arg: Type): Option[(Type, Symbol)] = if (isThisType(arg)) Some(getThisType(arg)) else None
  }

  protected def isAnnotatedType(tpe: Type): Boolean

  protected def getAnnotatedType(tpe: Type): (Type, Annotation)

  object AnnotatedType {
    def unapply(arg: Type): Option[(Type, Annotation)] = if (isAnnotatedType(arg)) Some(getAnnotatedType(arg)) else None
  }

  def isMethodType(tpe: Type): Boolean

  def isContextualMethod(tpe: Type): Boolean

  def isImplicitMethod(tpe: Type): Boolean

  def isErasedMethod(tpe: Type): Boolean

  object MethodType {
    def unapply(arg: Type): Option[(Boolean, Boolean, Boolean)] = {
      if (isMethodType(arg)) Some(isContextualMethod(arg), isImplicitMethod(arg), isErasedMethod(arg)) else None
    }
  }

  def isParamRef(tpe: Type): Boolean

  def paramRef(tpe: Type) : ParamRef

}

trait ASTTranslator[A <: AST] {

  val constants: ASTConstants[A#Constant]

  val names: ASTNames[A#Name, A#TermName, A#TypeName]

  val symbols: ASTSymbols[A#Symbol, A#Context, A#Name]

  val types: ASTTypes[A#Type, A#Context, A#Constant, A#Symbol, A#Annotation, A#ParamRef]

  def emptyTree: A#Tree

  def getTree(annotation: A#Annotation)(implicit ctx: A#Context): A#Tree

  // TODO is Template/class, Hole or type
  def shouldPickleTree(tree: A#Tree): Boolean

  def isType(tree: A#Tree): Boolean

  def isTermRef(tpe: A#Type): Boolean

  def withTypeRef(tree: A#Tree, tpe: A#Type)(implicit ctx: A#Context): A#Tree

  def getTpe(tree: A#Tree): A#Type

  def getSymbol(tree: A#Tree)(implicit ctx: A#Context): A#Symbol

  def isEmpty(tree: A#Tree): Boolean

  protected def isIdent(tree: A#Tree): Boolean

  object Ident {
    def unapply(tree: A#Tree): Option[(A#Name, A#Type)] = if (isIdent(tree)) Some(getIdentName(tree), getTpe(tree)) else None
  }

  protected def getIdentName(tree: A#Tree): A#Name

  protected def isThis(tree: A#Tree): Boolean

  protected def getThisQual(tree: A#Tree): A#Tree

  object This {
    def unapply(tree: A#Tree): Option[A#Tree] = if (isThis(tree)) Some(getThisQual(tree)) else None
  }

  protected def isMemberDef(tree: A#Tree): Boolean

  object MemberDef {
    def unapply(tree: A#Tree)(implicit ctx : A#Context): Option[A#Symbol] = if (isMemberDef(tree)) Some(getSymbol(tree)) else None
  }

  def isValDef(tpe: A#Tree): Boolean

  def getValDef(tpe: A#Tree)(implicit ctx: A#Context): (A#Symbol, A#Tree, A#Tree)

  object ValDef {
    def unapply(arg: A#Tree)(implicit ctx: A#Context): Option[(A#Symbol, A#Tree, A#Tree)] = if (isValDef(arg)) Some(getValDef(arg)) else None
  }

  def isDefDef(tpe: A#Tree): Boolean

  def getDefDef(tpe: A#Tree)(implicit ctx: A#Context): (A#Symbol, A#Tree, A#Tree, List[A#Tree], List[List[A#Tree]])

  object DefDef {
    def unapply(arg: A#Tree)(implicit ctx: A#Context): Option[(A#Symbol, A#Tree, A#Tree, List[A#Tree], List[List[A#Tree]])] = {
      if (isDefDef(arg)) Some(getDefDef(arg)) else None
    }
  }

  def isTypeDef(tpe: A#Tree): Boolean

  def getTypeDef(tpe: A#Tree)(implicit ctx: A#Context): (A#Symbol, A#Tree)

  object TypeDef {
    def unapply(arg: A#Tree)(implicit ctx: A#Context): Option[(A#Symbol, A#Tree)] = if (isTypeDef(arg)) Some(getTypeDef(arg)) else None
  }

  def isPackageDef(tree: A#Tree): Boolean

  def getPackageDef(tree: A#Tree): (A#Type, List[A#Tree])

  object PackageDef {
    def unapply(arg: A#Tree): Option[(A#Type, List[A#Tree])] = if (isPackageDef(arg)) Some(getPackageDef(arg)) else None
  }

}

