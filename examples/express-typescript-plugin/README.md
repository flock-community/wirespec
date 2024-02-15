# Example: How to use the Wirespec TypeScript Plugin

## Wirespec TypeScript Plugin Configuration

```json
{
  "name": "typescript",
  "main": "./src/main/express/index.js",
  "scripts": {
    "test": "echo \"Error: no test specified\" && exit 1",
    "generate": "wirespec compile --input-dir ./src/main/wirespec --package wirespec --output-dir ./src/main/express --language TypeScript",
    "build": "tsc",
    "start": "node ./src/main/express/dist/index.js",
    "dev": "nodemon ./src/main/express/index.ts"
  },
  "devDependencies": {
    "@flock/wirespec": "^0.8.46",
    "@types/express": "^4.17.21",
    "@types/node": "^20.11.18",
    "@types/uuid": "^9.0.8",
    "nodemon": "^3.0.3",
    "ts-node": "^10.9.2",
    "typescript": "^5.3.3"
  },
  "dependencies": {
    "express": "^4.18.2",
    "uuid": "^9.0.1"
  }
}
```

According to the [actual package.json](package.json) file.
