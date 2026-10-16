export const asyncapiExample = `{
  "asyncapi": "3.0.0",
  "info": {
    "title": "Todo events",
    "version": "1.0.0"
  },
  "defaultContentType": "application/json",
  "channels": {
    "todoUpdates": {
      "address": "todos/{todoId}/updates",
      "description": "Every change to a todo",
      "parameters": {
        "todoId": { "description": "The id of the todo" }
      },
      "messages": {
        "todoUpdated": { "$ref": "#/components/messages/TodoUpdated" }
      }
    },
    "todoEvents": {
      "address": "todos.events",
      "messages": {
        "todoCreated": { "$ref": "#/components/messages/TodoCreated" },
        "todoDeleted": { "$ref": "#/components/messages/TodoDeleted" }
      }
    }
  },
  "operations": {
    "publishTodoUpdate": {
      "action": "send",
      "channel": { "$ref": "#/channels/todoUpdates" },
      "messages": [
        { "$ref": "#/channels/todoUpdates/messages/todoUpdated" }
      ]
    },
    "onTodoEvent": {
      "action": "receive",
      "channel": { "$ref": "#/channels/todoEvents" }
    }
  },
  "components": {
    "messages": {
      "TodoUpdated": {
        "name": "TodoUpdated",
        "payload": { "$ref": "#/components/schemas/Todo" }
      },
      "TodoCreated": {
        "payload": { "$ref": "#/components/schemas/Todo" }
      },
      "TodoDeleted": {
        "payload": {
          "type": "object",
          "required": ["id"],
          "properties": {
            "id": { "$ref": "#/components/schemas/TodoId" },
            "reason": { "type": "string" }
          }
        }
      }
    },
    "schemas": {
      "TodoId": {
        "type": "string",
        "pattern": "^[0-9a-f]{8}$"
      },
      "Priority": {
        "type": "string",
        "enum": ["LOW", "MEDIUM", "HIGH"]
      },
      "Todo": {
        "type": "object",
        "description": "A task to do",
        "required": ["id", "title", "done", "priority"],
        "properties": {
          "id": { "$ref": "#/components/schemas/TodoId" },
          "title": { "type": "string", "description": "What needs doing" },
          "done": { "type": "boolean" },
          "priority": { "$ref": "#/components/schemas/Priority", "default": "MEDIUM" },
          "tags": { "type": "array", "items": { "type": "string" } },
          "assignee": {
            "type": "object",
            "required": ["name"],
            "properties": {
              "name": { "type": "string" },
              "email": { "type": ["string", "null"] }
            }
          }
        }
      }
    }
  }
}
`;
