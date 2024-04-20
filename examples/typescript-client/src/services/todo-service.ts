import {CreateTodo, Todo, UpdateTodo} from "../../gen/Todo.ts";
import Call = CreateTodo.Call;

const getTodos = async (): Promise<Todo[]> => {
    return fetch('/todos').then(res => res.json());
}

const updateTodo = async (id: string, done: boolean, name: string): Promise<Todo> => {
    let req = UpdateTodo.requestApplicationJson({
        id: id,
        body: {
            done: done,
            name: name
        }
    });
    const res = await updateTodoCall.updateTodo(req);
    if (res.status === 404) {
        console.log('Not found');
        throw new Error('Not found');
    }
    return res.content.body;
}

const updateTodoCall: UpdateTodo.Call = {
    async updateTodo(request: UpdateTodo.Request): Promise<UpdateTodo.Response> {
        const res = await fetch(request.path, {
            method: request.method,
            body: JSON.stringify(request.content.body),
        });
        const body = await res.json();
        return UpdateTodo.response200ApplicationJson({body});
    }
}

const createTodo = async (name: string): Promise<Todo> => {
    let req = CreateTodo.requestApplicationJson({
        body: {
            name: name,
            done: false
        }
    });
    const res = await createTodoCall.createTodo(req);
    if (res.status === 404) {
        console.log('Not found');
        throw new Error('Not found');
    }
    return res.content.body;
}

const createTodoCall: Call = {
    async createTodo(request: CreateTodo.Request): Promise<CreateTodo.Response> {
        const res = await fetch(CreateTodo.PATH, {
            method: request.method,
            body: JSON.stringify(request.content.body),
        });
        const body = await res.json();
        return CreateTodo.response200ApplicationJson({body});
    }
}

export const todoService = {
    getTodos,
    createTodo,
    updateTodo
}
