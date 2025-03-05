// First attempt generated with Jetbrains AI
// https://prismjs.com/tokens.html
Prism.languages['wirespec'] = Prism.languages.extend('clike', {
    'keyword': {
        pattern: /\b(type|enum|endpoint|channel)\b/,
        greedy: true
    },
    'builtin': {
        pattern: /\b(Integer|String|Boolean|Float|Double|Long|Short|Byte|Char|Date|DateTime|Time|UUID|URI|URL|BigDecimal|BigInteger)\b/,
        greedy: true,
    },
    'http-method': {
        pattern: /\b(GET|POST|PUT|DELETE|OPTIONS|HEAD|PATCH|TRACE)\b/,
        greedy: true
    },
    'status-code': {
        pattern: /\b[1-5][0-9][0-9]\b/,
        greedy: true
    },
    'type-name': {
        pattern: /\b[A-Z][a-zA-Z0-9_]*\b/,
        greedy: true
    },
    'identifier': {
        pattern: /[a-z`][a-zA-Z0-9_\-`]*/,
        greedy: true
    },
    'punctuation': {
        pattern: /([{}:,]|->)/,
        greedy: true
    },
    'string': {
        pattern: /"[^"]*"/,
        greedy: true
    },
    'regex': {
        pattern: /\/[^/\n]+\/[gimuy]*/,
        greedy: true
    },
    'comment': {
        pattern: /^\/\*(\*(?!\/)|[^*])*\*\//gm,
    }
});
