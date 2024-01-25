# Jackson integration lib

This library exposes a jackson module which adds specific serializers and deserializers to handel Wirespec refined and enum types. For more details about Jackson see: https://github.com/FasterXML/jackson

## Usage
```xml
<dependency>
    <groupId>community.flock.wirespec.integration</groupId>
    <artifactId>jackson</artifactId>
    <version>{VERSION}</version>
</dependency>
```

Register the Wirespec module

```java
ObjectMapper objectMapper = new ObjectMapper()
        .registerModules(new WirespecModule());
```

## Docs

### Refined
The wirespec Java and Kotlin emitter add an extra wrapper class for refined types. When objects are serialized wrapper class becomes visible in json representation.

```java

record TodoId(Sring value){}
record Todo(TodoId id, String name, boolean done){}
```
When serialized to json with the default object mapper this wil result in the following output

```json
{
  "id": {
    "value": "123"
  },
  "name": "My todo",
  "done": true
}
```
The Jackson module corrects this and flattens the output of the refined types

```json
{
  "id": "123",
  "name": "My todo",
  "done": true
}
```

### Enum

For Java and Kotlin some values are sanitized because the compiler does not except certain keywords. Wirespec emits an extra label with the original value for every enum. The toString method is overwritten and returns the orignal value. This module uses the toString method to serialize and deserialize enum values

```wirespec
enum MyEnum {
    true, false 
}
```

The java emitter will generate the following enum class. The value true and false will be escaped because these are reserved keywords.

```java
public enum MyEnum implements Wirespec.Enum {
    _true("false"),
    _false("false");

    public final String label;
    
    MyEnum(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return this.label;
    }
}
```

### Reserved keywords
In java reserved keywords cannot be used as field name. The Wirespec java emitter prefixes the fields with a `_`. The Jackson Module corrects this with a NamingStrategy that removes the `_` only for java record types

```wirespec
type MyType {
    final: Boolean
}
```

```java
public record MyType ( String _final){}
```

## Generate test classes
To test this module test classes are generated from a Wirespec specification. To regenerate the test classes run the following test [GenerateTestClasses.kt](src%2FjvmTest%2Fkotlin%2Fcommunity%2Fflock%2Fwirespec%2Fintegration%2Fjackson%2Fkotlin%2FGenerateTestClasses.kt)
