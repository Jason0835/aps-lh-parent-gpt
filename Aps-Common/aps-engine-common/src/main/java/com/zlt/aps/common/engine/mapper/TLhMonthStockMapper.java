package com.zlt.aps.common.engine.mapper;

import com.zlt.aps.common.engine.domain.TLhMonthStock;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.zlt.aps.common.engine.domain.TLhMonthStock
 */
public interface TLhMonthStockMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TLhMonthStock record);

    int insertSelective(TLhMonthStock record);

    TLhMonthStock selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TLhMonthStock record);

    int updateByPrimaryKey(TLhMonthStock record);

    List<TLhMonthStock> getByParams(TLhMonthStock entity);

    List<TLhMonthStock> selectBySapCodeAndMonth(@Param("list") List<String> list, @Param("stockMonth") String month);

    void mergeSql(List<TLhMonthStock> list);
}




