export module Wirespec {
  export type Method = "GET" | "PUT" | "POST" | "DELETE" | "OPTIONS" | "HEAD" | "PATCH" | "TRACE"
  export type Content<T> = { type:string, body:T }
  export type Request<T> = { path:string, method: Method, query?: Record<string, any[]>, headers?: Record<string, any[]>, content?:Content<T> }
  export type Response<T> = { status:number, headers?: Record<string, any[]>, content?:Content<T> }
}
export module GetTodos {
  export const PATH = "/todos"
  export const METHOD = "GET"
  type RequestUndefined = { path: `/todos`, method: "GET", headers: {}, query: {  "done"?: boolean} } 
  export type Request = RequestUndefined
  type Response200ApplicationJson = { status: 200, content: { type: "application/json", body: Todo[] } }
  type Response404ApplicationJson = { status: 404, content: { type: "application/json", body: Error } }
  export type Response = Response200ApplicationJson | Response404ApplicationJson
  export type Handler = (request:Request) => Promise<Response>
  export type Call = {
    getTodos: Handler
  }
  export const requestUndefined = (props:{  "done"?: boolean}) => ({path: `/todos`, method: "GET", query: {"done": props.done}, headers: {}} as const)
  export const response200ApplicationJson = (props:{  "body": Todo[]}) => ({status: 200, headers: {}, content: {type: "application/json", body: props.body}} as const)
  export const response404ApplicationJson = (props:{  "body": Error}) => ({status: 404, headers: {}, content: {type: "application/json", body: props.body}} as const)
}

export module CreateTodo {
  export const PATH = "/todos"
  export const METHOD = "POST"
  type RequestApplicationJson = { path: `/todos`, method: "POST", headers: {}, query: {}, content: { type: "application/json", body: TodoInput } } 
  export type Request = RequestApplicationJson
  type Response200ApplicationJson = { status: 200, content: { type: "application/json", body: Todo } }
  type Response404ApplicationJson = { status: 404, content: { type: "application/json", body: Error } }
  export type Response = Response200ApplicationJson | Response404ApplicationJson
  export type Handler = (request:Request) => Promise<Response>
  export type Call = {
    createTodo: Handler
  }
  export const requestApplicationJson = (props:{  "body": TodoInput}) => ({path: `/todos`, method: "POST", query: {}, headers: {}, content: {type: "application/json", body: props.body}} as const)
  export const response200ApplicationJson = (props:{  "body": Todo}) => ({status: 200, headers: {}, content: {type: "application/json", body: props.body}} as const)
  export const response404ApplicationJson = (props:{  "body": Error}) => ({status: 404, headers: {}, content: {type: "application/json", body: props.body}} as const)
}

export module GetTodoById {
  export const PATH = "/todos/:id"
  export const METHOD = "GET"
  type RequestUndefined = { path: `/todos/${string}`, method: "GET", headers: {}, query: {} } 
  export type Request = RequestUndefined
  type Response200ApplicationJson = { status: 200, content: { type: "application/json", body: Todo } }
  type Response404ApplicationJson = { status: 404, content: { type: "application/json", body: Error } }
  export type Response = Response200ApplicationJson | Response404ApplicationJson
  export type Handler = (request:Request) => Promise<Response>
  export type Call = {
    getTodoById: Handler
  }
  export const requestUndefined = (props:{  "id": string}) => ({path: `/todos/${props.id}`, method: "GET", query: {}, headers: {}} as const)
  export const response200ApplicationJson = (props:{  "body": Todo}) => ({status: 200, headers: {}, content: {type: "application/json", body: props.body}} as const)
  export const response404ApplicationJson = (props:{  "body": Error}) => ({status: 404, headers: {}, content: {type: "application/json", body: props.body}} as const)
}

export module UpdateTodo {
  export const PATH = "/todos/:id"
  export const METHOD = "PUT"
  type RequestApplicationJson = { path: `/todos/${string}`, method: "PUT", headers: {}, query: {}, content: { type: "application/json", body: TodoInput } } 
  export type Request = RequestApplicationJson
  type Response200ApplicationJson = { status: 200, content: { type: "application/json", body: Todo } }
  type Response404ApplicationJson = { status: 404, content: { type: "application/json", body: Error } }
  export type Response = Response200ApplicationJson | Response404ApplicationJson
  export type Handler = (request:Request) => Promise<Response>
  export type Call = {
    updateTodo: Handler
  }
  export const requestApplicationJson = (props:{  "id": string,  "body": TodoInput}) => ({path: `/todos/${props.id}`, method: "PUT", query: {}, headers: {}, content: {type: "application/json", body: props.body}} as const)
  export const response200ApplicationJson = (props:{  "body": Todo}) => ({status: 200, headers: {}, content: {type: "application/json", body: props.body}} as const)
  export const response404ApplicationJson = (props:{  "body": Error}) => ({status: 404, headers: {}, content: {type: "application/json", body: props.body}} as const)
}

export module DeleteTodo {
  export const PATH = "/todos/:id"
  export const METHOD = "DELETE"
  type RequestUndefined = { path: `/todos/${string}`, method: "DELETE", headers: {}, query: {} } 
  export type Request = RequestUndefined
  type Response200ApplicationJson = { status: 200, content: { type: "application/json", body: Todo } }
  type Response404ApplicationJson = { status: 404, content: { type: "application/json", body: Error } }
  export type Response = Response200ApplicationJson | Response404ApplicationJson
  export type Handler = (request:Request) => Promise<Response>
  export type Call = {
    deleteTodo: Handler
  }
  export const requestUndefined = (props:{  "id": string}) => ({path: `/todos/${props.id}`, method: "DELETE", query: {}, headers: {}} as const)
  export const response200ApplicationJson = (props:{  "body": Todo}) => ({status: 200, headers: {}, content: {type: "application/json", body: props.body}} as const)
  export const response404ApplicationJson = (props:{  "body": Error}) => ({status: 404, headers: {}, content: {type: "application/json", body: props.body}} as const)
}

export type Todo = {
  "id": string,
  "name": string,
  "done": boolean
}


export type TodoInput = {
  "name": string,
  "done": boolean
}


export type Error = {
  "code": string,
  "description"?: string
}

