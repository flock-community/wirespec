import './App.css'
import React, {useEffect, useState} from 'react'
import {Todo} from "../gen/Todo.ts";
import {todoService} from "./services/todo-service.ts";
import {Checkbox} from "antd";

const App: React.FC = () => {

    useEffect(() => {
        todoService.getTodos().then(todos => {
            return setTodos(todos);
        });
    }, []);

    const [todos, setTodos] = useState<Todo[]>([])

    return (<div className="todo__container">
        <div className="todo__list__container">
            <div className="todo__list__header">
                <button className="todo__list__header__add-button">
                    +
                </button>
                <div className="todo__list__header__title">
                    Todos
                </div>
            </div>
            <div className="todo__list__body">
                <TodoList todos={todos}></TodoList>
            </div>
        </div>
    </div>)
}

export default App
const TodoList: React.FC<{ todos: Todo[] }> = ({todos}) => {

    return (<div className="todo__list">
        {todos.map(todo => <TodoItem todo={todo}></TodoItem>)}
    </div>)
}

const TodoItem: React.FC<{ todo: Todo }> = ({todo}) => {
    return (<div className="todo__item">
        <div className={(todo.done ? "todo__item__name__checked " : "") + "todo__item__name"}>
            {todo.name}
        </div>
        <div className="todo__item__done">
            <Checkbox checked={todo.done} onChange={() => {
                checkOrUncheckTodo(todo)
            }}></Checkbox>
        </div>
    </div>)
}

const checkOrUncheckTodo = (todo: Todo) => {
    todoService.updateTodo(todo.id, !todo.done, todo.name).then(() => {
        useEffect(() => {
            todo.done = !todo.done;
        }, []);
    });
}
