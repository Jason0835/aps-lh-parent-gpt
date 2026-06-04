package com.zlt.aps.mp.factory.service;

import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.api.domain.entity.DpDemandPlan;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.api.domain.vo.AdjustsCxMachineVo;
import com.zlt.aps.mp.factory.dto.MpStructureAllocationExportStatisticsVo;
import com.zlt.aps.mp.factory.dto.MpStructureAllocationExportVo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpStructureAllocationService.java
 * 描    述：IMpStructureAllocationService排产过程_结构排产后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-29
 */
public interface IMpStructureAllocationService extends IDocService<MpStructureAllocation> {
    /**
     * 根据查询条件，获取结构排产信息
     *
     * @param param
     * @return
     */
    List<MpStructureAllocation> getDataList(MpStructureAllocation param);

    /**
     * 获取日期最接近的下一个结构
     * @param param
     * @return
     */
    MpStructureAllocation getNextStructure(MpStructureAllocation param);

    /**
     * 获取日期最接近的上一个结构
     * @param param
     * @return
     */
    MpStructureAllocation getPreviousStructure(MpStructureAllocation param);

    /**
     * 从缓存中获取调整机台
     * @return
     */
    AdjustsCxMachineVo getAdjustsCxMachineFromRedis();

    /**
     * 调整机台设置到缓存
     * @param adjustsCxMachineVo
     * @return
     */
    void setAdjustsCxMachineFromRedis(AdjustsCxMachineVo adjustsCxMachineVo);


    /**
     *  根据调整参数获取分配结构
     * @param createCondition 调整参数
     * @return 分配结构
     */
    Set<String> findStructureNames(DpDemandPlan createCondition);

    /**
     * 获取结构转产表导出数据
     * @param param
     * @return
     */
    MpStructureAllocationExportStatisticsVo getExportVo(MpStructureAllocation param);


    /**
     * 导出结构转产表数据
     * @param list
     * @return
     */
    byte[] getMpStructureAllocationExportByte(MpStructureAllocationExportStatisticsVo list);

    /**
     * 查询结构的比例配比
     * @param mpStructureAllocation
     * @return
     */
    List<MdmStructureLhRatio> queryMdmStructureLhRatio(MpStructureAllocation mpStructureAllocation);
    
    /**
     * 数据导入
     * @param fileBytes
     * @param importLog
     * @return
     */
    AjaxResult importData(byte[] fileBytes, ImportLog importLog);
    
    /**
     * 导入
     * @param list 列表数据
     * @param list4DayResult 月计划列表数据
     * @param updateSupport 覆盖
     * @param importLogId 导入日志ID
     * @param params 表头参数
     * @param monthPlanVersion 月计划版本
     * @param productVersion 生产版本
     * @return 结果
     */
    AjaxResult importDataStructureAllocation(List<MpStructureAllocationExportVo> list, List<FactoryMonthPlanMouldDayResult> list4DayResult, boolean updateSupport, Long importLogId, String[] params, String monthPlanVersion, String productVersion,
                                             Map<String, String> factoryMap, Map<String, String> productTypeMap);

    /**
     * 导入
     * @param list 列表数据
     * @param updateSupport 覆盖
     * @param importLogId 导入日志ID
     * @param params 表头参数
     * @param monthPlanVersion 月计划版本
     * @param productVersion 生产版本
     * @param isAdjust 是否调整
     * @return 结果
     */
    AjaxResult importDataDayResult(List<FactoryMonthPlanMouldDayResult> list, boolean updateSupport, Long importLogId, String[] params, String monthPlanVersion, String productVersion,
                                   Map<String, String> factoryMap, Map<String, String> productTypeMap, boolean isAdjust);
}
