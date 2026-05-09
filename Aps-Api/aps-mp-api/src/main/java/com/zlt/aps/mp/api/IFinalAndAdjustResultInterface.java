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
}
