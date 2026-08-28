graphql GetTodo Query {id: String} -> TodoResult?

graphql AddTodo Mutation {input: TodoInput} -> TodoResult

graphql OnTodoAdded Subscription {} -> TodoResult
