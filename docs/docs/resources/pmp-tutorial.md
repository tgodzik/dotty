---
layout: doc-page
title: Principled Meta Programming
---

Overview
--------

// TODO 

Quotes and splices
------------------

The basic building blocks of all staging and macros are two operators `'` and `~`. 

Code in quotes `'(...)`, `'{...}` and `'[...]` represent the actual code inside them. This code is not excecuted but staged for excecution in a later phase. 
// TODO mention `Expr` and `Type`

Splices `~` are the oposite of quoting, they allow the conversion from an `Expr[T]` or `Type[T]` to a `T` inside of another quote.

// TODO basics


```scala
def power(x: Double, n: Int): Double = 
  if (n == 0) 1.0
  else if (n % 2 == 1) { x * power(x, n - 1) }
  else { val y = power(x, n / 2); y * y }
```
Now assume that we know the value of `n` and we want to create code that evaluates it away. As we don't know the value of `x` yet it will also be code. Hence we will recive a `Expr[Double]` as `x` and will return an `Expr[Double]`. 

```scala
def powerCode(x: Expr[Double], n: Int): Expr[Double] = 
  ???
```
The first branch had `if (n == 0) 1.0` but we need to return an `Expr[Double]`. We can just use `'(1.0)` create the code needed.

```scala
def powerCode(x: Expr[Double], n: Int): Expr[Double] = 
  if (n == 0) '(1.0)
  else ???
```

Now we are left with two cases that have a recursion. The first one was `if (n % 2 == 1) { x * power(x, n - 1) }`, we could try to rewrite it as `if (n % 2 == 1) '{ ~x * power(~x, n - 1) }` which would create the code `x * power(x, n - 1)` but this code is not the one that we are aming for, we want to completly eliminate `n` from the result. Then we need to create code for `power(x, n - 1)` which is basically what `powerCode(x, n -1)` does. We just need to use it inside our quoted code. To insert the code we use the `~` operator on the computed value of `powerCode(x, n -1)`. This would be written as `val rec = powerCode(x, n - 1); '{ ~x * ~rec }` or just `'{ ~x * ~powerCode(x, n - 1) }`. Similarly for the other branch

```scala
def powerCode(x: Expr[Double], n: Int): Expr[Double] = 
  if (n == 0) '(0)
  else if (n % 2 == 1) '{ ~x * ~powerCode(x, n - 1) }
  else '{ val y = ~powerCode(x, n / 2); y * y }
```


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


#### Showing the quoted code

### Lifting values to quotes


Compile-time Meta Programing (macros)
-------------------------------------


