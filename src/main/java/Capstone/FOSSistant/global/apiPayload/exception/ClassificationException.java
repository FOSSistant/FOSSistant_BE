package Capstone.FOSSistant.global.apiPayload.exception;
import Capstone.FOSSistant.global.apiPayload.code.BaseErrorCode;
import Capstone.FOSSistant.global.apiPayload.code.status.ErrorStatus;

public class ClassificationException extends GeneralException {

    public ClassificationException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}