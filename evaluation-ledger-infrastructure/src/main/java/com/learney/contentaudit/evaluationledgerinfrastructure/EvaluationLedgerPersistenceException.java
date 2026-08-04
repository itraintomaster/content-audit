package com.learney.contentaudit.evaluationledgerinfrastructure;

/**
 * Unchecked wrapper for I/O failures while reading or appending to a
 * {@link FileSystemEvaluationLedger} file. This is not part of any Sentinel
 * contract: it is an implementation-only concern of this adapter.
 */
class EvaluationLedgerPersistenceException extends RuntimeException {

    EvaluationLedgerPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
