package community.flock.wirespec.ir.core

import kotlin.test.Test
import kotlin.test.assertEquals

class NameTest {

    @Test
    fun testOfSplitsHelloWorld() {
        assertEquals(listOf("Hello", "World"), Name.of("HelloWorld").parts)
    }

    @Test
    fun testOfSplitsUSA() {
        assertEquals(listOf("USA"), Name.of("USA").parts)
    }

    @Test
    fun testOfSplitsCodeUUID() {
        assertEquals(listOf("code", "UUID"), Name.of("codeUUID").parts)
    }

    @Test
    fun testOfSplitsFirstName() {
        assertEquals(listOf("first", "_", "name"), Name.of("first_name").parts)
    }

    @Test
    fun testOfSplitsSomeField() {
        assertEquals(listOf("some", "-", "field"), Name.of("some-field").parts)
    }

    @Test
    fun testOfSplitsGetHTTPResponse() {
        assertEquals(listOf("get", "HTTP", "Response"), Name.of("getHTTPResponse").parts)
    }

    @Test
    fun testOfSplitsRequestColonNew() {
        assertEquals(listOf("Request", "::", "new"), Name.of("Request::new").parts)
    }

    @Test
    fun testOfSplitsSimple() {
        assertEquals(listOf("simple"), Name.of("simple").parts)
    }

    @Test
    fun testOfSplitsCamelCase() {
        assertEquals(listOf("first", "Name"), Name.of("firstName").parts)
    }

    @Test
    fun testOfSplitsHTMLParser() {
        assertEquals(listOf("HTML", "Parser"), Name.of("HTMLParser").parts)
    }

    @Test
    fun testOfSplitsUnderscoreClass() {
        assertEquals(listOf("_", "class"), Name.of("_class").parts)
    }

    // value() tests

    @Test
    fun testValueHelloWorld() {
        assertEquals("HelloWorld", Name.of("HelloWorld").value())
    }

    @Test
    fun testValueFirstName() {
        assertEquals("first_name", Name.of("first_name").value())
    }

    @Test
    fun testValueSomeField() {
        assertEquals("some-field", Name.of("some-field").value())
    }

    @Test
    fun testValueUSA() {
        assertEquals("USA", Name.of("USA").value())
    }

    @Test
    fun testValueCodeUUID() {
        assertEquals("codeUUID", Name.of("codeUUID").value())
    }

    @Test
    fun testValueRequestColonNew() {
        assertEquals("Request::new", Name.of("Request::new").value())
    }

    @Test
    fun testValueSinglePart() {
        assertEquals("bar", Name(listOf("bar")).value())
    }

    @Test
    fun testValueMultipleParts() {
        assertEquals("HelloWorld", Name("Hello", "World").value())
    }

    // camelCase() tests

    @Test
    fun testCamelCaseHelloWorld() {
        assertEquals("helloWorld", Name.of("HelloWorld").camelCase())
    }

    @Test
    fun testCamelCaseFirstName() {
        assertEquals("firstName", Name.of("first_name").camelCase())
    }

    @Test
    fun testCamelCaseUSA() {
        assertEquals("uSA", Name.of("USA").camelCase())
    }

    @Test
    fun testCamelCaseCodeUUID() {
        assertEquals("codeUUID", Name.of("codeUUID").camelCase())
    }

    @Test
    fun testCamelCaseGetHTTPResponse() {
        assertEquals("getHTTPResponse", Name.of("getHTTPResponse").camelCase())
    }

    @Test
    fun testCamelCaseSimple() {
        assertEquals("simple", Name.of("simple").camelCase())
    }

    @Test
    fun testCamelCaseVararg() {
        assertEquals("helloWorld", Name("Hello", "World").camelCase())
    }

    // pascalCase() tests

    @Test
    fun testPascalCaseHelloWorld() {
        assertEquals("HelloWorld", Name.of("HelloWorld").pascalCase())
    }

    @Test
    fun testPascalCaseFirstName() {
        assertEquals("FirstName", Name.of("first_name").pascalCase())
    }

    @Test
    fun testPascalCaseSimple() {
        assertEquals("Simple", Name.of("simple").pascalCase())
    }

    @Test
    fun testPascalCaseVararg() {
        assertEquals("HelloWorld", Name("hello", "world").pascalCase())
    }

    // snakeCase() tests

    @Test
    fun testSnakeCaseHelloWorld() {
        assertEquals("hello_world", Name.of("HelloWorld").snakeCase())
    }

    @Test
    fun testSnakeCaseFirstName() {
        assertEquals("first_name", Name.of("first_name").snakeCase())
    }

    @Test
    fun testSnakeCaseCodeUUID() {
        assertEquals("code_uuid", Name.of("codeUUID").snakeCase())
    }

    @Test
    fun testSnakeCaseGetHTTPResponse() {
        assertEquals("get_http_response", Name.of("getHTTPResponse").snakeCase())
    }

    @Test
    fun testSnakeCaseSimple() {
        assertEquals("simple", Name.of("simple").snakeCase())
    }

    @Test
    fun testSnakeCaseVararg() {
        assertEquals("hello_world", Name("Hello", "World").snakeCase())
    }
}
