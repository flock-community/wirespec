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

    fun Any.toExpression(): Expression = when (this) {
        is Expression -> this
        is String -> {
            if (this.startsWith("\"") && this.endsWith("\"")) {
                Literal(this.substring(1, this.length - 1), Type.String)
            } else if (this.endsWith("()")) {
                MethodCall(method = this.substring(0, this.length - 2))
            } else if (this == "true" || this == "false") {
                Literal(this.toBoolean(), Type.Boolean)
            } else if (this.startsWith("call(")) {
                // This is a hack for the DSL to support call in print
                RawExpression(this)
            } else {
                RawExpression(this)
            }
        }
        is Int -> Literal(this, Type.Integer())
        is Long -> Literal(this, Type.Integer(Precision.P64))
        is Float -> Literal(this, Type.Number(Precision.P32))
        is Double -> Literal(this, Type.Number(Precision.P64))
        is Boolean -> Literal(this, Type.Boolean)
        else -> throw IllegalArgumentException("Cannot convert $this to Expression")
    }
}

@Dsl
interface ContainerBuilder : BaseBuilder {
    val elements: MutableList<Element>

    fun import(path: String) {
        elements.add(Import(path))
    }

    fun struct(name: String, interfaces: List<Type.Custom> = emptyList(), block: (StructBuilder.() -> Unit)? = null) {
        val builder = StructBuilder(name, interfaces)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun struct(name: String, interfaces: Type.Custom, block: (StructBuilder.() -> Unit)? = null) {
        struct(name, listOf(interfaces), block)
    }

    fun function(name: String, returnType: Type? = null, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null) {
        val builder = FunctionBuilder(name, returnType, isAsync = false, isStatic = isStatic, isOverride = isOverride)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun asyncFunction(name: String, returnType: Type? = null, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null) {
        val builder = FunctionBuilder(name, returnType, isAsync = true, isStatic = isStatic, isOverride = isOverride)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun static(name: String, extends: Type.Custom? = null, block: (StaticBuilder.() -> Unit)? = null) {
        val builder = StaticBuilder(name, extends)
        block?.let { builder.it() }
        elements.add(builder.build())
    }

    fun `interface`(name: String, extends: Type.Custom? = null, block: (InterfaceBuilder.() -> Unit)? = null) {
        val builder = InterfaceBuilder(name, extends)
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

    fun build(): File = File(name, elements)
}

@Dsl
class StaticBuilder(private val name: String, private val extends: Type.Custom? = null) : ContainerBuilder {
    override val elements = mutableListOf<Element>()

    fun build(): Static = Static(name, elements, extends)
}

@Dsl
class InterfaceBuilder(private val name: String, private val extends: Type.Custom? = null) : ContainerBuilder {
    override val elements = mutableListOf<Element>()

    fun build(): Interface = Interface(name, elements, extends)
}

@Dsl
class UnionBuilder(private val name: String, private val extends: Type.Custom? = null) {
    private val members = mutableListOf<String>()

    fun member(name: String) {
        members.add(name)
    }

    fun build(): Union = Union(name, extends, members)
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

    fun field(name: String, type: Type) {
        fields.add(Field(name, type))
    }

    fun constructo(block: StructConstructorBuilder.() -> Unit) {
        val builder = StructConstructorBuilder()
        builder.block()
        constructors.add(builder.build())
    }

    fun build(): Enum = Enum(name, extends, entries, fields, constructors, elements)
}

@Dsl
class StructBuilder(private val name: String, private val interfaces: List<Type.Custom> = emptyList()) : ContainerBuilder {
    private val fields = mutableListOf<Field>()
    private val constructors = mutableListOf<Constructor>()
    override val elements = mutableListOf<Element>()

    fun field(name: String, type: Type) {
        fields.add(Field(name, type))
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

    fun assign(name: String, value: Any) {
        val expr = value.toExpression()
        if (expr is Statement && body.lastOrNull() === expr) {
            body.removeAt(body.size - 1)
        }
        body.add(Assignment(name, expr, isProperty = name.startsWith("this.")))
    }

    fun construct(type: Type, block: ConstructorBuilder.() -> Unit = {}): ConstructorStatement {
        val builder = ConstructorBuilder(type)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    fun methodCall(method: String, receiver: Expression? = null, typeArguments: List<Type> = emptyList(), block: MethodCallBuilder.() -> Unit = {}): MethodCall {
        val builder = MethodCallBuilder(method, receiver, typeArguments)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    fun build(): Constructor = Constructor(parameters, body)
}

@Dsl
class FunctionBuilder(
    private val name: String,
    private val returnType: Type?,
    private val isAsync: Boolean = false,
    private val isStatic: Boolean = false,
    private val isOverride: Boolean = false,
) : BaseBuilder {
    private val typeParameters = mutableListOf<Type>()
    private val parameters = mutableListOf<Parameter>()
    private val body = mutableListOf<Statement>()

    fun typeParam(type: Type) {
        typeParameters.add(type)
    }

    fun arg(name: String, type: Type) {
        parameters.add(Parameter(name, type))
    }

    fun print(expression: Any) {
        body.add(PrintStatement(expression.toExpression()))
    }

    fun returns(expression: Any) {
        val expr = expression.toExpression()
        if (expr is Statement && body.lastOrNull() === expr) {
            body.removeAt(body.size - 1)
        }
        body.add(ReturnStatement(expr))
    }

    fun literal(value: Any, type: Type): Literal {
        val node = Literal(value, type)
        body.add(node)
        return node
    }

    fun listOf(values: List<Any>, type: Type): LiteralList {
        val node = LiteralList(values.map { it.toExpression() }, type)
        body.add(node)
        return node
    }

    fun emptyList(type: Type): LiteralList = listOf(emptyList(), type)

    fun mapOf(values: Map<String, Any>, keyType: Type, valueType: Type): LiteralMap {
        val node = LiteralMap(values.mapValues { it.value.toExpression() }, keyType, valueType)
        body.add(node)
        return node
    }

    fun emptyMap(keyType: Type, valueType: Type): LiteralMap = mapOf(emptyMap(), keyType, valueType)

    fun assign(name: String, value: Any) {
        val expr = value.toExpression()
        if (expr is Statement && body.lastOrNull() === expr) {
            body.removeAt(body.size - 1)
        }
        body.add(Assignment(name, expr))
    }

    fun construct(type: Type, block: ConstructorBuilder.() -> Unit = {}): ConstructorStatement {
        val builder = ConstructorBuilder(type)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    fun methodCall(method: String, receiver: Expression? = null, typeArguments: List<Type> = emptyList(), block: MethodCallBuilder.() -> Unit = {}): MethodCall {
        val builder = MethodCallBuilder(method, receiver, typeArguments)
        builder.block()
        val node = builder.build()
        body.add(node)
        return node
    }

    fun switch(expression: Any, block: SwitchBuilder.() -> Unit) {
        val builder = SwitchBuilder(expression.toExpression())
        builder.block()
        body.add(builder.build())
    }

    fun error(message: Any) {
        body.add(ErrorStatement(message.toExpression()))
    }

    fun build(): Function = Function(name, typeParameters, parameters, returnType, body, isAsync, isStatic, isOverride)
}

@Dsl
class SwitchBuilder(private val expression: Expression) : BaseBuilder {
    private val cases = mutableListOf<Case>()
    private var default: List<Statement>? = null

    fun case(value: Literal, block: CaseBuilder.() -> Unit) {
        val builder = CaseBuilder(value)
        builder.block()
        cases.add(builder.build())
    }

    fun case(type: Type, variable: String, block: CaseBuilder.() -> Unit) {
        val builder = CaseBuilder(RawExpression("type")) // value not used when type is present
        builder.block()
        cases.add(builder.build().copy(type = type, variable = variable))
    }

    inline fun <reified T : Any> case(variable: String, noinline block: CaseBuilder.() -> Unit) {
        val typeName = T::class.simpleName ?: throw IllegalArgumentException("Cannot get simple name for ${T::class}")
        case(Type.Custom(typeName), variable, block)
    }

    fun default(block: CaseBuilder.() -> Unit) {
        val builder = CaseBuilder(RawExpression("default")) // value not used for default
        builder.block()
        default = builder.build().body
    }

    fun build(): Switch = Switch(expression, cases, default)
}

@Dsl
class CaseBuilder(private val value: Expression) : BaseBuilder {
    private val body = mutableListOf<Statement>()

    fun print(expression: Any) {
        body.add(PrintStatement(expression.toExpression()))
    }

    fun returns(expression: Any) {
        val expr = expression.toExpression()
        if (expr is Statement && body.lastOrNull() === expr) {
            body.removeAt(body.size - 1)
        }
        body.add(ReturnStatement(expr))
    }

    fun assign(name: String, value: Any) {
        val expr = value.toExpression()
        if (expr is Statement && body.lastOrNull() === expr) {
            body.removeAt(body.size - 1)
        }
        body.add(Assignment(name, expr))
    }

    fun methodCall(method: String, receiver: Expression? = null, typeArguments: List<Type> = emptyList(), block: MethodCallBuilder.() -> Unit = {}): MethodCall {
        val builder = MethodCallBuilder(method, receiver, typeArguments)
        builder.block()
        val node = builder.build()
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

    fun error(message: Any) {
        body.add(ErrorStatement(message.toExpression()))
    }

    fun build(): Case = Case(value, body)
}

@Dsl
class MethodCallBuilder(private val method: String, private val receiver: Expression? = null, private val typeArguments: List<Type> = emptyList()) : BaseBuilder {
    private val arguments = mutableMapOf<String, Expression>()

    fun arg(name: String, value: Expression) {
        arguments[name] = value
    }

    fun arg(name: String, value: Any) {
        arguments[name] = value.toExpression()
    }

    fun methodCall(method: String, receiver: Expression? = null, typeArguments: List<Type> = emptyList(), block: MethodCallBuilder.() -> Unit = {}): MethodCall {
        val builder = MethodCallBuilder(method, receiver, typeArguments)
        builder.block()
        return builder.build()
    }

    fun literal(value: Any, type: Type): Literal = Literal(value, type)

    fun listOf(values: List<Any>, type: Type): LiteralList = LiteralList(values.map { it.toExpression() }, type)

    fun emptyList(type: Type): LiteralList = listOf(emptyList(), type)

    fun mapOf(values: Map<String, Any>, keyType: Type, valueType: Type): LiteralMap = LiteralMap(values.mapValues { it.value.toExpression() }, keyType, valueType)

    fun emptyMap(keyType: Type, valueType: Type): LiteralMap = mapOf(emptyMap(), keyType, valueType)

    fun build(): MethodCall = MethodCall(receiver, typeArguments, method, arguments)
}

@Dsl
class ConstructorBuilder(private val type: Type) : BaseBuilder {
    private val arguments = mutableMapOf<String, Expression>()

    fun arg(name: String, value: Expression) {
        arguments[name] = value
    }

    fun arg(name: String, value: Any) {
        arguments[name] = value.toExpression()
    }

    fun methodCall(method: String, receiver: Expression? = null, typeArguments: List<Type> = emptyList(), block: MethodCallBuilder.() -> Unit = {}): MethodCall {
        val builder = MethodCallBuilder(method, receiver, typeArguments)
        builder.block()
        return builder.build()
    }

    fun literal(value: Any, type: Type): Literal = Literal(value, type)

    fun listOf(values: List<Any>, type: Type): LiteralList = LiteralList(values.map { it.toExpression() }, type)

    fun emptyList(type: Type): LiteralList = listOf(emptyList(), type)

    fun mapOf(values: Map<String, Any>, keyType: Type, valueType: Type): LiteralMap = LiteralMap(values.mapValues { it.value.toExpression() }, keyType, valueType)

    fun emptyMap(keyType: Type, valueType: Type): LiteralMap = mapOf(emptyMap(), keyType, valueType)

    fun build(): ConstructorStatement = ConstructorStatement(type, arguments)
}

fun file(name: String, block: FileBuilder.() -> Unit): File {
    val builder = FileBuilder(name)
    builder.block()
    return builder.build()
}

fun struct(name: String, interfaces: List<Type.Custom> = emptyList(), block: (StructBuilder.() -> Unit)? = null): Struct {
    val builder = StructBuilder(name, interfaces)
    block?.let { builder.it() }
    return builder.build()
}

fun struct(name: String, interfaces: Type.Custom, block: (StructBuilder.() -> Unit)? = null): Struct = struct(name, listOf(interfaces), block)

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

fun `interface`(name: String, extends: Type.Custom? = null, block: (InterfaceBuilder.() -> Unit)? = null): Interface {
    val builder = InterfaceBuilder(name, extends)
    block?.let { builder.it() }
    return builder.build()
}

fun static(name: String, extends: Type.Custom? = null, block: (StaticBuilder.() -> Unit)? = null): Static {
    val builder = StaticBuilder(name, extends)
    block?.let { builder.it() }
    return builder.build()
}

fun function(name: String, returnType: Type? = null, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null): Function {
    val builder = FunctionBuilder(name, returnType, isAsync = false, isStatic = isStatic, isOverride = isOverride)
    block?.let { builder.it() }
    return builder.build()
}

fun asyncFunction(name: String, returnType: Type? = null, isStatic: Boolean = false, isOverride: Boolean = false, block: (FunctionBuilder.() -> Unit)? = null): Function {
    val builder = FunctionBuilder(name, returnType, isAsync = true, isStatic = isStatic, isOverride = isOverride)
    block?.let { builder.it() }
    return builder.build()
}
