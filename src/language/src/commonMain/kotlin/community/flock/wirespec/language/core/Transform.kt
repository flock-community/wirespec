package community.flock.wirespec.language.core

/**
 * Transformer interface for AST mutations.
 * Override specific methods to transform only certain node types.
 */
interface Transformer {
    fun transformType(type: Type): Type = type.transformChildren(this)
    fun transformElement(element: Element): Element = element.transformChildren(this)
    fun transformStatement(statement: Statement): Statement = statement.transformChildren(this)
    fun transformExpression(expression: Expression): Expression = expression.transformChildren(this)
    fun transformField(field: Field): Field = field.transformChildren(this)
    fun transformParameter(parameter: Parameter): Parameter = parameter.transformChildren(this)
    fun transformConstructor(constructor: Constructor): Constructor = constructor.transformChildren(this)
    fun transformCase(case: Case): Case = case.transformChildren(this)
}

/**
 * Creates an Transformer from lambda functions.
 */
fun transformer(
    transformType: (Type, Transformer) -> Type = { t, transformer -> t.transformChildren(transformer) },
    transformElement: (Element, Transformer) -> Element = { e, transformer -> e.transformChildren(transformer) },
    transformStatement: (Statement, Transformer) -> Statement = { s, transformer -> s.transformChildren(transformer) },
    transformExpression: (Expression, Transformer) -> Expression = { e, transformer -> e.transformChildren(transformer) },
    transformField: (Field, Transformer) -> Field = { f, transformer -> f.transformChildren(transformer) },
    transformParameter: (Parameter, Transformer) -> Parameter = { p, transformer -> p.transformChildren(transformer) },
    transformConstructor: (Constructor, Transformer) -> Constructor = { c, transformer ->
        c.transformChildren(
            transformer,
        )
    },
    transformCase: (Case, Transformer) -> Case = { c, transformer -> c.transformChildren(transformer) },
): Transformer = object : Transformer {
    override fun transformType(type: Type): Type = transformType(type, this)
    override fun transformElement(element: Element): Element = transformElement(element, this)
    override fun transformStatement(statement: Statement): Statement = transformStatement(statement, this)
    override fun transformExpression(expression: Expression): Expression = transformExpression(expression, this)
    override fun transformField(field: Field): Field = transformField(field, this)
    override fun transformParameter(parameter: Parameter): Parameter = transformParameter(parameter, this)
    override fun transformConstructor(constructor: Constructor): Constructor = transformConstructor(constructor, this)
    override fun transformCase(case: Case): Case = transformCase(case, this)
}

// Type transformation

fun Type.transformChildren(transformer: Transformer): Type = when (this) {
    is Type.Array -> copy(elementType = transformer.transformType(elementType))
    is Type.Dict -> copy(
        keyType = transformer.transformType(keyType),
        valueType = transformer.transformType(valueType),
    )

    is Type.Custom -> copy(generics = generics.map { transformer.transformType(it) })
    is Type.Nullable -> copy(type = transformer.transformType(type))
    is Type.Integer, is Type.Number, Type.Any, Type.String, Type.Boolean, Type.Bytes, Type.Unit, Type.Wildcard -> this
}

// Element transformation

fun Element.transformChildren(transformer: Transformer): Element = when (this) {
    is File -> copy(elements = elements.map { transformer.transformElement(it) })
    is Package -> this
    is Import -> copy(type = transformer.transformType(type) as Type.Custom)
    is Struct -> copy(
        fields = fields.map { transformer.transformField(it) },
        constructors = constructors.map { transformer.transformConstructor(it) },
        interfaces = interfaces.map { transformer.transformType(it) as Type.Custom },
        elements = elements.map { transformer.transformElement(it) },
    )

    is Function -> copy(
        parameters = parameters.map { transformer.transformParameter(it) },
        returnType = returnType?.let { transformer.transformType(it) },
        body = body.map { transformer.transformStatement(it) },
    )

    is Static -> copy(
        elements = elements.map { transformer.transformElement(it) },
        extends = extends?.let { transformer.transformType(it) as Type.Custom },
    )

    is Interface -> copy(
        elements = elements.map { transformer.transformElement(it) },
        extends = extends.map { transformer.transformType(it) as Type.Custom },
        fields = fields.map { transformer.transformField(it) },
    )

    is Union -> copy(
        extends = extends?.let { transformer.transformType(it) as Type.Custom },
        members = members.map { transformer.transformType(it) as Type.Custom },
        typeParameters = typeParameters.map {
            TypeParameter(
                transformer.transformType(it.type),
                it.extends.map { e -> transformer.transformType(e) },
            )
        },
    )

    is Enum -> copy(
        extends = extends?.let { transformer.transformType(it) as Type.Custom },
        fields = fields.map { transformer.transformField(it) },
        constructors = constructors.map { transformer.transformConstructor(it) },
        elements = elements.map { transformer.transformElement(it) },
    )

    is RawElement -> this
}

@Suppress("UNCHECKED_CAST")
fun <T : Element> T.transform(transformer: Transformer): T = transformer.transformElement(this) as T

// Field transformation

fun Field.transformChildren(transformer: Transformer): Field = copy(type = transformer.transformType(type))

// Parameter transformation

fun Parameter.transformChildren(transformer: Transformer): Parameter = copy(type = transformer.transformType(type))

// Constructor transformation

fun Constructor.transformChildren(transformer: Transformer): Constructor = copy(
    parameters = parameters.map { transformer.transformParameter(it) },
    body = body.map { transformer.transformStatement(it) },
)

// Statement transformation

fun Statement.transformChildren(transformer: Transformer): Statement = when (this) {
    is PrintStatement -> copy(expression = transformer.transformExpression(expression))
    is ReturnStatement -> copy(expression = transformer.transformExpression(expression))
    is ConstructorStatement -> copy(
        type = transformer.transformType(type),
        namedArguments = namedArguments.mapValues { transformer.transformExpression(it.value) },
    )

    is Literal -> copy(type = transformer.transformType(type))
    is LiteralList -> copy(
        values = values.map { transformer.transformExpression(it) },
        type = transformer.transformType(type),
    )

    is LiteralMap -> copy(
        values = values.mapValues { transformer.transformExpression(it.value) },
        keyType = transformer.transformType(keyType),
        valueType = transformer.transformType(valueType),
    )

    is Assignment -> copy(value = transformer.transformExpression(value))
    is ErrorStatement -> copy(message = transformer.transformExpression(message))
    is Switch -> copy(
        expression = transformer.transformExpression(expression),
        cases = cases.map { transformer.transformCase(it) },
        default = default?.map { transformer.transformStatement(it) },
    )

    is NullLiteral -> this
    is NullableEmpty -> this
    is VariableReference -> this
    is FieldCall -> copy(receiver = receiver?.let { transformer.transformExpression(it) })
    is FunctionCall -> copy(
        receiver = receiver?.let { transformer.transformExpression(it) },
        arguments = arguments.mapValues { transformer.transformExpression(it.value) },
    )

    is ArrayIndexCall -> copy(
        receiver = transformer.transformExpression(receiver),
        index = transformer.transformExpression(index),
    )

    is EnumReference -> copy(enumType = transformer.transformType(enumType) as Type.Custom)
    is EnumValueCall -> copy(expression = transformer.transformExpression(expression))
    is BinaryOp -> copy(
        left = transformer.transformExpression(left),
        right = transformer.transformExpression(right),
    )

    is TypeDescriptor -> copy(type = transformer.transformType(type))
    is NullCheck -> copy(
        expression = transformer.transformExpression(expression),
        body = transformer.transformExpression(body),
        alternative = alternative?.let { transformer.transformExpression(it) },
    )

    is NullableMap -> copy(
        expression = transformer.transformExpression(expression),
        body = transformer.transformExpression(body),
        alternative = transformer.transformExpression(alternative),
    )

    is NullableOf -> copy(expression = transformer.transformExpression(expression))
}

// Expression transformation

fun Expression.transformChildren(transformer: Transformer): Expression = when (this) {
    is RawExpression -> this
    is Statement -> transformChildren(transformer) as Expression
}

// Case transformation

fun Case.transformChildren(transformer: Transformer): Case = copy(
    value = transformer.transformExpression(value),
    body = body.map { transformer.transformStatement(it) },
    type = type?.let { transformer.transformType(it) },
)

// Match and Transform DSL

/**
 * Match and transform types using pattern matching.
 */
inline fun <reified M : Type, E : Element> E.transformMatching(
    crossinline transform: (M) -> Type,
): E = transform(
    transformer(
        transformType = { type, transformer ->
            val transformed = if (type is M) transform(type) else type
            transformed.transformChildren(transformer)
        },
    ),
)

/**
 * Match and transform elements using pattern matching.
 */
inline fun <reified M : Element, E : Element> E.transformMatchingElements(
    crossinline transform: (M) -> Element,
): E = transform(
    transformer(
        transformElement = { element, transformer ->
            val transformed = if (element is M) transform(element) else element
            transformed.transformChildren(transformer)
        },
    ),
)

/**
 * Match and transform fields using a predicate.
 */
fun <T : Element> T.transformFieldsWhere(
    predicate: (Field) -> Boolean,
    transform: (Field) -> Field,
): T = transform(
    transformer(
        transformField = { field, transformer ->
            val transformed = if (predicate(field)) transform(field) else field
            transformed.transformChildren(transformer)
        },
    ),
)

/**
 * Match and transform types by name.
 */
fun <T : Element> T.transformTypeByName(
    name: String,
    transform: (Type.Custom) -> Type,
): T = transformMatching { type: Type.Custom ->
    if (type.name == name) transform(type) else type
}

/**
 * Rename a custom type throughout the AST.
 */
fun <T : Element> T.renameType(oldName: String, newName: String): T = transformTypeByName(oldName) { it.copy(name = newName) }

/**
 * Rename a field throughout the AST.
 */
fun <T : Element> T.renameField(oldName: String, newName: String): T = transformFieldsWhere({ it.name == oldName }) { it.copy(name = newName) }

/**
 * Match and transform parameters using a predicate.
 */
fun <T : Element> T.transformParametersWhere(
    predicate: (Parameter) -> Boolean,
    transform: (Parameter) -> Parameter,
): T = transform(
    transformer(
        transformParameter = { parameter, transformer ->
            val transformed = if (predicate(parameter)) transform(parameter) else parameter
            transformed.transformChildren(transformer)
        },
    ),
)

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T : Element> T.withElements(elements: List<Element>): T = (
    when (this) {
        is File -> copy(elements = elements)
        is Struct -> copy(elements = elements)
        is Static -> copy(elements = elements)
        is Interface -> copy(elements = elements)
        is Enum -> copy(elements = elements)
        else -> this
    }
    ) as T

inline fun <reified T, E : Element> E.injectBefore(
    crossinline produce: (T) -> List<Element>,
): E where T : Element, T : HasElements = transformMatchingElements<T, E> { element ->
    val injected = produce(element)
    if (injected.isNotEmpty()) element.withElements(injected + element.elements) else element
}

inline fun <reified T, E : Element> E.injectAfter(
    crossinline produce: (T) -> List<Element>,
): E where T : Element, T : HasElements = transformMatchingElements<T, E> { element ->
    val injected = produce(element)
    if (injected.isNotEmpty()) element.withElements(element.elements + injected) else element
}

// Visitor interface for traversing AST without mutation

/**
 * Visitor interface for traversing AST nodes.
 */
interface Visitor {
    fun visitType(type: Type) {
        type.visitChildren(this)
    }

    fun visitElement(element: Element) {
        element.visitChildren(this)
    }

    fun visitStatement(statement: Statement) {
        statement.visitChildren(this)
    }

    fun visitExpression(expression: Expression) {
        expression.visitChildren(this)
    }

    fun visitField(field: Field) {
        field.visitChildren(this)
    }

    fun visitParameter(parameter: Parameter) {
        parameter.visitChildren(this)
    }

    fun visitConstructor(constructor: Constructor) {
        constructor.visitChildren(this)
    }

    fun visitCase(case: Case) {
        case.visitChildren(this)
    }
}

// Visitor child traversal functions

fun Type.visitChildren(visitor: Visitor) {
    when (this) {
        is Type.Array -> visitor.visitType(elementType)
        is Type.Dict -> {
            visitor.visitType(keyType)
            visitor.visitType(valueType)
        }

        is Type.Custom -> generics.forEach { visitor.visitType(it) }
        is Type.Nullable -> visitor.visitType(type)
        is Type.Integer, is Type.Number, Type.Any, Type.String, Type.Boolean, Type.Bytes, Type.Unit, Type.Wildcard -> {}
    }
}

fun Element.visitChildren(visitor: Visitor) {
    when (this) {
        is File -> elements.forEach { visitor.visitElement(it) }
        is Package -> {}
        is Import -> visitor.visitType(type)
        is Struct -> {
            fields.forEach { visitor.visitField(it) }
            constructors.forEach { visitor.visitConstructor(it) }
            interfaces.forEach { visitor.visitType(it) }
            elements.forEach { visitor.visitElement(it) }
        }

        is Function -> {
            parameters.forEach { visitor.visitParameter(it) }
            returnType?.let { visitor.visitType(it) }
            body.forEach { visitor.visitStatement(it) }
        }

        is Static -> {
            elements.forEach { visitor.visitElement(it) }
            extends?.let { visitor.visitType(it) }
        }

        is Interface -> {
            fields.forEach { visitor.visitField(it) }
            elements.forEach { visitor.visitElement(it) }
            extends.forEach { visitor.visitType(it) }
        }

        is Union -> {
            extends?.let { visitor.visitType(it) }
            members.forEach { visitor.visitType(it) }
            typeParameters.forEach { tp ->
                visitor.visitType(tp.type)
                tp.extends.forEach { visitor.visitType(it) }
            }
        }

        is Enum -> {
            extends?.let { visitor.visitType(it) }
            fields.forEach { visitor.visitField(it) }
            constructors.forEach { visitor.visitConstructor(it) }
            elements.forEach { visitor.visitElement(it) }
        }

        is RawElement -> {}
    }
}

fun Field.visitChildren(visitor: Visitor) {
    visitor.visitType(type)
}

fun Parameter.visitChildren(visitor: Visitor) {
    visitor.visitType(type)
}

fun Constructor.visitChildren(visitor: Visitor) {
    parameters.forEach { visitor.visitParameter(it) }
    body.forEach { visitor.visitStatement(it) }
}

fun Statement.visitChildren(visitor: Visitor) {
    when (this) {
        is PrintStatement -> visitor.visitExpression(expression)
        is ReturnStatement -> visitor.visitExpression(expression)
        is ConstructorStatement -> {
            visitor.visitType(type)
            namedArguments.values.forEach { visitor.visitExpression(it) }
        }

        is Literal -> visitor.visitType(type)
        is LiteralList -> {
            values.forEach { visitor.visitExpression(it) }
            visitor.visitType(type)
        }

        is LiteralMap -> {
            values.values.forEach { visitor.visitExpression(it) }
            visitor.visitType(keyType)
            visitor.visitType(valueType)
        }

        is Assignment -> visitor.visitExpression(value)
        is ErrorStatement -> visitor.visitExpression(message)
        is Switch -> {
            visitor.visitExpression(expression)
            cases.forEach { visitor.visitCase(it) }
            default?.forEach { visitor.visitStatement(it) }
        }

        is NullLiteral -> {}
        is NullableEmpty -> {}
        is VariableReference -> {}
        is FieldCall -> receiver?.let { visitor.visitExpression(it) }
        is FunctionCall -> {
            receiver?.let { visitor.visitExpression(it) }
            arguments.values.forEach { visitor.visitExpression(it) }
        }

        is ArrayIndexCall -> {
            visitor.visitExpression(receiver)
            visitor.visitExpression(index)
        }

        is EnumReference -> visitor.visitType(enumType)
        is EnumValueCall -> visitor.visitExpression(expression)
        is BinaryOp -> {
            visitor.visitExpression(left)
            visitor.visitExpression(right)
        }

        is TypeDescriptor -> visitor.visitType(type)
        is NullCheck -> {
            visitor.visitExpression(expression)
            visitor.visitExpression(body)
            alternative?.let { visitor.visitExpression(it) }
        }

        is NullableMap -> {
            visitor.visitExpression(expression)
            visitor.visitExpression(body)
            visitor.visitExpression(alternative)
        }

        is NullableOf -> visitor.visitExpression(expression)
    }
}

fun Expression.visitChildren(visitor: Visitor) {
    when (this) {
        is RawExpression -> {}
        is Statement -> visitChildren(visitor)
    }
}

fun Case.visitChildren(visitor: Visitor) {
    visitor.visitExpression(value)
    body.forEach { visitor.visitStatement(it) }
    type?.let { visitor.visitType(it) }
}

// Convenience extension functions for traversal

fun Element.visit(visitor: Visitor) = visitor.visitElement(this)

/**
 * Traverse all types in the AST.
 */
fun Element.forEachType(action: (Type) -> Unit) {
    visit(object : Visitor {
        override fun visitType(type: Type) {
            action(type)
            type.visitChildren(this)
        }
    })
}

/**
 * Traverse all elements in the AST.
 */
fun Element.forEachElement(action: (Element) -> Unit) {
    visit(object : Visitor {
        override fun visitElement(element: Element) {
            action(element)
            element.visitChildren(this)
        }
    })
}

/**
 * Traverse all fields in the AST.
 */
fun Element.forEachField(action: (Field) -> Unit) {
    visit(object : Visitor {
        override fun visitField(field: Field) {
            action(field)
            field.visitChildren(this)
        }
    })
}

/**
 * Collect all types in the AST.
 */
fun Element.collectTypes(): List<Type> = buildList {
    forEachType { add(it) }
}

/**
 * Collect all custom type names used in the AST.
 */
fun Element.collectCustomTypeNames(): Set<String> = buildSet {
    forEachType { type ->
        if (type is Type.Custom) add(type.name)
    }
}

/**
 * Find an element by type in the direct children.
 */
inline fun <reified T : Element> HasElements.findElement(): T? = elements.filterIsInstance<T>().firstOrNull()

/**
 * Find an element by type and predicate in the direct children.
 */
inline fun <reified T : Element> HasElements.findElement(predicate: (T) -> Boolean): T? = elements.filterIsInstance<T>().firstOrNull(predicate)

/**
 * Find all elements matching a predicate.
 */
inline fun <reified T : Element> Element.findAll(): List<T> = buildList {
    forEachElement { element ->
        if (element is T) add(element)
    }
}

/**
 * Find all types matching a predicate.
 */
inline fun <reified T : Type> Element.findAllTypes(): List<T> = buildList {
    forEachType { type ->
        if (type is T) add(type)
    }
}
