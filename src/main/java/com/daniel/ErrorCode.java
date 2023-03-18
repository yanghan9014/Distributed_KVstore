package com.daniel;

enum ErrorCode {
    SUCCESS(0x00),
    NON_EXISTENT_KEY(0x01),
    OUT_OF_SPACE(0x02),
    TEMPORARY_OVERLOAD(0x03),
    INTERNAL_FAILURE(0x04),
    UNRECOGNIZED_COMMAND(0x05),
    INVALID_KEY(0x06),
    INVALID_VALUE(0x07),
    // additional codes here...
    CUSTOM_ERROR(0x20);
    private final int code;
    ErrorCode(int code) {
        this.code = code;
    }
    public int getCode() {
        return this.code;
    }
}
class OutOfSpaceException extends Exception {
    public OutOfSpaceException() {
        super("Out-of-space");
    }
}

class TemporarySystemOverloadException extends Exception {
    public TemporarySystemOverloadException() {
        super("Temporary system overload");
    }
}