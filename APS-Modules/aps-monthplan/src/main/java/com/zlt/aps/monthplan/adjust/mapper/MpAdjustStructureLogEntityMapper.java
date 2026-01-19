package com.zlt.aps.monthplan.adjust.mapper;

import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureLog;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustStructureLogMapper.java
 * 描    述：调整-操作日志Mapper接口
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
public interface MpAdjustStructureLogEntityMapper extends CommBaseMapper<MpAdjustStructureLog> {
    /**
     * 通过版本删除调整结果
     * @param factoryCode
     * @param year
     * @param month
     * @param version
     */
    void deleteAdjustLogByVersion(@Param("factoryCode") String factoryCode,
                                     @Param("year") String year,
                                     @Param("month") String month,
                                     @Param("version") String version,
                                     @Param("structureName") String structureName);
}
