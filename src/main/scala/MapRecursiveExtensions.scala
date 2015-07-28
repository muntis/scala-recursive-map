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
  type IteratorFunction = (Any, Any) => Any
  type Iterators = PartialFunction[Any, IteratorFunction => Any]

  val DefaultIterators: Iterators = {
    case l: List[Any] => transform: IteratorFunction => l.zipWithIndex.map(o => transform( o._2, o._1))
    case m: RecursiveMap => transform: IteratorFunction => m.map(o => (o._1, transform(o._1, o._2)))
    case m: java.util.Map[String, Any] =>
      import collection.JavaConversions._
      transform: IteratorFunction => mapAsJavaMap(m.map(o => (o._1, transform(o._1, o._2))))
    case l: java.util.List[Any] =>
      import collection.JavaConversions._
      transform: IteratorFunction => seqAsJavaList(l.zipWithIndex.map(o => transform( o._2, o._1)))
  }

  implicit def MapToRecursiveExtension[T <: AnyRef](map: T) = new{
    def recursiveMap(f: TransformationFunction)(implicit iterators: Iterators = DefaultIterators): T = {
      val fullTransform = f.orElse[(Any, Any), Any]{case (k, a) => a}

      def transformValue(value: Any, path: Any): Any =
        processChilds(fullTransform((path, value)), path)

      def processChilds(value: Any, path: Any): Any = value match {
        case o: Any if iterators.isDefinedAt(o) => iterators(o)((k, v) => transformValue(v, notNullPath(path, k)))
        case a => a
      }

      def notNullPath(path: Any, item: Any) = if (path == null) item else new /(path, item)

      if(map == null) null.asInstanceOf[T] else processChilds(map, null).asInstanceOf[T]
    }
  }

}