package com.ruoyi.common.core.enums;

import com.ruoyi.common.i18n.utils.I18nUtil;

/**
 * 用户状态
 * 
 * @author ruoyi
 */
public enum UserStatus
{
    OK("0", I18nUtil.getMessage("common.user.status.normal")), DISABLE("1", I18nUtil.getMessage("common.user.status.disabled")), DELETED("2", I18nUtil.getMessage("common.user.status.deleted"));

    private final String code;
    private final String info;

    UserStatus(String code, String info)
    {
        this.code = code;
        this.info = info;
    }

    public String getCode()
    {
        return code;
    }

    public String getInfo()
    {
        return info;
    }
}
