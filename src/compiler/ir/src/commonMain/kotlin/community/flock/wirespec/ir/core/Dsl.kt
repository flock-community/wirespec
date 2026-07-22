package community.flock.wirespec.ir.core

@DslMarker
annotation class Dsl

@Dsl
interface BaseBuilder {
    val integer get() = Type.Integer()
    val integer32 get() = Type.Integer(Precision.P32)
    val integer64 get() = Type.Integer(Precision.P64)
    val number get() = Type.Number()
    val number32 get() = Type.Number(Precision.P32)
    val number64 get() = Type.Number(Precision.P64)
    val string get() = Type.String
    val boolean get() = Type.Boolean
    val bytes get() = Type.Bytes
    val unit get() = Type.Unit
    val wildcard get() = Type.Wildcard
    val reflect get() = Type.Reflect
    fun list(type: Type) = Type.Array(type)
    fun dict(keyType: Type, valueType: Type) = Type.Dict(keyType, valueType)
    fun type(name: String, vararg generics: Type): Type.Custom = Type.Custom(name, generics.toList())

    fun Type.nullable() = Type.Nullable(this)

    /**
     * A function type, optionally a receiver function type ([receiver] != null → `R.() -> T`)
     * and/or `suspend`. Used for lambda-typed parameters and properties
     * (e.g. `block: Builder.() -> Unit`).
     */
    fun functionType(
        returnType: Type,
        receiver: Type? = null,
        parameterTypes: List<Type> = emptyList(),
        isAsync: Boolean = false,
    ): Type.Function = Type.Function(parameterTypes, returnType, receiver, isAsync)

    /** A raw code fragment usable where an [Expression] is expected (e.g. `returns(rawExpr(...))`). */
    fun rawExpr(code: String): RawExpression = RawExpression(code)

    fun literal(value: String) = Literal(value, Type.String)
    fun literal(value: Int) = Literal(value, Type.Integer())
    fun literal(value: Long) = Literal(value, Type.Integer(Precision.P64))
    fun literal(value: Boolean) = Literal(value, Type.Boolean)
    fun literal(value: Float) = Literal(value, Type.Number(Precision.P32))
    fun literal(value: Double) = Literal(value, Type.Number(Precision.P64))

    fun classRef(type: Type): ClassReference = ClassReference(type)
    fun classRef(typeName: String): ClassReference = ClassReference(Type.Custom(typeName))

    fun cast(expression: Expression, targetType: Type): Cast = Cast(expression, targetType)
    fun cast(expression: Expression, targetTypeName: String): Cast = Cast(expression, Type.Custom(targetTypeName))
}

@Dsl
interface ContainerBuilder : BaseBuilder {
    val elements: MutableList<Element>

    fun import(path: String, type: Type.Custom, isTypeOnly: Boolean = false) {
        elements.add(Import(path, type, isTypeOnly))
    }

    fun import(path: String, type: String, isTypeOnly: Boolean = false) {
        elements.add(Import(path, Type.Custom(type), isTypeOnly))
    }

    fun raw(code: String) {
        elements.add(RawElement(code))
    }

    /**
     * Adds a property declaration — a class member, a top-level property, or (when [receiver]
     * is non-null) an extension property. [name] is kept verbatim (single-part [Name]) so wire
     * names such as `Refresh-Token` are backtick-escaped rather than normalised.
     */
    fun property(
        name: String,
        type: Type,
        isMutable: Boolean = false,
        visibility: Visibility? = null,
        receiver: Type? = null,
        annotations: List<String> = emptyList(),
        initializer: Expression? = null,
        getter: Expression? = null,
        isOverride: Boolean = false,
    ) {
        elements.add(
            Field(
                name = Name(name),
                type = type,
                isOverride = isOverride,
                isMutable = isMutable,
                visibility = visibility,
                annotations = annotations,
                receiver = receiver,
                initializer = initializer,
                getter = getter,
            ),
        )
    }

    fun struct(name: String, block: (StructBuilder.() -> Unit)? = null) {
        val builder = StructBuilder(name)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun struct(name: Name, block: (StructBuilder.() -> Unit)? = null) {
        val builder = StructBuilder(name)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun function(name: String, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null) {
        val builder = FunctionBuilder(name, isAsync = false, isStatic = isStatic, isOverride = isOverride)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun function(name: Name, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null) {
        val builder = FunctionBuilder(name, isAsync = false, isStatic = isStatic, isOverride = isOverride)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun asyncFunction(name: String, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null) {
        val builder = FunctionBuilder(name, isAsync = true, isStatic = isStatic, isOverride = isOverride)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun asyncFunction(name: Name, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null) {
        val builder = FunctionBuilder(name, isAsync = true, isStatic = isStatic, isOverride = isOverride)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun namespace(name: String, extends: Type.Custom? = null, block: (NamespaceBuilder.() -> Unit)? = null) {
        val builder = NamespaceBuilder(name, extends)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun namespace(name: Name, extends: Type.Custom? = null, block: (NamespaceBuilder.() -> Unit)? = null) {
        val builder = NamespaceBuilder(name, extends)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun `interface`(name: String, isSealed: Boolean = false, block: (InterfaceBuilder.() -> Unit)? = null) {
        val builder = InterfaceBuilder(name, isSealed)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun `interface`(name: Name, isSealed: Boolean = false, block: (InterfaceBuilder.() -> Unit)? = null) {
        val builder = InterfaceBuilder(name, isSealed)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun union(name: String, extends: Type.Custom? = null, block: (UnionBuilder.() -> Unit)? = null) {
        val builder = UnionBuilder(name, extends)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun union(name: Name, extends: Type.Custom? = null, block: (UnionBuilder.() -> Unit)? = null) {
        val builder = UnionBuilder(name, extends)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun enum(name: String, extends: Type.Custom? = null, block: (EnumBuilder.() -> Unit)? = null) {
        val builder = EnumBuilder(name, extends)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun enum(name: Name, extends: Type.Custom? = null, block: (EnumBuilder.() -> Unit)? = null) {
        val builder = EnumBuilder(name, extends)
        block?.let { builder.it() }
        elements.add(builder.build())
    }
}

@Dsl
class FileBuilder(private val name: Name) : ContainerBuilder {
    constructor(nameStr: String) : this(Name.of(nameStr))

    override val elements = mutableListOf<Element>()
    fun `package`(path: String) {
        elements.add(Package(path))
    }

    override fun function(name: String, isStatic: Boolean, isOverride: Boolean, block: (FunctionBuilder.() -> Unit)?) {
        val builder = FunctionBuilder(name)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    override fun function(name: Name, isStatic: Boolean, isOverride: Boolean, block: (FunctionBuilder.() -> Unit)?) {
        val builder = FunctionBuilder(name)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    override fun asyncFunction(name: String, isStatic: Boolean, isOverride: Boolean, block: (FunctionBuilder.() -> Unit)?) {
        val builder = FunctionBuilder(name, isAsync = true)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    override fun asyncFunction(name: Name, isStatic: Boolean, isOverride: Boolean, block: (FunctionBuilder.() -> Unit)?) {
        val builder = FunctionBuilder(name, isAsync = true)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    override fun struct(name: String, block: (StructBuilder.() -> Unit)?) {
        val builder = StructBuilder(name)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun main(isAsync: Boolean = false, block: FunctionBuilder.() -> Unit) {
        val builder = FunctionBuilder("main")
        builder.block()
        val fn = builder.build()
        elements.add(Main(body = fn.body, isAsync = isAsync))
    }

    fun main(isAsync: Boolean = false, statics: ContainerBuilder.() -> Unit, block: FunctionBuilder.() -> Unit) {
        val staticsBuilder = object : ContainerBuilder {
            override val elements = mutableListOf<Element>()
        }
        staticsBuilder.statics()
        val bodyBuilder = FunctionBuilder("main")
        bodyBuilder.block()
        val fn = bodyBuilder.build()
        elements.add(Main(statics = staticsBuilder.elements, body = fn.body, isAsync = isAsync))
    }

    fun build(): File = File(name, elements)
}

@Dsl
class NamespaceBuilder(private val name: Name, private val extends: Type.Custom? = null) : ContainerBuilder {
    constructor(nameStr: String, extends: Type.Custom? = null) : this(Name.of(nameStr), extends)

    override val elements = mutableListOf<Element>()

    fun build(): Namespace = Namespace(name, elements, extends)
}

@Dsl
class InterfaceBuilder(
    private val name: Name,
    private var isSealed: Boolean = false,
) : ContainerBuilder {
    constructor(nameStr: String, isSealed: Boolean = false) : this(Name.of(nameStr), isSealed)

    override val elements = mutableListOf<Element>()
    private val typeParameters = mutableListOf<TypeParameter>()
    private val extendsList = mutableListOf<Type.Custom>()
    private val fields = mutableListOf<Field>()

    fun typeParam(type: Type, vararg extends: Type) {
        typeParameters.add(TypeParameter(type, extends.toList()))
    }

    fun extends(type: Type.Custom) {
        extendsList.add(type)
    }

    fun sealed() {
        isSealed = true
    }

    fun field(name: String, type: Type, isOverride: Boolean = false) {
        fields.add(Field(Name.of(name), type, isOverride))
    }

    fun field(name: Name, type: Type, isOverride: Boolean = false) {
        fields.add(Field(name, type, isOverride))
    }

    fun build(): Interface = Interface(name, elements, extendsList, isSealed, typeParameters, fields)
}

@Dsl
class UnionBuilder(private val name: Name, private val extends: Type.Custom? = null) : BaseBuilder {
    constructor(nameStr: String, extends: Type.Custom? = null) : this(Name.of(nameStr), extends)

    private val members = mutableListOf<Type.Custom>()
    private val typeParameters = mutableListOf<TypeParameter>()

    fun member(name: String) {
        members.add(Type.Custom(name))
    }

    fun member(name: Name) {
        members.add(Type.Custom(name))
    }

    fun typeParam(type: Type, vararg extends: Type) {
        typeParameters.add(TypeParameter(type, extends.toList()))
    }

    fun build(): Union = Union(name, extends, members, typeParameters)
}

@Dsl
class EnumBuilder(private val name: Name, private val extends: Type.Custom? = null) : ContainerBuilder {
    constructor(nameStr: String, extends: Type.Custom? = null) : this(Name.of(nameStr), extends)

    private val entries = mutableListOf<Enum.Entry>()
    private val fields = mutableListOf<Field>()
    private val constructors = mutableListOf<Constructor>()
    override val elements = mutableListOf<Element>()

    fun entry(name: String, vararg values: String) {
        entries.add(Enum.Entry(Name.of(name), values.toList()))
    }

    fun field(name: String, type: Type, isOverride: Boolean = false) {
        fields.add(Field(Name.of(name), type, isOverride))
    }

    fun field(name: Name, type: Type, isOverride: Boolean = false) {
        fields.add(Field(name, type, isOverride))
    }

    fun constructo(block: StructConstructorBuilder.() -> Unit) {
        val builder = StructConstructorBuilder()
        builder.block()
        constructors.add(builder.build())
    }

    fun build(): Enum = Enum(name, extends, entries, fields, constructors, elements)
}

@Dsl
class StructBuilder(private val name: Name) : ContainerBuilder {
    constructor(nameStr: String) : this(Name.of(nameStr))

    private val fields = mutableListOf<Field>()
    private val constructors = mutableListOf<Constructor>()
    private val interfaces = mutableListOf<Type.Custom>()
    override val elements = mutableListOf<Element>()
    private val typeParameters = mutableListOf<TypeParameter>()
    private var visibility: Visibility? = null
    private val annotations = mutableListOf<String>()
    private var kind: Struct.Kind? = null
    private var constructorVisibility: Visibility? = null

    fun visibility(visibility: Visibility) {
        this.visibility = visibility
    }

    fun annotation(annotation: String) {
        annotations.add(annotation)
    }

    /** Renders as a stateful `class` (members from [elements]) rather than an inferred object/data class. */
    fun plainClass() {
        kind = Struct.Kind.PLAIN_CLASS
    }

    fun constructorVisibility(visibility: Visibility) {
        constructorVisibility = visibility
    }

    fun implements(type: Type.Custom) {
        interfaces.add(type)
    }

    fun typeParam(type: Type, vararg extends: Type) {
        typeParameters.add(TypeParameter(type, extends.toList()))
    }

    fun field(name: String, type: Type, isOverride: Boolean = false) {
        fields.add(Field(Name.of(name), type, isOverride))
    }

    fun field(name: Name, type: Type, isOverride: Boolean = false) {
        fields.add(Field(name, type, isOverride))
    }

    fun construct(type: Type, block: ConstructorBuilder.() -> Unit = {}): ConstructorStatement {
        val builder = ConstructorBuilder(type)
        builder.block()
        return builder.build()
    }

    fun constructo(block: StructConstructorBuilder.() -> Unit) {
        val builder = StructConstructorBuilder()
        builder.block()
        constructors.add(builder.build())
    }

    fun build(): Struct = Struct(
        name = name,
        fields = fields,
        constructors = constructors,
        interfaces = interfaces,
        elements = elements,
        typeParameters = typeParameters,
        visibility = visibility,
        annotations = annotations,
        kind = kind,
        constructorVisibility = constructorVisibility,
    )
}

@Dsl
class StructConstructorBuilder : BaseBuilder {
    private val parameters = mutableListOf<Parameter>()
    private val body = mutableListOf<Statement>()

    fun arg(name: String, type: Type) {
        parameters.add(Parameter(Name.of(name), type))
    }

    fun arg(name: Name, type: Type) {
        parameters.add(Parameter(name, type))
    }

    fun assign(name: String, value: Expression) {
        if (value is Statement && body.lastOrNull() === value) {
            body.removeAt(body.size - 1)
        }
        body.add(Assignment(Name.of(name), value, isProperty = name.startsWith("this.")))
    }

    fun construct(type: Type, block: ConstructorBuilder.() -> Unit = {}): ConstructorStatement {
        val builder = ConstructorBuilder(type)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    fun functionCall(name: String, receiver: Expression? = null, typeArguments: List<Type> = emptyList(), isAwait: Boolean = false, block: FunctionCallBuilder.() -> Unit = {}): FunctionCall {
        val builder = FunctionCallBuilder(name, receiver, typeArguments, isAwait)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    fun fieldCall(field: String, receiver: Expression? = null): FieldCall {
        val node = FieldCall(receiver, Name.of(field))
        body.add(node)
        return node
    }

    fun nullCheck(expression: Expression, alternative: Expression, bodyExpr: Expression): NullCheck {
        val node = NullCheck(expression, bodyExpr, alternative)
        body.add(node)
        return node
    }

    fun build(): Constructor = Constructor(parameters, body)
}

@Dsl
class FunctionBuilder(
    private val name: Name,
    private val isAsync: Boolean = false,
    private val isStatic: Boolean = false,
    private val isOverride: Boolean = false,
) : BaseBuilder {
    constructor(name: String, isAsync: Boolean = false, isStatic: Boolean = false, isOverride: Boolean = false) :
        this(Name.of(name), isAsync, isStatic, isOverride)
    private val typeParameters = mutableListOf<TypeParameter>()
    private val parameters = mutableListOf<Parameter>()
    private val body = mutableListOf<Statement>()
    private var returnType: Type? = null
    private var receiver: Type? = null
    private var visibility: Visibility? = null
    private val annotations = mutableListOf<String>()

    fun typeParam(type: Type, vararg extends: Type) {
        typeParameters.add(TypeParameter(type, extends.toList()))
    }

    fun returnType(type: Type) {
        returnType = type
    }

    /** Makes this an extension function on [type] (e.g. `fun Gen<Request>.call()`). */
    fun receiver(type: Type) {
        receiver = type
    }

    fun visibility(visibility: Visibility) {
        this.visibility = visibility
    }

    fun annotation(annotation: String) {
        annotations.add(annotation)
    }

    fun arg(name: String, type: Type) {
        parameters.add(Parameter(Name.of(name), type))
    }

    fun arg(name: Name, type: Type) {
        parameters.add(Parameter(name, type))
    }

    /** Parameter with a default value (`name: Type = <default>`). */
    fun arg(name: String, type: Type, default: Expression) {
        parameters.add(Parameter(Name.of(name), type, default))
    }

    fun print(expression: Expression) {
        body.add(PrintStatement(expression))
    }

    fun returns(expression: Expression) {
        if (expression is Statement && body.lastOrNull() === expression) {
            body.removeAt(body.size - 1)
        }
        body.add(ReturnStatement(expression))
    }

    fun literal(value: Any, type: Type): Literal {
        val node = Literal(value, type)
        body.add(node)
        return node
    }

    fun literalList(values: List<Expression>, type: Type): LiteralList {
        val node = LiteralList(values, type)
        body.add(node)
        return node
    }

    fun literalList(type: Type): LiteralList = literalList(emptyList(), type)

    fun literalMap(values: Map<String, Expression>, keyType: Type, valueType: Type): LiteralMap {
        val node = LiteralMap(values, keyType, valueType)
        body.add(node)
        return node
    }

    fun literalMap(keyType: Type, valueType: Type): LiteralMap = literalMap(emptyMap(), keyType, valueType)

    fun assign(name: String, value: Expression) {
        if (value is Statement && body.lastOrNull() === value) {
            body.removeAt(body.size - 1)
        }
        body.add(Assignment(Name.of(name), value))
    }

    fun construct(type: Type, block: ConstructorBuilder.() -> Unit = {}): ConstructorStatement {
        val builder = ConstructorBuilder(type)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    fun functionCall(name: String, receiver: Expression? = null, typeArguments: List<Type> = emptyList(), isAwait: Boolean = false, block: FunctionCallBuilder.() -> Unit = {}): FunctionCall {
        val builder = FunctionCallBuilder(name, receiver, typeArguments, isAwait)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    fun fieldCall(field: String, receiver: Expression? = null): FieldCall {
        val node = FieldCall(receiver, Name.of(field))
        body.add(node)
        return node
    }

    fun switch(expression: Expression, variable: String? = null, block: SwitchBuilder.() -> Unit) {
        val builder = SwitchBuilder(expression, variable?.let { Name.of(it) })
        builder.block()
        body.add(builder.build())
    }

    fun error(message: Expression) {
        body.add(ErrorStatement(message))
    }

    fun assertThat(expression: Expression, message: String) {
        body.add(AssertStatement(expression, message))
    }

    fun raw(code: String) {
        body.add(RawExpression(code))
    }

    fun nullCheck(expression: Expression, alternative: Expression, bodyExpr: Expression): NullCheck {
        val node = NullCheck(expression, bodyExpr, alternative)
        body.add(node)
        return node
    }

    fun build(): Function = Function(
        name = name,
        typeParameters = typeParameters,
        parameters = parameters,
        returnType = returnType,
        body = body,
        isAsync = isAsync,
        isStatic = isStatic,
        isOverride = isOverride,
        receiver = receiver,
        visibility = visibility,
        annotations = annotations,
    )
}

@Dsl
class SwitchBuilder(private val expression: Expression, private val variable: Name? = null) : BaseBuilder {
    private val cases = mutableListOf<Case>()
    private var default: List<Statement>? = null

    fun case(value: Literal, block: CaseBuilder.() -> Unit) {
        val builder = CaseBuilder(value)
        builder.block()
        cases.add(builder.build())
    }

    fun case(type: Type, block: CaseBuilder.() -> Unit) {
        val builder = CaseBuilder(RawExpression("type")) // value not used when type is present
        builder.block()
        cases.add(builder.build().copy(type = type))
    }

    inline fun <reified T : Any> case(noinline block: CaseBuilder.() -> Unit) {
        val typeName = T::class.simpleName ?: throw IllegalArgumentException("Cannot get simple name for ${T::class}")
        case(Type.Custom(typeName), block)
    }

    fun default(block: CaseBuilder.() -> Unit) {
        val builder = CaseBuilder(RawExpression("default")) // value not used for default
        builder.block()
        default = builder.build().body
    }

    fun build(): Switch = Switch(expression, cases, default, variable)
}

@Dsl
class CaseBuilder(private val value: Expression) : BaseBuilder {
    private val body = mutableListOf<Statement>()

    fun print(expression: Expression) {
        body.add(PrintStatement(expression))
    }

    fun returns(expression: Expression) {
        if (expression is Statement && body.lastOrNull() === expression) {
            body.removeAt(body.size - 1)
        }
        body.add(ReturnStatement(expression))
    }

    fun assign(name: String, value: Expression) {
        if (value is Statement && body.lastOrNull() === value) {
            body.removeAt(body.size - 1)
        }
        body.add(Assignment(Name.of(name), value))
    }

    fun functionCall(name: String, receiver: Expression? = null, typeArguments: List<Type> = emptyList(), isAwait: Boolean = false, block: FunctionCallBuilder.() -> Unit = {}): FunctionCall {
        val builder = FunctionCallBuilder(name, receiver, typeArguments, isAwait)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    fun fieldCall(field: String, receiver: Expression? = null): FieldCall {
        val node = FieldCall(receiver, Name.of(field))
        body.add(node)
        return node
    }

    fun construct(type: Type, block: ConstructorBuilder.() -> Unit = {}): ConstructorStatement {
        val builder = ConstructorBuilder(type)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    fun error(message: Expression) {
        body.add(ErrorStatement(message))
    }

    fun assertThat(expression: Expression, message: String) {
        body.add(AssertStatement(expression, message))
    }

    fun nullCheck(expression: Expression, alternative: Expression, bodyExpr: Expression): NullCheck {
        val node = NullCheck(expression, bodyExpr, alternative)
        body.add(node)
        return node
    }

    fun build(): Case = Case(value, body)
}

@Dsl
class FunctionCallBuilder(private val name: String, private val receiver: Expression? = null, private val typeArguments: List<Type> = emptyList(), private var isAwait: Boolean = false) : BaseBuilder {
    private val arguments = mutableMapOf<Name, Expression>()

    fun await() {
        isAwait = true
    }

    fun arg(argName: String, value: Expression) {
        arguments[Name.of(argName)] = value
    }

    fun arg(argName: Name, value: Expression) {
        arguments[argName] = value
    }

    fun functionCall(name: String, receiver: Expression? = null, typeArguments: List<Type> = emptyList(), isAwait: Boolean = false, block: FunctionCallBuilder.() -> Unit = {}): FunctionCall {
        val builder = FunctionCallBuilder(name, receiver, typeArguments, isAwait)
        builder.block()
        return builder.build()
    }

    fun fieldCall(field: String, receiver: Expression? = null): FieldCall = FieldCall(receiver, Name.of(field))

    fun literal(value: Any, type: Type): Literal = Literal(value, type)

    fun listOf(values: List<Expression>, type: Type): LiteralList = LiteralList(values, type)

    fun emptyList(type: Type): LiteralList = listOf(emptyList(), type)

    fun mapOf(values: Map<String, Expression>, keyType: Type, valueType: Type): LiteralMap = LiteralMap(values, keyType, valueType)

    fun emptyMap(keyType: Type, valueType: Type): LiteralMap = mapOf(emptyMap(), keyType, valueType)

    fun nullCheck(expression: Expression, alternative: Expression, bodyExpr: Expression): NullCheck = NullCheck(expression, bodyExpr, alternative)

    fun build(): FunctionCall = FunctionCall(receiver, typeArguments, Name.of(name), arguments, isAwait)
}

@Dsl
class ConstructorBuilder(private val type: Type) : BaseBuilder {
    private val arguments = mutableMapOf<Name, Expression>()

    fun arg(name: String, value: Expression) {
        arguments[Name.of(name)] = value
    }

    fun arg(name: Name, value: Expression) {
        arguments[name] = value
    }

    fun functionCall(name: String, receiver: Expression? = null, typeArguments: List<Type> = emptyList(), isAwait: Boolean = false, block: FunctionCallBuilder.() -> Unit = {}): FunctionCall {
        val builder = FunctionCallBuilder(name, receiver, typeArguments, isAwait)
        builder.block()
        return builder.build()
    }

    fun fieldCall(field: String, receiver: Expression? = null): FieldCall = FieldCall(receiver, Name.of(field))

    fun literal(value: Any, type: Type): Literal = Literal(value, type)

    fun listOf(values: List<Expression>, type: Type): LiteralList = LiteralList(values, type)

    fun emptyList(type: Type): LiteralList = listOf(emptyList(), type)

    fun mapOf(values: Map<String, Expression>, keyType: Type, valueType: Type): LiteralMap = LiteralMap(values, keyType, valueType)

    fun emptyMap(keyType: Type, valueType: Type): LiteralMap = mapOf(emptyMap(), keyType, valueType)

    fun nullCheck(expression: Expression, alternative: Expression, bodyExpr: Expression): NullCheck = NullCheck(expression, bodyExpr, alternative)

    fun build(): ConstructorStatement = ConstructorStatement(type, arguments)
}

fun file(name: String, block: FileBuilder.() -> Unit): File {
    val builder = FileBuilder(name)
    builder.block()
    return builder.build()
}

fun file(name: Name, block: FileBuilder.() -> Unit): File {
    val builder = FileBuilder(name)
    builder.block()
    return builder.build()
}

fun struct(name: String, block: (StructBuilder.() -> Unit)? = null): Struct {
    val builder = StructBuilder(name)
    block?.let { builder.it() }
    return builder.build()
}

fun struct(name: Name, block: (StructBuilder.() -> Unit)? = null): Struct {
    val builder = StructBuilder(name)
    block?.let { builder.it() }
    return builder.build()
}

fun enum(name: String, extends: Type.Custom? = null, block: (EnumBuilder.() -> Unit)? = null): Enum {
    val builder = EnumBuilder(name, extends)
    block?.let { builder.it() }
    return builder.build()
}

fun enum(name: Name, extends: Type.Custom? = null, block: (EnumBuilder.() -> Unit)? = null): Enum {
    val builder = EnumBuilder(name, extends)
    block?.let { builder.it() }
    return builder.build()
}

fun union(name: String, extends: Type.Custom? = null, block: (UnionBuilder.() -> Unit)? = null): Union {
    val builder = UnionBuilder(name, extends)
    block?.let { builder.it() }
    return builder.build()
}

fun union(name: Name, extends: Type.Custom? = null, block: (UnionBuilder.() -> Unit)? = null): Union {
    val builder = UnionBuilder(name, extends)
    block?.let { builder.it() }
    return builder.build()
}

fun `interface`(name: String, isSealed: Boolean = false, block: (InterfaceBuilder.() -> Unit)? = null): Interface {
    val builder = InterfaceBuilder(name, isSealed)
    block?.let { builder.it() }
    return builder.build()
}

fun `interface`(name: Name, isSealed: Boolean = false, block: (InterfaceBuilder.() -> Unit)? = null): Interface {
    val builder = InterfaceBuilder(name, isSealed)
    block?.let { builder.it() }
    return builder.build()
}

fun namespace(name: String, extends: Type.Custom? = null, block: (NamespaceBuilder.() -> Unit)? = null): Namespace {
    val builder = NamespaceBuilder(name, extends)
    block?.let { builder.it() }
    return builder.build()
}

fun namespace(name: Name, extends: Type.Custom? = null, block: (NamespaceBuilder.() -> Unit)? = null): Namespace {
    val builder = NamespaceBuilder(name, extends)
    block?.let { builder.it() }
    return builder.build()
}

fun function(name: String, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null): Function {
    val builder = FunctionBuilder(name, isAsync = false, isStatic = isStatic, isOverride = isOverride)
    block?.let { builder.it() }
    return builder.build()
}

fun function(name: Name, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null): Function {
    val builder = FunctionBuilder(name, isAsync = false, isStatic = isStatic, isOverride = isOverride)
    block?.let { builder.it() }
    return builder.build()
}

fun asyncFunction(name: String, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null): Function {
    val builder = FunctionBuilder(name, isAsync = true, isStatic = isStatic, isOverride = isOverride)
    block?.let { builder.it() }
    return builder.build()
}

fun asyncFunction(name: Name, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null): Function {
    val builder = FunctionBuilder(name, isAsync = true, isStatic = isStatic, isOverride = isOverride)
    block?.let { builder.it() }
    return builder.build()
}

fun import(path: String, type: Type.Custom, isTypeOnly: Boolean = false): Import = Import(path, type, isTypeOnly)

fun import(path: String, type: String, isTypeOnly: Boolean = false): Import = Import(path, Type.Custom(type), isTypeOnly)

fun main(isAsync: Boolean = false, block: FunctionBuilder.() -> Unit): Main {
    val builder = FunctionBuilder("main")
    builder.block()
    val fn = builder.build()
    return Main(body = fn.body, isAsync = isAsync)
}

fun raw(code: String): RawElement = RawElement(code)

fun Enum.withLabelField(
    sanitizeEntry: (String) -> String,
): Enum = copy(
    entries = entries.map {
        Enum.Entry(Name.of(sanitizeEntry(it.name.value())), it.values)
    },
    fields = listOf(Field(Name.of("label"), Type.String, isOverride = true)),
    constructors = listOf(
        Constructor(
            parameters = listOf(Parameter(Name.of("label"), Type.String)),
            body = listOf(Assignment(Name.of("this.label"), VariableReference(Name.of("label")), true)),
        ),
    ),
    elements = listOf(
        function("toString", isOverride = true) {
            returnType(Type.String)
            returns(VariableReference(Name.of("label")))
        },
    ),
)
