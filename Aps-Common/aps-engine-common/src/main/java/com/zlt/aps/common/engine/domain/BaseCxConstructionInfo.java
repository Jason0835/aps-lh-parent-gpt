package com.zlt.aps.common.engine.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 成型获取定额需要的相关属性
 */
@Data
public class BaseCxConstructionInfo implements Serializable {
    /**
     * 胎胚代码
     */
    private String embryoCode;

    /**
     * 胎胚施工版本
     */
    private String bomDataVersion;

    /**
     * 规格尺寸信息
     */
    private Double specDimension;

    /**
     * 规格描述
     */
    private String specDesc;

    /**
     * 胎体布层数
     */
    private Integer carcassBothLayer;

    /**
     * 是否补强；0:-是：1：否
     */
    private  String reinforce;

    /**
     * 轮胎类型
     */
    private String tireType;

    /**
     * 断面宽
     */
    private Double sectionWidth;

    /**
     * 安排任务的成型机台编号
     */
    private String cxMachineCode;


}
