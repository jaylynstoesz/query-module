# Query Module
## _For Play API projects_

### Overview

_Please note: documentation is WIP! I will have it updated as soon as possible._

This project is a set of tools that you will likely need for any API project built in
Play. Modules include helpers for:

* [Querystring parsing and SQL query composition](/app/querymodule/query)
* [DB Connection & SQL query execution](/app/querymodule/dbconnection)
* [Exports (CSV and JSON)](/app/querymodule/exporter)
* [Configuration](/app/querymodule/config)
* [Caching](/app/querymodule/cache)

### Dependencies

You will need to import dependencies from the `build.sbt` file in this library.

Frameworks:
* [Play 2.6](https://www.playframework.com/documentation/2.6.x/Home)
* [Scala 2.11.8](https://www.scala-lang.org/download/2.11.8.html)
* [Spark 2.2.0](https://spark.apache.org/docs/2.2.0/)
* MySQL

### Usage

Add to `build.sbt` in your project:
`TODO`

In your code, import the models you need from the library:
```
import querymodule.query.QueryRequest
```

### Contributing to this project

1) Clone library and make necessary changes. _Document all changes inline and in the
appropriate `README` files!_

2) Write unit tests for your changes in the `test` directory

3) Make sure all tests pass by running `sbt test` in your terminal

4) Increment `version := "X.X.X"` in `build.sbt` (library will not publish if existing version
already exists in the Nexus repository)

5) Publish the library locally, import it into the project you're using it for,
and test the new functionality by running `sbt publishLocal` in your terminal

6) Once tested, push to master branch (_please follow Git Flow
and squash feature branch commits!_), tag, and deploy with Jenkins

7) Increment dependency version wherever you're importing it and rebuild your project

8) ???

9) Profit
