package community.flock.wirespec.compiler.test

/**
 * Parts are erased before emitting, so [source] has to compile to exactly what [expanded],
 * the same spec with every spread written out by hand, compiles to.
 */
public object CompilePartTest : Fixture {

    override val source: String =
        // language=ws
        """
        |part Identifiable {
        |  id: String
        |}
        |
        |part Audited {
        |  ...Identifiable,
        |  createdAt: String,
        |  updatedAt: String?
        |}
        |
        |part Paging {
        |  page: Integer,
        |  size: Integer
        |}
        |
        |part Tracing {
        |  `X-Trace-Id`: String
        |}
        |
        |type Todo {
        |  ...Audited,
        |  name: String,
        |  done: Boolean
        |}
        |
        |endpoint GetTodos GET /todos ?{...Paging, done: Boolean?} #{...Tracing} -> {
        |  200 -> Todo[] #{...Tracing, total: Integer}
        |}
        |
        |rpc FindTodo {
        |  ...Identifiable
        |} -> Todo
        """.trimMargin()

    public val expanded: String =
        // language=ws
        """
        |type Todo {
        |  id: String,
        |  createdAt: String,
        |  updatedAt: String?,
        |  name: String,
        |  done: Boolean
        |}
        |
        |endpoint GetTodos GET /todos ?{page: Integer, size: Integer, done: Boolean?} #{`X-Trace-Id`: String} -> {
        |  200 -> Todo[] #{`X-Trace-Id`: String, total: Integer}
        |}
        |
        |rpc FindTodo {
        |  id: String
        |} -> Todo
        """.trimMargin()

    override val compiler: Compiler = source.let(::compile)

    public val expandedCompiler: Compiler = expanded.let(::compile)
}
