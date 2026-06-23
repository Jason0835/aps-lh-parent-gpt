package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.entity.TqStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface TqStockMapper extends BaseMapper<TqStock> {

    List<TqStock> checkStockListUnic(@Param("entity") TqStock stock);

    void mergeSql(@Param("list") List<TqStock> list);

    /**
     * 根据库存日期逻辑删除胎圈库存
     * 只删除指定库存日期的数据，历史数据保留
     *
     * @param stockDate  库存日期
     * @param updateBy   更新者
     * @param updateTime 更新时间
     * @return 更新的记录数
     */
    int logicDeleteByStockDate(@Param("stockDate") Date stockDate,
                               @Param("updateBy") String updateBy,
                               @Param("updateTime") Date updateTime);
}

