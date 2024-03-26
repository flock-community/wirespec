import {WsToTypeScript} from "@flock/wirespec";


window.onload = () => {
    let title = document.querySelector("#title");
    let wirespecDefinition = "type Todo {\n" +
        "    id: UUID,\n" +
        "    name: String,\n" +
        "    done: Boolean\n" +
        "}";
    console.log("Converting ws to typescript", wirespecDefinition)
    const wsToTypeScript1 = new WsToTypeScript();
    const wsToTypeScript = wsToTypeScript1.compile(wirespecDefinition);
    console.log("Converted ws to typescript", wsToTypeScript)
    console.log("Converted ws to typescript", wsToTypeScript.errors)
    console.log("Converted ws to typescript", wsToTypeScript.result?.value)

    if (title) title.innerHTML = JSON.stringify(wsToTypeScript.result?.value,null,4);
};
