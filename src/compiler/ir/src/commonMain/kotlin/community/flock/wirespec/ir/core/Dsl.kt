package community.flock.wirespec.ir.core

@DslMarker
internal annotation class Dsl

@Dsl
public interface BaseBuilder {
    public val integer: Type.Integer get() = Type.Integer()
    public val integer32: Type.Integer get() = Type.Integer(Precision.P32)
    public val integer64: Type.Integer get() = Type.Integer(Precision.P64)
    public val number: Type.Number get() = Type.Number()
    public val number32: Type.Number get() = Type.Number(Precision.P32)
    public val number64: Type.Number get() = Type.Number(Precision.P64)
    public val string: Type.String get() = Type.String
    public val boolean: Type.Boolean get() = Type.Boolean
    public val bytes: Type.Bytes get() = Type.Bytes
    public val unit: Type.Unit get() = Type.Unit
    public val wildcard: Type.Wildcard get() = Type.Wildcard
    public val reflect: Type.Reflect get() = Type.Reflect
    public fun list(type: Type): Type.Array = Type.Array(type)
    public fun dict(keyType: Type, valueType: Type): Type.Dict = Type.Dict(keyType, valueType)
    public fun type(name: String, vararg generics: Type): Type.Custom = Type.Custom(name, generics.toList())

    public fun Type.nullable(): Type.Nullable = Type.Nullable(this)

    public fun functionType(
        returnType: Type,
        receiver: Type? = null,
        parameterTypes: List<Type> = emptyList(),
        isAsync: Boolean = false,
    ): Type.Function = Type.Function(parameterTypes, returnType, receiver, isAsync)

    public fun rawExpr(code: String): RawExpression = RawExpression(code)

    public fun literal(value: String): Literal = Literal(value, Type.String)
    public fun literal(value: Int): Literal = Literal(value, Type.Integer())
    public fun literal(value: Long): Literal = Literal(value, Type.Integer(Precision.P64))
    public fun literal(value: Boolean): Literal = Literal(value, Type.Boolean)
    public fun literal(value: Float): Literal = Literal(value, Type.Number(Precision.P32))
    public fun literal(value: Double): Literal = Literal(value, Type.Number(Precision.P64))

    public fun classRef(type: Type): ClassReference = ClassReference(type)
    public fun classRef(typeName: String): ClassReference = ClassReference(Type.Custom(typeName))

    public fun cast(expression: Expression, targetType: Type): Cast = Cast(expression, targetType)
    public fun cast(expression: Expression, targetTypeName: String): Cast = Cast(expression, Type.Custom(targetTypeName))
}

@Dsl
public interface ContainerBuilder : BaseBuilder {
    public val elements: MutableList<Element>

    public fun import(path: String, type: Type.Custom, isTypeOnly: Boolean = false) {
        elements.add(Import(path, type, isTypeOnly))
    }

    public fun import(path: String, type: String, isTypeOnly: Boolean = false) {
        elements.add(Import(path, Type.Custom(type), isTypeOnly))
    }

    public fun raw(code: String) {
        elements.add(RawElement(code))
    }

    public fun property(
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

    public fun struct(name: String, block: (StructBuilder.() -> Unit)? = null) {
        val builder = StructBuilder(name)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    public fun struct(name: Name, block: (StructBuilder.() -> Unit)? = null) {
        val builder = StructBuilder(name)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    public fun function(name: String, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null) {
        val builder = FunctionBuilder(name, isAsync = false, isStatic = isStatic, isOverride = isOverride)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    public fun function(name: Name, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null) {
        val builder = FunctionBuilder(name, isAsync = false, isStatic = isStatic, isOverride = isOverride)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    public fun asyncFunction(name: String, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null) {
        val builder = FunctionBuilder(name, isAsync = true, isStatic = isStatic, isOverride = isOverride)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    public fun asyncFunction(name: Name, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null) {
        val builder = FunctionBuilder(name, isAsync = true, isStatic = isStatic, isOverride = isOverride)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    public fun namespace(name: String, extends: Type.Custom? = null, block: (NamespaceBuilder.() -> Unit)? = null) {
        val builder = NamespaceBuilder(name, extends)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    public fun namespace(name: Name, extends: Type.Custom? = null, block: (NamespaceBuilder.() -> Unit)? = null) {
        val builder = NamespaceBuilder(name, extends)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    public fun `interface`(name: String, isSealed: Boolean = false, block: (InterfaceBuilder.() -> Unit)? = null) {
        val builder = InterfaceBuilder(name, isSealed)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    public fun `interface`(name: Name, isSealed: Boolean = false, block: (InterfaceBuilder.() -> Unit)? = null) {
        val builder = InterfaceBuilder(name, isSealed)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    public fun union(name: String, extends: Type.Custom? = null, block: (UnionBuilder.() -> Unit)? = null) {
        val builder = UnionBuilder(name, extends)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    public fun union(name: Name, extends: Type.Custom? = null, block: (UnionBuilder.() -> Unit)? = null) {
        val builder = UnionBuilder(name, extends)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    public fun enum(name: String, extends: Type.Custom? = null, block: (EnumBuilder.() -> Unit)? = null) {
        val builder = EnumBuilder(name, extends)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    public fun enum(name: Name, extends: Type.Custom? = null, block: (EnumBuilder.() -> Unit)? = null) {
        val builder = EnumBuilder(name, extends)
        block?.let { builder.it() }
        elements.add(builder.build())
    }
}

@Dsl
public class FileBuilder(private val name: Name) : ContainerBuilder {
    public constructor(nameStr: String) : this(Name.of(nameStr))

    override val elements: MutableList<Element> = mutableListOf<Element>()
    public fun `package`(path: String) {
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

    public fun main(isAsync: Boolean = false, block: FunctionBuilder.() -> Unit) {
        val builder = FunctionBuilder("main")
        builder.block()
        val fn = builder.build()
        elements.add(Main(body = fn.body, isAsync = isAsync))
    }

    public fun main(isAsync: Boolean = false, statics: ContainerBuilder.() -> Unit, block: FunctionBuilder.() -> Unit) {
        val staticsBuilder = object : ContainerBuilder {
            override val elements = mutableListOf<Element>()
        }
        staticsBuilder.statics()
        val bodyBuilder = FunctionBuilder("main")
        bodyBuilder.block()
        val fn = bodyBuilder.build()
        elements.add(Main(statics = staticsBuilder.elements, body = fn.body, isAsync = isAsync))
    }

    public fun build(): File = File(name, elements)
}

@Dsl
public class NamespaceBuilder(private val name: Name, private val extends: Type.Custom? = null) : ContainerBuilder {
    public constructor(nameStr: String, extends: Type.Custom? = null) : this(Name.of(nameStr), extends)

    override val elements: MutableList<Element> = mutableListOf<Element>()

    public fun build(): Namespace = Namespace(name, elements, extends)
}

@Dsl
public class InterfaceBuilder(
    private val name: Name,
    private var isSealed: Boolean = false,
) : ContainerBuilder {
    public constructor(nameStr: String, isSealed: Boolean = false) : this(Name.of(nameStr), isSealed)

    override val elements: MutableList<Element> = mutableListOf<Element>()
    private val typeParameters = mutableListOf<TypeParameter>()
    private val extendsList = mutableListOf<Type.Custom>()
    private val fields = mutableListOf<Field>()

    public fun typeParam(type: Type, vararg extends: Type) {
        typeParameters.add(TypeParameter(type, extends.toList()))
    }

    public fun extends(type: Type.Custom) {
        extendsList.add(type)
    }

    public fun sealed() {
        isSealed = true
    }

    public fun field(name: String, type: Type, isOverride: Boolean = false) {
        fields.add(Field(Name.of(name), type, isOverride))
    }

    public fun field(name: Name, type: Type, isOverride: Boolean = false) {
        fields.add(Field(name, type, isOverride))
    }

    public fun build(): Interface = Interface(name, elements, extendsList, isSealed, typeParameters, fields)
}

@Dsl
public class UnionBuilder(private val name: Name, private val extends: Type.Custom? = null) : BaseBuilder {
    public constructor(nameStr: String, extends: Type.Custom? = null) : this(Name.of(nameStr), extends)

    private val members = mutableListOf<Type.Custom>()
    private val typeParameters = mutableListOf<TypeParameter>()

    public fun member(name: String) {
        members.add(Type.Custom(name))
    }

    public fun member(name: Name) {
        members.add(Type.Custom(name))
    }

    public fun typeParam(type: Type, vararg extends: Type) {
        typeParameters.add(TypeParameter(type, extends.toList()))
    }

    public fun build(): Union = Union(name, extends, members, typeParameters)
}

@Dsl
public class EnumBuilder(private val name: Name, private val extends: Type.Custom? = null) : ContainerBuilder {
    public constructor(nameStr: String, extends: Type.Custom? = null) : this(Name.of(nameStr), extends)

    private val entries = mutableListOf<Enum.Entry>()
    private val fields = mutableListOf<Field>()
    private val constructors = mutableListOf<Constructor>()
    override val elements: MutableList<Element> = mutableListOf<Element>()

    public fun entry(name: String, vararg values: String) {
        entries.add(Enum.Entry(Name.of(name), values.toList()))
    }

    public fun field(name: String, type: Type, isOverride: Boolean = false) {
        fields.add(Field(Name.of(name), type, isOverride))
    }

    public fun field(name: Name, type: Type, isOverride: Boolean = false) {
        fields.add(Field(name, type, isOverride))
    }

    public fun constructo(block: StructConstructorBuilder.() -> Unit) {
        val builder = StructConstructorBuilder()
        builder.block()
        constructors.add(builder.build())
    }

    public fun build(): Enum = Enum(name, extends, entries, fields, constructors, elements)
}

@Dsl
public class StructBuilder(private val name: Name) : ContainerBuilder {
    public constructor(nameStr: String) : this(Name.of(nameStr))

    private val fields = mutableListOf<Field>()
    private val constructors = mutableListOf<Constructor>()
    private val interfaces = mutableListOf<Type.Custom>()
    override val elements: MutableList<Element> = mutableListOf<Element>()
    private val typeParameters = mutableListOf<TypeParameter>()
    private var visibility: Visibility? = null
    private val annotations = mutableListOf<String>()
    private var kind: Struct.Kind? = null
    private var constructorVisibility: Visibility? = null

    public fun visibility(visibility: Visibility) {
        this.visibility = visibility
    }

    public fun annotation(annotation: String) {
        annotations.add(annotation)
    }

    public fun plainClass() {
        kind = Struct.Kind.PLAIN_CLASS
    }

    public fun constructorVisibility(visibility: Visibility) {
        constructorVisibility = visibility
    }

    public fun implements(type: Type.Custom) {
        interfaces.add(type)
    }

    public fun typeParam(type: Type, vararg extends: Type) {
        typeParameters.add(TypeParameter(type, extends.toList()))
    }

    public fun field(name: String, type: Type, isOverride: Boolean = false) {
        fields.add(Field(Name.of(name), type, isOverride))
    }

    public fun field(name: Name, type: Type, isOverride: Boolean = false) {
        fields.add(Field(name, type, isOverride))
    }

    public fun construct(type: Type, block: ConstructorBuilder.() -> Unit = {}): ConstructorStatement {
        val builder = ConstructorBuilder(type)
        builder.block()
        return builder.build()
    }

    public fun constructo(block: StructConstructorBuilder.() -> Unit) {
        val builder = StructConstructorBuilder()
        builder.block()
        constructors.add(builder.build())
    }

    public fun build(): Struct = Struct(
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
public class StructConstructorBuilder : BaseBuilder {
    private val parameters = mutableListOf<Parameter>()
    private val body = mutableListOf<Statement>()

    public fun arg(name: String, type: Type) {
        parameters.add(Parameter(Name.of(name), type))
    }

    public fun arg(name: Name, type: Type) {
        parameters.add(Parameter(name, type))
    }

    public fun assign(name: String, value: Expression) {
        if (value is Statement && body.lastOrNull() === value) {
            body.removeAt(body.size - 1)
        }
        body.add(Assignment(Name.of(name), value, isProperty = name.startsWith("this.")))
    }

    public fun construct(type: Type, block: ConstructorBuilder.() -> Unit = {}): ConstructorStatement {
        val builder = ConstructorBuilder(type)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    public fun functionCall(name: String, receiver: Expression? = null, typeArguments: List<Type> = emptyList(), isAwait: Boolean = false, block: FunctionCallBuilder.() -> Unit = {}): FunctionCall {
        val builder = FunctionCallBuilder(name, receiver, typeArguments, isAwait)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    public fun fieldCall(field: String, receiver: Expression? = null): FieldCall {
        val node = FieldCall(receiver, Name.of(field))
        body.add(node)
        return node
    }

    public fun nullCheck(expression: Expression, alternative: Expression, bodyExpr: Expression): NullCheck {
        val node = NullCheck(expression, bodyExpr, alternative)
        body.add(node)
        return node
    }

    public fun build(): Constructor = Constructor(parameters, body)
}

@Dsl
public class FunctionBuilder(
    private val name: Name,
    private val isAsync: Boolean = false,
    private val isStatic: Boolean = false,
    private val isOverride: Boolean = false,
) : BaseBuilder {
    public constructor(name: String, isAsync: Boolean = false, isStatic: Boolean = false, isOverride: Boolean = false) :
        this(Name.of(name), isAsync, isStatic, isOverride)
    private val typeParameters = mutableListOf<TypeParameter>()
    private val parameters = mutableListOf<Parameter>()
    private val body = mutableListOf<Statement>()
    private var returnType: Type? = null
    private var receiver: Type? = null
    private var visibility: Visibility? = null
    private val annotations = mutableListOf<String>()

    public fun typeParam(type: Type, vararg extends: Type) {
        typeParameters.add(TypeParameter(type, extends.toList()))
    }

    public fun returnType(type: Type) {
        returnType = type
    }

    public fun receiver(type: Type) {
        receiver = type
    }

    public fun visibility(visibility: Visibility) {
        this.visibility = visibility
    }

    public fun annotation(annotation: String) {
        annotations.add(annotation)
    }

    public fun arg(name: String, type: Type) {
        parameters.add(Parameter(Name.of(name), type))
    }

    public fun arg(name: Name, type: Type) {
        parameters.add(Parameter(name, type))
    }

    public fun arg(name: String, type: Type, default: Expression) {
        parameters.add(Parameter(Name.of(name), type, default))
    }

    public fun print(expression: Expression) {
        body.add(PrintStatement(expression))
    }

    public fun returns(expression: Expression) {
        if (expression is Statement && body.lastOrNull() === expression) {
            body.removeAt(body.size - 1)
        }
        body.add(ReturnStatement(expression))
    }

    public fun literal(value: Any, type: Type): Literal {
        val node = Literal(value, type)
        body.add(node)
        return node
    }

    public fun literalList(values: List<Expression>, type: Type): LiteralList {
        val node = LiteralList(values, type)
        body.add(node)
        return node
    }

    public fun literalList(type: Type): LiteralList = literalList(emptyList(), type)

    public fun literalMap(values: Map<String, Expression>, keyType: Type, valueType: Type): LiteralMap {
        val node = LiteralMap(values, keyType, valueType)
        body.add(node)
        return node
    }

    public fun literalMap(keyType: Type, valueType: Type): LiteralMap = literalMap(emptyMap(), keyType, valueType)

    public fun assign(name: String, value: Expression) {
        if (value is Statement && body.lastOrNull() === value) {
            body.removeAt(body.size - 1)
        }
        body.add(Assignment(Name.of(name), value))
    }

    public fun construct(type: Type, block: ConstructorBuilder.() -> Unit = {}): ConstructorStatement {
        val builder = ConstructorBuilder(type)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    public fun functionCall(name: String, receiver: Expression? = null, typeArguments: List<Type> = emptyList(), isAwait: Boolean = false, block: FunctionCallBuilder.() -> Unit = {}): FunctionCall {
        val builder = FunctionCallBuilder(name, receiver, typeArguments, isAwait)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    public fun fieldCall(field: String, receiver: Expression? = null): FieldCall {
        val node = FieldCall(receiver, Name.of(field))
        body.add(node)
        return node
    }

    public fun switch(expression: Expression, variable: String? = null, block: SwitchBuilder.() -> Unit) {
        val builder = SwitchBuilder(expression, variable?.let { Name.of(it) })
        builder.block()
        body.add(builder.build())
    }

    public fun error(message: Expression) {
        body.add(ErrorStatement(message))
    }

    public fun assertThat(expression: Expression, message: String) {
        body.add(AssertStatement(expression, message))
    }

    public fun raw(code: String) {
        body.add(RawExpression(code))
    }

    public fun nullCheck(expression: Expression, alternative: Expression, bodyExpr: Expression): NullCheck {
        val node = NullCheck(expression, bodyExpr, alternative)
        body.add(node)
        return node
    }

    public fun build(): Function = Function(
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
public class SwitchBuilder(private val expression: Expression, private val variable: Name? = null) : BaseBuilder {
    private val cases = mutableListOf<Case>()
    private var default: List<Statement>? = null

    public fun case(value: Literal, block: CaseBuilder.() -> Unit) {
        val builder = CaseBuilder(value)
        builder.block()
        cases.add(builder.build())
    }

    public fun case(type: Type, block: CaseBuilder.() -> Unit) {
        val builder = CaseBuilder(RawExpression("type")) // value not used when type is present
        builder.block()
        cases.add(builder.build().copy(type = type))
    }

    public inline fun <reified T : Any> case(noinline block: CaseBuilder.() -> Unit) {
        val typeName = T::class.simpleName ?: throw IllegalArgumentException("Cannot get simple name for ${T::class}")
        case(Type.Custom(typeName), block)
    }

    public fun default(block: CaseBuilder.() -> Unit) {
        val builder = CaseBuilder(RawExpression("default")) // value not used for default
        builder.block()
        default = builder.build().body
    }

    public fun build(): Switch = Switch(expression, cases, default, variable)
}

@Dsl
public class CaseBuilder(private val value: Expression) : BaseBuilder {
    private val body = mutableListOf<Statement>()

    public fun print(expression: Expression) {
        body.add(PrintStatement(expression))
    }

    public fun returns(expression: Expression) {
        if (expression is Statement && body.lastOrNull() === expression) {
            body.removeAt(body.size - 1)
        }
        body.add(ReturnStatement(expression))
    }

    public fun assign(name: String, value: Expression) {
        if (value is Statement && body.lastOrNull() === value) {
            body.removeAt(body.size - 1)
        }
        body.add(Assignment(Name.of(name), value))
    }

    public fun functionCall(name: String, receiver: Expression? = null, typeArguments: List<Type> = emptyList(), isAwait: Boolean = false, block: FunctionCallBuilder.() -> Unit = {}): FunctionCall {
        val builder = FunctionCallBuilder(name, receiver, typeArguments, isAwait)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    public fun fieldCall(field: String, receiver: Expression? = null): FieldCall {
        val node = FieldCall(receiver, Name.of(field))
        body.add(node)
        return node
    }

    public fun construct(type: Type, block: ConstructorBuilder.() -> Unit = {}): ConstructorStatement {
        val builder = ConstructorBuilder(type)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    public fun error(message: Expression) {
        body.add(ErrorStatement(message))
    }

    public fun assertThat(expression: Expression, message: String) {
        body.add(AssertStatement(expression, message))
    }

    public fun nullCheck(expression: Expression, alternative: Expression, bodyExpr: Expression): NullCheck {
        val node = NullCheck(expression, bodyExpr, alternative)
        body.add(node)
        return node
    }

    public fun build(): Case = Case(value, body)
}

@Dsl
public class FunctionCallBuilder(private val name: String, private val receiver: Expression? = null, private val typeArguments: List<Type> = emptyList(), private var isAwait: Boolean = false) : BaseBuilder {
    private val arguments = mutableMapOf<Name, Expression>()

    public fun await() {
        isAwait = true
    }

    public fun arg(argName: String, value: Expression) {
        arguments[Name.of(argName)] = value
    }

    public fun arg(argName: Name, value: Expression) {
        arguments[argName] = value
    }

    public fun functionCall(name: String, receiver: Expression? = null, typeArguments: List<Type> = emptyList(), isAwait: Boolean = false, block: FunctionCallBuilder.() -> Unit = {}): FunctionCall {
        val builder = FunctionCallBuilder(name, receiver, typeArguments, isAwait)
        builder.block()
        return builder.build()
    }

    public fun fieldCall(field: String, receiver: Expression? = null): FieldCall = FieldCall(receiver, Name.of(field))

    public fun literal(value: Any, type: Type): Literal = Literal(value, type)

    public fun listOf(values: List<Expression>, type: Type): LiteralList = LiteralList(values, type)

    public fun emptyList(type: Type): LiteralList = listOf(emptyList(), type)

    public fun mapOf(values: Map<String, Expression>, keyType: Type, valueType: Type): LiteralMap = LiteralMap(values, keyType, valueType)

    public fun emptyMap(keyType: Type, valueType: Type): LiteralMap = mapOf(emptyMap(), keyType, valueType)

    public fun nullCheck(expression: Expression, alternative: Expression, bodyExpr: Expression): NullCheck = NullCheck(expression, bodyExpr, alternative)

    public fun build(): FunctionCall = FunctionCall(receiver, typeArguments, Name.of(name), arguments, isAwait)
}

@Dsl
public class ConstructorBuilder(private val type: Type) : BaseBuilder {
    private val arguments = mutableMapOf<Name, Expression>()

    public fun arg(name: String, value: Expression) {
        arguments[Name.of(name)] = value
    }

    public fun arg(name: Name, value: Expression) {
        arguments[name] = value
    }

    public fun functionCall(name: String, receiver: Expression? = null, typeArguments: List<Type> = emptyList(), isAwait: Boolean = false, block: FunctionCallBuilder.() -> Unit = {}): FunctionCall {
        val builder = FunctionCallBuilder(name, receiver, typeArguments, isAwait)
        builder.block()
        return builder.build()
    }

    public fun fieldCall(field: String, receiver: Expression? = null): FieldCall = FieldCall(receiver, Name.of(field))

    public fun literal(value: Any, type: Type): Literal = Literal(value, type)

    public fun listOf(values: List<Expression>, type: Type): LiteralList = LiteralList(values, type)

    public fun emptyList(type: Type): LiteralList = listOf(emptyList(), type)

    public fun mapOf(values: Map<String, Expression>, keyType: Type, valueType: Type): LiteralMap = LiteralMap(values, keyType, valueType)

    public fun emptyMap(keyType: Type, valueType: Type): LiteralMap = mapOf(emptyMap(), keyType, valueType)

    public fun nullCheck(expression: Expression, alternative: Expression, bodyExpr: Expression): NullCheck = NullCheck(expression, bodyExpr, alternative)

    public fun build(): ConstructorStatement = ConstructorStatement(type, arguments)
}

public fun file(name: String, block: FileBuilder.() -> Unit): File {
    val builder = FileBuilder(name)
    builder.block()
    return builder.build()
}

public fun file(name: Name, block: FileBuilder.() -> Unit): File {
    val builder = FileBuilder(name)
    builder.block()
    return builder.build()
}

public fun struct(name: String, block: (StructBuilder.() -> Unit)? = null): Struct {
    val builder = StructBuilder(name)
    block?.let { builder.it() }
    return builder.build()
}

public fun struct(name: Name, block: (StructBuilder.() -> Unit)? = null): Struct {
    val builder = StructBuilder(name)
    block?.let { builder.it() }
    return builder.build()
}

internal fun enum(name: String, extends: Type.Custom? = null, block: (EnumBuilder.() -> Unit)? = null): Enum {
    val builder = EnumBuilder(name, extends)
    block?.let { builder.it() }
    return builder.build()
}

internal fun enum(name: Name, extends: Type.Custom? = null, block: (EnumBuilder.() -> Unit)? = null): Enum {
    val builder = EnumBuilder(name, extends)
    block?.let { builder.it() }
    return builder.build()
}

internal fun union(name: String, extends: Type.Custom? = null, block: (UnionBuilder.() -> Unit)? = null): Union {
    val builder = UnionBuilder(name, extends)
    block?.let { builder.it() }
    return builder.build()
}

internal fun union(name: Name, extends: Type.Custom? = null, block: (UnionBuilder.() -> Unit)? = null): Union {
    val builder = UnionBuilder(name, extends)
    block?.let { builder.it() }
    return builder.build()
}

public fun `interface`(name: String, isSealed: Boolean = false, block: (InterfaceBuilder.() -> Unit)? = null): Interface {
    val builder = InterfaceBuilder(name, isSealed)
    block?.let { builder.it() }
    return builder.build()
}

public fun `interface`(name: Name, isSealed: Boolean = false, block: (InterfaceBuilder.() -> Unit)? = null): Interface {
    val builder = InterfaceBuilder(name, isSealed)
    block?.let { builder.it() }
    return builder.build()
}

internal fun namespace(name: String, extends: Type.Custom? = null, block: (NamespaceBuilder.() -> Unit)? = null): Namespace {
    val builder = NamespaceBuilder(name, extends)
    block?.let { builder.it() }
    return builder.build()
}

internal fun namespace(name: Name, extends: Type.Custom? = null, block: (NamespaceBuilder.() -> Unit)? = null): Namespace {
    val builder = NamespaceBuilder(name, extends)
    block?.let { builder.it() }
    return builder.build()
}

public fun function(name: String, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null): Function {
    val builder = FunctionBuilder(name, isAsync = false, isStatic = isStatic, isOverride = isOverride)
    block?.let { builder.it() }
    return builder.build()
}

public fun function(name: Name, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null): Function {
    val builder = FunctionBuilder(name, isAsync = false, isStatic = isStatic, isOverride = isOverride)
    block?.let { builder.it() }
    return builder.build()
}

public fun import(path: String, type: Type.Custom, isTypeOnly: Boolean = false): Import = Import(path, type, isTypeOnly)

public fun import(path: String, type: String, isTypeOnly: Boolean = false): Import = Import(path, Type.Custom(type), isTypeOnly)

public fun main(isAsync: Boolean = false, block: FunctionBuilder.() -> Unit): Main {
    val builder = FunctionBuilder("main")
    builder.block()
    val fn = builder.build()
    return Main(body = fn.body, isAsync = isAsync)
}

public fun raw(code: String): RawElement = RawElement(code)

internal fun Enum.withLabelField(
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
