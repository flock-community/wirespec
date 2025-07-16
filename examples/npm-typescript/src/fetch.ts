import * as assert from "node:assert";

// @ts-ignore
const handler = async (path, init)=> {
    assert.equal(path, "test?test=test")
    assert.deepEqual(init.headers, {"test":"TEST"})
    return Promise.resolve({
        headers: new Map([
            ["Content-Type", "application/json"],
            ["Content-Length", "2"],
        ]),
        text: () => Promise.resolve("{}")
    })
}

const req = {
    method: "GET",
    path: ["test"],
    queries: {
        "test": "test"
    },
    headers: {
        "test": "TEST"
    },
    body: "",
}

const fetchTest = async () => {
    const { wirespecFetch } = await import("wirespec/fetch");
    // @ts-ignore
    const res = await wirespecFetch(req, handler)
    console.log(res)
    assert.equal(res.body, "{}")
}

fetchTest()

