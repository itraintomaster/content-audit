package com.learney.contentaudit.auditinfrastructure;

/**
 * Thrown when an audit report cannot be read from or written to the filesystem.
 */
class AuditPersistenceException extends RuntimeException {

    AuditPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
