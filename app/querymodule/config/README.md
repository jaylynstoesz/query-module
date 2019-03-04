# Using Play Configuration

### Overview

This set of tools is used for:
* [retrieving and validating configured values](#retrieve-config-values)
* [helpers for checking common values against the configuration](#check-configuration-against-environment)

### Usage

#### Import the models you need

In your code, import the models you need from the library:
```
import querymodule.config.{ ... }
```

#### Retrieve config values

1) Configure connections using the Play `db.default` configuration
file: `/conf/application.conf`. See [Play documentation](https://www.playframework.com/documentation/2.6.x/ScalaDatabase) for details.

You should have three config files:
* `conf/application.conf` (development)
* `conf/stage/application.conf` (stage environment)
* `conf/prod/application.conf` (production)

2) Inject the Play Configuration class into the class where it's needed as a dependency:
```
class SampleReport(
    implicit configuration: Configuration
) {
    //...
}
```

3) Import helpers from `ConfigCommon` and use them to retrieve configurable values:
```
class SampleReport(
    implicit configuration: Configuration
) {
    import querymodule.config.ConfigCommon._

    val configPath: String = "reports.sampleReport"

    val timeout: Int = {
        val fallbackTimeout: Option[Int] = Some(120)

        getConfigInt(
            key = "timeout",
            configPath,
            default = fallbackTimeout
        )
    }

    // Will throw error if no default value is provided
    val destination: String = {
        getConfigStr(
            key = "destination",
            configPath
        )
    }
}
```

#### Check configuration against environment

For checking that you're operating in a safe environment, a `checkEnv` function is available in `ConfigCommon` that checks database configuration
against the application environment. This can be used anywhere that queries are executed.

_Note: please add additional wrappers and sanity checks as needed!_

### Important Links

Full Scaladocs (_TODO_)


