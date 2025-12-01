package com.zlt.aps.itf.vo;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;

import java.util.HashMap;

public class ItfAjaxResult extends HashMap<String, Object> {
    private static final long serialVersionUID = 1L;
    public static final String CODE_TAG = "code";
    public static final String MSG_TAG = "message";
    public static final String DATA_TAG = "data";
    public static final String STATUS_TAG = "status";

    public ItfAjaxResult() {
    }

    public ItfAjaxResult(int code, String msg) {
        super.put("code", code);
        super.put("message", msg);
    }

    public ItfAjaxResult(int code, String msg, Object data, String status) {
        super.put("code", code);
        super.put("message", msg);
        super.put("status", status);
        if (StringUtils.isNotNull(data)) {
            super.put("data", data);
        }

    }

    public ItfAjaxResult put(String key, Object value) {
        super.put(key, value);
        return this;
    }

    public ItfAjaxResult(Type type, String msg, String status) {
        super.put("code", type.value);
        super.put("message", msg);
        super.put("status", status);
    }

    public ItfAjaxResult(Type type, String msg, Object data, String status) {
        super.put("code", type.value);
        super.put("message", msg);
        super.put("status", status);
        if (StringUtils.isNotNull(data)) {
            super.put("data", data);
        }

    }

    public static ItfAjaxResult success() {
        return success(I18nUtil.getMessage("common.msg.ajax.operation.success"));
    }

    public static ItfAjaxResult success(Object data) {
        return success(I18nUtil.getMessage("common.msg.ajax.operation.success"), data);
    }

    public static ItfAjaxResult success(String msg) {
        return success(msg, (Object) null);
    }

    public static ItfAjaxResult success(String msg, Object data) {
        return new ItfAjaxResult(200, msg, data, "success");
    }

    public static ItfAjaxResult error() {
        return error(I18nUtil.getMessage("common.msg.ajax.operation.fail"));
    }

    public static ItfAjaxResult error(String msg) {
        return error((String) msg, (Object) null);
    }

    public static ItfAjaxResult error(String msg, Object data) {
        return new ItfAjaxResult(500, msg, data,"fail");
    }

    public static ItfAjaxResult error(int code, String msg) {
        return new ItfAjaxResult(code, msg, (Object) null, "fail");
    }

    public ItfAjaxResult error(Type type, String message) {
        return new ItfAjaxResult(type.value, message);
    }

    public static enum Type {
        SUCCESS(0),
        WARN(301),
        ERROR(500);

        private final int value;

        private Type(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }
}
