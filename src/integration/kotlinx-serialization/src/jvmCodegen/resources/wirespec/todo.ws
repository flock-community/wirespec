type TodoId = String(/^[0-9]+$/g)
type Priority = Integer(1,5)
type Progress = Number(0.0,1.0)

enum Status { OPEN, DONE }

type Todo {
    id: TodoId,
    name: String,
    priority: Priority,
    progress: Progress,
    status: Status,
    parent: TodoId?,
    related: TodoId[]
}
