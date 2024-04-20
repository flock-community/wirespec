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
    const [isCreatingNewTodo, createNewTodo] = useState<boolean>(false)

    let inputName = "";
    const onKeyDownEvent = (key: string) => {
        if (key === 'Enter') {
            createNewTodo(false);
            todoService.createTodo(inputName).then(todo => {
                setTodos([...todos, todo]);
            });
        }
    }
    const onInputChangeEvent = (name: string) => {
        inputName = name;
    }

    return (<div className="todo__container">
        <div className="todo__list__container">
            <div className="todo__list__header">
                <button className="todo__list__header__add-button" onClick={() => createNewTodo(true)}>
                    +
                </button>
                <div className="todo__list__header__title">
                    Todos
                </div>
            </div>
            <div className="todo__list__body">
                <TodoList todos={todos}></TodoList>
                {isCreatingNewTodo ? <input onChange={(e) => {
                    onInputChangeEvent(e.target.value)
                }} onKeyDown={(e) => onKeyDownEvent(e.code)} type="text" className="todo__list__input"
                                            placeholder="Name"></input> : null}
            </div>
        </div>
    </div>)
}

export default App
const TodoList: React.FC<{ todos: Todo[] }> = ({todos}) => {

    return <div className="todo__list">
        {todos.map(todo => <TodoItem initialTodo={todo}></TodoItem>)}
    </div>
}

const TodoItem: React.FC<{ initialTodo: Todo }> = ({initialTodo}) => {
    const [todo, setTodo] = useState(initialTodo)
    const checkOrUncheckTodo = (todo: Todo) => {
        todoService.updateTodo(todo.id, !todo.done, todo.name).then(() => {
            setTodo({
                ...todo,
                done: !todo.done
            });
        });
    }

    return <div className="todo__item">
        <div className={(todo.done ? "todo__item__name__checked " : "") + "todo__item__name"}>
            {todo.name}
        </div>
        <div className="todo__item__done">
            <Checkbox checked={todo.done} onChange={() => {
                checkOrUncheckTodo(todo)
            }}></Checkbox>
        </div>
    </div>
}


