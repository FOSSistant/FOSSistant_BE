package Capstone.FOSSistant.global.apiPayload.exception;

import Capstone.FOSSistant.global.apiPayload.code.BaseErrorCode;

public class GithubApiException extends GeneralException {
    public GithubApiException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
