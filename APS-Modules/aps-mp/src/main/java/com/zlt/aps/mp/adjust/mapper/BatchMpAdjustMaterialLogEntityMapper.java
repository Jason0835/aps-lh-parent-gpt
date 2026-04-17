package com.zlt.aps.mp.adjust.mapper;

import com.ruoyi.common.datasource.service.IBaseMapper;
import com.zlt.aps.mp.api.domain.entity.MpAdjustMaterialLog;
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
public interface BatchMpAdjustMaterialLogEntityMapper extends IBaseMapper<MpAdjustMaterialLog> {

    /**
     * 插入单条记录
     * @param record 调整日志实体
     * @return 插入数量
     */
    int insert(MpAdjustMaterialLog record);
}
