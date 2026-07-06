package com.zlt.aps.tm.mapper;

import com.zlt.aps.tm.api.domain.entity.TmStock;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

@Mapper
public interface TmStockMapper extends CommBaseMapper<TmStock> {

    /**
     * 根据库存日期逻辑删除胎面库存，只删当天数据，历史数据保留
     *
     * @param stockDate  库存日期
     * @param updateBy   更新者
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE T_TM_STOCK SET IS_DELETE = 1, UPDATE_BY = #{updateBy}, UPDATE_TIME = #{updateTime} "
            + "WHERE DATE(STOCK_DATE) = #{stockDate} AND IS_DELETE = 0")
    int logicDeleteByStockDate(@Param("stockDate") Date stockDate,
                               @Param("updateBy") String updateBy,
                               @Param("updateTime") Date updateTime);
}
