import {http, HttpResponse} from "msw";
import {Todo} from "../../gen/Todo.ts";

const allTodos: Map<string, Todo> = new Map(
    [
        ["1", {id: "1", text: 'Create a todo app', name: 'Create a todo app', done: true}],
        ["2", {id: "2", text: 'Take a break', name: 'Take a break', done: false}],
    ]
);

export const handlers = [
    http.get('/todos', () => {
        return HttpResponse.json(Array.from(allTodos.values()));
    }),

    http.put('/todos/:id', async (request) => {
        const updatedTodo = await request.request.json()
        const id = request.params.id;

        if (id instanceof Array) {
            throw new Error('Invalid id')
        }


        if (!allTodos.has(id)) {
            return HttpResponse.json({}, {status: 404})
        }

        allTodos.delete(id);
        allTodos.set(id, updatedTodo);

        return HttpResponse.json(updatedTodo, {status: 200})
    }),

    http.post('/todos', async (request) => {
        const newTodo = await request.request.json();
        const id = Math.random().toString();
        allTodos.set(id, newTodo);
        return HttpResponse.json(newTodo, {status: 200})
    }),
]
