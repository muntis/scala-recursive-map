# Scala Recursive Map
Scala utility to recursively traverse and transform json like tree structure made of Lists and Maps

# Installation

There is no maven artifact published jet. One way how to add this project is to add direct dependency to github repo in build file

build.sbt:

```scala
lazy val p = project.
  dependsOn(uri("git://github.com/muntis/scala-recursive-map"))
```
or project/Build.scala:

```scala
lazy val p = Project(id = "project" ...).
  dependsOn(uri("git://github.com/muntis/scala-recursive-map"))
```

# Usage

```scala
  import org.recmap.MapRecursiveExtensions._
  tree.recursiveMap{
    // partial transformation function
  }
```
Partial function signature: 

```scala
  ((path: Any, value: Any)) => newValue: Any
```

To unleash full power of scala matcher, path is case class created by "/" operand. Path nodes are strings for map keys and integers for list indexes.

# Example

Let's consider structure

```scala
  val department = Map(
    "name" -> "Sales",
    "employees" -> List(
      Map("name" -> "Bob", "surname"-> "Smith", "salary" -> BigDecimal(1000.00)),
      Map("name" -> "Mary", "surname"-> "Jones", "salary" -> BigDecimal(1700.10))
    )
  )
```

Partial function will be called with values:

```scala
("name", "Sales")
("employees", List(Map("name" -> "Bob", "surname" -> "Smith", "salary" -> BigDecimal(1000.0)), Map("name" -> "Mary", "surname" -> "Jones", "salary" -> BigDecimal(1700.1))))
("employees" / 0, Map("name" -> "Bob", "surname" -> "Smith", "salary" -> BigDecimal(1000.0)))
("employees" / 0 / "name", "Bob")
("employees" / 0 / "surname", "Smith")
("employees" / 0 / "salary", BigDecimal(1000.0))
("employees" / 1, Map("name" -> "Mary", "surname" -> "Jones", "salary" -> BigDecimal(1700.1)))
("employees" / 1 / "name", "Mary")
("employees" / 1 / "surname", "Jones")
("employees" / 1 / "salary", BigDecimal(1700.1))
```

Transformations:

Adding index to employee:

```scala
  department.recursiveMap{
    case ("employees" / index, employee: Map[String, Any]) => employee + ("index" -> index)
  }
  
  // Result: 
  // Map(name -> Sales, employees ->  List(
  //  Map(name -> Bob, surname -> Smith, salary -> 1000.0, index -> 0), 
  //  Map(name -> Mary, surname -> Jones, salary -> 1700.1, index -> 1)
  // ))
```

Employee name/surname to lower case:

```scala
  department.recursiveMap{
    case ("employees" / _ / ("name"| "surname"), name: String) => name.toLowerCase
  }
  
  // Result:
  // Map(name -> Sales, employees ->  List(
  //  Map(name -> bob, surname -> smith, salary -> 1000.0), 
  //  Map(name -> mary, surname -> jones, salary -> 1700.1)
  // ))
```

Change types of tree leaves (I'm looking at you MongoDb): 
```scala
  department.recursiveMap{
    case (_, bd: BigDecimal) => "Now as string: "+bd.toString
  }
  
  // Result:
  // Map(name -> Sales, employees ->  List(
  //  Map(name -> Bob, surname -> Smith, salary -> Now as string: 1000.0), 
  //  Map(name -> Mary, surname -> Jones, salary -> Now as string: 1700.1)
  // ))
```

Or mix them all in one transformation:
```scala
  department.recursiveMap{
    case ("employees" / index, employee: Map[String, Any]) => employee + ("index" -> index)
    case ("employees" / _ / ("name"| "surname"), name: String) => name.toLowerCase
    case (_, bd: BigDecimal) => "Now as string: "+bd.toString
  }
  
  // Result:
  // Map(name -> Sales, employees ->  List(
  //  Map(name -> bob, surname -> smith, salary -> Now as string: 1000.0, index -> 0), 
  //  Map(name -> mary, surname -> jones, salary -> Now as string: 1700.1, index -> 1)
  // ))  
  
```
