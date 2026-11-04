type TodoId = String(/^[0-9]+$/g)
type Priority = Integer(1,5)
type Progress = Number(0.0,1.0)

type Todo {
    id: TodoId,
    priority: Priority,
    progress: Progress,
    parent: TodoId?,
    related: TodoId[]
}
