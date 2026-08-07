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

    /** 按工厂和库存日期失效旧快照。 */
    int logicDeleteByScope(@Param("factoryCode") String factoryCode,
                           @Param("stockDate") Date stockDate,
                           @Param("updateBy") String updateBy,
                           @Param("updateTime") Date updateTime);
}
