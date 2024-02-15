export type TodoId = {
  value: string;
};
const validateTodoId = (type: TodoId) =>
  new RegExp(
    "^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$",
  ).test(type.value);

export type Todo = {
  id: TodoId;
  name: string;
  done: boolean;
};

export type TodoInput = {
  name: string;
  done: boolean;
};

export type Error = {
  code: string;
  description: string;
};
