package com.zlt.aps.mp.engine.enums;

import lombok.Getter;

/**
 * 续作衔接校验类型匹配
 * 01 没有找到最早收尾的硫化组-机台
 * 02 硫化组收尾日期已经达到结构收尾日
 * 03 没有找到需要排产的计划信息
 *
 * @author ZLT
 * @date 20251230
 */
@Getter
public enum ContinueValidTypeEnum {
    /**
     * 01 没有找到最早收尾的硫化组-机台
     */
    NO_FIND_EARLIEST_CONCLUSION_LH_GROUP("01", "没有找到最早收尾的硫化组-机台"),
    /**
     * 02 硫化组收尾日期已经达到结构收尾日
     */
    PASS_GROUP_END_DAY("02", "硫化组收尾日期已经达到结构收尾日"),
    /**
     * 03 没有找到需要排产的计划信息
     */
    NO_HAS_PRODUCTION_PLAN("03", "没有找到需要排产的计划信息"),
    /**
     * -1 校验通过
     */
    NO_PROBLEM("-1", "校验通过");

    private String validType;

    private String desc;

    ContinueValidTypeEnum(String validType, String desc) {
        this.validType = validType;
        this.desc = desc;
    }
}
