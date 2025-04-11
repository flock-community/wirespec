# wirespec

![npm version](https://img.shields.io/npm/v/@flock/wirespec)
![license](https://img.shields.io/npm/l/@flock/wirespec)

## Wirespec your API's

Simplify your API development workflows, accelerate implementation, and guarantee strict adherence to defined contract specifications

Wirespec is a tool that simplifies interface design using a contract-first approach, with concise, human-readable specifications as the single source of truth.

Visit [wirespec.io](https://wirespec.io) for more information about the project.

## Features

- **Generate Wirespec files**: Automatically create Wirespec templates or definitions programmatically.
- **Validation**: Ensure your Wirespec files adhere to defined schema and specifications.
- **CLI Support**: Easily integrate with your development workflow using simple CLI commands.
- **Integration Ready**: Works well with modern JavaScript/TypeScript projects.

## Installation

You can install `wirespec` globally or as a dev dependency in your project:

```bash
# Install as a dev dependency
npm install --save-dev wirespec

# Or install globally
npm install -g wirespec
```

## Usage

### CLI

The `wirespec` plugin can be used directly through the command line. Below are some example commands:

```bash
# Create a new Wirespec file
wirespec generate <filename>

# Validate an existing Wirespec file
wirespec validate <filename>
```

### Programmatic Usage

You can also use `wirespec` in your JavaScript or TypeScript code:

```javascript
import { generate, validate } from 'wirespec';

// Generate a Wirespec file
generate('example.wirespec');

// Validate a Wirespec file
const isValid = validate('example.wirespec');
console.log(`Is valid: ${isValid}`);
```

## Contributing

Contributions are welcome! If you find any issues or want to suggest new features, feel free to open an issue or submit
a pull request.

### Development

To set up the project locally:

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/wirespec.git
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Run the tests:
   ```bash
   npm test
   ```

## License

This project is licensed under the [MIT License](LICENSE).

---

*Elevate your Wirespec development workflow with the `wirespec` npm library.*
