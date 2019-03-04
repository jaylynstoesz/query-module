# Application-level Caching

### Overview

This set of tools is used for:
* [HTTP Caching](#http-request-caching)

### Usage

#### Import the models you need

In your code, import the models you need from the library:
```
import querymodule.cache.{ ... }
```

#### HTTP request caching

1) Configure connections using the Play `play.cache` configuration
file: `/conf/application.conf`. See [Play documentation](https://www.playframework.com/documentation/2.6.x/ScalaDatabase) for details.

You should have three config files:
* `conf/application.conf` (development)
* `conf/stage/application.conf` (stage environment)
* `conf/prod/application.conf` (production)

Set `play.cache.ttl.default` (in seconds), as well as any other resource-specific configs:
```
play.cache {
    ttl {
        default = 60
        sampleResource = 120
    }
}
```

2) Inject the `HttpCache` class into the class where it's needed as a dependency:

```
@Singleton
class SampleController @Inject() (
    cc:          ControllerComponents,
    httpCache:   HttpCache
)(
    //...
) extends AbstractController(cc) {
    //...
}
```

2) Wrap Actions with the `getSuccessful` function to retrieve the result of the last successful call within
the configured timeframe:
```
@Singleton
class SampleController @Inject() (
    cc:          ControllerComponents,
    httpCache:   HttpCache
)(
    //...
) extends AbstractController(cc) {
    def sampleResource: EssentialAction = {
        httpCache.getSuccessful(
          query.async { implicit request =>
              //...
          }, configPath = "sampleResource"
        )
    }
}
```

### Important Links

Full Scaladocs (_TODO_)


