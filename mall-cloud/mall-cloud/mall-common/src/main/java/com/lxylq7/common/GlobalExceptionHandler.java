package com.lxylq7.common;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        if (fe == null) {
            return Result.fail("参数校验失败");
        }
        String field = fe.getField();
        String msg = fe.getDefaultMessage();
        if (msg == null || msg.isBlank()) {
            msg = "参数不合法";
        }
        return Result.fail(field + ":" + msg);
    }

    @ExceptionHandler(BindException.class)
    public Result<Object> handleBindException(BindException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        if (fe == null) {
            return Result.fail("参数绑定失败");
        }
        String field = fe.getField();
        String msg = fe.getDefaultMessage();
        if (msg == null || msg.isBlank()) {
            msg = "参数不合法";
        }
        return Result.fail(field + ":" + msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Object> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.fail("缺少参数:" + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Object> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String name = e.getName();
        if (name == null || name.isBlank()) {
            name = "参数";
        }
        return Result.fail(name + "类型不匹配");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Object> handleNotReadable(HttpMessageNotReadableException e) {
        return Result.fail("请求体不合法");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Object> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.fail("不支持的请求方法:" + e.getMethod());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Object> handleIllegalArgument(IllegalArgumentException e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = "参数不合法";
        }
        return Result.fail(msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = "系统异常";
        }
        return Result.fail(msg);
    }
}
