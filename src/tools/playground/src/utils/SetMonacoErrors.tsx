import {Monaco} from "@monaco-editor/react";
import { WsError } from "@flock/wirespec";

export function setMonacoErrors(monaco: Monaco, errors: WsError[]) {
    const model = monaco.editor.getModels();

    monaco.editor.setModelMarkers(
        model[0],
        "owner",
        errors.map(error => {
            let startLineNumber = 1;
            let startColumn = 1;
            let message = error.value;
            // To be removed once WS returns line number of error instead of index
            const match = error.value.match(/at line (\d+) and position (\d+)/);
            if (match) {
                startLineNumber = parseInt(match[1]);
                startColumn = parseInt(match[2]);
                message = message.slice(0, message.indexOf("at line"));
            }
            return {
                startLineNumber: startLineNumber,
                startColumn,
                endLineNumber: startLineNumber,
                endColumn: startColumn + error.length,
                message,
                severity: monaco.MarkerSeverity.Error
            }
        })
    );
}
