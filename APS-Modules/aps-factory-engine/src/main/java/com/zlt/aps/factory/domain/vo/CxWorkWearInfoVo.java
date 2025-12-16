package com.zlt.aps.factory.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 成型工装信息
 *
 * @author ZLT
 * @date 20251215
 */
@Data
public class CxWorkWearInfoVo implements Serializable {

    /**
     * 工厂编码
     */
    private String factoryCode;
    /**
     * 工装名称
     */
    private String workWearName;

    /**
     * 规格型号
     */
    private String specificationModel;

    /**
     * 工装类型
     */
    private String workWearType;

    /**
     * 使用机型
     */
    private String usedType;

    /**
     * 数量
     */
    private String qty;
}
