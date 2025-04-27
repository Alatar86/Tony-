# PrivacyEmail Application Testing Plan

## Overview
This document outlines the testing strategy for the PrivacyEmail application, with a particular focus on security-critical components like the FileService sandbox, the CredentialsService, and the recently refactored dependency injection system.

## FileService Sandbox Security Testing

### Directory Traversal Prevention Tests

| Test Case ID | Description | Input | Expected Result |
|--------------|-------------|-------|-----------------|
| FS-SEC-001 | Basic directory traversal attempt | `"../outsideFile.txt"` | SecurityException |
| FS-SEC-002 | Multi-level directory traversal | `"../../hostSystemFile.txt"` | SecurityException |
| FS-SEC-003 | Hidden traversal within valid path | `"validDir/../../../systemFile.txt"` | SecurityException |
| FS-SEC-004 | Windows-style directory traversal | `"..\\..\\Windows\\system32\\config.sys"` | SecurityException |
| FS-SEC-005 | Mixed slash directory traversal | `"valid/..\\../etc/passwd"` | SecurityException |
| FS-SEC-006 | URL-encoded traversal characters | `"%2e%2e/%2e%2e/etc/passwd"` | SecurityException |
| FS-SEC-007 | Unicode/UTF-8 encoded traversal | Various Unicode representations of "../" | SecurityException |

### Absolute Path Tests

| Test Case ID | Description | Input | Expected Result |
|--------------|-------------|-------|-----------------|
| FS-SEC-101 | Unix absolute path | `"/etc/passwd"` | SecurityException |
| FS-SEC-102 | Windows absolute path with drive | `"C:\\Windows\\system32\\config.sys"` | SecurityException |
| FS-SEC-103 | Windows UNC path | `"\\\\server\\share\\file.txt"` | SecurityException |
| FS-SEC-104 | URI scheme | `"file:///etc/passwd"` | SecurityException |

### Path Edge Cases

| Test Case ID | Description | Input | Expected Result |
|--------------|-------------|-------|-----------------|
| FS-SEC-201 | Empty path | `""` | Appropriate error (likely IllegalArgumentException) |
| FS-SEC-202 | Single dot | `"."` | Success (represents current directory) |
| FS-SEC-203 | Path with only whitespace | `"   "` | Appropriate error |
| FS-SEC-204 | Very long path | 4096+ character path | Appropriate handling or error |
| FS-SEC-205 | Special characters | `"file$name.txt"`, `"file*name.txt"`, etc. | Correct handling based on OS rules |
| FS-SEC-206 | Non-ASCII characters | `"ファイル.txt"`, `"файл.txt"` | Correct handling of Unicode filenames |

### Symbolic Link Tests

| Test Case ID | Description | Preparation | Expected Result |
|--------------|-------------|-------------|-----------------|
| FS-SEC-301 | Read through symlink to sandbox file | Create symlink inside sandbox pointing to another file inside sandbox | Access denied (symlinks disallowed) |
| FS-SEC-302 | Read through symlink to outside file | Create symlink inside sandbox pointing to file outside sandbox | Access denied (symlinks disallowed) |
| FS-SEC-303 | Create symlink | Attempt to create a symlink via FileService | Operation rejected |
| FS-SEC-304 | List directory with symlinks | Directory contains symlinks | Symlinks excluded from results |

### Valid Operations Tests

| Test Case ID | Description | Input | Expected Result |
|--------------|-------------|-------|-----------------|
| FS-SEC-401 | Read valid file | `"validFile.txt"` | File content returned |
| FS-SEC-402 | Write valid file | `"newFile.txt"`, content | File created with content |
| FS-SEC-403 | List valid directory | `"validDir"` | Directory contents returned |
| FS-SEC-404 | Create valid directory | `"newDir"` | Directory created |
| FS-SEC-405 | Delete valid file | `"validFile.txt"` | File deleted |
| FS-SEC-406 | Valid nested path operations | `"dir1/dir2/file.txt"` | Operations succeed |

## CredentialsService Testing

### Unit Tests

| Test Case ID | Description | Input | Expected Result |
|--------------|-------------|-------|-----------------|
| CRED-001 | Save valid credentials | Valid email and password | Credentials saved to Java Preferences |
| CRED-002 | Load existing credentials | N/A | Correct credentials returned |
| CRED-003 | Load non-existent credentials | N/A | Null or empty result |
| CRED-004 | Delete credentials | N/A | Credentials removed |
| CRED-005 | Save with empty email | Empty email, valid password | Appropriate error |
| CRED-006 | Save with empty password | Valid email, empty password | Appropriate error |
| CRED-007 | Save with null values | Null email or password | Appropriate error |

### Security Tests

| Test Case ID | Description | Method | Expected Result |
|--------------|-------------|--------|-----------------|
| CRED-SEC-001 | Password encryption | Review code and debug | Password stored in encrypted form |
| CRED-SEC-002 | Memory handling | Review code | Sensitive data not kept in memory longer than necessary |
| CRED-SEC-003 | Storage location security | Review OS-level storage | Preferences stored in secure OS location |

## DI Refactoring Tests (MainWindowController)

### Manual Testing Plan

| Test Case ID | Description | Steps | Expected Result |
|--------------|-------------|-------|-----------------|
| DI-001 | Basic controller initialization | Launch application | Controller initializes successfully with injected dependencies |
| DI-002 | Controller functionality | Perform basic operations | All controller functionality works with DI structure |
| DI-003 | Error handling | Simulate error conditions | Errors properly handled with DI structure |

### Integration Tests ToDo

- Implement proper JavaFX unit test environment to enable automated testing of controller logic
- Implement mocking framework to provide mock implementations of dependencies
- Create comprehensive test suite for all controller methods

## Future UI Testing Plan

### TestFX Framework Implementation

- Set up TestFX framework for UI automation testing
- Configure CI/CD pipeline to run UI tests on build

### Planned UI Test Cases

| Component | Test Areas |
|-----------|------------|
| Hub View | Loading, navigation, error handling |
| Account Management | Account creation, login, profile updates |
| Message Composition | Text editing, attachments, sending |
| Message Viewing | Rendering, attachments, actions |
| Settings | Configuration changes, persistence |

## Test Directory Structure

Recommended test directory organization:

```
frontend/src/test/java/com/privacyemail/
  ├── services/
  │   ├── credentials/
  │   ├── file/
  │   └── ... (other services)
  ├── controllers/
  │   ├── main/
  │   └── ... (other controllers)
  ├── models/
  │   └── ... (model tests)
  ├── ui/
  │   └── ... (UI tests)
  └── util/
      └── ... (utility tests)
```

## Test Implementation Priority

1. FileService sandbox security tests (highest priority)
2. CredentialsService tests
3. DI structure tests
4. UI automation framework setup
5. Comprehensive UI tests

## Test Environment Requirements

- JUnit 5 for unit testing
- Mockito for mocking dependencies
- TestFX for UI testing
- Coverage reporting tool (JaCoCo)
