import {GetTodos} from "../gen/Todo";

type TodoClient = GetTodos.Call

const handleCall = async (req:any) => {
    const headers = req.content ? {"Content-Type": req.content?.type } : undefined
    const body = req.content != undefined ? JSON.stringify(req.content.body) : undefined
    const res = await fetch(req.path, {method:req.method, body, headers})
    const json = await res.json()
    return {
        status: res.status,
        content:{
            type: res.headers.get("Content-Type"),
            body:json
        }
    } as any
}

export const todoClient: TodoClient = {
    getTodos: (req) => handleCall(req),
}
