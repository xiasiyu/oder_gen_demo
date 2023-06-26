package com.tw.darkhorse.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    INVALID_PARAMETER("10001"),
    CRESERVE_EXCEPTION("10002"),
    CREATE_ORDER_EXCEPTION("10003"),
    GET_ORDER_EXCEPTION("10004"),
    CANCEL_ORDER_EXCEPTION("10005");

    private final String value;

    ErrorCode(String value) {
        this.value = value;
    }
}
