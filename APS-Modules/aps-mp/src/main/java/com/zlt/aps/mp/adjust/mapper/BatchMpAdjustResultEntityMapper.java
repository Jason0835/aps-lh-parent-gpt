package com.zlt.aps.mp.adjust.mapper;

import com.ruoyi.common.datasource.service.IBaseMapper;
import com.zlt.aps.mp.api.domain.entity.MpAdjustResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 月计划调整结果Mapper，仅用于支持批量操作
 */
@Mapper
public interface BatchMpAdjustResultEntityMapper extends IBaseMapper<MpAdjustResult> {

    /**
     * 插入单条记录
     * @param record 调整结果实体
     * @return 插入数量
     */
    int insert(MpAdjustResult record);
}
