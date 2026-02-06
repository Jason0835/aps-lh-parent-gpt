package com.zlt.aps.monthplan.demand.mapper;

import com.zlt.aps.monthplan.api.domain.entity.MpSimulatedResult;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpSimulatedResultMapper.java
 * 描    述：S2-1004.实单模拟排产Mapper接口
 *@author yelq
 *@date 2025-12-31
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Mapper
public interface MpSimulatedResultEntityMapper extends CommBaseMapper<MpSimulatedResult> {
    /**
     *  导出实单模拟
     * @param queryVO 查询条件
     * @return 实单模拟数据
     */
    List<MpSimulatedResult> listExportData(MpSimulatedResult queryVO);
}
