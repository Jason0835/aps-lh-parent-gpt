package com.zlt.mix.common.core.enums;

import com.ruoyi.common.i18n.utils.I18nUtil;

/**
 * 收尾计划转换枚举
 * @author: Chen
 * @since: 2022/5/31 15:18
 */
public enum IsFinishEnum {

    /**
     * 收尾计划：否
     */
    IS_FINISH_NOT("0", I18nUtil.getMessage("schedule.glueCollectPlan.isFinishing.not")),
    /**
     * 收尾计划：是，准备收尾
     */
    IS_FINISH_YES("1", I18nUtil.getMessage("schedule.glueCollectPlan.isFinishing.yes"));

    private final String dictLabel;
    private final String dictValue;

    IsFinishEnum(String dictValue, String dictLabel) {
        this.dictValue = dictValue;
        this.dictLabel = dictLabel;
    }

    public String getDictLabel() {
        return dictLabel;
    }

    public String getDictValue() {
        return dictValue;
    }
}
