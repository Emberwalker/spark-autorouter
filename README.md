# spark-autorouter
Annotation-driven route configuration for Spark.

## Usage
First, annotate a route, filter or exception handler with the annotations in `io.drakon.spark.autorouter.Routes`:
```java
import static io.drakon.spark.autorouter.Routes.*;

public class Example {
    @GET(path = "/")
    public static Object index(spark.Request request, spark.Response response) {
        return "Hello, world!";
    }
}
```

Then call the router in your application init code:
```java
import io.drakon.spark.autorouter.Autorouter;

public class Main {
    public static void main(String[] argv) {
        Autorouter router = new Autorouter("your.pkg.here");
        router.route();
    }
}
```

Done!

## Requirements
- Java Development Kit 8+ (for compile *and* runtime)
- Intellij IDEA if developing this project

## Maven
Jars, sources and JavaDocs are available on the Tethys Maven server. For Gradle dependencies block:
```groovy
maven {
    name 'Tethys'
    url 'http://tethys.drakon.io/maven'
}
```

The current artifact ID is:
- Group: `io.drakon.spark`
- ID: `autorouter`
- Version: `0.0.1`

Available classifiers:
- `sources`
- `javadoc`