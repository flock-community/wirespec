# Example: How to use Wirespec with Python

This example demonstrates how to use Wirespec to generate type-safe Python code for API clients and servers.

## Overview

Wirespec generates **functional and dependency-free** Python code from contract definitions. The generated code includes:
- Type-safe data models with dataclasses
- Refined types with validation (regex patterns, range constraints)
- Endpoint request/response handlers
- Serialization/deserialization interfaces
- No external runtime dependencies!

## Prerequisites

- **Python 3.10 or higher**
- **Wirespec CLI** (see [Building the CLI](#building-the-wirespec-cli))

## Quick Start

### 1. Generate Python Code

Generate type-safe Python code from the Wirespec contract:

```bash
wirespec compile -i ./wirespec -o ./src/generated -l Python --shared
```

**Command breakdown:**
- `-i ./wirespec` - Input directory containing `.ws` contract files
- `-o ./src/generated` - Output directory for generated Python code
- `-l Python` - Target language
- `--shared` - Include shared Wirespec base classes

### 2. Install Dependencies

The generated code has **no runtime dependencies**! Only install dev dependencies for testing:

```bash
# Using pip
pip install -r requirements.txt

# Or using the project config
pip install -e ".[dev]"
```

### 3. Run Tests

Run the example tests to see the generated code in action:

```bash
pytest tests/
```

## Project Structure

```
python-todo/
├── wirespec/
│   └── todo.ws              # Wirespec contract definition
├── src/
│   └── generated/           # Generated Python code (after running compile)
│       ├── __init__.py
│       ├── wirespec.py      # Shared Wirespec base classes
│       ├── model/           # Generated type definitions
│       │   ├── __init__.py
│       │   ├── TodoDto.py
│       │   ├── PotentialTodoDto.py
│       │   ├── TodoError.py
│       │   ├── TodoId.py    # Refined type with UUID validation
│       │   ├── TestInt.py   # Refined type with range constraint
│       │   └── TestNum.py   # Refined type with range constraint
│       └── endpoint/        # Generated endpoint handlers
│           ├── __init__.py
│           ├── GetTodos.py
│           ├── GetTodoById.py
│           ├── PostTodo.py
│           └── DeleteTodoById.py
├── tests/
│   └── test_client.py       # Example usage and tests
├── pyproject.toml           # Python project configuration
├── requirements.txt         # Development dependencies
└── README.md                # This file
```

## The Contract

The example uses a simple Todo API contract defined in `wirespec/todo.ws`:

```wirespec
type TodoId = String(/^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/g)

type TodoDto {
    id: TodoId,
    name: String,
    done: Boolean
}

endpoint GetTodos GET /api/todos ? {done:Boolean?} -> {
    200 -> TodoDto[] # {`X-Total`:Integer}
    400 -> TodoError
}
```

## Using the Generated Code

### 1. Working with Types

```python
from src.generated.model.TodoDto import TodoDto
from src.generated.model.TodoId import TodoId

# Create a todo with type safety
todo = TodoDto(
    id="550e8400-e29b-41d4-a716-446655440000",
    name="Complete task",
    done=True
)
```

### 2. Making API Requests

```python
from src.generated.endpoint.GetTodos import GetTodos
from src.generated.wirespec import Wirespec
import json

# Implement a serializer (you can use any JSON library)
class JsonSerializer(Wirespec.Serialization):
    def serialize(self, value, t):
        if hasattr(value, '__dict__'):
            return json.dumps(value.__dict__)
        return json.dumps(value)

    def deserialize(self, value, t):
        if value is None:
            return None
        data = json.loads(value)
        return t(**data) if hasattr(t, '__init__') else data

# Create endpoint instance
endpoint = GetTodos()
serializer = JsonSerializer()

# Convert request to raw HTTP request
request = GetTodos.Request(
    path=GetTodos.Request.Path(),
    queries=GetTodos.Request.Queries(done=True),
    headers=GetTodos.Request.Headers(),
    body=None
)

raw_request = endpoint.to_raw_request(serializer, request)
# raw_request contains: method, path, queries, headers, body

# After receiving HTTP response, convert back
raw_response = Wirespec.RawResponse(
    status_code=200,
    headers={"X-Total": ["10"]},
    body='[{"id": "...", "name": "...", "done": true}]'
)

response = endpoint.from_raw_response(serializer, raw_response)
# response.body contains List[TodoDto]
# response.headers contains parsed headers
```

### 3. Refined Types with Validation

Wirespec supports refined types with built-in validation:

```python
from src.generated.model.TodoId import TodoId

# TodoId validates UUID format
valid_id = TodoId("550e8400-e29b-41d4-a716-446655440000")  # ✓

# Invalid format would fail validation
# invalid_id = TodoId("not-a-uuid")  # ✗ Raises validation error
```

### 4. Implementing a Server Handler

```python
from src.generated.endpoint.GetTodos import GetTodos
from src.generated.wirespec import Wirespec

class TodoHandler(GetTodos.Handler):
    def handle(self, request: GetTodos.Request) -> GetTodos.Response:
        # Your business logic here
        todos = fetch_todos(done=request.queries.done)

        # Return type-safe response
        return GetTodos.Response200(
            status=200,
            headers=GetTodos.Response.Headers(X_Total=len(todos)),
            body=todos
        )

# Use the handler
handler = TodoHandler()
serializer = JsonSerializer()

# Process incoming raw HTTP request
raw_request = Wirespec.RawRequest(
    method="GET",
    path=["api", "todos"],
    queries={"done": ["true"]},
    headers={},
    body=None
)

request = GetTodos.from_raw_request(serializer, raw_request)
response = handler.handle(request)
raw_response = GetTodos.to_raw_response(serializer, response)
```

## Generated Code Features

### Type Safety
All types are generated as Python dataclasses with type hints for complete IDE support.

### Dependency-Free
The generated code has **zero runtime dependencies**. It only uses Python standard library.

### Refined Types
Supports type constraints:
- **Regex validation**: `String(/pattern/)`
- **Range constraints**: `Integer(min,max)`, `Number(min,max)`
- **Custom types**: All validated at runtime

### Functional Design
The generated code follows functional programming principles:
- Immutable data structures (frozen dataclasses)
- Pure functions for conversions
- No side effects in type definitions

## Building the Wirespec CLI

If you don't have the Wirespec CLI installed, you can build it from the repository root:

```bash
# From the repository root
./gradlew :src:plugin:cli:installDist

# The CLI will be available at:
# src/plugin/cli/build/install/cli/bin/wirespec
```

Add it to your PATH or use the full path:

```bash
# Using full path
/path/to/wirespec/src/plugin/cli/build/install/cli/bin/wirespec compile \
  -i ./wirespec \
  -o ./src/generated \
  -l Python \
  --shared
```

## Integration with Web Frameworks

The generated code integrates easily with any Python web framework:

### Flask Example

```python
from flask import Flask, request, jsonify
from src.generated.endpoint.GetTodos import GetTodos
from src.generated.wirespec import Wirespec

app = Flask(__name__)

@app.route('/api/todos', methods=['GET'])
def get_todos():
    # Convert Flask request to Wirespec request
    raw_request = Wirespec.RawRequest(
        method=request.method,
        path=request.path.split('/'),
        queries={k: v for k, v in request.args.lists()},
        headers={k: v for k, v in request.headers},
        body=request.get_data(as_text=True)
    )

    # Process with generated endpoint
    handler = TodoHandler()
    serializer = JsonSerializer()

    req = GetTodos.from_raw_request(serializer, raw_request)
    resp = handler.handle(req)
    raw_resp = GetTodos.to_raw_response(serializer, resp)

    return jsonify(raw_resp.body), raw_resp.status_code
```

### FastAPI Example

```python
from fastapi import FastAPI
from src.generated.model.TodoDto import TodoDto
from src.generated.endpoint.GetTodos import GetTodos

app = FastAPI()

@app.get("/api/todos", response_model=list[TodoDto])
async def get_todos(done: bool | None = None):
    # Your business logic
    todos = fetch_todos(done=done)
    return todos
```

## Testing

The `tests/test_client.py` file contains comprehensive examples of:
- Making type-safe API requests
- Handling responses and errors
- Working with refined types
- Serialization and deserialization
- Integration patterns

Run tests with:

```bash
# Run all tests
pytest

# Run with verbose output
pytest -v

# Run specific test
pytest tests/test_client.py::TestTodoClient::test_get_todos_success
```

## Learn More

- **Wirespec Documentation**: [https://flock-community.github.io/wirespec](https://flock-community.github.io/wirespec)
- **Contract-First Design**: See the blog posts in the documentation
- **Other Examples**: Check out the Java, Kotlin, and TypeScript examples

## Troubleshooting

### Module not found errors

Make sure you've generated the code first:
```bash
wirespec compile -i ./wirespec -o ./src/generated -l Python --shared
```

### Import errors

The generated code uses relative imports. Make sure you're running from the project root:
```bash
# From the python-todo directory
pytest tests/
```

### Type validation errors

Refined types enforce their constraints at runtime. Check that your values match the defined patterns and ranges in the Wirespec contract.

## Contributing

This example is part of the Wirespec project. Contributions are welcome!

## License

Apache-2.0
