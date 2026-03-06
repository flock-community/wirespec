package community.flock.wirespec.compiler.test

object CompileComplexModelTest : Fixture {

    override val source =
        // language=ws
        """
        |type Email = String(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}${'$'}/g)
        |type PhoneNumber = String(/^\+[1-9]\d{1,14}${'$'}/g)
        |type Tag = String(/^[a-z][a-z0-9-]{0,19}${'$'}/g)
        |type EmployeeAge = Integer(18,65)
        |
        |type ContactInfo {
        |  email: Email,
        |  phone: PhoneNumber?
        |}
        |
        |type Employee {
        |  name: String,
        |  age: EmployeeAge,
        |  contactInfo: ContactInfo,
        |  tags: Tag[]
        |}
        |
        |type Department {
        |  name: String,
        |  employees: Employee[]
        |}
        |
        |type Company {
        |  name: String,
        |  departments: Department[]
        |}
        """.trimMargin()

    override val compiler = source.let(::compile)
}
