import express, { RequestHandler } from "express";
import {
  Wirespec,
  GetAllTodos,
  GetTodoById,
  SaveTodo,
  DeleteTodo,
} from "./wirespec/Todos";
import {
  deleteTodoById,
  getAllTodos,
  getTodoById,
  saveTodo,
} from "./TodoRepository";

const router = express.Router();
type Handler = (request: any) => Promise<Wirespec.Response<any>>;

export const wirespecHandler = <Call extends Handler>(
  method: string,
  path: string,
  call: Call,
): RequestHandler => {
  const handler: RequestHandler = async (req, res) => {
    const data = await call({
      content: req.body,
      method: req.method.toUpperCase() as Wirespec.Method,
      path: req.url.toString(),
    });
    return res.send(data.content?.body);
  };

  switch (method) {
    case "GET":
      return router.get(path, handler);
    case "POST":
      return router.post(path, handler);
    case "PUT":
      return router.put(path, handler);
    case "DELETE":
      return router.delete(path, handler);
  }

  throw new Error(`Cannot match request ${method}`);
};

wirespecHandler(GetAllTodos.METHOD, GetAllTodos.PATH, async () => {
  return GetAllTodos.response200ApplicationJson({ body: await getAllTodos() });
});

wirespecHandler(GetTodoById.METHOD, GetTodoById.PATH, async (request) => {
  const todoId = { value: request.path.split("/")[2] };
  const todos = await getTodoById(todoId);
  if (!todos) {
    return GetTodoById.response404ApplicationJson({ body: undefined });
  }
  return GetTodoById.response200ApplicationJson({ body: todos });
});

wirespecHandler(SaveTodo.METHOD, SaveTodo.PATH, async (request) => {
  return SaveTodo.response200ApplicationJson({
    body: await saveTodo(request.content),
  });
});

wirespecHandler(DeleteTodo.METHOD, DeleteTodo.PATH, async (request) => {
  const todoId = { value: request.path.split("/")[2] };
  return DeleteTodo.response200ApplicationJson({
    body: await deleteTodoById(todoId),
  });
});

export default router;
