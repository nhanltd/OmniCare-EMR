# Rule: Git Commit Message Standard (Conventional Commits v1.0.0)

All Git commit messages generated, executed via `git commit`, or suggested to the user MUST strictly follow the Conventional Commits v1.0.0 specification (https://www.conventionalcommits.org/en/v1.0.0/).

### Structure Format
`<type>[optional scope]: <description>`

`[optional body]`

`[optional footer(s)]`

### Standard Types
- **feat**: A new feature for the user or application (correlates with SemVer MINOR).
- **fix**: A bug fix for the application (correlates with SemVer PATCH).
- **docs**: Documentation only changes.
- **style**: Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc.).
- **refactor**: A code change that neither fixes a bug nor adds a feature.
- **perf**: A code change that improves performance.
- **test**: Adding missing tests or correcting existing tests.
- **build**: Changes that affect the build system or external dependencies (e.g., Maven pom.xml, Gradle, npm).
- **ci**: Changes to CI configuration files and scripts (e.g., GitHub Actions, Docker).
- **chore**: Other changes that don't modify src or test files.

### Breaking Changes
- Append a `!` before the colon in type/scope (e.g. `feat(api)!: drop support for legacy V1 endpoints`), OR
- Include a footer starting with `BREAKING CHANGE: <description>` (correlates with SemVer MAJOR).

### Formatting Rules
1. The description MUST be short, lowercase, and imperative (e.g., `add patient search endpoint`, NOT `Added patient search`).
2. Do NOT end the description with a period.
3. Separate the body and footers with a single blank line.
