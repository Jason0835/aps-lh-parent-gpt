package com.zlt.aps.common.engine.mapper;

import com.zlt.aps.common.engine.domain.MonthSumProcessLog;

/**
 * @Entity com.zlt.aps.common.engine.domain.TMonthSumProcessLog
 */
public interface TMonthSumProcessLogMapper {

    int deleteByPrimaryKey(Long id);

    int insert(MonthSumProcessLog record);

    int insertSelective(MonthSumProcessLog record);

    MonthSumProcessLog selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(MonthSumProcessLog record);

    int updateByPrimaryKey(MonthSumProcessLog record);

}




