# Contributing to MockHub

Thank you for your interest in contributing to MockHub! This project serves as a teaching platform for AI integration, so contributions that improve the learning experience are especially welcome.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/mockhub.git`
3. Create a feature branch: `git checkout -b feature/your-feature-name`
4. Follow the setup instructions in [README.md](README.md)

## Development Workflow

### Before You Code

- Read [ARCHITECTURE.md](ARCHITECTURE.md) to understand the project structure and design decisions
- Read [CLAUDE.md](CLAUDE.md) for coding conventions
- Check existing issues to see if your idea is already being discussed

### While You Code

- Follow the coding conventions in CLAUDE.md
- Write tests for all new functionality
- Keep commits focused — one logical change per commit
- Use imperative mood in commit messages: "Add feature" not "Added feature"

### Submitting Changes

1. Ensure all tests pass:
   ```bash
   # Backend
   cd backend && ./gradlew test

   # Frontend
   cd frontend && npm test

   # E2E (requires running stack)
   cd frontend && npx playwright test
   ```

2. Ensure code passes linting:
   ```bash
   # Frontend
   cd frontend && npm run lint
   ```

3. Push your branch and open a Pull Request against `main`
4. Fill out the PR template with a clear description of your changes

## What to Contribute

### High-Value Contributions

- **AI feature implementations** — Implement the stub endpoints in the `ai/` package
- **Bug fixes** — Found something broken? Fix it!
- **Test coverage** — More tests are always welcome
- **Documentation** — Improve explanations, add examples, fix typos
- **Accessibility** — Improve keyboard navigation, screen reader support, ARIA attributes

### Before Adding New Dependencies

New dependencies should be discussed in an issue first. The project intentionally keeps its dependency footprint manageable for students. Reference ARCHITECTURE.md section 4.10 for the current dependency list.

### Before Changing Architecture

Changes to the database schema, API contracts, or package structure should be discussed in an issue first and reflected in ARCHITECTURE.md if accepted.

## Code Review Standards

Pull requests will be reviewed for:

- **Correctness** — Does it work? Are edge cases handled?
- **Tests** — Are there adequate unit and integration tests?
- **Conventions** — Does it follow the patterns in CLAUDE.md?
- **Teaching value** — Is the code clear enough for students to learn from?
- **Security** — No hardcoded secrets, no SQL injection, proper input validation

## Reporting Issues

When reporting a bug:

1. Check if the issue already exists
2. Include steps to reproduce
3. Include expected vs. actual behavior
4. Include your environment (Java version, Node version, OS)

When suggesting a feature:

1. Describe the use case
2. Explain how it benefits the learning experience
3. Consider how it fits into the existing architecture

## Questions?

Open a discussion or issue. We're happy to help!
