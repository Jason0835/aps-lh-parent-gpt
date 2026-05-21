package com.zlt.aps.mp.adjust.mapper;

import com.zlt.aps.mp.api.domain.entity.MpAdjustResult;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustResultMapper.java
 * 描    述：调整-调整结果记录Mapper接口
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
public interface MpAdjustResultEntityMapper extends CommBaseMapper<MpAdjustResult> {

    /**
     * 动态更新（只更新非空字段）
     * @param entity 实体（必须包含 id）
     * @return 影响行数
     */
    int forceUpdateById(MpAdjustResult entity);

    /**
     * 通过版本删除调整结果
     * @param factoryCode
     * @param year
     * @param month
     * @param version
     */
    void deleteAdjustResultByVersion(@Param("factoryCode") String factoryCode,
                                            @Param("year") String year,
                                            @Param("month") String month,
                                            @Param("version") String version,
                                            @Param("structureName") String structureName);

    /**
     * 查询版本列表
     * @param queryVO 查询参数
     * @return 结果
     */
    List<MpAdjustResult> getVersionList(MpAdjustResult queryVO);

    /**
     * 查询调整版本列表
     * @param queryVO 查询参数
     * @return 调整版本列表
     */
    List<MpAdjustResult> getAdjustVersionList(MpAdjustResult queryVO);

}
