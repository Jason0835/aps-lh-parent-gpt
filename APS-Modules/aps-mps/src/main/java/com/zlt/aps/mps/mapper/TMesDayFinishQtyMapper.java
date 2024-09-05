package com.zlt.aps.mps.mapper;
import java.util.List;

import com.zlt.aps.mps.domain.*;
import org.apache.ibatis.annotations.Param;

/**
 * @Entity com.zlt.aps.mps.domain.TMesTmDayFinishQty
 */
public interface TMesDayFinishQtyMapper {

    List<TMesTmDayFinishQty> getTmByDataVersionAndIsDelete(@Param("dataVersion") String dataVersion);
    List<TMesTcDayFinishQty> getTcByDataVersionAndIsDelete(@Param("dataVersion") String dataVersion);
    List<TMesNcDayFinishQty> getNcByDataVersionAndIsDelete(@Param("dataVersion") String dataVersion);
    List<TMesCd15DayFinishQty> getCd15ByDataVersionAndIsDelete(@Param("dataVersion") String dataVersion);
    List<TMesCd90DayFinishQty> getCd90ByDataVersionAndIsDelete(@Param("dataVersion") String dataVersion);
    List<TMesXwyyDayFinishQty> getXwyyByDataVersionAndIsDelete(@Param("dataVersion") String dataVersion);
    List<TMesTqDayFinishQty> getTqByDataVersionAndIsDelete(@Param("dataVersion") String dataVersion);
    List<TMesGsqDayFinishQty> getGsqByDataVersionAndIsDelete(@Param("dataVersion") String dataVersion);

    void mergeTmFinish(List<TTmDayFinishQty> list);
    void mergeTcFinish(List<TTcDayFinishQty> list);
    void mergeNcFinish(List<TNcDayFinishQty> list);
    void mergeCd15Finish(List<TCd15DayFinishQty> list);
    void mergeCd90Finish(List<TCd90DayFinishQty> list);
    void mergeXwyyFinish(List<TXwyyDayFinishQty> list);
    void mergeTqFinish(List<TTqDayFinishQty> list);
    void mergeGsqFinish(List<TGsqDayFinishQty> list);

    List<TTmDayFinishQty> selectTmByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<TTcDayFinishQty> selectTcByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<TNcDayFinishQty> selectNcByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<TCd15DayFinishQty> selectCd15ByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<TCd90DayFinishQty> selectCd90ByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<TXwyyDayFinishQty> selectXwyyByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<TTqDayFinishQty> selectTqByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<TGsqDayFinishQty> selectGsqByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
}




