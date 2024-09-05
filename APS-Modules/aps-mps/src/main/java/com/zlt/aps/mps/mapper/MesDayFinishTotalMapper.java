package com.zlt.aps.mps.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.mps.domain.ClassFinishQty;
import com.zlt.aps.mps.domain.TCd15DayFinishQty;
import com.zlt.aps.mps.domain.TCd90DayFinishQty;
import com.zlt.aps.mps.domain.TGsqDayFinishQty;
import com.zlt.aps.mps.domain.TNcDayFinishQty;
import com.zlt.aps.mps.domain.TTcDayFinishQty;
import com.zlt.aps.mps.domain.TTmDayFinishQty;
import com.zlt.aps.mps.domain.TTqDayFinishQty;
import com.zlt.aps.mps.domain.TXwyyDayFinishQty;

public interface MesDayFinishTotalMapper {
    /** 半部件工序的日完成量中间库数据同步 **/
    List<TTmDayFinishQty> getTmDayFinishByDataVersion(@Param("dataVersion") String dataVersion);
    List<TTcDayFinishQty> getTcDayFinishByDataVersion(@Param("dataVersion") String dataVersion);
    List<TNcDayFinishQty> getNcDayFinishByDataVersion(@Param("dataVersion") String dataVersion);
    List<TCd15DayFinishQty> getCd15DayFinishByDataVersion(@Param("dataVersion") String dataVersion);
    List<TCd90DayFinishQty> getCd90DayFinishByDataVersion(@Param("dataVersion") String dataVersion);
    List<TXwyyDayFinishQty> getXwyyDayFinishByDataVersion(@Param("dataVersion") String dataVersion);
    List<TTqDayFinishQty> getTqDayFinishByDataVersion(@Param("dataVersion") String dataVersion);
    List<TGsqDayFinishQty> getGsqDayFinishByDataVersion(@Param("dataVersion") String dataVersion);
    
    /** 半部件工序的日完成量中间表数据同步至业务表 **/
    void mergeTmDayFinish(@Param("dataVersion") String dataVersion);
    void mergeTcDayFinish(@Param("dataVersion") String dataVersion);
    void mergeNcDayFinish(@Param("dataVersion") String dataVersion);
    void mergeCd15DayFinish(@Param("dataVersion") String dataVersion);
    void mergeCd90DayFinish(@Param("dataVersion") String dataVersion);
    void mergeXwyyDayFinish(@Param("dataVersion") String dataVersion);
    void mergeTqDayFinish(@Param("dataVersion") String dataVersion);
    void mergeGsqDayFinish(@Param("dataVersion") String dataVersion);
    
    /** 半部件工序的日完成量中间库数据同步 **/
    List<TTmDayFinishQty> selectTmByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<TTcDayFinishQty> selectTcByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<TNcDayFinishQty> selectNcByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<TCd15DayFinishQty> selectCd15ByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<TCd90DayFinishQty> selectCd90ByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<TXwyyDayFinishQty> selectXwyyByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<TTqDayFinishQty> selectTqByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<TGsqDayFinishQty> selectGsqByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    
    /** 查询各工序班次完成量 **/
    List<ClassFinishQty> listMesClassFinishQty(@Param("dataVersion") String dataVersion);
    /** 合并各工序班次完成量 **/
    int mergeClassFinishQty(@Param("list") List<ClassFinishQty> mesList);
}
