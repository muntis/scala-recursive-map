package org.recmap

import scala.language.implicitConversions

object MapRecursiveExtensions {
  // path matcher support
  case class /(node: Any, item: Any){
    def /(i2: Any) = new /(this, i2)
    override def toString = node + "/" + item
  }
  implicit def atRoot(s: String) =  new{
    def /(s2: Any) = new /(s, s2)
  }

  // map traveller
  type RecursiveMap = Map[String, Any]
  type TransformationFunction = PartialFunction[(Any, Any), Any]

  implicit def MapToRecursiveExtension(map: RecursiveMap) = new{
    def recursiveMap(f: TransformationFunction): RecursiveMap = {
      val fullTransform = f.orElse[(Any, Any), Any]{case (k, a) => a}

      def transformValue(value: Any, path: Any): Any = {
        fullTransform((path, value)) match {
          case (newKey: String) / (m1: RecursiveMap @unchecked) => newKey / iterateMap(m1, path)
          case (newKey: String) / (l: List[Any]) => newKey / iterateList(l, path)
          case (newKey: String) / a => newKey / a
          case _ / _ => sys.error("Only string keys supported in transformation function result")
          case m1: RecursiveMap @unchecked => iterateMap(m1, path)
          case l: List[Any] => iterateList(l, path)
          case a => a
        }
      }

      def notNullPath(path: Any, item: Any) = if (path == null) item else new /(path, item)

      def iterateMap(m: RecursiveMap, path: Any): RecursiveMap =
        m.map{case (k, v) => transformValue(v, notNullPath(path, k)) match {
          case (newKey: String) / newValue =>  (newKey, newValue)
          case newValue => (k, newValue)
        }}

      def iterateList(list: List[Any], path: Any):  List[Any] =
        list.zipWithIndex.map{case (v, i) => transformValue(v,notNullPath(path, i))}

      if(map == null) null else iterateMap(map, null)
    }
  }

}