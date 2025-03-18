<a href="https://github.com/encalmo/upickle-utils">![GitHub](https://img.shields.io/badge/github-%23121011.svg?style=for-the-badge&logo=github&logoColor=white)</a> <a href="https://central.sonatype.com/artifact/org.encalmo/upickle-utils_3" target="_blank">![Maven Central Version](https://img.shields.io/maven-central/v/org.encalmo/upickle-utils_3?style=for-the-badge)</a> <a href="https://encalmo.github.io/upickle-utils/scaladoc/org/encalmo/utils.html" target="_blank"><img alt="Scaladoc" src="https://img.shields.io/badge/docs-scaladoc-red?style=for-the-badge"></a>

# upickle-utils

This Scala 3 library provides set of extensions to the [upickle](https://github.com/com-lihaoyi/upickle) library provided with [Scala toolkit](https://docs.scala-lang.org/toolkit/introduction.html).

## Dependencies

   - [Scala](https://www.scala-lang.org) >= 3.3.5
   - org.fusesource.jansi [**jansi** 2.4.1](https://central.sonatype.com/artifact/org.fusesource.jansi/jansi)
   - com.lihaoyi [**upickle** 4.1.0](https://github.com/com-lihaoyi/upickle)

## Usage

Use with SBT

    libraryDependencies += "org.encalmo" %% "upickle-utils" % "0.9.9"

or with SCALA-CLI

    //> using dep org.encalmo::upickle-utils:0.9.9

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
