package com.zlt.aps.common.engine.mapper;
import java.util.List;

import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import org.apache.ibatis.annotations.Param;

import com.zlt.aps.common.engine.domain.TSyncMps2ApsFac;

/**
 * @Entity com.zlt.aps.common.engine.domain.TSyncMps2ApsFac
 */
public interface TSyncMps2ApsFacMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TSyncMps2ApsFac record);

    int insertSelective(TSyncMps2ApsFac record);

    TSyncMps2ApsFac selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TSyncMps2ApsFac record);

    int updateByPrimaryKey(TSyncMps2ApsFac record);

    TSyncMps2ApsFac selectOneByYearAndMonthAndIsDelete(@Param("year") Integer year, @Param("month") Integer month);

    TSyncMps2ApsFac selectOneByYearAndMonthAndProductionVersion(@Param("year") Integer year, @Param("month") Integer month, @Param("productionVersion") String productionVersion);

    List<TSyncMps2ApsFac> selectAllByYearAndMonthAndProductionVersion(@Param("year") Integer year, @Param("month") Integer month, @Param("productionVersion") String productionVersion);

    List<TSyncMps2ApsFac> selectAllByProductionVersionAndIsDelete(@Param("productionVersion") String productionVersion);

    List<CxMachineInfo> selectCxMachineInfoList();
}




