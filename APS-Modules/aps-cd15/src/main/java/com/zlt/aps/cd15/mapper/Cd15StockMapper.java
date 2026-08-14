package com.zlt.aps.cd15.mapper;

import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * 斜裁库存管理 Mapper。
 */
@Mapper
public interface Cd15StockMapper extends CommBaseMapper<Cd15Stock> {

    /** 按工厂、库存日期和班次失效旧快照。 */
    int logicDeleteByScope(@Param("factoryCode") String factoryCode,
                           @Param("stockDate") Date stockDate,
                           @Param("shiftCode") String shiftCode,
                           @Param("updateBy") String updateBy,
                           @Param("updateTime") Date updateTime);
}
