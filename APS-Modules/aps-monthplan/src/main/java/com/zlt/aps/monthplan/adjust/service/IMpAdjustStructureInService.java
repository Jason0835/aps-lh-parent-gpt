package com.zlt.aps.monthplan.adjust.service;


import com.zlt.aps.monthplan.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.monthplan.api.domain.entity.MdmWorkCalendar;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.monthplan.api.domain.vo.DailyMouldAvailabilityResult;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpAdjustStructureInService.java
 * 描    述：IMpAdjustStructureInService调整-结构内调整记录后端接口
 *@author zlt
 *@date 2025-12-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface IMpAdjustStructureInService  extends IDocService<MpAdjustStructureIn>{

    /**
     * 查询结构内列表
     * @param contextDTO
     * @return
     */
    List<MpAdjustStructureIn> selectMpAdjustStructureInList(MpRollAdjustContextDTO contextDTO);

    /**
     * 获取月定稿数据
     * @param contextDTO 周程滚动调整上下文对象
     * @return 月定稿列表
     */
    List<FactoryMonthPlanFinalAdjustVo> selectMpFinalList(MpRollAdjustContextDTO contextDTO);

    /**
     * 获取周程滚动参数
     * @return
     */
    Map<String, Object> getMpWeekAdjustParam(String factoryCode,String productType);

    /**
     * 根据排产版本获取结构转产列表
     * @param contextDTO 周程滚动调整上下文对象
     * @return 结构转产列表
     */
    List<MpStructureAllocation> selectMpStructureAllocationList(MpRollAdjustContextDTO contextDTO);

    /**
     * 初始锁定日
     * @param contextDTO 周程滚动调整上下文对象
     */
    Integer getLockEndDay(MpRollAdjustContextDTO contextDTO);

    /**
     * 初始结构开始日\收尾日
     * @param contextDTO 周程滚动调整上下文对象
     */
    void initStructureStartAndEndDay(MpRollAdjustContextDTO contextDTO);

    /**
     * 获取工作日历
     * @param contextDTO
     */
    Map<Integer, MdmWorkCalendar> getWorkCalendarMap(MpRollAdjustContextDTO contextDTO);

    /**
     * 获取每日型腔/活块数量
     * @param contextDTO 周程滚动调整上下文对象
     */
    Map<Integer, DailyMouldAvailabilityResult> getCavityAndBlockQtyMap(MpRollAdjustContextDTO contextDTO);
}
