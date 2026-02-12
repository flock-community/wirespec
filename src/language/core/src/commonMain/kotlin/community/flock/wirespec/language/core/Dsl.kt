package community.flock.wirespec.language.core

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
    fun list(type: Type) = Type.Array(type)
    fun dict(keyType: Type, valueType: Type) = Type.Dict(keyType, valueType)
    fun type(name: String, vararg generics: Type): Type.Custom = Type.Custom(name, generics.toList())

    fun Type.nullable() = Type.Nullable(this)

    fun literal(value: String) = Literal(value, Type.String)
    fun literal(value: Int) = Literal(value, Type.Integer())
    fun literal(value: Long) = Literal(value, Type.Integer(Precision.P64))
    fun literal(value: Boolean) = Literal(value, Type.Boolean)
    fun literal(value: Float) = Literal(value, Type.Number(Precision.P32))
    fun literal(value: Double) = Literal(value, Type.Number(Precision.P64))
}

@Dsl
interface ContainerBuilder : BaseBuilder {
    val elements: MutableList<Element>

    fun import(path: String, type: Type.Custom) {
        elements.add(Import(path, type))
    }

    fun import(path: String, type: String) {
        elements.add(Import(path, Type.Custom(type)))
    }

    fun raw(code: String) {
        elements.add(RawElement(code))
    }

    fun struct(name: String, block: (StructBuilder.() -> Unit)? = null) {
        val builder = StructBuilder(name)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun function(name: String, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null) {
        val builder = FunctionBuilder(name, isAsync = false, isStatic = isStatic, isOverride = isOverride)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun asyncFunction(name: String, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null) {
        val builder = FunctionBuilder(name, isAsync = true, isStatic = isStatic, isOverride = isOverride)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun static(name: String, extends: Type.Custom? = null, block: (StaticBuilder.() -> Unit)? = null) {
        val builder = StaticBuilder(name, extends)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun `interface`(name: String, isSealed: Boolean = false, block: (InterfaceBuilder.() -> Unit)? = null) {
        val builder = InterfaceBuilder(name, isSealed)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun union(name: String, extends: Type.Custom? = null, block: (UnionBuilder.() -> Unit)? = null) {
        val builder = UnionBuilder(name, extends)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun enum(name: String, extends: Type.Custom? = null, block: (EnumBuilder.() -> Unit)? = null) {
        val builder = EnumBuilder(name, extends)
        block?.let { builder.it() }
        elements.add(builder.build())
    }
}

@Dsl
class FileBuilder(private val name: String) : ContainerBuilder {
    override val elements = mutableListOf<Element>()
    fun `package`(path: String) {
        elements.add(Package(path))
    }

    override fun function(name: String, isStatic: Boolean, isOverride: Boolean, block: (FunctionBuilder.() -> Unit)?) {
        val builder = FunctionBuilder(name)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    override fun asyncFunction(name: String, isStatic: Boolean, isOverride: Boolean, block: (FunctionBuilder.() -> Unit)?) {
        val builder = FunctionBuilder(name, isAsync = true)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    override fun struct(name: String, block: (StructBuilder.() -> Unit)?) {
        val builder = StructBuilder(name)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun build(): File = File(name, elements)
}

@Dsl
class StaticBuilder(private val name: String, private val extends: Type.Custom? = null) : ContainerBuilder {
    override val elements = mutableListOf<Element>()

    fun build(): Static = Static(name, elements, extends)
}

@Dsl
class InterfaceBuilder(
    private val name: String,
    private var isSealed: Boolean = false,
) : ContainerBuilder {
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
        fields.add(Field(name, type, isOverride))
    }

    fun build(): Interface = Interface(name, elements, extendsList, isSealed, typeParameters, fields)
}

@Dsl
class UnionBuilder(private val name: String, private val extends: Type.Custom? = null) : BaseBuilder {
    private val members = mutableListOf<Type.Custom>()
    private val typeParameters = mutableListOf<TypeParameter>()

    fun member(name: String) {
        members.add(Type.Custom(name))
    }

    fun typeParam(type: Type, vararg extends: Type) {
        typeParameters.add(TypeParameter(type, extends.toList()))
    }

    fun build(): Union = Union(name, extends, members, typeParameters)
}

@Dsl
class EnumBuilder(private val name: String, private val extends: Type.Custom? = null) : ContainerBuilder {
    private val entries = mutableListOf<Enum.Entry>()
    private val fields = mutableListOf<Field>()
    private val constructors = mutableListOf<Constructor>()
    override val elements = mutableListOf<Element>()

    fun entry(name: String, vararg values: String) {
        entries.add(Enum.Entry(name, values.toList()))
    }

    fun field(name: String, type: Type, isOverride: Boolean = false) {
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
class StructBuilder(private val name: String) : ContainerBuilder {
    private val fields = mutableListOf<Field>()
    private val constructors = mutableListOf<Constructor>()
    private val interfaces = mutableListOf<Type.Custom>()
    override val elements = mutableListOf<Element>()

    fun implements(type: Type.Custom) {
        interfaces.add(type)
    }

    fun field(name: String, type: Type, isOverride: Boolean = false) {
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

    fun build(): Struct = Struct(name, fields, constructors, interfaces, elements)
}

@Dsl
class StructConstructorBuilder : BaseBuilder {
    private val parameters = mutableListOf<Parameter>()
    private val body = mutableListOf<Statement>()

    fun arg(name: String, type: Type) {
        parameters.add(Parameter(name, type))
    }

    fun assign(name: String, value: Expression) {
        if (value is Statement && body.lastOrNull() === value) {
            body.removeAt(body.size - 1)
        }
        body.add(Assignment(name, value, isProperty = name.startsWith("this.")))
    }

    fun construct(type: Type, block: ConstructorBuilder.() -> Unit = {}): ConstructorStatement {
        val builder = ConstructorBuilder(type)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    fun functionCall(name: String, receiver: Expression? = null, typeArguments: List<Type> = emptyList(), block: FunctionCallBuilder.() -> Unit = {}): FunctionCall {
        val builder = FunctionCallBuilder(name, receiver, typeArguments)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    fun fieldCall(field: String, receiver: Expression? = null): FieldCall {
        val node = FieldCall(receiver, field)
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
    private val name: String,
    private val isAsync: Boolean = false,
    private val isStatic: Boolean = false,
    private val isOverride: Boolean = false,
) : BaseBuilder {
    private val typeParameters = mutableListOf<TypeParameter>()
    private val parameters = mutableListOf<Parameter>()
    private val body = mutableListOf<Statement>()
    private var returnType: Type? = null

    fun typeParam(type: Type, vararg extends: Type) {
        typeParameters.add(TypeParameter(type, extends.toList()))
    }

    fun returnType(type: Type) {
        returnType = type
    }

    fun arg(name: String, type: Type) {
        parameters.add(Parameter(name, type))
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

    fun listOf(values: List<Expression>, type: Type): LiteralList {
        val node = LiteralList(values, type)
        body.add(node)
        return node
    }

    fun emptyList(type: Type): LiteralList = listOf(emptyList(), type)

    fun mapOf(values: Map<String, Expression>, keyType: Type, valueType: Type): LiteralMap {
        val node = LiteralMap(values, keyType, valueType)
        body.add(node)
        return node
    }

    fun emptyMap(keyType: Type, valueType: Type): LiteralMap = mapOf(emptyMap(), keyType, valueType)

    fun assign(name: String, value: Expression) {
        if (value is Statement && body.lastOrNull() === value) {
            body.removeAt(body.size - 1)
        }
        body.add(Assignment(name, value))
    }

    fun construct(type: Type, block: ConstructorBuilder.() -> Unit = {}): ConstructorStatement {
        val builder = ConstructorBuilder(type)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    fun functionCall(name: String, receiver: Expression? = null, typeArguments: List<Type> = emptyList(), block: FunctionCallBuilder.() -> Unit = {}): FunctionCall {
        val builder = FunctionCallBuilder(name, receiver, typeArguments)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    fun fieldCall(field: String, receiver: Expression? = null): FieldCall {
        val node = FieldCall(receiver, field)
        body.add(node)
        return node
    }

    fun switch(expression: Expression, variable: String? = null, block: SwitchBuilder.() -> Unit) {
        val builder = SwitchBuilder(expression, variable)
        builder.block()
        body.add(builder.build())
    }

    fun error(message: Expression) {
        body.add(ErrorStatement(message))
    }

    fun nullCheck(expression: Expression, alternative: Expression, bodyExpr: Expression): NullCheck {
        val node = NullCheck(expression, bodyExpr, alternative)
        body.add(node)
        return node
    }

    fun build(): Function = Function(name, typeParameters, parameters, returnType, body, isAsync, isStatic, isOverride)
}

@Dsl
class SwitchBuilder(private val expression: Expression, private val variable: String? = null) : BaseBuilder {
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
        body.add(Assignment(name, value))
    }

    fun functionCall(name: String, receiver: Expression? = null, typeArguments: List<Type> = emptyList(), block: FunctionCallBuilder.() -> Unit = {}): FunctionCall {
        val builder = FunctionCallBuilder(name, receiver, typeArguments)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    fun fieldCall(field: String, receiver: Expression? = null): FieldCall {
        val node = FieldCall(receiver, field)
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

    fun nullCheck(expression: Expression, alternative: Expression, bodyExpr: Expression): NullCheck {
        val node = NullCheck(expression, bodyExpr, alternative)
        body.add(node)
        return node
    }

    fun build(): Case = Case(value, body)
}

@Dsl
class FunctionCallBuilder(private val name: String, private val receiver: Expression? = null, private val typeArguments: List<Type> = emptyList()) : BaseBuilder {
    private val arguments = mutableMapOf<String, Expression>()

    fun arg(argName: String, value: Expression) {
        arguments[argName] = value
    }

    fun functionCall(name: String, receiver: Expression? = null, typeArguments: List<Type> = emptyList(), block: FunctionCallBuilder.() -> Unit = {}): FunctionCall {
        val builder = FunctionCallBuilder(name, receiver, typeArguments)
        builder.block()
        return builder.build()
    }

    fun fieldCall(field: String, receiver: Expression? = null): FieldCall = FieldCall(receiver, field)

    fun literal(value: Any, type: Type): Literal = Literal(value, type)

    fun listOf(values: List<Expression>, type: Type): LiteralList = LiteralList(values, type)

    fun emptyList(type: Type): LiteralList = listOf(emptyList(), type)

    fun mapOf(values: Map<String, Expression>, keyType: Type, valueType: Type): LiteralMap = LiteralMap(values, keyType, valueType)

    fun emptyMap(keyType: Type, valueType: Type): LiteralMap = mapOf(emptyMap(), keyType, valueType)

    fun nullCheck(expression: Expression, alternative: Expression, bodyExpr: Expression): NullCheck = NullCheck(expression, bodyExpr, alternative)

    fun build(): FunctionCall = FunctionCall(receiver, typeArguments, name, arguments)
}

@Dsl
class ConstructorBuilder(private val type: Type) : BaseBuilder {
    private val arguments = mutableMapOf<String, Expression>()

    fun arg(name: String, value: Expression) {
        arguments[name] = value
    }

    fun functionCall(name: String, receiver: Expression? = null, typeArguments: List<Type> = emptyList(), block: FunctionCallBuilder.() -> Unit = {}): FunctionCall {
        val builder = FunctionCallBuilder(name, receiver, typeArguments)
        builder.block()
        return builder.build()
    }

    fun fieldCall(field: String, receiver: Expression? = null): FieldCall = FieldCall(receiver, field)

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

fun struct(name: String, block: (StructBuilder.() -> Unit)? = null): Struct {
    val builder = StructBuilder(name)
    block?.let { builder.it() }
    return builder.build()
}

fun enum(name: String, extends: Type.Custom? = null, block: (EnumBuilder.() -> Unit)? = null): Enum {
    val builder = EnumBuilder(name, extends)
    block?.let { builder.it() }
    return builder.build()
}

fun union(name: String, extends: Type.Custom? = null, block: (UnionBuilder.() -> Unit)? = null): Union {
    val builder = UnionBuilder(name, extends)
    block?.let { builder.it() }
    return builder.build()
}

fun `interface`(name: String, isSealed: Boolean = false, block: (InterfaceBuilder.() -> Unit)? = null): Interface {
    val builder = InterfaceBuilder(name, isSealed)
    block?.let { builder.it() }
    return builder.build()
}

fun static(name: String, extends: Type.Custom? = null, block: (StaticBuilder.() -> Unit)? = null): Static {
    val builder = StaticBuilder(name, extends)
    block?.let { builder.it() }
    return builder.build()
}

fun function(name: String, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null): Function {
    val builder = FunctionBuilder(name, isAsync = false, isStatic = isStatic, isOverride = isOverride)
    block?.let { builder.it() }
    return builder.build()
}

fun asyncFunction(name: String, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null): Function {
    val builder = FunctionBuilder(name, isAsync = true, isStatic = isStatic, isOverride = isOverride)
    block?.let { builder.it() }
    return builder.build()
}

fun import(path: String, type: Type.Custom): Import = Import(path, type)

fun import(path: String, type: String): Import = Import(path, Type.Custom(type))

fun raw(code: String): RawElement = RawElement(code)
