"""
Example tests demonstrating how to use Wirespec-generated Python client code.

This example shows:
1. How to work with generated types
2. How to make type-safe API requests
3. How to handle responses
4. How to mock HTTP interactions for testing

Note: Before running these tests, generate the Python code with:
  wirespec compile -i ./wirespec -o ./src/generated -l Python --shared
"""

import pytest
from typing import Dict, List, Optional
from dataclasses import dataclass


# Mock classes to demonstrate the pattern until code is generated
# After generation, you would import from: src.generated.model and src.generated.endpoint


@dataclass
class MockTodoDto:
    """
    Mock representing the generated TodoDto type.
    After code generation, import from: src.generated.model.TodoDto
    """
    id: str
    name: str
    done: bool


@dataclass
class MockPotentialTodoDto:
    """
    Mock representing the generated PotentialTodoDto type.
    After code generation, import from: src.generated.model.PotentialTodoDto
    """
    name: str
    done: bool


@dataclass
class MockTodoError:
    """
    Mock representing the generated TodoError type.
    After code generation, import from: src.generated.model.TodoError
    """
    code: int
    description: str


class MockSerializer:
    """
    Mock serializer demonstrating the Wirespec.Serialization interface.
    The generated code expects a serializer for JSON serialization/deserialization.
    """

    def serialize(self, value, t):
        """Serialize Python object to JSON string"""
        if isinstance(value, list):
            return f'[{",".join(self.serialize(v, type(v)) for v in value)}]'
        if hasattr(value, '__dict__'):
            # Simple dict serialization for demo
            import json
            return json.dumps(value.__dict__)
        return str(value)

    def deserialize(self, value: Optional[str], t):
        """Deserialize JSON string to Python object"""
        if value is None:
            return None
        import json
        data = json.loads(value)
        if t == MockTodoDto:
            return MockTodoDto(**data)
        elif t == MockPotentialTodoDto:
            return MockPotentialTodoDto(**data)
        elif t == MockTodoError:
            return MockTodoError(**data)
        return data


class TestTodoClient:
    """
    Example tests showing how to use the generated Wirespec client.

    After code generation, you would:
    1. Import endpoint handlers from src.generated.endpoint
    2. Implement request handlers (HTTP client logic)
    3. Use the generated request/response types
    """

    def test_get_todos_success(self):
        """
        Example: GET /api/todos - Successful response

        This demonstrates:
        - Making a GET request with query parameters
        - Handling a 200 response with array body and custom headers
        """
        # After generation, you would use the generated GetTodos endpoint
        # from src.generated.endpoint.GetTodos import GetTodosRequest, GetTodosResponse

        # Mock request parameters
        query_params = {"done": True}

        # Mock successful response
        mock_response_body = [
            MockTodoDto(
                id="550e8400-e29b-41d4-a716-446655440000",
                name="Write Python example",
                done=True
            ),
            MockTodoDto(
                id="550e8400-e29b-41d4-a716-446655440001",
                name="Update documentation",
                done=False
            )
        ]

        # Verify the response structure
        assert len(mock_response_body) == 2
        assert mock_response_body[0].done is True
        assert mock_response_body[1].done is False

        # Custom header demonstration (X-Total from the spec)
        custom_headers = {"X-Total": "2"}
        assert custom_headers["X-Total"] == "2"

    def test_get_todo_by_id_success(self):
        """
        Example: GET /api/todos/{id} - Successful response

        This demonstrates:
        - Making a GET request with path parameters
        - Handling a 200 response with single object body
        """
        # Mock path parameter
        todo_id = "550e8400-e29b-41d4-a716-446655440000"

        # Mock successful response
        mock_response = MockTodoDto(
            id=todo_id,
            name="Complete task",
            done=True
        )

        # Verify response
        assert mock_response.id == todo_id
        assert mock_response.name == "Complete task"
        assert mock_response.done is True

    def test_get_todo_by_id_not_found(self):
        """
        Example: GET /api/todos/{id} - 404 Not Found

        This demonstrates:
        - Handling error responses
        - Working with error types
        """
        # Mock 404 error response
        mock_error = MockTodoError(
            code=404,
            description="Todo not found"
        )

        # Verify error response
        assert mock_error.code == 404
        assert "not found" in mock_error.description.lower()

    def test_post_todo_success(self):
        """
        Example: POST /api/todos - Create new todo

        This demonstrates:
        - Making a POST request with body
        - Sending a request object
        - Handling the created resource in response
        """
        # Mock request body
        new_todo = MockPotentialTodoDto(
            name="New task to complete",
            done=False
        )

        # Mock successful creation response
        mock_response = MockTodoDto(
            id="550e8400-e29b-41d4-a716-446655440099",
            name=new_todo.name,
            done=new_todo.done
        )

        # Verify the created resource
        assert mock_response.id is not None
        assert mock_response.name == new_todo.name
        assert mock_response.done == new_todo.done

    def test_delete_todo_by_id_success(self):
        """
        Example: DELETE /api/todos/{id} - Delete todo

        This demonstrates:
        - Making a DELETE request
        - Handling successful deletion
        """
        # Mock path parameter
        todo_id = "550e8400-e29b-41d4-a716-446655440000"

        # Mock successful deletion (returns the deleted item)
        mock_response = MockTodoDto(
            id=todo_id,
            name="Deleted task",
            done=True
        )

        # Verify the deleted resource
        assert mock_response.id == todo_id

    def test_serialization_example(self):
        """
        Example: Working with the Wirespec serializer

        This demonstrates:
        - Serializing Python objects to JSON
        - Deserializing JSON to Python objects
        - Type-safe conversions
        """
        serializer = MockSerializer()

        # Create a todo
        todo = MockTodoDto(
            id="550e8400-e29b-41d4-a716-446655440000",
            name="Test serialization",
            done=True
        )

        # Serialize to JSON
        json_str = serializer.serialize(todo, MockTodoDto)
        assert "550e8400-e29b-41d4-a716-446655440000" in json_str
        assert "Test serialization" in json_str

        # Deserialize back to object
        deserialized = serializer.deserialize(json_str, MockTodoDto)
        assert deserialized.id == todo.id
        assert deserialized.name == todo.name
        assert deserialized.done == todo.done


class TestRefinedTypes:
    """
    Example tests showing how to work with refined types.

    The Wirespec spec includes:
    - TodoId: String with UUID format validation
    - TestInt: Integer with range constraint (0,_)
    - TestNum: Number with range constraint (_,0.5)
    """

    def test_todo_id_validation(self):
        """
        Example: Working with refined TodoId type

        TodoId is defined as: String(/^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}$/g)
        """
        # Valid UUID
        valid_id = "550e8400-e29b-41d4-a716-446655440000"

        # After code generation, the TodoId refined type would validate this
        # For now, we demonstrate the pattern
        import re
        uuid_pattern = r'^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$'

        assert re.match(uuid_pattern, valid_id) is not None

        # Invalid UUID would fail validation
        invalid_id = "not-a-uuid"
        assert re.match(uuid_pattern, invalid_id) is None

    def test_refined_integer(self):
        """
        Example: Working with refined TestInt type

        TestInt is defined as: Integer(0,_) meaning >= 0
        """
        # Valid values
        valid_int = 42
        assert valid_int >= 0

        # After generation, the refined type would enforce this constraint
        # Invalid value would be rejected
        invalid_int = -1
        assert not (invalid_int >= 0)

    def test_refined_number(self):
        """
        Example: Working with refined TestNum type

        TestNum is defined as: Number(_,0.5) meaning <= 0.5
        """
        # Valid values
        valid_num = 0.3
        assert valid_num <= 0.5

        # After generation, the refined type would enforce this constraint
        # Invalid value would be rejected
        invalid_num = 0.8
        assert not (invalid_num <= 0.5)


# Integration example showing the full flow
def test_integration_example():
    """
    Full integration example showing the typical flow.

    After code generation, this would look like:

    ```python
    from src.generated.endpoint.GetTodos import GetTodos
    from src.generated.wirespec import Wirespec
    import json

    # Create serializer
    class JsonSerializer(Wirespec.Serialization):
        def serialize(self, value, t):
            return json.dumps(value.__dict__)

        def deserialize(self, value, t):
            return t(**json.loads(value))

    # Use the endpoint
    endpoint = GetTodos()
    serializer = JsonSerializer()

    # Make request (pseudo-code)
    raw_request = endpoint.to_raw_request(serializer, request)
    # ... send via HTTP ...
    # ... receive response ...
    response = endpoint.from_raw_response(serializer, raw_response)
    ```
    """
    # This test documents the expected usage pattern
    assert True  # Placeholder for actual integration test
