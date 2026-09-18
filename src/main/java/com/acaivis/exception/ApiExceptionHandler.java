package com.acaivis.exception;
import org.springframework.http.*; import org.springframework.web.bind.MethodArgumentNotValidException; import org.springframework.web.bind.annotation.*; import java.time.LocalDateTime; import java.util.stream.Collectors;
@RestControllerAdvice public class ApiExceptionHandler { record ErrorResponse(LocalDateTime timestamp,int status,String error,String message) {}
 @ExceptionHandler(ResourceNotFoundException.class) ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException e){return build(404,"Not Found",e.getMessage());}
 @ExceptionHandler(BusinessException.class) ResponseEntity<ErrorResponse> business(BusinessException e){return build(400,"Bad Request",e.getMessage());}
 @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<ErrorResponse> illegalArgument(IllegalArgumentException e){return build(400,"Bad Request",e.getMessage());}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e){String msg=e.getBindingResult().getFieldErrors().stream().map(x->x.getField()+": "+x.getDefaultMessage()).collect(Collectors.joining(", ")); return build(400,"Validation Error",msg);}
 private ResponseEntity<ErrorResponse> build(int s,String err,String msg){return ResponseEntity.status(s).body(new ErrorResponse(LocalDateTime.now(),s,err,msg));}}
