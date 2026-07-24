import { kotestWirespecGenerator } from "@flock/wirespec/generator";
import { expect, test } from "vitest";
import { TodoDtoGenerator } from "./gen/generator/TodoDtoGenerator";
import { UserGenerator } from "./gen/generator/UserGenerator";
import { validateIntRefinedLowerAndUpper } from "./gen/model/IntRefinedLowerAndUpper";
import { validateNumberRefinedLowerAndUpper } from "./gen/model/NumberRefinedLowerAndUpper";
import { validateTodoId } from "./gen/model/TodoId";

test("generates a TodoDto that satisfies its constraints", () => {
  const generator = kotestWirespecGenerator(42);
  const todo = TodoDtoGenerator.generate(generator, []);

  expect(validateTodoId(todo.id)).toBe(true);
  expect(typeof todo.name).toBe("string");
  expect(typeof todo.done).toBe("boolean");
  expect(validateIntRefinedLowerAndUpper(todo.testInt2)).toBe(true);
  expect(validateNumberRefinedLowerAndUpper(todo.testNum2)).toBe(true);
});

test("the same seed produces the same data", () => {
  const first = UserGenerator.generate(kotestWirespecGenerator(7), []);
  const second = UserGenerator.generate(kotestWirespecGenerator(7), []);

  expect(first).toEqual(second);
});

test("different seeds produce different data", () => {
  const first = TodoDtoGenerator.generate(kotestWirespecGenerator(1), []);
  const second = TodoDtoGenerator.generate(kotestWirespecGenerator(2), []);

  expect(first).not.toEqual(second);
});
