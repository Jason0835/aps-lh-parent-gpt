package com.zlt.aps.mps.mapper;

import com.zlt.aps.mps.domain.TSapStock;

import java.util.List;

/**
 * @Entity com.zlt.aps.mps.domain.TSapStock
 */
public interface TSapStockMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TSapStock record);

    int insertSelective(TSapStock record);

    TSapStock selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TSapStock record);

    int updateByPrimaryKey(TSapStock record);

    void mergeSql(List<TSapStock> list);

}




