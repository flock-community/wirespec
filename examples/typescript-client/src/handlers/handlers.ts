import {http, HttpResponse} from "msw";

export const handlers = [
    // Intercept "GET https://example.com/user" requests...
    http.get('http://localhost:1234/todos', () => {
        // ...and respond to them using this JSON response.
        return HttpResponse.json({
            id: "1",
            name: "whatever",
            done: false
        })
    }),
]
