package com.zlt.aps.cd90.mapper;

import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
public interface Cd90StockMapper extends CommBaseMapper<Cd90Stock> {

    /**
     * 逻辑删除指定工厂、数据来源、库存日期、班次的未删除记录（MES 同步前置清理）。
     *
     * @param factoryCode 工厂编码
     * @param dataSource  数据来源
     * @param stockDate   库存日期
     * @param shiftCode   班次
     * @param updateBy    更新人
     * @param updateTime  更新时间
     * @return 受影响行数
     */
    int logicDeleteByFactoryCodeAndDataSourceAndShiftCode(@Param("factoryCode") String factoryCode,
                                                          @Param("dataSource") String dataSource,
                                                          @Param("stockDate") Date stockDate,
                                                          @Param("shiftCode") String shiftCode,
                                                          @Param("updateBy") String updateBy,
                                                          @Param("updateTime") Date updateTime);
}