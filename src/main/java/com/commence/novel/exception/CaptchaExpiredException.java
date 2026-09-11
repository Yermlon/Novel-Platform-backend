package com.commence.novel.exception;

//验证码过期
public class CaptchaExpiredException extends RuntimeException{
    public CaptchaExpiredException(){
        super();
    }
    public CaptchaExpiredException(String message){
        super(message);
    }
}
