package org.encalmo.utils

import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.*
import org.fusesource.jansi.Ansi.Color.*
import ujson.*

import java.text.NumberFormat
import java.util.Locale

object JsonFormatter {

  final def parse(string: String): Value =
    if (string.isBlank()) then ujson.Null
    else if (!(string.trim().startsWith("{") || string.trim().startsWith("[")))
    then ujson.Str(string)
    else ujson.read(string)

  inline val quote = '"'

  val numberFormattter = {
    val formatter = NumberFormat.getInstance(Locale.US)
    formatter.setGroupingUsed(false)
    formatter
  }

  final def prettyPrintWithAnsiColors(
      value: Value,
      indentLevel: Int = 0,
      ansi: Ansi,
      syntaxColor: Color = BLUE,
      nameColor: Color = YELLOW,
      stringColor: Color = CYAN,
      valueColor: Color = MAGENTA,
      indentSize: Int = 2
  ): Ansi =
    value.match {
      case Str(v)  => ansi.fg(stringColor).a(quote).a(v).a(quote).reset()
      case Num(v)  => ansi.fg(valueColor).a(numberFormattter.format(v)).reset()
      case Bool(v) => ansi.fg(if (v) GREEN else RED).a(v).reset()
      case Null    => ansi.fg(syntaxColor).a("null").reset()

      case Arr(array) =>
        val hasObject = array.exists { case Obj(_) => true; case _ => false }
        ansi
          .fg(syntaxColor)
          .a("[")
        if (hasObject) ansi.newline()
        array.zipWithIndex.foreach { case (v, index) =>
          if (hasObject) ansi.a(" " * indentSize * (indentLevel + 1))
          prettyPrintWithAnsiColors(v, indentLevel + 1, ansi)
          ansi
            .fg(syntaxColor)
            .a(if (index < array.size - 1) "," else "")
            .reset()
          if (hasObject && index < array.size - 1) ansi.newline()
        }
        if (hasObject)
          ansi.newline().a(" " * indentSize * indentLevel)
        ansi
          .fg(syntaxColor)
          .a("]")
          .reset()

      case Obj(fields) =>
        ansi
          .fg(syntaxColor)
          .a("{")
          .newline()
        fields.zipWithIndex.foreach { case ((name, v), index) =>
          ansi
            .fg(nameColor)
            .a(" " * indentSize * (indentLevel + 1))
            .a(quote)
            .a(name)
            .a(quote)
            .fg(syntaxColor)
            .a(": ")
          prettyPrintWithAnsiColors(v, indentLevel + 1, ansi)
          ansi
            .fg(syntaxColor)
            .a(if (index < fields.size - 1) "," else "")
            .reset()
            .newline()
        }
        ansi
          .a(" " * indentSize * indentLevel)
          .fg(syntaxColor)
          .a("}")
          .reset()
    }

}
