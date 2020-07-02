package com.fastchar.pay.exception;

public class WxPayException extends RuntimeException {
    private static final long serialVersionUID = -7312426516166496605L;

    public WxPayException() {
        super();
    }

    public WxPayException(String message) {
        super(message);
    }

    public WxPayException(String message, Throwable cause) {
        super(message, cause);
    }

    public WxPayException(Throwable cause) {
        super(cause);
    }
}
