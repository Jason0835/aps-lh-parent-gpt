package com.zlt.aps.cd15.mapper;

import com.zlt.aps.cd15.api.domain.entity.Cd15StorageLaneLimit;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * 斜裁库排限制 Mapper。
 */
@Mapper
public interface Cd15StorageLaneLimitMapper extends CommBaseMapper<Cd15StorageLaneLimit> {

    /** 按工厂、日期和班次失效旧库排快照。 */
    int logicDeleteByScope(@Param("factoryCode") String factoryCode,
                           @Param("laneDate") Date laneDate,
                           @Param("shiftCode") String shiftCode,
                           @Param("updateBy") String updateBy,
                           @Param("updateTime") Date updateTime);
}
