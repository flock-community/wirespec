import {worker} from './mocks/worker'
import {todoClient} from "./client";
import {GetTodos} from "../gen/Todo";

// `worker.start()` returns a Promise that resolves
// once the Service Worker is up and ready to intercept requests.
worker.start().then(() => {
    console.log('Service Worker started');
    const request = GetTodos.requestUndefined({done: true});
    todoClient.getTodos(request).then(todos => {
        if (todos.status === 404) {
            console.log('Not found');
            return;
        }
        console.log(todos.content.body);
    });
});
