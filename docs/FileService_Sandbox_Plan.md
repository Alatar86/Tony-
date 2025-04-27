# FileService Sandbox Security Plan

## Overview
This document outlines the security design and implementation plan for the FileService sandbox in the PrivacyEmail application. The sandbox is a critical security feature that ensures file operations are strictly confined to a designated area, preventing unauthorized access to the host system.

## Core Principles

### Isolation Boundary
- The FileService MUST rigorously prevent any file access (read, write, list, delete, create) outside the single, user-designated root sandbox folder specified in the application settings.
- All file operations must be validated against this boundary before execution.
- The sandbox root directory will be specified in the application configuration and cannot be modified at runtime without proper authorization.

## Security Implementation

### Path Validation
- **All paths** will be validated before any file operation is performed.
- **Directory Traversal Prevention**:
  - Paths containing parent directory references (`../`, `..\\`) will be explicitly rejected.
  - Path normalization will be performed to eliminate any attempts to bypass security through path manipulation.
  - All path components will be checked individually to ensure compliance with security rules.
- **Implementation Strategy**:
  - Use Java's `Path.normalize()` method to resolve paths, then verify the normalized path is a child of the sandbox root.
  - Implement a dedicated `PathValidator` class that performs all security checks consistently across all FileService methods.

### Absolute Paths
- **Absolute paths** are strictly forbidden in all FileService operations.
- All paths provided to the service must be relative to the sandbox root.
- If an absolute path is detected, the operation will be rejected with a security violation exception.
- Implementation will use `Path.isAbsolute()` to detect absolute paths before any file operation.

### Symbolic Links
- **Symbolic links** will be disallowed entirely within the sandbox.
- The service will check for symbolic links before performing any operations:
  - When listing directories, symbolic links will be excluded from results.
  - When accessing a file, the service will verify it is not a symbolic link.
  - The service will refuse to create symbolic links.
- Implementation will use `Files.isSymbolicLink()` to detect symbolic links.

### Permissions and Access Control
- The FileService operates with the application's user-level permissions but is logically constrained by the sandbox boundary.
- No file operations will use elevated privileges.
- File permissions within the sandbox will follow the principle of least privilege.
- Regular permission checks will be performed to ensure operations don't fail due to system-level permission issues.

## FileService API Design

### Method Signatures
All methods in the FileService will operate exclusively on paths relative to the sandbox root:

```java
public interface FileService {
    // File operations
    byte[] readFile(String relativePath) throws IOException, SecurityException;
    void writeFile(String relativePath, byte[] content) throws IOException, SecurityException;
    boolean deleteFile(String relativePath) throws IOException, SecurityException;
    boolean fileExists(String relativePath) throws SecurityException;

    // Directory operations
    List<String> listFiles(String relativePath) throws IOException, SecurityException;
    boolean createDirectory(String relativePath) throws IOException, SecurityException;
    boolean deleteDirectory(String relativePath, boolean recursive) throws IOException, SecurityException;
    boolean directoryExists(String relativePath) throws SecurityException;

    // Utility methods
    String getSandboxRoot();
    long getFileSize(String relativePath) throws IOException, SecurityException;
}
```

**Note**: All path arguments are relative within the sandbox. The implementation will internally resolve them against the sandbox root path.

## Error Handling

- Security violations will throw specific `SecurityException` subclasses to distinguish them from regular I/O errors.
- Detailed error messages for security violations will be logged but not exposed to the user interface to prevent information leakage.
- Common error types include:
  - `SandboxPathViolationException`: When a path attempts to access outside the sandbox.
  - `InvalidPathException`: When a path contains invalid characters or formats.
  - `SymbolicLinkException`: When an operation involves a symbolic link.

## Security Testing

The FileService sandbox implementation will undergo rigorous security testing, as detailed in the separate Testing Plan document. This will include attempts to bypass the sandbox boundary through path manipulation, symbolic links, and other common attack vectors.
