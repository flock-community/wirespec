---
title: New Type Constraints in Wirespec 0.16.0
slug: /type-constraints
description: Learn about the new type constraints feature introduced in Wirespec 0.16.0
authors: [wilmveel]
tags: [wirespec, types, constraints, validation]
image: /img/code-snippet.png
---

## Introducing Type Constraints in Wirespec 0.16.0

We're excited to announce a powerful new feature in Wirespec 0.16.0: type constraints! This enhancement allows you to refine all types with specific constraints, giving you more control and precision when defining your API specifications.

<!-- truncate -->

## What Are Type Constraints?

Type constraints allow you to add validation rules to your type definitions. Instead of just specifying that a field is a String, Integer, or Number, you can now define exactly what values are acceptable for that field. This helps ensure that your API data meets specific requirements and follows business rules.

## How to Use Type Constraints

In Wirespec 0.16.0 and later, all types can be refined using a simple and intuitive syntax:

```wirespec
type Example {
  string: String(/.{0,50}/g),
  integer: Integer(0, 10),
  number: Number(0.0, 5.5)
}
```

Let's break down each type of constraint:

### String Constraints

For String types, you can use regular expressions to define patterns that the string must match:

```wirespec
// A string with length between 0 and 50 characters
string: String(/.{0,50}/g)

// An email address
email: String(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/g)

// A date in YYYY-MM-DD format
date: String(/^\d{4}-\d{2}-\d{2}$/g)
```

### Integer Constraints

For Integer types, you can specify minimum and maximum values:

```wirespec
// An integer between 0 and 10 (inclusive)
integer: Integer(0, 10)

// A positive integer
positiveInt: Integer(1)

// An integer with an upper bound
maxInt: Integer(, 100)  // Any integer up to 100
```

### Number Constraints

For Number types, you can also specify minimum and maximum values, including decimal numbers:

```wirespec
// A number between 0.0 and 5.5 (inclusive)
number: Number(0.0, 5.5)

// A positive number
positiveNum: Number(0.1, _)

// A number with an upper bound
maxNum: Number(_ , 99.9)  // Any number up to 99.9
```

## Using Constraints with Refined Types

One of the most powerful features of Wirespec is the ability to create refined types. These are custom types that you define based on existing types. You can also apply constraints to these refined types, giving you even more flexibility and control:

```wirespec
// Define a refined type with constraints
type Email = String(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/g)

// Use the refined type in another type definition
type User {
  email: Email,
  username: String(/.{3,20}/g)
}

// Define a refined numeric type with constraints
type PositiveInteger = Integer(1, _)

// Use the refined numeric type
type Product {
  id: PositiveInteger,
  quantity: Integer(0, 1000)
}
```

This approach allows you to create reusable, constrained types that can be used throughout your API specification, promoting consistency and reducing duplication.

## Benefits of Type Constraints

Adding constraints to your types provides several advantages:

1. **Improved Validation**: Catch invalid data before it enters your system
2. **Better Documentation**: Clearly communicate the expected format and range of values
3. **Enhanced Code Generation**: Generate more precise validation code in your target language
4. **Reduced Bugs**: Prevent issues caused by unexpected data formats or values

## Conclusion

Type constraints are a powerful addition to Wirespec that help you define more precise and robust API specifications. By clearly defining the acceptable values for each field, you can improve the quality and reliability of your APIs.

We encourage you to upgrade to Wirespec 0.16.0 and start using type constraints in your specifications. As always, we welcome your feedback and suggestions for future improvements!

**#Wirespec #API #TypeConstraints #Validation #NewFeature**
