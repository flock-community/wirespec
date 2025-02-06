import {Monaco} from "@monaco-editor/react";

const KEYWORDS = ['Boolean', 'String', 'Integer'];

export function initializeMonaco(monaco: Monaco) {
    const model = monaco.editor.getModels()
    console.log(model)
    if (!model) {
        return;
    }

    monaco.languages.register({id: 'wirespec'})

    monaco?.editor.defineTheme(
        'vs-dark', {
            colors: {
                "editor.foreground": "#BDAE9D",
                "editor.background": "#2A211C",
            },
            inherit: true,
            base: 'vs-dark',
            rules: [
                {token: 'keyword', foreground: '#FFC300', fontStyle: 'bold'},
                {token: 'variable', foreground: '#55f1ba'},
                {token: 'customKeyword', foreground: '#B151CBFF', fontStyle: 'bold'},
                {token: 'regExp', foreground: '#D54E96FF'},
            ]
        }
    )

    monaco.languages.setMonarchTokensProvider('wirespec', {
            keywords: KEYWORDS,
            tokenizer: {
                root: [
                    [/\/.*?\/g/, 'regExp'],
                    [/@?[a-zA-Z][\w$]*/, {
                        cases: {
                            'refined': 'customKeyword',
                            'enum': 'customKeyword',
                            'type': 'customKeyword',
                            'endpoint': 'customKeyword',
                            '@keywords': 'keyword',
                            '@default': 'variable',
                        }
                    }],
                    [/".*?"/, 'string'],
                ]
            }
        }
    )
}