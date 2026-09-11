package com.commence.novel.exception;

//用户名已存在
public class UsernameExistsException extends RuntimeException{
    public UsernameExistsException(){
        super();
    }
    public UsernameExistsException(String message){
        super(message);
    }
}
