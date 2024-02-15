import { Todo, TodoId, TodoInput } from "./wirespec/Todos";
import { v4 as uuidv4 } from "uuid";

const todoId: TodoId = { value: "f20ad876-c6a8-48b8-9a23-71787c1ae34a" };
const todos: Todo[] = [{ id: todoId, name: "Learn TypeScript", done: true }];

export async function getAllTodos(): Promise<Todo[]> {
  return new Promise((resolve) => {
    setTimeout(() => resolve(todos), 100);
  });
}

export async function getTodoById(id: TodoId): Promise<Todo | undefined> {
  return new Promise((resolve) => {
    const todo = todos.find((todo) => todo.id.value === id.value);
    setTimeout(() => resolve(todo), 100);
  });
}

export async function saveTodo(todoInput: TodoInput): Promise<Todo> {
  return new Promise((resolve) => {
    const newTodo: Todo = { id: { value: uuidv4() }, ...todoInput };
    todos.push(newTodo);
    setTimeout(() => resolve(newTodo), 100);
  });
}

export async function deleteTodoById(id: TodoId): Promise<Todo> {
  return new Promise((resolve) => {
    const index = todos.findIndex((todo) => todo.id.value === id.value);
    const todo = todos[index];
    todos.splice(index, 1);
    setTimeout(() => resolve(todo), 100);
  });
}
