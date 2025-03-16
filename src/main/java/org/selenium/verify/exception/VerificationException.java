package org.selenium.verify.exception;

public class VerificationException extends Exception{

    public VerificationException(String msg){
        super(msg);
    }

    public VerificationException(Throwable t){
        super(t);
    }
}
