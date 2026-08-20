package com.zlt.aps.mp.engine.enums;

import lombok.Getter;

/**
 * 胎胚分配：获取类型
 * 1、<(小于)平均数
 * 2、>(大于)平均数
 * 3、=(等于)平均数
 *
 * @author ZLT
 * @date 20251230
 */
@Getter
public enum EmbryoFindType {
    /**
     * 1 <(小于)平均数
     */
    LT_AVERAGE(1, "<(小于)平均数"),
    /**
     * 2 >(大于)平均数
     */
    GT_AVERAGE(2, ">(大于)平均数"),
    /**
     * 3 =(等于)平均数
     */
    ET_AVERAGE(3, "=(等于)平均数");

    private int type;

    private String desc;

    EmbryoFindType(int type, String desc) {
        this.type = type;
        this.desc = desc;
    }
}
