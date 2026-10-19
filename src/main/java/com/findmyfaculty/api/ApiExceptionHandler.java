package com.findmyfaculty.api;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,Object>> status(ResponseStatusException ex){return body(ex.getStatus(),ex.getReason());}
    @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException ex){String message=ex.getBindingResult().getFieldErrors().isEmpty()?"Invalid request":ex.getBindingResult().getFieldErrors().get(0).getField()+" is required";return body(HttpStatus.BAD_REQUEST,message);}
    @ExceptionHandler(Exception.class) public ResponseEntity<Map<String,Object>> other(Exception ex){return body(HttpStatus.INTERNAL_SERVER_ERROR,"Unexpected server error");}
    private ResponseEntity<Map<String,Object>> body(HttpStatus status,String message){Map<String,Object> m=new LinkedHashMap<>();m.put("timestamp",LocalDateTime.now());m.put("status",status.value());m.put("error",status.getReasonPhrase());m.put("message",message);return ResponseEntity.status(status).body(m);}
}
