package org.encalmo.utils

class JsonUtilsSpec extends munit.FunSuite {

  import JsonUtils.*

  def parseTestJsonExample: ujson.Value = s"""|{
      |  "foo": {
      |     "bar": {
      |         "zoo": 123.45,
      |         "zaz": "Hello!"
      |     },
      |     "baz": false,
      |     "bat": [1,2,3,4,5],
      |     "bru": [{"foo": "bar", "faz": [{"zoo": "z1"}, {"zoo": "z2"}]},{"foo": "baz", "faz": [{"zoo": "z3"}, {"zoo": "z4"}]}]
      |  },
      |  "faz": true,
      |  "fos": null
      |}
      |""".stripMargin.readAsJson

  test("writeAsJsonOrNull") {
    assertEquals(Some(123.45).writeAsJsonOrNull, ujson.Num(123.45))
    assertEquals(Some("foo").writeAsJsonOrNull, ujson.Str("foo"))
    assertEquals(Some(false).writeAsJsonOrNull, ujson.Bool(false))
    assertEquals(Some(true).writeAsJsonOrNull, ujson.Bool(true))
    assertEquals(Some(123).writeAsJsonOrNull, ujson.Num(123))
  }

  case class Example(message: String) derives upickle.default.ReadWriter

  test("README examples") {
    val text = """{"message":"Hello World!"}"""
    val json: ujson.Value = text.readAsJson
    val jsonOpt: Option[ujson.Value] = text.maybeReadAsJson
    val example = text.readAs[Example]
    val exampleOpt = text.maybeReadAs[Example]
  }

  test("maybe read value from json") {
    val json = parseTestJsonExample
    assertEquals(json.readByPath[Double]("foo.bar.zoo"), Some(123.45))
    assertEquals(json.readByPath[String]("foo.bar.zaz"), Some("Hello!"))
    assertEquals(json.readByPath[Boolean]("foo.baz"), Some(false))
    assertEquals(json.readByPath[Seq[Int]]("foo.bat"), Some(Seq(1, 2, 3, 4, 5)))
    assertEquals(json.readByPath[Int]("foo.bat.0"), Some(1))
    assertEquals(json.readByPath[Int]("foo.bat[0]"), Some(1))
    assertEquals(json.get("foo.bat.1"), Some(ujson.Num(2)))
    assertEquals(json.get("foo.bat.2"), Some(ujson.Num(3)))
    assertEquals(json.get("foo.bat.3"), Some(ujson.Num(4)))
    assertEquals(json.get("foo.bat.4"), Some(ujson.Num(5)))
    assertEquals(json.get("foo.bat.5"), None)
    assertEquals(json.get("foo.bru[0].foo"), Some(ujson.Str("bar")))
    assertEquals(json.get("foo.bru[0].bar"), None)
    assertEquals(json.get("foo.bru[1].foo"), Some(ujson.Str("baz")))
    assertEquals(json.get("foo.bru[2].foo"), None)
    assertEquals(json.get("foo.bru[*].foo"), Some(ujson.Arr("bar", "baz")))
    assertEquals(json.get("foo.bru.*.foo"), Some(ujson.Arr("bar", "baz")))
    assertEquals(
      json.get("foo.bru[*].faz[*].zoo"),
      Some(ujson.Arr(ujson.Arr("z1", "z2"), ujson.Arr("z3", "z4")))
    )
    assertEquals(json.get("faz"), Some(ujson.Bool(true)))
    assertEquals(json.get("foo.bar.zzz"), None)
    assertEquals(json.get("foo.bar.zoo.1"), None)
    assertEquals(json.get("foo.bar.zoo.foo.bar.zoo"), None)
    assertEquals(
      json.get("foo.bar"),
      Some(ujson.Obj("zoo" -> 123.45, "zaz" -> "Hello!"))
    )
    assertEquals(json.get("fos"), None)
  }

  test("get value from json") {
    val json = parseTestJsonExample
    assertEquals(json.get("foo.bar.zoo"), Some(ujson.Num(123.45)))
    assertEquals(json.get("foo.bar.zaz"), Some(ujson.Str("Hello!")))
    assertEquals(json.get("foo.baz"), Some(ujson.Bool(false)))
    assertEquals(json.get("foo.bat"), Some(ujson.Arr(1, 2, 3, 4, 5)))
    assertEquals(json.get("foo.bat.0"), Some(ujson.Num(1)))
    assertEquals(json.get("foo.bat[0]"), Some(ujson.Num(1)))
    assertEquals(json.get("foo.bat.1"), Some(ujson.Num(2)))
    assertEquals(json.get("foo.bat.2"), Some(ujson.Num(3)))
    assertEquals(json.get("foo.bat.3"), Some(ujson.Num(4)))
    assertEquals(json.get("foo.bat.4"), Some(ujson.Num(5)))
    assertEquals(json.get("foo.bat.5"), None)
    assertEquals(json.get("foo.bru[0].foo"), Some(ujson.Str("bar")))
    assertEquals(json.get("foo.bru[0].bar"), None)
    assertEquals(json.get("foo.bru[1].foo"), Some(ujson.Str("baz")))
    assertEquals(json.get("foo.bru[2].foo"), None)
    assertEquals(json.get("foo.bru[*].foo"), Some(ujson.Arr("bar", "baz")))
    assertEquals(json.get("foo.bru.*.foo"), Some(ujson.Arr("bar", "baz")))
    assertEquals(
      json.get("foo.bru[*].faz[*].zoo"),
      Some(ujson.Arr(ujson.Arr("z1", "z2"), ujson.Arr("z3", "z4")))
    )
    assertEquals(json.get("faz"), Some(ujson.Bool(true)))
    assertEquals(json.get("foo.bar.zzz"), None)
    assertEquals(json.get("foo.bar.zoo.1"), None)
    assertEquals(json.get("foo.bar.zoo.foo.bar.zoo"), None)
    assertEquals(
      json.get("foo.bar"),
      Some(ujson.Obj("zoo" -> 123.45, "zaz" -> "Hello!"))
    )
    assertEquals(json.get("fos"), None)
  }

  test("set value in json") {
    val obj = ujson.Obj()
    val value = ujson.Str("Here I am!")
    obj.set("foo", value, force = false)
    assertEquals(obj.get("foo"), None)
    obj.set("foo", value, force = true)
    assertEquals(obj.get("foo"), Some(value))
    obj.set("foo", ujson.Bool(true), force = false)
    assertEquals(obj.get("foo"), Some(ujson.Bool(true)))
    assertEquals(obj.get("faz"), None)
    assertEquals(obj.get("foo.faz"), None)
    assertEquals(obj.get("faz.foo"), None)
    obj.set("faz", value, force = true)
    assertEquals(obj.get("faz"), Some(value))
    obj.set("foo.faz", value, force = true)
    assertEquals(obj.get("foo.faz"), Some(value))
    assertEquals(obj.get("foo.faz.0"), None)
    obj.set("foo.faz", ujson.Bool(true), force = true)
    assertEquals(obj.get("foo.faz"), Some(ujson.Bool(true)))
    assertEquals(obj.get("foo.faz.0"), None)
    obj.set("foo.faz.0", value, force = true)
    assertEquals(obj.get("foo.faz.0"), Some(value))

    val json = parseTestJsonExample
    assertEquals(json.get("foo.bat[0]"), Some(ujson.Num(1)))
    json.set("foo.bat[0]", value, force = true)
    assertEquals(json.get("foo.bat[0]"), Some(value))
    json.set("foo.bat[7].foo", value, force = true)
    assertEquals(json.get("foo.bat[7].foo"), Some(value))
    json.set("foo.bat[9]", value, force = false)
    assertEquals(json.get("foo.bat[9]"), None)
    json.set("foo.bat[9]", value, force = true)
    assertEquals(json.get("foo.bat[9]"), Some(value))
    json.set("foo.bat[*]", value, force = true)
    for (i <- 0 to 9) do assertEquals(json.get(s"foo.bat[$i]"), Some(value))
    assertEquals(json.get("foo.bat[10]"), None)
  }

  test("modify value in json") {
    val json = parseTestJsonExample
    val value = ujson.Str("Here I am!")
    json.transform(
      "foo.bar.zoo",
      { case ujson.Num(n) => ujson.Num(n * 2) },
      force = false
    )
    assertEquals(json.get("foo.bar.zoo"), Some(ujson.Num(246.90)))
    json.transform(
      "foo.bar.zuu",
      { case ujson.Num(n) => ujson.Num(n * 2) },
      force = true
    )
    assertEquals(json.get("foo.bar.zuu"), None)
    json.transform(
      "foo.bar.zuu",
      { case _ => ujson.Num(999) },
      force = true
    )
    assertEquals(json.get("foo.bar.zuu"), Some(ujson.Num(999)))
    json.transform(
      "foo.bat[0]",
      { case ujson.Num(n) => ujson.Num(n + 0.5) },
      force = true
    )
    assertEquals(json.get("foo.bat[0]"), Some(ujson.Num(1.5)))
    json.transform(
      "foo.bat[*]",
      { case ujson.Num(n) => ujson.Num(n + 0.1) },
      force = false
    )
    assertEquals(json.get("foo.bat[0]"), Some(ujson.Num(1.6)))
    assertEquals(json.get("foo.bat[1]"), Some(ujson.Num(2.1)))
    assertEquals(json.get("foo.bat[2]"), Some(ujson.Num(3.1)))
    assertEquals(json.get("foo.bat[3]"), Some(ujson.Num(4.1)))
    assertEquals(json.get("foo.bat[4]"), Some(ujson.Num(5.1)))
    assertEquals(json.get("foo.bat[5]"), None)
    json.transform(
      "foo.bat[0]",
      { case ujson.Num(n) => ujson.Obj("num" -> ujson.Num(n + 0.2)) },
      force = true
    )
    assertEquals(json.get("foo.bat[0].num"), Some(ujson.Num(1.8)))
    json.transform(
      "foo.bat[*].num",
      { case _ => ujson.Null },
      force = false
    )
    assertEquals(json.get("foo.bat[0].num"), None)
    assertEquals(json.get("foo.bat[1].num"), None)
  }

  test("set and remove value in json") {
    val json = ujson.Obj()
    json.set("foo", "Hello!")
    assertEquals(json.get("foo"), Some(ujson.Str("Hello!")))
    json.set("faz", 1)
    assertEquals(json.get("faz"), Some(ujson.Num(1)))
    json.set("fas", 12.345)
    assertEquals(json.get("fas"), Some(ujson.Num(12.345)))
    json.set("fun", true)
    assertEquals(json.get("fun"), Some(ujson.Bool(true)))
    json.set("fah", false)
    assertEquals(json.get("fah"), Some(ujson.Bool(false)))
    json.remove("foo")
    assertEquals(json.get("foo"), None)
    json.remove("faz")
    assertEquals(json.get("faz"), None)
    json.remove("fas")
    assertEquals(json.get("fas"), None)
    json.remove("fun")
    assertEquals(json.get("fun"), None)
    json.remove("fah")
    assertEquals(json.get("fah"), None)
    json.set("foo.1.val", "Hello!")
    assertEquals(json.get("foo.1.val"), Some(ujson.Str("Hello!")))
    json.set("faz.1.val", 1)
    assertEquals(json.get("faz.1.val"), Some(ujson.Num(1)))
    json.set("fas.1.val", 12.345)
    assertEquals(json.get("fas.1.val"), Some(ujson.Num(12.345)))
    json.set("fun.1.val", true)
    assertEquals(json.get("fun.1.val"), Some(ujson.Bool(true)))
    json.set("fah.1.val", false)
    assertEquals(json.get("fah.1.val"), Some(ujson.Bool(false)))
    json.remove("foo.1.val")
    assertEquals(json.get("foo.1.val"), None)
    json.remove("faz.1.val")
    assertEquals(json.get("faz.1.val"), None)
    json.remove("fas.1.val")
    assertEquals(json.get("fas.1.val"), None)
    json.remove("fun.1.val")
    assertEquals(json.get("fun.1.val"), None)
    json.remove("fah.1.val")
    assertEquals(json.get("fah.1.val"), None)
  }

  test("readAsEither") {
    assertEquals(
      "\"hello\"".readAsEither[Seq[String], String],
      Right("hello")
    )
    assertEquals(
      ujson.Str("hello").readAsEither[Seq[String], String],
      Right("hello")
    )
    assertEquals(
      "[\"hello\"]".readAsEither[Seq[String], String],
      Left(Seq("hello"))
    )
    assertEquals(
      ujson.Arr("hello").readAsEither[Seq[String], String],
      Left(Seq("hello"))
    )
  }

  test("maybeReadAsEither") {
    assertEquals(
      "[\"hello\"]".maybeReadAsEither[Seq[String], Seq[Int]],
      Some(Left(Seq("hello")))
    )
    assertEquals(
      "[1,2,3,4,5]".maybeReadAsEither[Seq[String], Seq[Int]],
      Some(Right(Seq(1, 2, 3, 4, 5)))
    )
    assertEquals(
      "{}".maybeReadAsEither[Seq[String], Seq[Int]],
      None
    )
    assertEquals(
      ujson.Arr("hello").maybeReadAsEither[Seq[String], Seq[Int]],
      Some(Left(Seq("hello")))
    )
    assertEquals(
      ujson.Arr(1, 2, 3, 4, 5).maybeReadAsEither[Seq[String], Seq[Int]],
      Some(Right(Seq(1, 2, 3, 4, 5)))
    )
    assertEquals(
      ujson.Obj().maybeReadAsEither[Seq[String], Seq[Int]],
      None
    )
  }

  test("Either.writeAsString") {
    assertEquals(Right[Int, String]("hello").writeAsString, "\"hello\"")
    assertEquals(Left[Int, String](12345).writeAsString, "12345")
  }

  test("Either.writeAsJson") {
    assertEquals(Right[Int, String]("hello").writeAsJson, ujson.Str("hello"))
    assertEquals(Left[Int, String](12345).writeAsJson, ujson.Num(12345))
  }

  test("add and remove field to an object") {
    val value: ujson.Value = ujson.Obj("a" -> 1)
    assertEquals(
      value + ("b" -> false),
      ujson.Obj("a" -> 1, "b" -> false)
    )
    assertEquals(
      value + ("b" -> false) - "a",
      ujson.Obj("b" -> false)
    )
    assertEquals(
      value - "a",
      ujson.Obj()
    )
  }

}
