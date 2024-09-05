package com.ruoyi.common4ui.enums;

import com.ruoyi.common.i18n.utils.I18nUtil;

/**
 * 用户会话
 * 
 * @author ruoyi
 */
public enum OnlineStatus
{
    /** 用户状态 */
    on_line(I18nUtil.getMessage("ui.onlineStatus.online")), off_line(I18nUtil.getMessage("ui.onlineStatus.offline"));

    private final String info;

    private OnlineStatus(String info)
    {
        this.info = info;
    }

    public String getInfo()
    {
        return info;
    }
}
