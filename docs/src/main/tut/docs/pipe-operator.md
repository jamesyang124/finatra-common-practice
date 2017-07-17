---
layout: docs
title: Pipe Operator
---

# Pipe Operator

*Pipe operators* provide a way to chain the functions in the context which may not supply `map`, `antThen`, or `compose` methods.

```scala
import com.twitter.inject.Logging

object PipeOperator extends Logging {
  implicit class Pipe[T](val v: T) extends AnyVal {
    def |>[U](f: T => U): U = f(v)

    def $$[U](f: T => U): T = {
      f(v); v
    }

    def #!(str: String = ""): T = {
      debug(s"$str:$v"); v
    }
  }
}
```

1. `|>` operator works very similar as `map` function. The context may not retain due to the return type of `f`.

2. `$$` execute the function application of `f`, but return the original input `v`. So it can compose with impure functions but still remain the original value for next chaining call without bothering input `v`.

3. `#!` is for logging.
