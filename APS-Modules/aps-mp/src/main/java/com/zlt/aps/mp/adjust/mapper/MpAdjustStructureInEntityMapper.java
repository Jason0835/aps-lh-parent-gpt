package com.zlt.aps.mp.adjust.mapper;

import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureIn;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustStructureInMapper.java
 * 描    述：调整-结构内调整记录Mapper接口
 *@author zlt
 *@date 2025-12-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface MpAdjustStructureInEntityMapper extends CommBaseMapper<MpAdjustStructureIn> {

    /**
     * 查询版本列表
     * @param queryVO 查询参数
     * @return 结果
     */
    List<MpAdjustStructureIn> getVersionList(MpAdjustStructureIn queryVO);

    /**
     * 按物料编码+施工阶段批量更新实际调整量（无需先查询，直接按条件 UPDATE）
     *
     * @param factoryCode        工厂编号
     * @param year               年份
     * @param month              月份
     * @param version            调整版本
     * @param materialStageMap   "物料编码|施工阶段" -> 实际调整量 映射
     */
    void updateActualAdjustQtyBatch(@Param("factoryCode") String factoryCode,
                                    @Param("year") Integer year,
                                    @Param("month") Integer month,
                                    @Param("version") String version,
                                    @Param("materialStageMap") Map<String, Integer> materialStageMap);

}
