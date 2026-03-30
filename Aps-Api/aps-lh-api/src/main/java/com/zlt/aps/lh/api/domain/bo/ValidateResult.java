package com.zlt.aps.lh.api.domain.bo;


import lombok.Data;

/**
 * 验证结果
 */
@Data
public class ValidateResult {

    public static final boolean  SUCCESS=true;

    public static final boolean FAILE=false;

    private boolean isSuccess;

    private String msg;

    public ValidateResult() {
    }

    public ValidateResult(boolean isSuccess, String msg) {
        this.isSuccess=isSuccess;
        this.msg=msg;
    }

    public static ValidateResult success() {
        return success("");
    }

    public static ValidateResult success(String msg) {
        return success(true,msg);
    }

    public static ValidateResult success(boolean isSuccess, String msg) {
        return new ValidateResult(isSuccess, msg);
    }

    public static ValidateResult error(boolean isSuccess, String msg) {
        return new ValidateResult(isSuccess, msg);
    }

    public static ValidateResult error() {
        return error("");
    }

    public static ValidateResult error(String msg) {
        return error(false,msg);
    }

}
