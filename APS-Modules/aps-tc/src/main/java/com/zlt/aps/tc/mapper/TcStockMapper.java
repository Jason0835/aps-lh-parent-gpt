package com.zlt.aps.tc.mapper;

import com.zlt.aps.tc.api.domain.entity.TcStock;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

@Mapper
public interface TcStockMapper extends CommBaseMapper<TcStock> {

    /**
     * 根据库存日期逻辑删除胎侧库存，只删当天数据，历史数据保留
     *
     * @param stockDate  库存日期
     * @param updateBy   更新者
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE T_TC_STOCK SET IS_DELETE = 1, UPDATE_BY = #{updateBy}, UPDATE_TIME = #{updateTime} "
            + "WHERE DATE(STOCK_DATE) = #{stockDate} AND IS_DELETE = 0")
    int logicDeleteByStockDate(@Param("stockDate") Date stockDate,
                               @Param("updateBy") String updateBy,
                               @Param("updateTime") Date updateTime);
}