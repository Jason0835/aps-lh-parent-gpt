package com.zlt.aps.mp.api;

/**
 * 月计划定稿和月计划调整，方法调用需通用字段定义
 * Copyright (c) 2024, All rights reserved。
 * 文件名称：IFinalAndAdjustResultInterface.java
 * 描    述：月计划定稿和月计划调整，方法调用需通用字段定义
 *
 * @author cxy
 * @version 1.0
 */
public interface IFinalAndAdjustResultInterface {

    default String getLastMonthPlanVersion() {
        return "";
    }

    String getMonthPlanVersion();

    String getProductionVersion();

    String getMaterialCode();

    /**
     * 获取施工阶段，用于定稿结果与调整结果按相同业务维度匹配。
     *
     * @return 施工阶段
     */
    default String getConstructionStage() {
        return "";
    }

    String getMainPattern();

    Integer getMouldCavityQty();

    default void setMaxMouldCavityQty(Integer maxMouldCavityQty) {

    }

    default String getTbrProSize() {
        return "";
    }

    String getStructureName();

    default Integer getMaxMouldCavityQty() {
        return 0;
    }

    Integer getTypeBlockQty();

    String getMaterialDesc();

    String getEmbryoCode();
}
