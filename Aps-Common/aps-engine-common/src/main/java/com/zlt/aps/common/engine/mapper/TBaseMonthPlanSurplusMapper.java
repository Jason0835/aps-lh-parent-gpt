package com.zlt.aps.common.engine.mapper;

import com.zlt.aps.common.engine.domain.MonthPlanSurplusBaseEntity;
import com.zlt.aps.common.engine.domain.TGdyyMonthPlanSurplus;
import com.zlt.aps.common.engine.domain.TXwyyMonthPlanSurplus;

import java.util.List;

import org.apache.ibatis.annotations.Param;


public interface TBaseMonthPlanSurplusMapper {

    <K extends MonthPlanSurplusBaseEntity> void mergeTm(List<K> list);
    <K extends MonthPlanSurplusBaseEntity> void mergeTc(List<K> list);
    <K extends MonthPlanSurplusBaseEntity> void mergeNc(List<K> list);
    <K extends MonthPlanSurplusBaseEntity> void mergeTq(List<K> list);
    <K extends MonthPlanSurplusBaseEntity> void mergeGsq(List<K> list);
    <K extends MonthPlanSurplusBaseEntity> void mergeCd15(List<K> list);
    <K extends MonthPlanSurplusBaseEntity> void mergeCd90(List<K> list);
     void mergeXwyy(List<TXwyyMonthPlanSurplus> list);

    void mergeGdyy(List<TGdyyMonthPlanSurplus> list);
    
	/**
	 * 通过年月获取月度计划版本号
	 * 
	 * @param year  年
	 * @param month 月
	 * @return 版本号
	 */
	String selectMonthPlanApsVersion(@Param("year") String year, @Param("month") String month);
}




