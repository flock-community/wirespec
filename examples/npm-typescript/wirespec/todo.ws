type TodoId = String(/^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/g)

type StringRefinedRegex = String(/^[0-9a-f]{8}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{12}$/g)
type StringRefined = String

type IntRefinedNoBound = Integer(_,_)
type IntRefinedLowerBound = Integer(-1,_)
type IntRefinedUpperound = Integer(_,2)
type IntRefinedLowerAndUpper = Integer(3,4)

type NumberRefinedNoBound = Number(_,_)
type NumberRefinedLowerBound = Number(-1.0,_)
type NumberRefinedUpperound = Number(_,2.0)
type NumberRefinedLowerAndUpper = Number(3.0,4.0)


type TodoDto {
    id: TodoId,
    name: String,
    done: Boolean,
    testInt0: IntRefinedNoBound,
    testInt1: IntRefinedLowerBound,
    testInt2: IntRefinedLowerAndUpper,
    testNum0: NumberRefinedNoBound,
    testNum1: NumberRefinedUpperound,
    testNum2: NumberRefinedLowerAndUpper
}

type PotentialTodoDto {
    name: String,
    done: Boolean,
    testInt0: IntRefinedNoBound,
    testInt1: IntRefinedLowerBound,
    testInt2: IntRefinedLowerAndUpper,
    testNum0: NumberRefinedNoBound,
    testNum1: NumberRefinedUpperound,
    testNum2: NumberRefinedLowerAndUpper
}

type TodoError {
    code: Integer,
    description: String
}

endpoint GetTodos GET /api/todos ? {done:Boolean?} -> {
    200 -> TodoDto[] # {`X-Total`:Integer}
    400 -> TodoError
}

endpoint GetTodoById GET /api/todos/{id: TodoId} -> {
    200 -> TodoDto
    404 -> TodoError
}

endpoint PostTodo POST PotentialTodoDto /api/todos -> {
    200 -> TodoDto
}

endpoint DeleteTodoById DELETE /api/todos/{id: TodoId} -> {
    200 -> TodoDto
    404 -> TodoError
}

