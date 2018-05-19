---
layout: doc-page
title: Principled Meta Programming
---

Overview
--------

// TODO 

Quotes and splices
------------------
// TODO basics

// power example


Runtime Meta Programing (staging)
---------------------------------

// TODO

### Setting up the project

You can install a template project that is ready for runtime meta programing with: 
```
> sbt new lampepfl/dotty-quoted.g8
```

The only extra configuration that this template has is a dependency on the compiler at runtime. You can add the dependency in your SBT project adding the library dependencies like in the [template project](https://github.com/lampepfl/dotty-quoted.g8/blob/master/src/main/g8/build.sbt). 

If you are using the comand line tools `dotc` and `dotr` to run the code; you can use the flag `-with-compiler` compile and run with the compiler dependencies.

### Executing quoted code

The method `run` on `Expr[T]` will evaluate the contens of any quote and return the result of type `T`.

```scala
val code: Expr[Int] = '{ 1 + 3 }
val value: Int = code.run // evaluates 1 + 3 and returns 4
```
The evaluation is performed by JIT compilation of the code and then running the code. Note that this is why the compiler is a runtime dependency. To be able to call run you need to explicitly import the compiler toolbox with `import dotty.tools.dotc.quoted.Toolbox._`.

Compilation is of code like `1 + 3` is a lot slower that executing the code `1 + 3`. That is why we want to favor `run` on quotes containg lambdas. For example running `'{ (n: Int) => n + 3 }` would return a the function value `(n: Int) => n + 3` which can be invoked as many times as needed. 

Using this previous thechique we can stage the `powerCode` for some specific value of `n`.
```scala
def stagedPower(n: Int): Double => Double = {
  val code = '{ (x: Double) => ~powerCode(n, '(x)) }
  code.run
}
val square: Double => Double = stagedPower(2) // (x: Double) => x * x
val forthPower: Double => Double = stagedPower(4) // (x: Double) => { val x1 = x * x; x1 * x1 }
```


Compile-time Meta Programing (macros)
-------------------------------------


