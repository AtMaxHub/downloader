package cn.go.app;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.ConstraintViolationException;

//@ControllerAdvice
public class GlobalExceptionHandler {
    /**
     * Exception Handler.
     *
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ResponseEntity<String> handle(ConstraintViolationException e) {
        return new ResponseEntity<>("be valid due to validation error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ResponseEntity<String> handleAll(Exception e) {
        return new ResponseEntity<>("be valid due to validation error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
