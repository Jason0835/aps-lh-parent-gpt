package com.zlt.aps.mp.factory.service;

import com.zlt.aps.mp.api.domain.entity.DpDemandPlan;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.factory.dto.MpStructureAllocationExportVo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
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
    MpStructureAllocationExportVo getExportVo(MpStructureAllocation param);
}
