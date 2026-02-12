package community.flock.wirespec.language.generator

import community.flock.wirespec.language.core.FieldCall
import community.flock.wirespec.language.core.FunctionCall
import community.flock.wirespec.language.core.LiteralList
import community.flock.wirespec.language.core.NullCheck
import community.flock.wirespec.language.core.RawExpression
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.VariableReference
import community.flock.wirespec.language.core.file
import kotlin.test.Test
import kotlin.test.assertTrue

class DslTest {

    @Test
    fun testNestedNullable() {
        val file = file("NestedNullableModule") {
            struct("Data") {
                field("tags", list(string.nullable()))
            }
            function("process") {
                returnType(string.nullable())
                arg("input", list(string.nullable()).nullable())
                returns(RawExpression("null"))
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

        assertTrue(tsCode.contains("\"tags\": (string | undefined)[],"))
        assertTrue(tsCode.contains("export function process(input: (string | undefined)[] | undefined): string | undefined"))
    }

    @Test
    fun testDslAndGeneration() {
        val file = file("MyModule") {
            struct("User") {
                field("id", integer)
                field("name", string.nullable())
            }

            function("greet") {
                returnType(string)
                arg("user", type("User"))
                print(RawExpression("\"Greeting \" + user.name"))
                returns(RawExpression("user.name"))
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

        assertTrue(pythonCode.contains("@dataclass"))
        assertTrue(pythonCode.contains("class User:"))
        assertTrue(pythonCode.contains("id: int"))
        assertTrue(pythonCode.contains("name: Optional[str]"))
        assertTrue(pythonCode.contains("def greet(user: User) -> str:"))

        assertTrue(tsCode.contains("export type User = {"))
        assertTrue(tsCode.contains("\"id\": number,"))
        assertTrue(tsCode.contains("\"name\": string | undefined,"))
        assertTrue(tsCode.contains("export function greet(user: User): string {"))
    }

    @Test
    fun testStaticElement() {
        val file = file("StaticModule") {
            static("MyService", extends = type("BaseService")) {
                struct("Config") {
                    field("url", string)
                }
                function("start") {
                    print(literal("Starting service"))
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

        assertTrue(tsCode.contains("export namespace MyService {"))
        assertTrue(tsCode.contains("export type Config = {"))
        assertTrue(tsCode.contains("\"url\": string,"))
        assertTrue(tsCode.contains("export function start() {"))
        assertTrue(tsCode.contains("console.log('Starting service');"))

        assertTrue(pythonCode.contains("class MyService(BaseService):"))
        assertTrue(pythonCode.contains("@dataclass"))
        assertTrue(pythonCode.contains("class Config:"))
        assertTrue(pythonCode.contains("url: str"))
    }

    @Test
    fun testNestedStatic() {
        val file = file("NestedModule") {
            static("Outer", type("Base")) {
                static("Inner") {
                    function("test") {
                        returns(RawExpression("1"))
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
        assertTrue(tsCode.contains("export namespace Outer {"))
        assertTrue(tsCode.contains("export namespace Inner {"))
        assertTrue(tsCode.contains("export function test() {"))

        // Python
        assertTrue(pythonCode.contains("class Outer(Base):"))
        assertTrue(pythonCode.contains("class Inner:"))
        assertTrue(pythonCode.contains("@staticmethod"))
        assertTrue(pythonCode.contains("def test():"))
    }

    @Test
    fun testAsyncFunction() {
        val file = file("AsyncModule") {
            asyncFunction("fetchData") {
                returnType(string)
                arg("id", integer)
                returns(literal("data"))
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

        assertTrue(javaCode.contains("java.util.concurrent.CompletableFuture<String> fetchData(Integer id) {"))
        assertTrue(pythonCode.contains("async def fetchData(id: int) -> str:"))
        assertTrue(tsCode.contains("export async function fetchData(id: number): string {"))
    }

    @Test
    fun testFunctionInterface() {
        val file = file("InterfaceModule") {
            static("Api") {
                function("getData") {
                    returnType(string)
                }
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
        assertTrue(pythonCode.contains("@staticmethod"))
        assertTrue(pythonCode.contains("def getData() -> str:"))
        assertTrue(pythonCode.contains("..."))
        assertTrue(pythonCode.contains("async def postData(data: str):"))

        // TypeScript
        assertTrue(tsCode.contains("export namespace Api {"))
        assertTrue(tsCode.contains("getData(): string;"))
        assertTrue(tsCode.contains("postData(data: string): Promise<void>;"))
    }

    @Test
    fun testReturnStatement() {
        val file = file("ReturnModule") {
            function("test") {
                returns(
                    functionCall("myFunc") {
                        arg("a", RawExpression("1"))
                    },
                )
            }
            function("test2") {
                returns(
                    construct(type("User")) {
                        arg("id", RawExpression("1"))
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
        assertTrue(tsCode.contains("return myFunc(1);"))
        assertTrue(tsCode.contains("return { id: 1 };"))
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

            function("process") {
                returnType(boolean)
                arg("count", integer)
                arg("name", string)
                print(literal("count: count"))
                returns(literal(true))
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
        assertTrue(javaCode.contains("Boolean process(Integer count, String name) {"))
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
        assertTrue(tsCode.contains("return { id: 1, name: 'John' };"))
    }

    @Test
    fun testArrayType() {
        val file = file("ArrayModule") {
            struct("User") {
                field("tags", list(string))
                field("scores", list(integer))
            }

            function("process") {
                returnType(list(boolean))
                arg("tags", list(string))
                returns(RawExpression("tags"))
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
        assertTrue(pythonCode.contains("def process(tags: list[str]) -> list[bool]:"))

        // TypeScript
        assertTrue(tsCode.contains("\"tags\": string[],"))
        assertTrue(tsCode.contains("\"scores\": number[],"))
        assertTrue(tsCode.contains("export function process(tags: string[]): boolean[] {"))
    }

    @Test
    fun testAssignmentStatement() {
        val file = file("AssignmentModule") {
            function("test") {
                assign("x", literal(1, integer))
                assign(
                    "y",
                    functionCall("myFunc") {
                        arg("a", RawExpression("1"))
                    },
                )
                returns(RawExpression("x"))
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
        assertTrue(tsCode.contains("const y = myFunc(1);"))
    }

    @Test
    fun testLiteralList() {
        val file = file("LiteralListModule") {
            function("test") {
                assign("tags", listOf(listOf(literal("tag1"), literal("tag2")), string))
                returns(RawExpression("tags"))
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
                assign("scores", mapOf(mapOf("Alice" to literal(10), "Bob" to literal(20)), string, integer))
                returns(RawExpression("scores"))
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

                functionCall("myFunc") {
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
        assertTrue(tsCode.contains("const emptyMap = {};"))
        assertTrue(tsCode.contains("myFunc([], {});"))
        assertTrue(tsCode.contains("{ tags: [] };"))
    }

    @Test
    fun testStructConstructor() {
        val file = file("StructConstructorModule") {
            struct("User") {
                field("id", integer)
                field("name", string)
                constructo {
                    arg("id", integer)
                    assign("this.id", RawExpression("id"))
                    assign("this.name", literal("Anonymous"))
                }
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)
        val kotlinCode = KotlinGenerator.generate(file)

        println("--- Java (Struct Constructor) ---")
        println(javaCode)
        println("--- Python (Struct Constructor) ---")
        println(pythonCode)
        println("--- TypeScript (Struct Constructor) ---")
        println(tsCode)
        println("--- Kotlin (Struct Constructor) ---")
        println(kotlinCode)

        // Java
        assertTrue(javaCode.contains("public record User"))
        assertTrue(javaCode.contains("Integer id,"))
        assertTrue(javaCode.contains("String name"))
        assertTrue(javaCode.contains(") {"))
        assertTrue(javaCode.contains("User(Integer id) {"))
        assertTrue(javaCode.contains("this(id, \"Anonymous\");"))

        // Kotlin
        assertTrue(kotlinCode.contains("data class User("))
        assertTrue(kotlinCode.contains("val id: Int,"))
        assertTrue(kotlinCode.contains("val name: String"))
        assertTrue(kotlinCode.contains("constructor(id: Int) : this(id, \"Anonymous\")"))
    }

    @Test
    fun testSwitchStatement() {
        val file = file("SwitchModule") {
            function("test") {
                arg("x", integer)
                switch(RawExpression("x")) {
                    case(literal(1)) {
                        print(literal("one"))
                        returns(literal("ONE"))
                    }
                    case(literal(2)) {
                        assign("y", literal(22))
                        returns(RawExpression("y"))
                    }
                    default {
                        returns(literal("MANY"))
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
                error(literal("Something went wrong"))
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
            struct("User") {
                implements(type("BaseUser"))
                field("id", integer)
            }
            struct("Data") {
                implements(type("BaseData"))
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

        assertTrue(tsCode.contains("export type User = {"))
        assertTrue(tsCode.contains("export type Data = {"))

        assertTrue(pythonCode.contains("class User(BaseUser):"))
        assertTrue(pythonCode.contains("class Data(BaseData):"))
    }

    @Test
    fun testImport() {
        val file = file("ImportModule") {
            import("java.util", "List")
            import("com.example", "Other")
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

        assertTrue(pythonCode.contains("from java.util import List"))
        assertTrue(pythonCode.contains("from com.example import Other"))

        assertTrue(tsCode.contains("import { List } from 'java.util';"))
        assertTrue(tsCode.contains("import { Other } from 'com.example';"))
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
        // TypeScript: packages are not emitted (they have no meaning in TS)
        assertTrue(tsCode.contains("export type MyStruct = {"))
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
        assertTrue(javaCode.contains("public sealed interface Response permits Success, Error {}"))
        assertTrue(javaCode.contains("public record Success"))
        assertTrue(javaCode.contains(") implements Response {"))
        assertTrue(javaCode.contains("public record Error"))
        assertTrue(javaCode.contains(") implements Response {"))

        // Python
        assertTrue(pythonCode.contains("class Response:"))
        assertTrue(pythonCode.contains("class Success(Response):"))
        assertTrue(pythonCode.contains("class Error(Response):"))

        // TypeScript
        assertTrue(tsCode.contains("export type Response = Success | Error"))
        assertTrue(tsCode.contains("export type Success = {"))
        assertTrue(tsCode.contains("export type Error = {"))
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

        assertTrue(javaCode.contains("public sealed interface Response permits Response2XX {}"))
        assertTrue(javaCode.contains("public sealed interface Response2XX extends Response permits Response200 {}"))
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
        assertTrue(tsCode.contains("export type SharedStruct = {"))

        // Python
        assertTrue(pythonCode.contains("class SharedStruct(UnionA, UnionB):"))
    }

    @Test
    fun testGenericsInExtends() {
        val file = file("GenericsExtendsModule") {
            struct("Box") {
                implements(type("BaseBox", string))
                field("value", string)
            }
            static("Api", extends = type("BaseApi", integer)) {
                function("getData") {
                    returnType(string)
                }
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

        assertTrue(tsCode.contains("export type Box = {"))
        assertTrue(tsCode.contains("export namespace Api {"))
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

    @Test
    fun testNestedStructInline() {
        val file = file("NestedStructModule") {
            struct("Response201") {
                field("status", integer)
                struct("Headers") {
                    field("token", type("Token"))
                    field("refreshToken", type("Token").nullable())
                }
                field("headers", type("Headers"))
                field("body", type("TodoDto"))
            }
        }

        val tsCode = TypeScriptGenerator.generate(file)

        println("--- TypeScript (Nested Struct Inline) ---")
        println(tsCode)

        // The nested Headers struct should be inlined as an anonymous object type
        assertTrue(tsCode.contains("\"headers\": {\"token\": Token, \"refreshToken\": Token | undefined},"))
        // There should be no separate "export type Headers" declaration
        assertTrue(!tsCode.contains("export type Headers"))
        // The parent struct should still be emitted normally
        assertTrue(tsCode.contains("export type Response201 = {"))
        assertTrue(tsCode.contains("\"status\": number,"))
        assertTrue(tsCode.contains("\"body\": TodoDto,"))
    }

    @Test
    fun testNullCheck() {
        // Build AST: NullCheck on request.queries.page with a function call body and empty list fallback
        val nullCheckExpr = NullCheck(
            expression = FieldCall(FieldCall(VariableReference("request"), "queries"), "page"),
            body = FunctionCall(
                receiver = VariableReference("serialization"),
                name = "serializeParam",
                arguments = mapOf(
                    "value" to VariableReference("it"),
                ),
            ),
            alternative = LiteralList(emptyList(), Type.String),
        )

        val file = file("NullCheckModule") {
            function("test") {
                returns(nullCheckExpr)
            }
        }

        val javaCode = JavaGenerator.generate(file)
        val kotlinCode = KotlinGenerator.generate(file)
        val tsCode = TypeScriptGenerator.generate(file)
        val pythonCode = PythonGenerator.generate(file)

        println("--- Java (NullCheck) ---")
        println(javaCode)
        println("--- Kotlin (NullCheck) ---")
        println(kotlinCode)
        println("--- TypeScript (NullCheck) ---")
        println(tsCode)
        println("--- Python (NullCheck) ---")
        println(pythonCode)

        // Java: Optional.ofNullable(...).map(it -> ...).orElse(...)
        assertTrue(javaCode.contains("java.util.Optional.ofNullable(request.queries().page())"))
        assertTrue(javaCode.contains(".map(it -> serialization.serializeParam(it))"))
        assertTrue(javaCode.contains(".orElse(java.util.List.of())"))

        // Kotlin: expression?.let { body } ?: alternative
        assertTrue(kotlinCode.contains("request.queries.page?.let { serialization.serializeParam(it) } ?: listOf()"))

        // TypeScript: ternary with inlined expression
        assertTrue(tsCode.contains("request.queries.page != null"))
        assertTrue(tsCode.contains("serialization.serializeParam(request.queries.page)"))
        assertTrue(tsCode.contains(": []"))

        // Python: conditional with inlined expression
        assertTrue(pythonCode.contains("serialization.serializeParam(request.queries.page)"))
        assertTrue(pythonCode.contains("if request.queries.page is not None else"))
        assertTrue(pythonCode.contains("[]"))
    }

    @Test
    fun testDataObjectWithFields() {
        val file = file("DataObjectModule") {
            struct("Response400") {
                implements(type("Response4XX", Type.Unit))
                implements(type("ResponseUnit"))
                field("status", integer, isOverride = true)
                field("headers", type("Headers"), isOverride = true)
                field("body", Type.Unit, isOverride = true)
                constructo {
                    assign("this.status", RawExpression("400"))
                    assign("this.headers", RawExpression("Headers"))
                    assign("this.body", RawExpression("Unit"))
                }
                struct("Headers") {
                    implements(type("Wirespec.Response.Headers"))
                }
            }
        }

        val kotlinCode = KotlinGenerator.generate(file)

        println("--- Kotlin (Data Object With Fields) ---")
        println(kotlinCode)

        // Should be data object, not data class
        assertTrue(kotlinCode.contains("data object Response400"))
        // Should implement interfaces
        assertTrue(kotlinCode.contains("Response4XX<Unit>"))
        assertTrue(kotlinCode.contains("ResponseUnit"))
        // Override fields with default values from constructor
        assertTrue(kotlinCode.contains("override val status: Int = 400"))
        assertTrue(kotlinCode.contains("override val headers: Headers = Headers"))
        assertTrue(kotlinCode.contains("override val body: Unit = Unit"))
        // Nested struct should be present
        assertTrue(kotlinCode.contains("object Headers : Wirespec.Response.Headers"))
        // Should NOT contain data class
        assertTrue(!kotlinCode.contains("data class Response400"))
    }
}
