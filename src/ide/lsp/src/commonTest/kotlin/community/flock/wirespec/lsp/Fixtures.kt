package community.flock.wirespec.lsp

object Fixtures {

    /**
     * A realistic Wirespec source covering refined types, records, type references,
     * endpoints with path parameters, and built-in primitives. Used as the document
     * under test by the end-to-end tests.
     *
     * Line numbers (0-indexed) referenced by tests:
     *  - 0  : `type Name = String(/^[a-zA-Z]{1,50}$/g)`
     *  - 4  : `  firstname: Name,`
     *  - 5  : `  lastName: Name,`
     *  - 9  : `type Todo {`
     *  - 11 : `  owner: Person,`
     *  - 14 : `endpoint GetTodos GET /todos -> {`
     *  - 15 : `    200 -> Todo[]`
     */
    const val TODOS = """type Name = String(/^[a-zA-Z]{1,50}$/g)

type Person {
  id: Integer,
  firstname: Name,
  lastName: Name,
  age: Integer
}

type Todo {
  id: Integer,
  owner: Person,
  done: Boolean
}

endpoint GetTodos GET /todos -> {
    200 -> Todo[]
}
"""

    const val PARTS = """part Audited {
  createdAt: String
}

part Unused {
  note: String
}

type Todo {
  ...Audited,
  name: String
}
"""

    const val NAME = """type Name = String(/^[a-zA-Z]{1,50}$/g)
"""

    /** Refers to `Name`, which only [NAME] defines. */
    const val PERSON = """type Person {
  firstname: Name
}
"""

    /**
     * A file header with `@spec:` and `@file:` annotations, followed by a definition.
     *
     * Line numbers (0-indexed) referenced by tests:
     *  - 0 : `@spec:OpenAPIInfo(title: "Todo API")`
     *  - 3 : `type Todo {`
     */
    const val HEADER = """@spec:OpenAPIInfo(title: "Todo API")
@file:Owner("todos")

type Todo {
  done: Boolean
}
"""

    /** A broken document — missing type after `name:`. Used to verify diagnostic reporting. */
    const val BROKEN = """type Person {
  name:
}
"""
}
