package com.zlt.aps.lh.service;

import java.time.YearMonth;
import java.util.Date;

/**
 * 硫化月计划起始业务处理接口
 *
 * @author ZLT
 * @since 2026-08-31
 */
public interface ILhMonthStartService {

    /**
     * 获取硫化计算月计划的计划量及完成量的起始日期
     * 1、先确定最近库存抓取日
     * 1.1、如果下个月有定稿，则看下个月与本月计划中的库存抓取日，取最大
     * 1.2、如果下个月没有定稿，则看本月的库存抓取日
     * 2、如果确定的库存抓取日在本月范围内，则库存抓取日作为起始日期
     * 否则，本月1号作为起始日期
     *
     * @param factory   工厂编码
     * @param yearMonth 排产年-月份
     * @return
     */
    Date getMonthPlanStartDate(String factory, YearMonth yearMonth);
}
