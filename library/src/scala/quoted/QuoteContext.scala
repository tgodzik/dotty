package scala.quoted

import scala.annotation.{implicitAmbiguous, implicitNotFound}

// TODO add @implicitAmbiguous("...")
// TODO add some fake default and give better error message in ReifyQuotes
@implicitNotFound("Could not find an implicit QuotedContex.\nIf this is a method that returns an `Expr[T]` you can use `Staged[T]` instead.\n\nQuotedContex is provided inside of top level splices in `inline` macros or within a call to `Toolbox.run`.\n")
trait QuoteContext extends scala.runtime.quoted.Unpickler {
   def show[T](expr: Expr[T]): String
   def show[T](tpe: Type[T]): String
}

object QuoteContext {
   /** Compiler QuoteContext available in a top level ~ of an inline macro */
   def macroContext: QuoteContext = throw new Exception("Not in inline macro.")
}

trait MacroContext extends QuoteContext {
  // def reflect: Tasty
}
