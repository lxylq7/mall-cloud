package com.lxylq7.common;

import java.time.Instant;

public class Result<T> {

    private boolean success;
    private int code;
    private String message;
    private T data;
    private long ts;

    public Result() {
        this.ts = Instant.now().toEpochMilli();
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.success = true;
        r.code = 0;
        r.message = "OK";
        r.data = data;
        return r;
    }

    public static <T> Result<T> ok(String message, T data) {
        Result<T> r = new Result<>();
        r.success = true;
        r.code = 0;
        r.message = message;
        r.data = data;
        return r;
    }

    public static <T> Result<T> fail(String message) {
        Result<T> r = new Result<>();
        r.success = false;
        r.code = 1;
        r.message = message;
        r.data = null;
        return r;
    }

    public static <T> Result<T> fail(String message, T data) {
        Result<T> r = new Result<>();
        r.success = false;
        r.code = 1;
        r.message = message;
        r.data = data;
        return r;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.success = false;
        r.code = code;
        r.message = message;
        r.data = null;
        return r;
    }

    public static <T> Result<T> fail(int code, String message, T data) {
        Result<T> r = new Result<>();
        r.success = false;
        r.code = code;
        r.message = message;
        r.data = data;
        return r;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }
}
