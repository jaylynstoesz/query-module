package exporter

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.scalatestplus.play._
import querymodule.exporter.ExportFormatting._

@RunWith(classOf[JUnitRunner])
class ExportFormattingSpec extends PlaySpec {
  "Camel case converter" must {
    "correctly convert strings" in {
      toCamelCase("foo_bar_baz") mustEqual "fooBarBaz"

      toCamelCase("foo.bar.baz") mustEqual "fooBarBaz"

      toCamelCase("foo-bar-baz") mustEqual "fooBarBaz"
    }
  }

  "Snake case converter" must {
    "correctly convert strings" in {
      toSnakeCase("foo_bar_baz") mustEqual "foo_bar_baz"

      toSnakeCase("foo.bar.baz") mustEqual "foo_bar_baz"

      toSnakeCase("foo-bar-baz") mustEqual "foo_bar_baz"

      toSnakeCase("foo_bar.baz") mustEqual "foo_bar_baz"
    }
  }

  "Kebab case converter" must {
    "correctly convert strings" in {
      toKebabCase("foo_bar_baz") mustEqual "foo-bar-baz"

      toKebabCase("foo.bar.baz") mustEqual "foo-bar-baz"

      toKebabCase("foo-bar-baz") mustEqual "foo-bar-baz"

      toKebabCase("foo_bar.baz") mustEqual "foo-bar-baz"
    }
  }

  "Dot notation converter" must {
    "correctly convert strings" in {
      toDotNotation("foo_bar_baz") mustEqual "foo.bar.baz"

      toDotNotation("foo.bar.baz") mustEqual "foo.bar.baz"

      toDotNotation("foo-bar-baz") mustEqual "foo.bar.baz"

      toDotNotation("foo_bar.baz") mustEqual "foo.bar.baz"
    }
  }

}
