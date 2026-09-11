package com.commence.novel.exception;

//邮箱不存在
public class EmailExistsException extends RuntimeException{

    public EmailExistsException() {
        super();
    }
    public EmailExistsException(String message) {
        super(message);
    }
}
