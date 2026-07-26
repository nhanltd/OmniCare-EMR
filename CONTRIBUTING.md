# Contributing to OmniCare EMR

Thank you for your interest in contributing to OmniCare EMR! This document outlines our contribution guidelines and code standards.

## Code Standards & Conventions

### 1. Git Commit Specification (Conventional Commits v1.0.0)
All commit messages MUST follow the [Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/) specification:

`<type>[optional scope]: <description>`

Common types:
- `feat`: A new feature for the user or application.
- `fix`: A bug fix for the application.
- `docs`: Documentation updates.
- `refactor`: Code changes that neither fix a bug nor add a feature.
- `test`: Adding or correcting unit/integration tests.
- `chore`: Infrastructure or tooling changes.

### 2. Git Branching Strategy (Git Flow)
- `main`: Production-ready releases only.
- `develop`: Integration branch for ongoing development.
- `feature/<feature-name>`: Short-lived feature branches branched from `develop`.

### 3. Pull Request Process
1. Fork the repository and create your feature branch from `develop`.
2. Ensure all 79+ unit and integration tests pass by running `mvn test`.
3. Submit a Pull Request targeting the `develop` branch.
4. Ensure GitHub Actions CI pipeline passes all build and CodeQL security checks.
