package com.wirelang

import community.flock.wirespec.language.core.file
import community.flock.wirespec.language.core.generator.JavaGenerator
import community.flock.wirespec.language.core.generator.PythonGenerator
import community.flock.wirespec.language.core.generator.TypeScriptGenerator
import kotlin.test.Test
import kotlin.test.assertTrue

class DslTest {

    @Test
    fun testNestedNullable() {
        val file = file("NestedNullableModule") {
            struct("Data") {
                field("tags", list(string.nullable()))
            }
            function("process", string.nullable()) {
                arg("input", list(string.nullable()).nullable())
                returns("null")
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)

        println("--- Java (Nested Nullable) ---")
        println(javaCode)
        println("--- TypeScript (Nested Nullable) ---")
        println(tsCode)

        assertTrue(javaCode.contains("public record Data"))
        assertTrue(javaCode.contains("java.util.List<java.util.Optional<String>> tags"))
        assertTrue(javaCode.contains(") {"))
        assertTrue(javaCode.contains("java.util.Optional<String> process(java.util.Optional<java.util.List<java.util.Optional<String>>> input)"))

        assertTrue(tsCode.contains("public tags: (string | null)[];"))
        assertTrue(tsCode.contains("function process(input: (string | null)[] | null): string | null"))
    }

    @Test
    fun testDslAndGeneration() {
        val file = file("MyModule") {
            struct("User") {
                field("id", integer)
                field("name", string.nullable())
            }

            function("greet", string) {
                arg("user", type("User"))
                print("\"Greeting \" + user.name")
                returns("user.name")
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)

        println("--- Java ---")
        println(javaCode)
        println("--- Python ---")
        println(pythonCode)
        println("--- TypeScript ---")
        println(tsCode)

        assertTrue(javaCode.contains("public record User"))
        assertTrue(javaCode.contains("Integer id,"))
        assertTrue(javaCode.contains("java.util.Optional<String> name"))
        assertTrue(javaCode.contains(") {"))
        assertTrue(javaCode.contains("String greet(User user) {"))

        assertTrue(pythonCode.contains("class User:"))
        assertTrue(pythonCode.contains("def __init__(self, id, name):"))
        assertTrue(pythonCode.contains("self.id = id"))
        assertTrue(pythonCode.contains("self.name = name"))
        assertTrue(pythonCode.contains("def greet(user):"))

        assertTrue(tsCode.contains("class User {"))
        assertTrue(tsCode.contains("public id: number;"))
        assertTrue(tsCode.contains("public name: string | null;"))
        assertTrue(tsCode.contains("function greet(user: User): string {"))
    }

    @Test
    fun testStaticElement() {
        val file = file("StaticModule") {
            static("MyService", extends = type("BaseService")) {
                struct("Config") {
                    field("url", string)
                }
                function("start") {
                    print("\"Starting service\"")
                }
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)

        println("--- Java (Static) ---")
        println(javaCode)
        println("--- TypeScript (Static) ---")
        println(tsCode)
        println("--- Python (Static) ---")
        println(pythonCode)

        assertTrue(javaCode.contains("public interface MyService extends BaseService {"))
        assertTrue(javaCode.contains("public static record Config"))
        assertTrue(javaCode.contains("String url"))
        assertTrue(javaCode.contains(") {"))
        assertTrue(javaCode.contains("static void start() {"))
        assertTrue(javaCode.contains("System.out.println(\"Starting service\");"))

        assertTrue(tsCode.contains("interface MyService extends BaseService {"))
        assertTrue(tsCode.contains("class Config {"))
        assertTrue(tsCode.contains("public url: string;"))
        assertTrue(tsCode.contains("function start() {"))
        assertTrue(tsCode.contains("console.log('Starting service');"))

        assertTrue(pythonCode.contains("class MyService(BaseService):"))
        assertTrue(pythonCode.contains("class Config:"))
        assertTrue(pythonCode.contains("def __init__(self, url):"))
    }

    @Test
    fun testNestedStatic() {
        val file = file("NestedModule") {
            static("Outer", type("Base")) {
                static("Inner") {
                    function("test") {
                        returns("1")
                    }
                }
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)

        println("--- Java (Nested Static) ---")
        println(javaCode)
        println("--- TypeScript (Nested Static) ---")
        println(tsCode)
        println("--- Python (Nested Static) ---")
        println(pythonCode)

        // Java
        assertTrue(javaCode.contains("public interface Outer extends Base {"))
        assertTrue(javaCode.contains("public interface Inner {"))
        assertTrue(javaCode.contains("public static void test() {"))

        // TypeScript
        assertTrue(tsCode.contains("interface Outer extends Base {"))
        assertTrue(tsCode.contains("interface Inner {"))
        assertTrue(tsCode.contains("function test() {"))

        // Python
        assertTrue(pythonCode.contains("class Outer(Base):"))
        assertTrue(pythonCode.contains("class Inner:"))
        assertTrue(pythonCode.contains("def test():"))
    }

    @Test
    fun testAsyncFunction() {
        val file = file("AsyncModule") {
            asyncFunction("fetchData", string) {
                arg("id", integer)
                returns("\"data\"")
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)

        println("--- Java (Async) ---")
        println(javaCode)
        println("--- Python (Async) ---")
        println(pythonCode)
        println("--- TypeScript (Async) ---")
        println(tsCode)

        assertTrue(javaCode.contains("java.util.concurrent.CompletableFuture<String> fetchData(int id) {"))
        assertTrue(pythonCode.contains("async def fetchData(id):"))
        assertTrue(tsCode.contains("async function fetchData(id: number): string {"))
    }

    @Test
    fun testFunctionInterface() {
        val file = file("InterfaceModule") {
            static("Api") {
                function("getData", string)
                asyncFunction("postData") {
                    arg("data", string)
                }
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)

        println("--- Java (Interface) ---")
        println(javaCode)
        println("--- Python (Interface) ---")
        println(pythonCode)
        println("--- TypeScript (Interface) ---")
        println(tsCode)

        // Java
        assertTrue(javaCode.contains("public interface Api {"))
        assertTrue(javaCode.contains("public static String getData();"))
        assertTrue(javaCode.contains("public static java.util.concurrent.CompletableFuture<Void> postData(String data);"))

        // Python
        assertTrue(pythonCode.contains("class Api:"))
        assertTrue(pythonCode.contains("def getData():"))
        assertTrue(pythonCode.contains("pass"))
        assertTrue(pythonCode.contains("async def postData(data):"))

        // TypeScript
        assertTrue(tsCode.contains("interface Api {"))
        assertTrue(tsCode.contains("getData(): string"))
        assertTrue(tsCode.contains("postData(data: string): Promise<void>"))
    }

    @Test
    fun testReturnStatement() {
        val file = file("ReturnModule") {
            function("test") {
                returns(
                    call("myFunc") {
                        arg("a", "1")
                    },
                )
            }
            function("test2") {
                returns(
                    construct(type("User")) {
                        arg("id", "1")
                    },
                )
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)

        println("--- Java (Return Statement) ---")
        println(javaCode)
        println("--- Python (Return Statement) ---")
        println(pythonCode)
        println("--- TypeScript (Return Statement) ---")
        println(tsCode)

        // Java
        assertTrue(javaCode.contains("return myFunc(1);"))
        assertTrue(javaCode.contains("return new User(1);"))

        // Python
        assertTrue(pythonCode.contains("return myFunc(a=1)"))
        assertTrue(pythonCode.contains("return User(id=1)"))

        // TypeScript
        assertTrue(tsCode.contains("return myFunc({ a: 1 });"))
        assertTrue(tsCode.contains("return new User({ id: 1 });"))
    }

    @Test
    fun testPrimitiveTypes() {
        val file = file("PrimitiveModule") {
            struct("Data") {
                field("count", integer)
                field("name", string)
                field("active", boolean)
                field("ratio", number)
            }

            function("process", returnType = boolean) {
                arg("count", integer)
                arg("name", string)
                print("\"count: count\"")
                returns("true")
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)

        println("--- Java (Primitives) ---")
        println(javaCode)
        println("--- Python (Primitives) ---")
        println(pythonCode)
        println("--- TypeScript (Primitives) ---")
        println(tsCode)

        // Java
        assertTrue(javaCode.contains("public record Data"))
        assertTrue(javaCode.contains("Integer count,"))
        assertTrue(javaCode.contains("String name,"))
        assertTrue(javaCode.contains("Boolean active,"))
        assertTrue(javaCode.contains("Double ratio"))
        assertTrue(javaCode.contains(") {"))
        assertTrue(javaCode.contains("boolean process(int count, String name) {"))
        assertTrue(javaCode.contains("return true;"))
        assertTrue(javaCode.contains("System.out.println(\"count: count\");"))

        // Python
        assertTrue(pythonCode.contains("class Data:"))
        assertTrue(pythonCode.contains("print('count: count')"))
        assertTrue(pythonCode.contains("return True"))

        // TypeScript
        assertTrue(tsCode.contains("console.log('count: count');"))
        assertTrue(tsCode.contains("return true;"))
    }

    @Test
    fun testLiteralStatement() {
        val file = file("LiteralModule") {
            function("test") {
                literal(1, integer)
                literal("hello", string)
                literal(true, boolean)
                literal(1.2, number)

                returns(
                    construct(type("User")) {
                        arg("id", literal(1, integer))
                        arg("name", literal("John", string))
                    },
                )
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)

        println("--- Java (Literal) ---")
        println(javaCode)
        println("--- Python (Literal) ---")
        println(pythonCode)
        println("--- TypeScript (Literal) ---")
        println(tsCode)

        // Java
        assertTrue(javaCode.contains("1;"))
        assertTrue(javaCode.contains("\"hello\";"))
        assertTrue(javaCode.contains("true;"))
        assertTrue(javaCode.contains("1.2;"))
        assertTrue(javaCode.contains("return new User("))

        // Python
        assertTrue(pythonCode.contains("1"))
        assertTrue(pythonCode.contains("'hello'"))
        assertTrue(pythonCode.contains("True"))
        assertTrue(pythonCode.contains("1.2"))
        assertTrue(pythonCode.contains("return User(id=1, name='John')"))

        // TypeScript
        assertTrue(tsCode.contains("1;"))
        assertTrue(tsCode.contains("'hello';"))
        assertTrue(tsCode.contains("true;"))
        assertTrue(tsCode.contains("1.2;"))
        assertTrue(tsCode.contains("return new User({ id: 1, name: 'John' });"))
    }

    @Test
    fun testArrayType() {
        val file = file("ArrayModule") {
            struct("User") {
                field("tags", list(string))
                field("scores", list(integer))
            }

            function("process", list(boolean)) {
                arg("tags", list(string))
                returns("tags")
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)

        println("--- Java (Array) ---")
        println(javaCode)
        println("--- Python (Array) ---")
        println(pythonCode)
        println("--- TypeScript (Array) ---")
        println(tsCode)

        // Java
        assertTrue(javaCode.contains("public record User"))
        assertTrue(javaCode.contains("java.util.List<String> tags,"))
        assertTrue(javaCode.contains("java.util.List<Integer> scores"))
        assertTrue(javaCode.contains(") {"))
        assertTrue(javaCode.contains("java.util.List<Boolean> process(java.util.List<String> tags) {"))
        assertTrue(javaCode.contains("return tags;"))

        // Python
        assertTrue(pythonCode.contains("def process(tags):"))

        // TypeScript
        assertTrue(tsCode.contains("tags: string[];"))
        assertTrue(tsCode.contains("scores: number[];"))
        assertTrue(tsCode.contains("function process(tags: string[]): boolean[] {"))
    }

    @Test
    fun testAssignmentStatement() {
        val file = file("AssignmentModule") {
            function("test") {
                assign("x", literal(1, integer))
                assign(
                    "y",
                    call("myFunc") {
                        arg("a", "1")
                    },
                )
                returns("x")
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)

        println("--- Java (Assignment) ---")
        println(javaCode)
        println("--- Python (Assignment) ---")
        println(pythonCode)
        println("--- TypeScript (Assignment) ---")
        println(tsCode)

        // Java
        assertTrue(javaCode.contains("final var x = 1;"))
        assertTrue(javaCode.contains("final var y = myFunc(1);"))

        // Python
        assertTrue(pythonCode.contains("x = 1"))
        assertTrue(pythonCode.contains("y = myFunc(a=1)"))

        // TypeScript
        assertTrue(tsCode.contains("const x = 1;"))
        assertTrue(tsCode.contains("const y = myFunc({ a: 1 });"))
    }

    @Test
    fun testLiteralList() {
        val file = file("LiteralListModule") {
            function("test") {
                assign("tags", listOf(listOf("\"tag1\"", "\"tag2\""), string))
                returns("tags")
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)

        println("--- Java (Literal List) ---")
        println(javaCode)
        println("--- Python (Literal List) ---")
        println(pythonCode)
        println("--- TypeScript (Literal List) ---")
        println(tsCode)

        // Java
        assertTrue(javaCode.contains("final var tags = java.util.List.of(\"tag1\", \"tag2\");"))

        // Python
        assertTrue(pythonCode.contains("tags = ['tag1', 'tag2']"))

        // TypeScript
        assertTrue(tsCode.contains("const tags = ['tag1', 'tag2'];"))
    }

    @Test
    fun testLiteralMap() {
        val file = file("LiteralMapModule") {
            function("test") {
                assign("scores", mapOf(mapOf("Alice" to 10, "Bob" to 20), string, integer))
                returns("scores")
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)

        println("--- Java (Literal Map) ---")
        println(javaCode)
        println("--- Python (Literal Map) ---")
        println(pythonCode)
        println("--- TypeScript (Literal Map) ---")
        println(tsCode)

        // Java
        assertTrue(javaCode.contains("final var scores = java.util.Map.ofEntries(java.util.Map.entry(\"Alice\", 10), java.util.Map.entry(\"Bob\", 20));"))

        // Python
        assertTrue(pythonCode.contains("scores = {'Alice': 10, 'Bob': 20}"))

        // TypeScript
        assertTrue(tsCode.contains("const scores = { 'Alice': 10, 'Bob': 20 };"))
    }

    @Test
    fun testEmptyLiterals() {
        val file = file("EmptyLiteralsModule") {
            function("test") {
                assign("emptyList", emptyList(string))
                assign("emptyMap", emptyMap(string, integer))

                call("myFunc") {
                    arg("list", emptyList(integer))
                    arg("map", emptyMap(string, string))
                }

                construct(type("User")) {
                    arg("tags", emptyList(string))
                }
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)

        println("--- Java (Empty Literals) ---")
        println(javaCode)
        println("--- TypeScript (Empty Literals) ---")
        println(tsCode)

        assertTrue(javaCode.contains("final var emptyList = java.util.List.of();"))
        assertTrue(javaCode.contains("final var emptyMap = java.util.Collections.emptyMap();"))
        assertTrue(javaCode.contains("myFunc(java.util.List.of(), java.util.Collections.emptyMap());"))
        assertTrue(javaCode.contains("new User(java.util.List.of());"))

        assertTrue(tsCode.contains("const emptyList = [];"))
        assertTrue(tsCode.contains("const emptyMap = {  };"))
        assertTrue(tsCode.contains("myFunc({ list: [], map: {  } });"))
        assertTrue(tsCode.contains("new User({ tags: [] });"))
    }

    @Test
    fun testStructConstructor() {
        val file = file("StructConstructorModule") {
            struct("User") {
                field("id", integer)
                field("name", string)
                constructo {
                    arg("id", integer)
                    assign("this.id", "id")
                    assign("this.name", "\"Anonymous\"")
                }
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)

        println("--- Java (Struct Constructor) ---")
        println(javaCode)
        println("--- Python (Struct Constructor) ---")
        println(pythonCode)
        println("--- TypeScript (Struct Constructor) ---")
        println(tsCode)

        // Java
        assertTrue(javaCode.contains("public record User"))
        assertTrue(javaCode.contains("Integer id,"))
        assertTrue(javaCode.contains("String name"))
        assertTrue(javaCode.contains(") {"))
        assertTrue(javaCode.contains("User(int id) {"))
        assertTrue(javaCode.contains("this(id, \"Anonymous\");"))
    }

    @Test
    fun testSwitchStatement() {
        val file = file("SwitchModule") {
            function("test") {
                arg("x", integer)
                switch("x") {
                    case(literal(1)) {
                        print("\"one\"")
                        returns("\"ONE\"")
                    }
                    case(literal(2)) {
                        assign("y", 22)
                        returns("y")
                    }
                    default {
                        returns("\"MANY\"")
                    }
                }
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)

        println("--- Java (Switch) ---")
        println(javaCode)
        println("--- Python (Switch) ---")
        println(pythonCode)
        println("--- TypeScript (Switch) ---")
        println(tsCode)

        // Java
        assertTrue(javaCode.contains("switch (x) {"))
        assertTrue(javaCode.contains("case 1 -> {"))
        assertTrue(javaCode.contains("System.out.println(\"one\");"))
        assertTrue(javaCode.contains("return \"ONE\";"))
        assertTrue(javaCode.contains("default -> {"))
        assertTrue(javaCode.contains("return \"MANY\";"))
        // Python
        assertTrue(pythonCode.contains("match x:"))
        assertTrue(pythonCode.contains("case 1:"))
        assertTrue(pythonCode.contains("print('one')"))
        assertTrue(pythonCode.contains("return 'ONE'"))
        assertTrue(pythonCode.contains("case 2:"))
        assertTrue(pythonCode.contains("y = 22"))
        assertTrue(pythonCode.contains("return y"))
        assertTrue(pythonCode.contains("case _:"))
        assertTrue(pythonCode.contains("return 'MANY'"))
        // TypeScript
        assertTrue(tsCode.contains("switch (x) {"))
        assertTrue(tsCode.contains("case 1:"))
        assertTrue(tsCode.contains("console.log('one');"))
        assertTrue(tsCode.contains("return 'ONE';"))
        assertTrue(tsCode.contains("case 2:"))
        assertTrue(tsCode.contains("const y = 22;"))
        assertTrue(tsCode.contains("return y;"))
        assertTrue(tsCode.contains("default:"))
    }

    @Test
    fun testErrorStatement() {
        val file = file("ErrorModule") {
            function("test") {
                error("\"Something went wrong\"")
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)

        println("--- Java (Error) ---")
        println(javaCode)
        println("--- Python (Error) ---")
        println(pythonCode)
        println("--- TypeScript (Error) ---")
        println(tsCode)

        // Java
        assertTrue(javaCode.contains("throw new IllegalStateException(\"Something went wrong\");"))

        // Python
        assertTrue(pythonCode.contains("raise Exception('Something went wrong')"))

        // TypeScript
        assertTrue(tsCode.contains("throw new Error('Something went wrong');"))
    }

    @Test
    fun testStructExtends() {
        val file = file("ExtendsModule") {
            struct("User", interfaces = type("BaseUser")) {
                field("id", integer)
            }
            struct("Data", interfaces = type("BaseData")) {
                field("value", string)
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)

        println("--- Java (Struct Extends) ---")
        println(javaCode)
        println("--- TypeScript (Struct Extends) ---")
        println(tsCode)
        println("--- Python (Struct Extends) ---")
        println(pythonCode)

        assertTrue(javaCode.contains("public record User"))
        assertTrue(javaCode.contains(") implements BaseUser {"))
        assertTrue(javaCode.contains("public record Data"))
        assertTrue(javaCode.contains(") implements BaseData {"))

        assertTrue(tsCode.contains("class User extends BaseUser {"))
        assertTrue(tsCode.contains("class Data extends BaseData {"))

        assertTrue(pythonCode.contains("class User(BaseUser):"))
        assertTrue(pythonCode.contains("class Data(BaseData):"))
    }

    @Test
    fun testImport() {
        val file = file("ImportModule") {
            import("java.util.List")
            import("com.example.Other")
            struct("MyStruct") {
                field("other", type("Other"))
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)

        println("--- Java (Import) ---")
        println(javaCode)
        println("--- Python (Import) ---")
        println(pythonCode)
        println("--- TypeScript (Import) ---")
        println(tsCode)

        assertTrue(javaCode.contains("import java.util.List;"))
        assertTrue(javaCode.contains("import com.example.Other;"))

        assertTrue(pythonCode.contains("import java.util.List"))
        assertTrue(pythonCode.contains("import com.example.Other"))

        assertTrue(tsCode.contains("import java.util.List;"))
        assertTrue(tsCode.contains("import com.example.Other;"))
    }

    @Test
    fun testPackage() {
        val file = file("PackageModule") {
            `package`("com.example.test")
            struct("MyStruct") {
                field("id", integer)
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)

        println("--- Java (Package) ---")
        println(javaCode)
        println("--- Python (Package) ---")
        println(pythonCode)
        println("--- TypeScript (Package) ---")
        println(tsCode)

        assertTrue(javaCode.contains("package com.example.test;"))
        assertTrue(pythonCode.contains("# package com.example.test"))
        assertTrue(tsCode.contains("// package com.example.test"))
    }

    @Test
    fun testUnion() {
        val file = file("UnionModule") {
            union("Response") {
                member("Success")
                member("Error")
            }
            struct("Success") {
                field("data", string)
            }
            struct("Error") {
                field("message", string)
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)

        println("--- Java (Union) ---")
        println(javaCode)
        println("--- Python (Union) ---")
        println(pythonCode)
        println("--- TypeScript (Union) ---")
        println(tsCode)

        // Java
        assertTrue(javaCode.contains("public sealed interface Response {}"))
        assertTrue(javaCode.contains("public record Success"))
        assertTrue(javaCode.contains(") implements Response {"))
        assertTrue(javaCode.contains("public record Error"))
        assertTrue(javaCode.contains(") implements Response {"))

        // Python
        assertTrue(pythonCode.contains("class Response:"))
        assertTrue(pythonCode.contains("class Success(Response):"))
        assertTrue(pythonCode.contains("class Error(Response):"))

        // TypeScript
        assertTrue(tsCode.contains("interface Response {}"))
        assertTrue(tsCode.contains("class Success implements Response {"))
        assertTrue(tsCode.contains("class Error implements Response {"))
    }

    @Test
    fun testMultipleUnions() {
        val file = file("MultipleUnionsModule") {
            union("Response") {
                member("Response2XX")
            }
            union("Response2XX") {
                member("Response200")
            }
            struct("Response200") {
                field("body", string)
            }
        }

        val javaCode = JavaGenerator.generate(file)
        println("--- Java (Multiple Unions) ---")
        println(javaCode)

        assertTrue(javaCode.contains("public sealed interface Response {}"))
        assertTrue(javaCode.contains("public sealed interface Response2XX extends Response {}"))
        assertTrue(javaCode.contains("public record Response200"))
        assertTrue(javaCode.contains("String body"))
        assertTrue(javaCode.contains(") implements Response2XX {"))
    }

    @Test
    fun testMultiDimensionalUnions() {
        val file = file("MultiDimensionalModule") {
            union("UnionA") {
                member("SharedStruct")
            }
            union("UnionB") {
                member("SharedStruct")
            }
            struct("SharedStruct") {
                field("id", string)
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)

        println("--- Java (Multi-Dimensional Unions) ---")
        println(javaCode)
        println("--- TypeScript (Multi-Dimensional Unions) ---")
        println(tsCode)
        println("--- Python (Multi-Dimensional Unions) ---")
        println(pythonCode)

        // Java
        assertTrue(javaCode.contains("public record SharedStruct"))
        assertTrue(javaCode.contains(") implements UnionA, UnionB {"))

        // TypeScript
        assertTrue(tsCode.contains("class SharedStruct implements UnionA, UnionB {"))

        // Python
        assertTrue(pythonCode.contains("class SharedStruct(UnionA, UnionB):"))
    }

    @Test
    fun testGenericsInExtends() {
        val file = file("GenericsExtendsModule") {
            struct("Box", interfaces = type("BaseBox", string)) {
                field("value", string)
            }
            static("Api", extends = type("BaseApi", integer)) {
                function("getData", string)
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)

        println("--- Java (Generics Extends) ---")
        println(javaCode)
        println("--- Python (Generics Extends) ---")
        println(pythonCode)
        println("--- TypeScript (Generics Extends) ---")
        println(tsCode)

        assertTrue(javaCode.contains("public record Box"))
        assertTrue(javaCode.contains(") implements BaseBox<String> {"))
        assertTrue(javaCode.contains("public interface Api extends BaseApi<Integer> {"))

        assertTrue(pythonCode.contains("class Box(BaseBox[str]):"))
        assertTrue(pythonCode.contains("class Api(BaseApi[int]):"))

        assertTrue(tsCode.contains("class Box extends BaseBox<string> {"))
        assertTrue(tsCode.contains("interface Api extends BaseApi<number> {"))
    }

    @Test
    fun testEmptyStruct() {
        val file = file("EmptyStructModule") {
            struct("Empty")
        }

        val javaCode = JavaGenerator.generate(file)
        println("--- Java (Empty Struct) ---")
        println(javaCode)

        assertTrue(javaCode.contains("public record Empty"), "Should be a record")
    }
}
