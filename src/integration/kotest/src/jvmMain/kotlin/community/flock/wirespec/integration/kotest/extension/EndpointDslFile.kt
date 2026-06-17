package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.file

/**
 * Builds the per-endpoint chained Kotest DSL file (`<Endpoint>Dsl.kt`) as an IR
 * [File], so the wrapping `KotlinIrEmitter` renders it alongside the generated
 * models and endpoints. The DSL body is rendered as raw Kotlin via [raw] — the
 * shapes are specific enough that hand-rolled templates are clearer than IR nodes.
 */
internal object EndpointDslFile {

    fun build(
        endpoint: Endpoint,
        packageName: PackageName,
        types: Map<String, Type> = emptyMap(),
        refined: Map<String, Refined> = emptyMap(),
    ): File {
        val shape = EndpointShape.from(endpoint, types, refined)
        val kotestPkg = "${packageName.value}.kotest"
        val endpointPkg = "${packageName.value}.endpoint"
        val modelPkg = "${packageName.value}.model"
        val fileName = PackageName(kotestPkg).toDir() + "${shape.name}Dsl"

        return file(Name.of(fileName)) {
            `package`(kotestPkg)

            import("io.kotest.extensions.wirespec.dsl", "endpointCall")
            import("io.kotest.extensions.wirespec.dsl", "WirespecScenarioDsl")
            import("kotlin.time", "Duration")
            import(endpointPkg, shape.name)
            val needsGen = shape.bodyType != null ||
                shape.pathFields.isNotEmpty() ||
                shape.queryFields.isNotEmpty() ||
                shape.headerFields.isNotEmpty()
            if (needsGen) {
                import("io.kotest.property", "Gen")
            }
            val isListBody = shape.bodyKind == EndpointShape.BodyKind.List
            // Nullable path/query/header params default to `Arb.constant(null)` so callers
            // may omit them; non-nullable params are required (no default).
            val needsArbConstant = (shape.pathFields + shape.queryFields + shape.headerFields).any { it.isNullable }
            if (isListBody || needsArbConstant) {
                import("io.kotest.property", "Arb")
            }
            if (isListBody) {
                // List body builder samples its element count with `Arb.int(count)`.
                // Arb.int is an extension on Arb.Companion in io.kotest.property.arbitrary.
                import("io.kotest.property.arbitrary", "int")
            }
            if (needsArbConstant) {
                import("io.kotest.property.arbitrary", "constant")
            }
            shape.modelImports.forEach { import(modelPkg, it) }

            raw(renderCallClass(shape))
            if (shape.bodyType != null && shape.bodyFieldShapes.isNotEmpty()) {
                val element = shape.bodyElementType
                    ?: error("bodyFieldShapes present but no bodyElementType for ${shape.name}")
                raw(renderBodyBuilder(element, shape.bodyFieldShapes, shape.name))
                // Emit nested-type builders, deduped by type name. Each appears once per file.
                val nestedDefs = collectNestedBuilders(shape.bodyFieldShapes, alreadyEmitted = setOf(element))
                nestedDefs.forEach { (typeName, fields) ->
                    raw(renderBodyBuilder(typeName, fields, shape.name))
                }
            }
        }
    }

    private fun renderCallClass(shape: EndpointShape): String = buildString {
        appendLine("@WirespecScenarioDsl")
        appendLine("public class ${shape.name}Call internal constructor() {")
        appendLine("    @PublishedApi internal val inner = endpointCall(${shape.name}.Handler, ${shape.name})")
        if (shape.bodyType != null) append(renderBodySlot(shape, shape.bodyType))
        if (shape.pathFields.isNotEmpty()) append(renderPathSlot(shape))
        if (shape.queryFields.isNotEmpty()) append(renderQuerySlot(shape))
        if (shape.headerFields.isNotEmpty()) append(renderHeaderSlot(shape))
        append(renderResponseDsl(shape))
        append("\n}")
    }

    private fun renderHeaderSlot(shape: EndpointShape): String =
        renderParamSlot("header", "headerGen", shape, shape.headerFields)

    private fun renderQuerySlot(shape: EndpointShape): String =
        renderParamSlot("query", "queryGen", shape, shape.queryFields)

    private fun renderPathSlot(shape: EndpointShape): String =
        renderParamSlot("path", "pathGen", shape, shape.pathFields)

    /**
     * Renders a `header` / `query` / `path` slot function. Nullable fields default to
     * `Arb.constant(null)` (callers may omit them); non-nullable fields are required
     * parameters with no default. Either way each field is always registered, since
     * every parameter now resolves to a concrete generator.
     */
    private fun renderParamSlot(
        slot: String,
        genFn: String,
        shape: EndpointShape,
        fields: List<EndpointShape.NamedTypedField>,
    ): String = buildString {
        val call = "${shape.name}Call"
        val params = fields.joinToString(", ") { f ->
            if (f.isNullable) {
                "${f.name}: Gen<${f.kotlinType}> = Arb.constant(null)"
            } else {
                "${f.name}: Gen<${f.kotlinType}>"
            }
        }
        appendLine("    public fun $slot($params): $call = apply {")
        fields.forEach { f ->
            appendLine("        inner.$genFn(\"${f.name}\", ${f.name})")
        }
        appendLine("    }")
    }

    private fun renderBodySlot(shape: EndpointShape, bodyType: String): String = buildString {
        val call = "${shape.name}Call"
        appendLine("    public fun body(gen: Gen<$bodyType>): $call =")
        appendLine("        apply { inner.body(gen) }")
        if (shape.bodyFieldShapes.isNotEmpty()) {
            val element = shape.bodyElementType ?: error("bodyFieldShapes present but no bodyElementType")
            val builderName = "${shape.name}${element}BodyBuilder"
            val rootPrefix = if (shape.bodyKind == EndpointShape.BodyKind.List) listOf("\"*\"") else emptyList()
            val isList = shape.bodyKind == EndpointShape.BodyKind.List
            val signature = if (isList) {
                "body(count: IntRange = 1..3, block: $builderName.() -> Unit)"
            } else {
                "body(block: $builderName.() -> Unit)"
            }
            appendLine("    public fun $signature: $call = apply {")
            appendLine("        val builder = $builderName().apply(block)")
            if (isList) {
                appendLine("        inner.bodyListSize(Arb.int(count))")
            }
            appendLine("        inner.bodyFields {")
            renderFieldRegistrations(this, "builder", shape.bodyFieldShapes, rootPrefix, indent = "            ", builderPrefix = shape.name)
            appendLine("        }")
            appendLine("    }")
        }
    }

    private fun renderFieldRegistrations(
        out: StringBuilder,
        receiver: String,
        fields: List<EndpointShape.BodyFieldShape>,
        pathPrefix: List<String>,
        indent: String,
        builderPrefix: String,
    ) {
        fields.forEach { f ->
            val nameSegment = "\"${f.name}\""
            val pathArgs = (pathPrefix + nameSegment).joinToString(", ")
            when (f) {
                is EndpointShape.BodyFieldShape.Primitive -> {
                    out.appendLine("$indent$receiver.${f.name}?.let { registerPath($pathArgs) { it } }")
                }
                is EndpointShape.BodyFieldShape.NestedObject -> {
                    val nestedBuilder = "$builderPrefix${f.typeName}BodyBuilder"
                    val nestedVar = "nested_${f.name}"
                    out.appendLine("$indent$receiver._${f.name}Block?.let { block ->")
                    out.appendLine("$indent    val $nestedVar = $nestedBuilder().apply(block)")
                    renderFieldRegistrations(out, nestedVar, f.fields, pathPrefix + nameSegment, "$indent    ", builderPrefix)
                    out.appendLine("$indent}")
                }
                is EndpointShape.BodyFieldShape.NestedList -> {
                    val nestedBuilder = "$builderPrefix${f.elementTypeName}BodyBuilder"
                    val nestedVar = "nested_${f.name}"
                    out.appendLine("$indent$receiver._${f.name}Block?.let { block ->")
                    out.appendLine("$indent    val $nestedVar = $nestedBuilder().apply(block)")
                    renderFieldRegistrations(out, nestedVar, f.fields, pathPrefix + nameSegment + "\"*\"", "$indent    ", builderPrefix)
                    out.appendLine("$indent}")
                }
            }
        }
    }

    private fun renderBodyBuilder(
        elementType: String,
        fields: List<EndpointShape.BodyFieldShape>,
        builderPrefix: String,
    ): String = buildString {
        appendLine("@WirespecScenarioDsl")
        appendLine("public class $builderPrefix${elementType}BodyBuilder {")
        fields.forEach { f ->
            when (f) {
                is EndpointShape.BodyFieldShape.Primitive -> {
                    appendLine("    public var ${f.name}: Gen<${f.kotlinType}>? = null")
                }
                is EndpointShape.BodyFieldShape.NestedObject -> {
                    appendLine("    @PublishedApi internal var _${f.name}Block: ($builderPrefix${f.typeName}BodyBuilder.() -> Unit)? = null")
                    appendLine("    public fun ${f.name}(block: $builderPrefix${f.typeName}BodyBuilder.() -> Unit) { _${f.name}Block = block }")
                }
                is EndpointShape.BodyFieldShape.NestedList -> {
                    appendLine("    @PublishedApi internal var _${f.name}Block: ($builderPrefix${f.elementTypeName}BodyBuilder.() -> Unit)? = null")
                    appendLine("    public fun ${f.name}(block: $builderPrefix${f.elementTypeName}BodyBuilder.() -> Unit) { _${f.name}Block = block }")
                }
            }
        }
        append("}")
    }

    private fun collectNestedBuilders(
        fields: List<EndpointShape.BodyFieldShape>,
        alreadyEmitted: Set<String>,
    ): List<Pair<String, List<EndpointShape.BodyFieldShape>>> {
        val result = mutableListOf<Pair<String, List<EndpointShape.BodyFieldShape>>>()
        val emitted = alreadyEmitted.toMutableSet()
        fun walk(fs: List<EndpointShape.BodyFieldShape>) {
            fs.forEach { f ->
                when (f) {
                    is EndpointShape.BodyFieldShape.Primitive -> Unit
                    is EndpointShape.BodyFieldShape.NestedObject -> {
                        if (emitted.add(f.typeName)) {
                            result += f.typeName to f.fields
                            walk(f.fields)
                        }
                    }
                    is EndpointShape.BodyFieldShape.NestedList -> {
                        if (emitted.add(f.elementTypeName)) {
                            result += f.elementTypeName to f.fields
                            walk(f.fields)
                        }
                    }
                }
            }
        }
        walk(fields)
        return result
    }

    private fun renderResponseDsl(shape: EndpointShape): String = buildString {
        val resp = "${shape.name}.Response<*>"
        appendLine("    public suspend inline fun <reified R : $resp> expecting(): R =")
        appendLine("        inner.expecting<R>()")
        appendLine("    public suspend inline fun <reified R : $resp> expecting(noinline block: (R) -> Unit): R =")
        appendLine("        inner.expecting<R>(block)")
        appendLine("    public suspend inline fun <reified R : $resp, T> returning(noinline projection: (R) -> T): T =")
        appendLine("        inner.returning<R, T>(projection)")
        appendLine("    public suspend inline fun <reified R : $resp> collecting(count: Int, noinline block: (List<R>) -> Unit) {")
        appendLine("        inner.collecting<R>(count, block)")
        appendLine("    }")
        appendLine("    public suspend inline fun <reified R : $resp> collecting(duration: Duration, noinline block: (List<R>) -> Unit) {")
        append("        inner.collecting<R>(duration, block)\n    }")
    }
}
