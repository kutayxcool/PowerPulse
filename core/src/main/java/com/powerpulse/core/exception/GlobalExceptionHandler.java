package com.powerpulse.core.exception;

import com.powerpulse.core.ignite.IgniteUnavailableException;
import com.powerpulse.core.registration.RegistrationPublishException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("Gönderilen bilgiler geçerli değildir.");

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "İstek gövdesi eksik veya geçersiz JSON formatındadır.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(RegistrationPublishException.class)
    public ResponseEntity<ApiErrorResponse> handleKafkaFailure(
            RegistrationPublishException exception,
            HttpServletRequest request
    ) {
        log.error("Registration mesajı Kafka'ya gönderilemedi.", exception);

        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Ev kaydedildi ancak telemetry servisine şu anda ulaşılamıyor.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedError(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Beklenmeyen sunucu hatası oluştu.", exception);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "İşlem sırasında beklenmeyen bir sunucu hatası oluştu.",
                request.getRequestURI()
        );
    }
    @ExceptionHandler(IgniteUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleIgniteUnavailable(
            IgniteUnavailableException exception,
            HttpServletRequest request
    ) {
        log.error("Apache Ignite işlemi başarısız oldu.", exception);

        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Canlı enerji verilerine şu anda ulaşılamıyor.",
                request.getRequestURI()
        );
    }
    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            String path
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                OffsetDateTime.now()
        );

        return ResponseEntity.status(status).body(response);
    }
}