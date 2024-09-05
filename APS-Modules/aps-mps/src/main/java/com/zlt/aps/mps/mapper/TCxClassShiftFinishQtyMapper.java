package com.zlt.aps.mps.mapper;
import java.util.Date;
import com.zlt.aps.mps.domain.TCxClassShiftFinishQty;

import com.zlt.aps.mps.domain.*;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @Entity com.zlt.aps.mps.domain.TCxClassShiftFinishQty
 */
public interface TCxClassShiftFinishQtyMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TCxClassShiftFinishQty record);

    int insertSelective(TCxClassShiftFinishQty record);

    TCxClassShiftFinishQty selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TCxClassShiftFinishQty record);

    int updateByPrimaryKey(TCxClassShiftFinishQty record);

    List<TMesCxShiftFinishQty> getMesCxFinishByDataVersion(@Param("dataVersion") String dataVersion);

    List<TMesCxDayFinishQty> getMesCxDayFinishByDataVersion(@Param("dataVersion") String dataVersion);

    List<TMesLhShiftFinishQty> getMesLhFinishByDataVersion(@Param("dataVersion") String dataVersion);

    List<TMesLhDayFinishQty> getMesLhDayFinishByDataVersion(@Param("dataVersion") String dataVersion);

    public void mergeCxFinishSql(List<TCxClassShiftFinishQty> list);

    public void mergeCxDayFinishSql(List<TCxDayFinishQty> list);

    List<TCxClassShiftFinishQty> selectCxByScheduleDate(@Param("scheduleMonth") String scheduleMonth);

    List<TCxDayFinishQty> selectCxDayByFinishDate(@Param("finishDate") String finishDate);

    public void mergeLhFinishSql(List<TLhClassShiftFinishQty> list);

    public void mergeLhDayFinishSql(List<TLhDayFinishQty> list);

    List<TLhClassShiftFinishQty> selectLhByScheduleDate(@Param("scheduleMonth") String scheduleMonth);

    List<TLhDayFinishQty> selectLhDayByFinishDate(@Param("finishDate") String finishDate);
}




