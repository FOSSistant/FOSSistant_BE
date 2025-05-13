package Capstone.FOSSistant.global.apiPayload.exception;

import Capstone.FOSSistant.global.apiPayload.code.BaseErrorCode;


public class RedisConnectionException extends GeneralException {

    public RedisConnectionException(BaseErrorCode errorCode) {
        super(errorCode);
    }

}
