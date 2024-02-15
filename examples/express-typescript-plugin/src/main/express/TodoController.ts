import express, { Request, Response } from "express";
import { Todo, TodoInput } from "./wirespec/Todos";
import {
  deleteTodoById,
  getAllTodos,
  getTodoById,
  saveTodo,
} from "./TodoRepository";

const router = express.Router();

router.get("/", async (req: Request, res: Response<Todo[]>) => {
  const todos = await getAllTodos();
  res.send(todos);
});

router.get(
  "/:id",
  async (
    req: Request,
    res: Response<Todo | { code: string; description: string }>,
  ) => {
    const id = req.params.id;
    const todo = await getTodoById({ value: id });
    if (todo) {
      res.send(todo);
    } else {
      res
        .status(404)
        .send({ code: "NOT_FOUND", description: "Todo not found" });
    }
  },
);

router.post("/", async (req: Request, res: Response<Todo>) => {
  const todoInput: TodoInput = req.body;
  const savedTodo = await saveTodo(todoInput);
  res.send(savedTodo);
});

router.delete("/:id", async (req: Request, res: Response<Todo>) => {
  const id = req.params.id;
  const todo = await deleteTodoById({ value: id });
  res.send(todo);
});

export default router;
