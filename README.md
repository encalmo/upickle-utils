![Maven Central Version](https://img.shields.io/maven-central/v/org.encalmo/upickle-utils_3?style=for-the-badge)

# upickle-utils

This Scala 3 library provides set of extensions to the [upickle](https://github.com/com-lihaoyi/upickle) library provided with [Scala toolkit](https://docs.scala-lang.org/toolkit/introduction.html).

## Dependencies

## Usage

Use with SBT

    libraryDependencies += "org.encalmo" %% "upickle-utils" % "0.9.4"

or with SCALA-CLI

    //> using dep org.encalmo::upickle-utils:0.9.4

## Examples

```scala
import org.encalmo.utils.JsonUtils.*

case class Example(message: String) derives upickle.default.ReadWriter

val text = """{"message":"Hello World!"}"""

val text = """{"message":"Hello World!"}"""
val json: ujson.Value = text.readAsJson
val jsonOpt: Option[ujson.Value] = text.maybeReadAsJson
val example = text.readAs[Example]
val exampleOpt = text.maybeReadAs[Example]
```
