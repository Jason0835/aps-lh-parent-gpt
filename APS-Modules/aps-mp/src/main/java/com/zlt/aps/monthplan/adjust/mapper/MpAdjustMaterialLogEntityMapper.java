package com.zlt.aps.monthplan.adjust.mapper;

import com.zlt.aps.monthplan.api.domain.entity.MpAdjustMaterialLog;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustMaterialLogMapper.java
 * 描    述：S2-0808.调整-调整日志（未调整及已调整）Mapper接口
 *@author zlt
 *@date 2026-02-09
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface MpAdjustMaterialLogEntityMapper extends CommBaseMapper<MpAdjustMaterialLog> {

    /**
     * 查询版本列表
     * @param queryVO 查询参数
     * @return 结果
     */
    List<MpAdjustMaterialLog> getVersionList(MpAdjustMaterialLog queryVO);

    /**
     * 通过版本删除调整过程日志
     * @param factoryCode
     * @param year
     * @param month
     * @param version
     */
    void deleteAdjustProcLogByVersion(@Param("factoryCode") String factoryCode,
                                     @Param("year") String year,
                                     @Param("month") String month,
                                     @Param("version") String version);
}
