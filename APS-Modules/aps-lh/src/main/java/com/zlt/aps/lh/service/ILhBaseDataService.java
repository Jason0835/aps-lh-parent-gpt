package com.zlt.aps.lh.service;

import com.zlt.aps.lh.context.LhScheduleContext;

import java.util.Date;

/**
 * 硫化排程基础数据服务接口
 * <p>负责加载排程所需的所有基础数据到上下文</p>
 *
 * @author APS
 */
public interface ILhBaseDataService {

    /**
     * 加载所有基础数据到排程上下文
     *
     * @param context 排程上下文
     */
    void loadAllBaseData(LhScheduleContext context);

    /**
     * 仅加载硫化余量计算所需的基础数据（供报表等只读场景复用，不执行完整排程初始化）。
     * <p>加载内容：定稿排产版本、月计划（含 planStartDate）、硫化日计划调整量、月累计完成量、
     * T日班次完成量、前日物料日完成量、前日排程结果、工作日历。</p>
     * <p>月份范围取「排程窗口 + 提前生产观察期」与报表末端的较大者，调用方需先按可能需要覆盖的
     * 最远月份传入末端，加载完成后再按定稿版本判断是否裁剪。</p>
     *
     * @param context            排程上下文（日期/参数字段已就绪，仅缺余量相关数据）
     * @param reportRangeEndDate 报表日期末端（可为空，为空时只覆盖排程窗口与提前生产观察期）
     */
    void loadMonthSurplusData(LhScheduleContext context, Date reportRangeEndDate);
}
