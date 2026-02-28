# Repository Guidelines

## Project Structure & Module Organization
VerifyMC has three active modules:
- `plugin/`: core Bukkit/Spigot/Paper/Folia plugin (Java 17). Java source: `src/main/java`, resources: `src/main/resources`, tests: `src/test/java`.
- `plugin-proxy/`: proxy-side plugin for BungeeCord/Velocity (Java 17).
- `frontend/glassx/`: Vue 3 + TypeScript + Vite web UI.

Supporting paths:
- `docs/`: documentation assets and screenshots.
- `.github/workflows/plugin.yml`: CI for frontend and Java artifacts.
- `version.yml`: release metadata consumed by CI.

## Build, Test, and Development Commands
Run commands from each module directory.

Frontend (`frontend/glassx`):
- `npm ci`: install exact dependencies.
- `npm run dev`: start Vite dev server.
- `npm run build`: produce production bundle.
- `npm run test`: run Vitest test suite.
- `npm run lint`: run ESLint and auto-fix issues.

Core plugin (`plugin`):
- `mvn clean package`: compile and package shaded JAR.
- `mvn test`: run JUnit tests.

Proxy plugin (`plugin-proxy`):
- `mvn clean package`: compile and package proxy artifact.

## Coding Style & Naming Conventions
- Java: 4-space indentation, UTF-8, Java 17.
- Vue/TS: follow ESLint + Prettier in `frontend/glassx`.
- Vue components: `PascalCase.vue` (e.g., `UserManagement.vue`).
- Composables/utilities: `camelCase` files (e.g., `useNotification.ts`).
- Java packages remain under `team.kitemc.verifymc`.

## Testing Guidelines
- Frontend uses Vitest; test files follow `*.spec.ts` (example: `src/composables/useAdminUsers.spec.ts`).
- Java tests live under `src/test/java` in each Maven module.
- Before PR: run frontend `lint` + `test`, and `mvn test` for touched Java modules.

## Commit & Pull Request Guidelines
- Use concise, imperative commit messages; conventional prefixes are recommended (e.g., `fix(frontend): ...`, `refactor(admin): ...`).
- Keep commits focused by module or concern.
- PRs should include:
  - behavior summary,
  - linked issue (if any),
  - screenshots for UI changes (`frontend/glassx`),
  - local verification commands executed.
- Target `master` and ensure CI passes before merge.
