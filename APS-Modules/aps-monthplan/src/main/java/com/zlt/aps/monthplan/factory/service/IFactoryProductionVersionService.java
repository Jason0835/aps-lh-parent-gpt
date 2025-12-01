package com.zlt.aps.monthplan.factory.service;


import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IFactoryProductionVersionService.java
 * 描    述：IFactoryProductionVersionService-分厂排产版本业务接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-05-26
 */
public interface IFactoryProductionVersionService {

    /**
     * 设置分厂排产版本的周期
     * 需要根据参数 SYS003 值在[2,28]之间则为非自然月，其它值则判定为自然月
     * 自然月的起始周期，为year-month的起始日期
     * 非自然月的周期：
     * 开始日为前一个月的SYS003天，
     * 结束日为year-month的SYS003前一天
     *
     * @param factoryProductionVersion
     */
    void setProductionVersionCycleDate(FactoryProductionVersion factoryProductionVersion);


    /**
     * 根据分厂编码，及日期，获取定稿版本信息
     *
     * @param factoryCode 分厂编码
     * @param date        日期
     * @return
     */
    FactoryProductionVersion getFinalVersion(String factoryCode, Date date);

    /**
     * 根据排产版本，获取排产版本信息
     *
     * @param productionVersion 排产版本号
     * @return
     */
    FactoryProductionVersion getProductionVersion(String productionVersion);

    /**
     * 根据分厂编码、年、月获取定稿版本信息
     *
     * @param factoryCode 分厂编码
     * @param year        年
     * @param month       月
     * @return
     */
    FactoryProductionVersion getFinalVersionByYearMonth(String factoryCode, Integer year, Integer month);
}
