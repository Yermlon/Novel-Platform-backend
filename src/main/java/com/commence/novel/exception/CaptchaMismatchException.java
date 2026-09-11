package com.commence.novel.exception;

//验证码不匹配
public class CaptchaMismatchException extends RuntimeException{
    public CaptchaMismatchException() {
        super();
    }

    public CaptchaMismatchException(String message) {
        super(message);
    }
}
