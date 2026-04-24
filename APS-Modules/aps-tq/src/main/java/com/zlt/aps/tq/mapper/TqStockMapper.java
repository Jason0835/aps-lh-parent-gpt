package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.entity.TqStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TqStockMapper extends BaseMapper<TqStock> {

    List<TqStock> checkStockListUnic(@Param("entity") TqStock stock);

    void mergeSql(@Param("list") List<TqStock> list);
}
