package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WirespecType
import community.flock.wirespec.compiler.core.tokenize.Precision.P64
import kotlin.test.Test

class TokenizeAnnotationTest {

    @Test
    fun testSimpleAnnotation() = testTokenizer(
        "@deprecated",
        Annotation,
        EndOfProgram,
    )

    @Test
    fun testAnnotationWithType() = testTokenizer(
        """
        |@deprecated
        |type User = String
        """.trimMargin(),
        Annotation,
        TypeDefinition,
        WirespecType,
        Equals,
        WsString,
        EndOfProgram,
    )

    @Test
    fun testMultipleAnnotations() = testTokenizer(
        """
        |@deprecated
        |@internal
        |type User = String
        """.trimMargin(),
        Annotation,
        Annotation,
        TypeDefinition,
        WirespecType,
        Equals,
        WsString,
        EndOfProgram,
    )

    @Test
    fun testAnnotationWithParameters() = testTokenizer(
        """
        |@since("1.0.0")
        |type User = String
        """.trimMargin(),
        Annotation, LeftParenthesis, LiteralString, RightParenthesis,
        TypeDefinition, WirespecType, Equals, WsString,
        EndOfProgram,
    )

    @Test
    fun testAnnotationOnEndpoint() = testTokenizer(
        """
        |@authenticated
        |endpoint GetUser GET /user/{id} -> User
        """.trimMargin(),
        Annotation,
        EndpointDefinition, WirespecType, Method, Path, ForwardSlash, LeftCurly, DromedaryCaseIdentifier, RightCurly, Arrow, WirespecType,
        EndOfProgram,
    )

    @Test
    fun testAnnotationOnEnum() = testTokenizer(
        """
        |@deprecated
        |enum Status {
        |  ACTIVE,
        |  INACTIVE
        |}
        """.trimMargin(),
        Annotation,
        EnumTypeDefinition, WirespecType, LeftCurly,
        WirespecType, Comma,
        WirespecType,
        RightCurly,
        EndOfProgram,
    )

    @Test
    fun testAnnotationOnField() = testTokenizer(
        """
        |type User {
        |  @required
        |  name: String,
        |  @optional
        |  email: String?
        |}
        """.trimMargin(),
        TypeDefinition, WirespecType, LeftCurly,
        Annotation,
        DromedaryCaseIdentifier, Colon, WsString, Comma,
        Annotation,
        DromedaryCaseIdentifier, Colon, WsString, QuestionMark,
        RightCurly,
        EndOfProgram,
    )

    @Test
    fun testComplexAnnotationWithMultipleParameters() = testTokenizer(
        """
        |@validate(min: 0, max: 100)
        |type Age = Integer
        """.trimMargin(),
        Annotation, LeftParenthesis,
        DromedaryCaseIdentifier, Colon, Integer, Comma,
        DromedaryCaseIdentifier, Colon, Integer, RightParenthesis,
        TypeDefinition, WirespecType, Equals, WsInteger(P64),
        EndOfProgram,
    )

    @Test
    fun testAnnotationWithSingleQuotedString() = testTokenizer(
        """
        |@description("This is a description")
        |type User = String
        """.trimMargin(),
        Annotation, LeftParenthesis, LiteralString, RightParenthesis,
        TypeDefinition, WirespecType, Equals, WsString,
        EndOfProgram,
    )

    @Test
    fun testAnnotationWithEscapedQuotesInString() = testTokenizer(
        """
        |@example("Say \"Hello World\"")
        |type Message = String
        """.trimMargin(),
        Annotation, LeftParenthesis, LiteralString, RightParenthesis,
        TypeDefinition, WirespecType, Equals, WsString,
        EndOfProgram,
    )

    @Test
    fun testAnnotationWithMixedParameterTypes() = testTokenizer(
        """
        |@config(name: "database", port: 5432, enabled: true)
        |type Config = String
        """.trimMargin(),
        Annotation, LeftParenthesis,
        DromedaryCaseIdentifier, Colon, LiteralString, Comma,
        DromedaryCaseIdentifier, Colon, Integer, Comma,
        DromedaryCaseIdentifier, Colon, DromedaryCaseIdentifier, RightParenthesis,
        TypeDefinition, WirespecType, Equals, WsString,
        EndOfProgram,
    )

    @Test
    fun testMultipleAnnotationsWithLiteralStrings() = testTokenizer(
        """
        |@description("User entity")
        |@example("John Doe")
        |type User = String
        """.trimMargin(),
        Annotation, LeftParenthesis, LiteralString, RightParenthesis,
        Annotation, LeftParenthesis, LiteralString, RightParenthesis,
        TypeDefinition, WirespecType, Equals, WsString,
        EndOfProgram,
    )

    @Test
    fun testAnnotationWithEmptyString() = testTokenizer(
        """
        |@default("")
        |type OptionalField = String
        """.trimMargin(),
        Annotation, LeftParenthesis, LiteralString, RightParenthesis,
        TypeDefinition, WirespecType, Equals, WsString,
        EndOfProgram,
    )

    @Test
    fun testAnnotationWithStringContainingSpecialCharacters() = testTokenizer(
        """
        |@pattern("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$")
        |type Email = String
        """.trimMargin(),
        Annotation, LeftParenthesis, LiteralString, RightParenthesis,
        TypeDefinition, WirespecType, Equals, WsString,
        EndOfProgram,
    )
}
