package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WirespecType
import community.flock.wirespec.compiler.core.tokenize.LeftBracket
import community.flock.wirespec.compiler.core.tokenize.Precision.P64
import community.flock.wirespec.compiler.core.tokenize.RightBracket
import kotlin.test.Test

class TokenizeAnnotationTest {

    @Test
    fun testSimpleAnnotation() = testTokenizer(
        "@Deprecated",
        Annotation,
        EndOfProgram,
    )

    @Test
    fun testAnnotationWithType() = testTokenizer(
        """
        |@Deprecated
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
        |@Deprecated
        |@Internal
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
        |@Since("1.0.0")
        |type User = String
        """.trimMargin(),
        Annotation, LeftParenthesis, LiteralString, RightParenthesis,
        TypeDefinition, WirespecType, Equals, WsString,
        EndOfProgram,
    )

    @Test
    fun testAnnotationOnEndpoint() = testTokenizer(
        """
        |@Authenticated
        |endpoint GetUser GET /user/{id} -> User
        """.trimMargin(),
        Annotation,
        EndpointDefinition, WirespecType, Method, Path, ForwardSlash, LeftCurly, DromedaryCaseIdentifier, RightCurly, Arrow, WirespecType,
        EndOfProgram,
    )

    @Test
    fun testAnnotationOnEnum() = testTokenizer(
        """
        |@Deprecated
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
        |  @Required
        |  name: String,
        |  @Optional
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
        |@Validate(min: 0, max: 100)
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
        |@Description("This is a description")
        |type User = String
        """.trimMargin(),
        Annotation, LeftParenthesis, LiteralString, RightParenthesis,
        TypeDefinition, WirespecType, Equals, WsString,
        EndOfProgram,
    )

    @Test
    fun testAnnotationWithEscapedQuotesInString() = testTokenizer(
        """
        |@Example("Say \"Hello World\"")
        |type Message = String
        """.trimMargin(),
        Annotation, LeftParenthesis, LiteralString, RightParenthesis,
        TypeDefinition, WirespecType, Equals, WsString,
        EndOfProgram,
    )

    @Test
    fun testAnnotationWithMixedParameterTypes() = testTokenizer(
        """
        |@Config(name: "database", port: 5432, enabled: true)
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
        |@Description("User entity")
        |@Example("John Doe")
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
        |@Default("")
        |type OptionalField = String
        """.trimMargin(),
        Annotation, LeftParenthesis, LiteralString, RightParenthesis,
        TypeDefinition, WirespecType, Equals, WsString,
        EndOfProgram,
    )

    @Test
    fun testAnnotationWithStringContainingSpecialCharacters() = testTokenizer(
        """
        |@Pattern("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$")
        |type Email = String
        """.trimMargin(),
        Annotation, LeftParenthesis, LiteralString, RightParenthesis,
        TypeDefinition, WirespecType, Equals, WsString,
        EndOfProgram,
    )

    @Test
    fun testAnnotationWithArrayParameter() = testTokenizer(
        """
        |@Tag(["TagA", "TagB"])
        |type User = String
        """.trimMargin(),
        Annotation, LeftParenthesis, LeftBracket, LiteralString, Comma, LiteralString, RightBracket, RightParenthesis,
        TypeDefinition, WirespecType, Equals, WsString,
        EndOfProgram,
    )

    @Test
    fun testAnnotationWithNamedArrayParameter() = testTokenizer(
        """
        |@Security(roles: ["RoleA", "RoleB"])
        |type Config = String
        """.trimMargin(),
        Annotation, LeftParenthesis, DromedaryCaseIdentifier, Colon, LeftBracket, LiteralString, Comma, LiteralString, RightBracket, RightParenthesis,
        TypeDefinition, WirespecType, Equals, WsString,
        EndOfProgram,
    )

    @Test
    fun testAnnotationWithDictParameter() = testTokenizer(
        """
        |@Test(dict: {test: "hello"})
        |type Hello = Integer
        """.trimMargin(),
        Annotation, LeftParenthesis,
        DromedaryCaseIdentifier, Colon,
        LeftCurly, DromedaryCaseIdentifier, Colon, LiteralString, RightCurly,
        RightParenthesis,
        TypeDefinition, WirespecType, Equals, WsInteger(P64),
        EndOfProgram,
    )
}
