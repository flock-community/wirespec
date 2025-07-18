import { wirespecFetch, HandleFetch } from "wirespec/fetch";
import { wirespecSerialization } from "wirespec/serialization";
import {expect, test, vi} from "vitest";
import {client} from "./gen/client";

// @ts-ignore
const mockHandler = vi.fn<HandleFetch>(() => Promise.resolve({
    status: 200,
    headers: new Map([
        ["Content-Type", "application/json"],
        ["Content-Length", "2"],
    ]),
    text: () => Promise.resolve(`{"id": 123, "title": "test", "completed": false}`)
}))

test('testGetTodoById', async () => {
    const apiClient = client(wirespecSerialization, (req) => wirespecFetch(req, mockHandler))
    const res = await apiClient.GetTodoById({id: "123"})
    expect(res.status).toEqual(200)
    expect(res.headers).toEqual({})
    expect(res.body).toEqual({
        "completed": false,
        "id": 123,
        "title": "test",
    })
})
