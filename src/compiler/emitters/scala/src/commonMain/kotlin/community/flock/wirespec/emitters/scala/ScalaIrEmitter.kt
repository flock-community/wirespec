package community.flock.wirespec.emitters.scala

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.HasPackageName
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.needImports
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.Shared
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.ir.converter.convert
import community.flock.wirespec.ir.converter.convertConstraint
import community.flock.wirespec.ir.converter.convertWithValidation
import community.flock.wirespec.ir.core.Assignment
import community.flock.wirespec.ir.core.Constructor
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.findAll
import community.flock.wirespec.ir.core.findElement
import community.flock.wirespec.ir.core.function
import community.flock.wirespec.ir.core.raw
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.withLabelField
import community.flock.wirespec.ir.emit.IrEmitter
import community.flock.wirespec.ir.generator.ScalaGenerator
import community.flock.wirespec.ir.generator.generateScala
import community.flock.wirespec.compiler.core.parse.ast.Shared as AstShared
import community.flock.wirespec.ir.core.Enum as LanguageEnum
import community.flock.wirespec.ir.core.File as LanguageFile
import community.flock.wirespec.ir.core.Function as LanguageFunction
import community.flock.wirespec.ir.core.Import as LanguageImport
import community.flock.wirespec.ir.core.Package as LanguagePackage
import community.flock.wirespec.ir.core.Type as LanguageType

open class ScalaIrEmitter(
    override val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared(),
) : IrEmitter, HasPackageName {

    override val generator = ScalaGenerator

    val import = """
        |
        |import $DEFAULT_SHARED_PACKAGE_STRING.scala.Wirespec
        |import scala.reflect.ClassTag
        |
    """.trimMargin()

    override val extension = FileExtension.Scala

    override val shared = object : Shared {
        override val packageString = "$DEFAULT_SHARED_PACKAGE_STRING.scala"
        override val source = """
            |package $packageString
            |import scala.reflect.ClassTag
            |object Wirespec {
            |  trait Model {
            |    def validate(): List[String]
            |  }
            |  trait Enum {
            |    def label: String
            |  }
            |  trait Endpoint
            |  trait Channel
            |  trait Refined[T] {
            |    def value: T
            |    def validate(): Boolean
            |  }
            |  trait Path
            |  trait Queries
            |  trait Headers
            |  trait Handler
            |  enum Method {
            |    case GET
            |    case PUT
            |    case POST
            |    case DELETE
            |    case OPTIONS
            |    case HEAD
            |    case PATCH
            |    case TRACE
            |  }
            |  object Request {
            |    trait Headers
            |  }
            |  trait Request[T] {
            |    def path: Path
            |    def method: Method
            |    def queries: Queries
            |    def headers: Request.Headers
            |    def body: T
            |  }
            |  object Response {
            |    trait Headers
            |  }
            |  trait Response[T] {
            |    def status: Int
            |    def headers: Response.Headers
            |    def body: T
            |  }
            |  trait BodySerializer {
            |    def serializeBody[T](t: T, `type`: scala.reflect.ClassTag[?]): Array[Byte]
            |  }
            |  trait BodyDeserializer {
            |    def deserializeBody[T](raw: Array[Byte], `type`: scala.reflect.ClassTag[?]): T
            |  }
            |  trait BodySerialization extends BodySerializer with BodyDeserializer
            |  trait PathSerializer {
            |    def serializePath[T](t: T, `type`: scala.reflect.ClassTag[?]): String
            |  }
            |  trait PathDeserializer {
            |    def deserializePath[T](raw: String, `type`: scala.reflect.ClassTag[?]): T
            |  }
            |  trait PathSerialization extends PathSerializer with PathDeserializer
            |  trait ParamSerializer {
            |    def serializeParam[T](value: T, `type`: scala.reflect.ClassTag[?]): List[String]
            |  }
            |  trait ParamDeserializer {
            |    def deserializeParam[T](values: List[String], `type`: scala.reflect.ClassTag[?]): T
            |  }
            |  trait ParamSerialization extends ParamSerializer with ParamDeserializer
            |  trait Serializer extends BodySerializer with PathSerializer with ParamSerializer
            |  trait Deserializer extends BodyDeserializer with PathDeserializer with ParamDeserializer
            |  trait Serialization extends Serializer with Deserializer
            |  case class RawRequest(
            |    val method: String,
            |    val path: List[String],
            |    val queries: Map[String, List[String]],
            |    val headers: Map[String, List[String]],
            |    val body: Option[Array[Byte]]
            |  )
            |  case class RawResponse(
            |    val statusCode: Int,
            |    val headers: Map[String, List[String]],
            |    val body: Option[Array[Byte]]
            |  )
            |  trait Transportation {
            |    def transport(request: RawRequest): RawResponse
            |  }
            |  trait ServerEdge[Req <: Request[?], Res <: Response[?]] {
            |    def from(request: RawRequest): Req
            |    def to(response: Res): RawResponse
            |  }
            |  trait ClientEdge[Req <: Request[?], Res <: Response[?]] {
            |    def to(request: Req): RawRequest
            |    def from(response: RawResponse): Res
            |  }
            |  trait Client[Req <: Request[?], Res <: Response[?]] {
            |    val pathTemplate: String
            |    val method: String
            |    def client(serialization: Serialization): ClientEdge[Req, Res]
            |  }
            |  trait Server[Req <: Request[?], Res <: Response[?]] {
            |    val pathTemplate: String
            |    val method: String
            |    def server(serialization: Serialization): ServerEdge[Req, Res]
            |  }
            |}
        """.trimMargin()
    }

    override fun emit(module: Module, logger: Logger): NonEmptyList<File> =
        super.emit(module, logger).let {
            if (emitShared.value) it + File(
                Name.of(PackageName("${DEFAULT_SHARED_PACKAGE_STRING}.scala").toDir() + "Wirespec"),
                listOf(RawElement(shared.source))
            )
            else it
        }

    override fun emit(definition: Definition, module: Module, logger: Logger): File =
        super.emit(definition, module, logger).let { file ->
            val subPackageName = packageName + definition
            File(
                name = Name.of(subPackageName.toDir() + file.name.pascalCase()),
                elements = listOf(LanguagePackage(subPackageName.value)) +
                    (if (module.needImports()) listOf(RawElement(import)) else emptyList()) +
                    file.elements
            )
        }

    fun Identifier.sanitize() = value
        .split(".", " ")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .asSequence()
        .filter { it.isLetterOrDigit() || it in listOf('_') }
        .joinToString("")
        .sanitizeFirstIsDigit()
        .let { if (this is FieldIdentifier) it.sanitizeKeywords() else it }

    fun String.sanitizeFirstIsDigit() = if (firstOrNull()?.isDigit() == true) "_${this}" else this

    fun String.sanitizeKeywords() = if (this in reservedKeywords) addBackticks() else this

    override fun emit(type: Type, module: Module): File =
        type.convertWithValidation(module)
            .transform {
                matchingElements { struct: Struct ->
                    if (struct.fields.isEmpty()) struct.copy(constructors = listOf(Constructor(emptyList(), emptyList())))
                    else struct
                }
            }

    private fun Definition.emitEndpointImports() = importReferences()
        .distinctBy { it.value }
        .map { "import ${packageName.value}.model.${it.value}" }.joinToString("\n") { it.trimStart() }

    override fun emit(endpoint: Endpoint): File {
        val imports = endpoint.emitEndpointImports()
        val file = endpoint.convert()
        val endpointNamespace = file.findElement<Namespace>()!!
        val flattened = flattenNestedStructs(endpointNamespace)
        val requestIsObject = isRequestObject(flattened)
        val body = flattened
            .injectCompanionObject(endpoint, requestIsObject)
            .withClientServerObjects(endpoint, requestIsObject)

        return if (imports.isNotEmpty()) LanguageFile(Name.of(endpoint.identifier.sanitize()), listOf(RawElement(imports), body))
        else LanguageFile(Name.of(endpoint.identifier.sanitize()), listOf(body))
    }

    private fun isRequestObject(namespace: Namespace): Boolean {
        val requestStruct = namespace.elements.filterIsInstance<Struct>()
            .firstOrNull { it.name.pascalCase() == "Request" } ?: return false
        return (requestStruct.constructors.size == 1 && requestStruct.constructors.single().parameters.isEmpty()) ||
            (requestStruct.fields.isEmpty() && requestStruct.constructors.isEmpty())
    }

    private fun flattenNestedStructs(namespace: Namespace): Namespace {
        val newElements = mutableListOf<community.flock.wirespec.ir.core.Element>()
        for (element in namespace.elements) {
            when (element) {
                is Struct -> {
                    val nested = element.elements.filterIsInstance<Struct>()
                    if (nested.isNotEmpty()) {
                        val nestedNames = nested.map { it.name.pascalCase() }.toSet()
                        for (nestedStruct in nested) {
                            newElements.add(nestedStruct.copy(name = Name.of("${element.name.pascalCase()}${nestedStruct.name.pascalCase()}")))
                        }
                        newElements.add(qualifyNestedRefs(element, nestedNames))
                    } else {
                        newElements.add(element)
                    }
                }
                else -> newElements.add(element)
            }
        }
        return namespace.copy(elements = newElements)
    }

    private fun qualifyNestedRefs(struct: Struct, nestedNames: Set<String>): Struct {
        val qualifiedFields = struct.fields.map { field ->
            val typeName = (field.type as? LanguageType.Custom)?.name
            if (typeName != null && typeName in nestedNames) {
                field.copy(type = LanguageType.Custom("${struct.name.pascalCase()}$typeName"))
            } else field
        }
        val qualifiedConstructors = struct.constructors.map { c ->
            c.copy(body = c.body.map { stmt ->
                if (stmt is Assignment) {
                    val value = stmt.value
                    if (value is ConstructorStatement) {
                        val typeName = (value.type as? LanguageType.Custom)?.name
                        if (typeName != null && typeName in nestedNames) {
                            Assignment(stmt.name, value.copy(type = LanguageType.Custom("${struct.name.pascalCase()}$typeName")))
                        } else stmt
                    } else stmt
                } else stmt
            })
        }
        return struct.copy(
            fields = qualifiedFields,
            constructors = qualifiedConstructors,
            elements = struct.elements.filter { it !is Struct },
        )
    }

    private fun Namespace.injectCompanionObject(endpoint: Endpoint, requestIsObject: Boolean): Namespace {
        val companion = companionObject(endpoint, requestIsObject)
        val rawHandleFunction = raw(emitHandleFunction(endpoint, requestIsObject))
        val targetFunctionName = endpoint.identifier.value.replaceFirstChar { it.lowercase() }

        return transform {
            matchingElements { iface: Interface ->
                if (iface.name == Name.of("Handler")) {
                    iface.transform {
                        matchingElements { fn: LanguageFunction ->
                            if (fn.name.camelCase() == targetFunctionName) rawHandleFunction else fn
                        }
                        injectAfter { _: Interface -> listOf(companion) }
                    }
                } else {
                    iface
                }
            }
        }
    }

    open fun emitHandleFunction(endpoint: Endpoint, requestIsObject: Boolean): String {
        val functionName = endpoint.identifier.value.replaceFirstChar { it.lowercase() }
        val requestType = if (requestIsObject) "Request.type" else "Request"
        return "def $functionName(request: $requestType): Response[?]\n"
    }

    fun companionObject(endpoint: Endpoint, requestIsObject: Boolean): RawElement {
        val pathTemplate = "/" + endpoint.path.joinToString("/") {
            when (it) {
                is Endpoint.Segment.Literal -> it.value
                is Endpoint.Segment.Param -> "{${it.identifier.value}}"
            }
        }
        val reqType = if (requestIsObject) "Request.type" else "Request"
        return """
            |object Companion extends Wirespec.Server[$reqType, Response[?]] with Wirespec.Client[$reqType, Response[?]] {
            |  override val pathTemplate: String = "$pathTemplate"
            |  override val method: String = "${endpoint.method}"
            |  override def server(serialization: Wirespec.Serialization): Wirespec.ServerEdge[$reqType, Response[?]] = new Wirespec.ServerEdge[$reqType, Response[?]] {
            |    override def from(request: Wirespec.RawRequest): $reqType = fromRawRequest(serialization, request)
            |    override def to(response: Response[?]): Wirespec.RawResponse = toRawResponse(serialization, response)
            |  }
            |  override def client(serialization: Wirespec.Serialization): Wirespec.ClientEdge[$reqType, Response[?]] = new Wirespec.ClientEdge[$reqType, Response[?]] {
            |    override def to(request: $reqType): Wirespec.RawRequest = toRawRequest(serialization, request)
            |    override def from(response: Wirespec.RawResponse): Response[?] = fromRawResponse(serialization, response)
            |  }
            |}
        """.trimMargin().let(::raw)
    }

    private fun Namespace.withClientServerObjects(endpoint: Endpoint, requestIsObject: Boolean): Namespace {
        val reqType = if (requestIsObject) "Request.type" else "Request"
        val pathTemplate = "/" + endpoint.path.joinToString("/") {
            when (it) {
                is Endpoint.Segment.Literal -> it.value
                is Endpoint.Segment.Param -> "{${it.identifier.value}}"
            }
        }
        val clientObject = raw(
            """
            |object Client extends Wirespec.Client[$reqType, Response[?]] {
            |  override val pathTemplate: String = "$pathTemplate"
            |  override val method: String = "${endpoint.method}"
            |  override def client(serialization: Wirespec.Serialization): Wirespec.ClientEdge[$reqType, Response[?]] = new Wirespec.ClientEdge[$reqType, Response[?]] {
            |    override def to(request: $reqType): Wirespec.RawRequest = toRawRequest(serialization, request)
            |    override def from(response: Wirespec.RawResponse): Response[?] = fromRawResponse(serialization, response)
            |  }
            |}
            """.trimMargin()
        )
        val serverObject = raw(
            """
            |object Server extends Wirespec.Server[$reqType, Response[?]] {
            |  override val pathTemplate: String = "$pathTemplate"
            |  override val method: String = "${endpoint.method}"
            |  override def server(serialization: Wirespec.Serialization): Wirespec.ServerEdge[$reqType, Response[?]] = new Wirespec.ServerEdge[$reqType, Response[?]] {
            |    override def from(request: Wirespec.RawRequest): $reqType = fromRawRequest(serialization, request)
            |    override def to(response: Response[?]): Wirespec.RawResponse = toRawResponse(serialization, response)
            |  }
            |}
            """.trimMargin()
        )
        return copy(elements = elements + clientObject + serverObject)
    }

    private fun Definition.emitChannelImports() = importReferences()
        .distinctBy { it.value }
        .map { "import ${packageName.value}.model.${it.value}" }.joinToString("\n") { it.trimStart() }

    override fun emit(channel: Channel): File {
        val imports = channel.emitChannelImports()
        val file = channel.convert()
        return if (imports.isNotEmpty()) file.copy(elements = listOf(RawElement(imports)) + file.elements)
        else file
    }

    override fun emit(enum: Enum, module: Module): File = enum
        .convert()
        .transform {
            matchingElements { languageEnum: LanguageEnum ->
                languageEnum.withLabelField(
                    sanitizeEntry = { it.sanitizeEnum() },
                    labelFieldOverride = true,
                    labelExpression = RawExpression("label"),
                )
            }
        }

    fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//")
        .joinToString("_")
        .sanitizeFirstIsDigit()
        .sanitizeKeywords()

    override fun emit(union: Union): File = union
        .convert()

    override fun emit(refined: Refined): File {
        val file = refined.convert()
        val struct = file.findElement<Struct>()!!
        val toStringExpr = when (refined.reference.type) {
            is Reference.Primitive.Type.String -> "value"
            else -> "value.toString"
        }
        val updatedStruct = struct.copy(
            fields = struct.fields.map { f -> f.copy(isOverride = true) },
            elements = listOf(
                function("toString", isOverride = true) {
                    returnType(LanguageType.String)
                    returns(RawExpression(toStringExpr))
                },
                function("validate", isOverride = true) {
                    returnType(LanguageType.Boolean)
                    returns(refined.reference.convertConstraint(VariableReference(Name.of("value"))))
                },
            ),
        )
        return LanguageFile(Name.of(refined.identifier.sanitize()), listOf(updatedStruct))
    }

    companion object : Keywords {
        override val reservedKeywords = setOf(
            "abstract", "case", "class", "def", "do",
            "else", "extends", "false", "final", "for",
            "forSome", "if", "implicit", "import", "lazy",
            "match", "new", "null", "object", "override",
            "package", "private", "protected", "return", "sealed",
            "super", "this", "throw", "trait", "true",
            "try", "type", "val", "var", "while",
            "with", "yield", "given", "using", "enum",
            "export", "then",
        )
    }
}
