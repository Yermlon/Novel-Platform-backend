package com.commence.novel.exception;

//验证码不存在
public class CaptchaNotFoundException extends  RuntimeException {
    public CaptchaNotFoundException(){
        super();
    }
    public CaptchaNotFoundException(String message) {
        super(message);
    }
}
