package com.zlt.aps.common.engine.mapper;
import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.common.engine.domain.TCxPlanProductStatus;

/**
 * @Entity com.zlt.aps.common.engine.domain.TCxPlanProductStatus
 */
public interface TCxPlanProductStatusMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TCxPlanProductStatus record);

    int insertSelective(TCxPlanProductStatus record);

    TCxPlanProductStatus selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TCxPlanProductStatus record);

    int updateByPrimaryKey(TCxPlanProductStatus record);

    int deleteByApsVersion(@Param("monthPlanApsVersion") String monthPlanApsVersion);

    int insertBatch(@Param("tCxPlanProductStatusCollection") List<TCxPlanProductStatus> tCxPlanProductStatusCollection);

    List<TCxPlanProductStatus> selectAllByMonthPlanApsVersion(@Param("monthPlanApsVersion") String monthPlanApsVersion);

    TCxPlanProductStatus selectOneByMonthPlanApsVersionAndSapCodeAndEmbryoCode(@Param("monthPlanApsVersion") String monthPlanApsVersion,
                                                                                         @Param("sapCode") String sapCode,
                                                                                         @Param("embryoCode") String embryoCode);
}




