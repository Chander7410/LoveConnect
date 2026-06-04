package com.loveconnect.mongoapp.controller;

import com.loveconnect.mongoapp.dto.ApiMessage;
import com.mongodb.MongoException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, SecurityException.class})
    ResponseEntity<ApiMessage> badRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new ApiMessage(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiMessage> validation(MethodArgumentNotValidException ex) {
        var first = ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .orElse("Validation failed");
        return ResponseEntity.badRequest().body(new ApiMessage(first));
    }

    @ExceptionHandler({MongoException.class, DataAccessResourceFailureException.class})
    ResponseEntity<ApiMessage> database(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ApiMessage("MongoDB is not reachable. Check MONGODB_URI and MongoDB Atlas Network Access."));
    }
}
