package org.recmap

import org.scalatest.{Matchers, FlatSpec}

import scala.language.reflectiveCalls
import MapRecursiveExtensions._

class MapRecursiveExtensionsSpecs extends FlatSpec with Matchers{
  behavior of "MapRecursiveExtensions.recursiveMap"

  it should "not fail on empty values" in {
    val m: Map[String, Any] = null
    m.recursiveMap{
      case (_, b) => b
    } should be (null)

    Map("a"->"b").recursiveMap{
      case (_, b) => b
    } should be (Map("a"->"b"))

    Map.empty[String, Any].recursiveMap{
      case (_, b) => b
    } should be (Map())
  }

  def readPaths(map: RecursiveMap)= {
    var paths = List.empty[(Any, Any)]
    map.recursiveMap{
      case (a, b) =>
        paths = (a, b) :: paths
        b
    }
    paths
  }

  it should "make proper paths (maps only)" in {
    {
      val paths = readPaths(Map("a"->"b"))
      paths should contain theSameElementsAs List(("a", "b"))
    }

    {
      val paths = readPaths(Map("a"->"b", "c"->"d"))
      paths should contain theSameElementsAs List(
        ("a", "b"),
        ("c", "d")
      )
    }

    {
      val paths = readPaths(Map("a"->Map("b1" -> "d1", "b2" -> "d2"), "c"->"d"))
      paths should contain theSameElementsAs List(
        ("a", Map("b1" -> "d1", "b2" -> "d2")),
        ("c", "d"),
        ("a" / "b1", "d1"),
        ("a" / "b2", "d2")
      )
    }

    {
      val paths = readPaths(Map("a"->Map("b1" -> "d1", "b2" -> Map("d3" -> "t3")), "c"->"d"))
      paths should contain theSameElementsAs List(
        ("a", Map("b1" -> "d1", "b2" -> Map("d3" -> "t3"))),
        ("c", "d"),
        ("a" / "b1", "d1"),
        ("a" / "b2", Map("d3" -> "t3")),
        ("a" / "b2" / "d3", "t3")
      )
    }
  }

  it should "make proper paths (with lists)" in {
    {
      val paths = readPaths(Map("a"->List("d1", "d2"), "c"->"d"))
      paths should contain theSameElementsAs List(
        ("a", List("d1", "d2")),
        ("c", "d"),
        ("a" / 0, "d1"),
        ("a" / 1, "d2")
      )
    }
    {
      val paths = readPaths(Map("a"->List("d1", List("d2", "d3")), "c"->"d"))
      paths should contain theSameElementsAs List(
        ("a", List("d1", List("d2", "d3"))),
        ("c", "d"),
        ("a" / 0, "d1"),
        ("a" / 1,  List("d2", "d3")),
        ("a" / 1 / 0, "d2"),
        ("a" / 1 / 1, "d3")
      )
    }

    {
      val paths = readPaths(Map("a"->List("d1", Map("1"->"d2", "2"->"d3")), "c"->"d"))
      paths should contain theSameElementsAs List(
        ("a", List("d1", Map("1"->"d2", "2"->"d3"))),
        ("c", "d"),
        ("a" / 0, "d1"),
        ("a" / 1,  Map("1"->"d2", "2"->"d3")),
        ("a" / 1 / "1", "d2"),
        ("a" / 1 / "2", "d3")
      )
    }

  }

  it should "make actual transformations" in {
    Map("a"->"b").recursiveMap{
      case ("a", _) => "c"
    } should be (Map("a"->"c"))

    Map("a"->"b").recursiveMap{
      case ("b", _) => "c"
    } should be (Map("a"->"b"))

    Map("a"->"b", "c"->"d").recursiveMap{
      case ("a", _) => "e"
    } should be (Map("a"->"e", "c"->"d"))

    Map("a"-> Map("b" -> "f", "g" -> "t"), "c"->"d").recursiveMap{
      case ("a" / "b", _) => "e"
    } should be (Map("a"-> Map("b" -> "e", "g" -> "t"), "c"->"d"))
  }

  it should "make actual transformations with patterns" in {
    Map("a"-> Map("b" -> Map("f"->"1"), "g" -> Map("f" -> "2"))).recursiveMap{
      case ("a" / _ / "f", _) => 1
    } should be (Map("a"-> Map("b" -> Map("f"->1), "g" -> Map("f" -> 1))))

    Map("a"-> Map("b" -> Map("f"->"1"), "g" -> Map("f" -> "2"))).recursiveMap{
      case ("a" / _ / "f", a: String) => a.toInt
    } should be (Map("a"-> Map("b" -> Map("f"->1), "g" -> Map("f" -> 2))))

    Map("a"-> Map("b" -> List(Map("f"->"a"), Map("g"-> "a")))).recursiveMap{
      case ("a" / "b" / index, a: Map[String, Any] @unchecked) => a + ("index"-> index)
    } should be (Map("a"-> Map("b" -> List(Map("f"->"a", "index"-> 0), Map("g"-> "a", "index"-> 1)))))

    Map("a"-> Map("b" -> List(Map("f"->"a", "index"-> 0), Map("g"-> "a", "index"-> 1)))).recursiveMap{
      case ("a" / "b" / _, a: Map[String, Any] @unchecked) => a - "index"
    } should be (Map("a"-> Map("b" -> List(Map("f"->"a"), Map("g"-> "a")))))

    Map("a"-> Map("b" -> List(Map("f"->"a", "index"-> 0), Map("g"-> "a", "index"-> 1)))).recursiveMap{
      case ("a" / "b" / 1, a: Map[String, Any] @unchecked) => a - "index"
    } should be (Map("a"-> Map("b" -> List(Map("f"->"a", "index"-> 0), Map("g"-> "a")))))
  }

  it should "make multiple transformations for multiple patterns" in {
    Map("a"->"b", "c"->"d").recursiveMap{
      case ("a", _) => "b1"
      case ("c", _) => "d1"
    } should be (Map("a"->"b1", "c"->"d1"))
  }

  it should "remap keys for map" in {
    Map("a"->"b", "c"->"d").recursiveMap{
      case ("a", v) => "f" / v
    } should be (Map("f"->"b", "c"->"d"))

    Map("a"->Map("b"->Map("c"->"d"))).recursiveMap{
      case ("a" / "b" / "c", v) => "c2" / v
    } should be (Map("a"->Map("b"->Map("c2"->"d"))))

    Map("a"->Map("b"->Map("c"->"d"))).recursiveMap{
      case ("a" , v) => "a2" / v
      case ("a" / "b" , v) => "b2" / v
      case ("a" / "b" / "c", v) => "c2" / v
    } should be (Map("a2"->Map("b2"->Map("c2"->"d"))))

    intercept[Exception] {
      Map("a"->Map("b"->Map("c"->"d"))).recursiveMap{
        case ("a" / "b" / "c", v) => /(1, v)
      }
    }.getMessage should be ("Only string keys supported in transformation function result")

    intercept[Exception] {
      Map("a"->Map("b"->Map("c"->"d"))).recursiveMap{
        case ("a" / "b" / "c", v) => /(null, v)
      }
    }.getMessage should be ("Only string keys supported in transformation function result")
  }
}
