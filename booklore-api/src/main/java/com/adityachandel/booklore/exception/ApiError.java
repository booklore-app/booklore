package com.adityachandel.booklore.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public enum ApiError {
    GENERIC_NOT_FOUND(HttpStatus.NOT_FOUND, "%s"),
    GENERIC_BAD_REQUEST(HttpStatus.BAD_REQUEST, "%s"),
    GENERIC_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "%s"),

    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "Book not found with ID: %d"),
    EMAIL_PROVIDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Email provider with ID %s not found"),
    DEFAULT_EMAIL_PROVIDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Default email provider not found"),
    EMAIL_RECIPIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Email recipient with ID %s not found"),
    DEFAULT_EMAIL_RECIPIENT_NOT_FOUND(HttpStatus.NOT_FOUND, " Default Email recipient not found"),
    UNSUPPORTED_BOOK_TYPE(HttpStatus.BAD_REQUEST, "Unsupported book type for viewer settings"),
    INVALID_VIEWER_SETTING(HttpStatus.BAD_REQUEST, "Invalid viewer setting for the book"),
    FILE_READ_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading files from path"),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "Image not found or not readable"),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "Invalid file format, only pdf and epub are supported"),
    LIBRARY_NOT_FOUND(HttpStatus.NOT_FOUND, "Library not found with ID: %d"),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "File size exceeds the limit: %d MB"),
    CACHE_TOO_LARGE(HttpStatus.BAD_REQUEST, "Book archive is too large to cache with current settings"),
    DIRECTORY_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create the directory: %s"),
    INVALID_LIBRARY_PATH(HttpStatus.BAD_REQUEST, "Invalid library path"),
    FILE_ALREADY_EXISTS(HttpStatus.CONFLICT, "File already exists"),
    INVALID_QUERY_PARAMETERS(HttpStatus.BAD_REQUEST, "Query parameters are required for the search."),
    SHELF_ALREADY_EXISTS(HttpStatus.CONFLICT, "Shelf already exists: %s"),
    SHELF_NOT_FOUND(HttpStatus.NOT_FOUND, "Shelf not found with ID: %d"),
    SCHEDULE_REFRESH_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to schedule metadata refresh job. Error: %s"),
    ANOTHER_METADATA_JOB_RUNNING(HttpStatus.CONFLICT, "A metadata refresh job is currently running. Please wait for it to complete before initiating a new one."),
    METADATA_SOURCE_NOT_IMPLEMENT_OR_DOES_NOT_EXIST(HttpStatus.BAD_REQUEST, "Metadata source not implement or does not exist"),
    FAILED_TO_DOWNLOAD_FILE(HttpStatus.INTERNAL_SERVER_ERROR, "Error while downloading file, bookId: %s"),
    INVALID_REFRESH_TYPE(HttpStatus.BAD_REQUEST, "The refresh type is invalid"),
    METADATA_LOCKED(HttpStatus.FORBIDDEN, "Attempt to update locked metadata"),
    USERNAME_ALREADY_TAKEN(HttpStatus.BAD_REQUEST, "Username already taken: %s"),
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "User not found: %s"),
    CANNOT_DELETE_ADMIN(HttpStatus.FORBIDDEN, "Admin user cannot be deleted"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "%s"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "%s"),
    PASSWORD_INCORRECT(HttpStatus.BAD_REQUEST, "Incorrect current password"),
    PASSWORD_TOO_SHORT(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters long"),
    PASSWORD_SAME_AS_CURRENT(HttpStatus.BAD_REQUEST, "New password cannot be the same as the current password"),
    INVALID_CREDENTIALS(HttpStatus.BAD_REQUEST, "Invalid credentials"),
    REMOTE_AUTH_DISABLED(HttpStatus.NON_AUTHORITATIVE_INFORMATION, "Remote login is disabled"),
    SELF_DELETION_NOT_ALLOWED(HttpStatus.FORBIDDEN, "You cannot delete your own account"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "%s"),
    FILE_DELETION_DISABLED(HttpStatus.BAD_REQUEST, "File deletion is disabled"),
    UNSUPPORTED_FILE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "%s"),
    CONFLICT(HttpStatus.CONFLICT, "%s"),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "File not found: %s");

    private final HttpStatus status;
    private final String message;

    ApiError(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public APIException createException(Object... details) {
        String formattedMessage = (details.length > 0) ? String.format(message, details) : message;
        return new APIException(formattedMessage, this.status);
    }
}
