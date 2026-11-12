package com.runwise.admin;

import com.runwise.common.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.ConstraintViolationException;

/** 管理接口保留统一业务码，同时返回真实的认证 HTTP 状态。 */
@Order(-1)
@RestControllerAdvice(assignableTypes=AdminController.class)
public class AdminExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> business(BusinessException e) {
        int status = e.getCode() >= 400 && e.getCode() <= 599 ? e.getCode() : 400;
        return ResponseEntity.status(status).body(Result.error(e.getCode(),e.getMessage()));
    }
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> invalid(ConstraintViolationException e) {
        return ResponseEntity.badRequest().body(Result.error(400,"查询参数不合法"));
    }
}
