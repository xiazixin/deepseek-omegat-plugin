# Windows Build Scripts

This directory contains batch scripts for building the DeepSeek OmegaT Plugin on Windows.

## Available Scripts

### `build.bat`
Compiles the project and packages it.
```
build.bat
```

### `clean.bat`
Removes all build artifacts from the `build/` directory.
```
clean.bat
```

### `test.bat`
Runs all unit tests.
```
test.bat
```

### `build-all.bat`
Performs a complete build cycle: clean → build → test.
```
build-all.bat
```

## Prerequisites

- Java Development Kit (JDK) 11 or higher
- The project includes a Gradle wrapper, so no separate Gradle installation is required

## Usage

1. Open Command Prompt or PowerShell in the project root directory
2. Run the desired script:
   ```
   build.bat
   ```

## Output

- Compiled classes: `build/classes/`
- Build output: `build/libs/`
- Test reports: `build/reports/`

## Troubleshooting

If you get a "gradlew.bat not found" error:
1. Ensure you're running the scripts from the project root directory
2. Generate the Gradle wrapper if it's missing:
   ```
   gradle wrapper
   ```

## Environment

These scripts are designed for Windows. For macOS/Linux, use the corresponding `.sh` scripts or run Gradle commands directly.
