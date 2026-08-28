graphql GetTodo Query todo(id: String) -> TodoResult?

graphql AddTodo Mutation addTodo(input: TodoInput) -> TodoResult

graphql OnTodoAdded Subscription todoAdded -> TodoResult
