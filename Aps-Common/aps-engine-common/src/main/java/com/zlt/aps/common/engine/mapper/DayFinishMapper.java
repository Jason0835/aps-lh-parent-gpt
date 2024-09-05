package com.zlt.aps.common.engine.mapper;

import com.zlt.aps.common.engine.domain.DayFinishVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Gim
 */
public interface DayFinishMapper {

    // 成型日完成量
    List<DayFinishVo> selectCxDayByFinishDate(@Param("finishDate") String finishDate);
    // 硫化完成量
    List<DayFinishVo> selectLhDayByFinishDate(@Param("finishDate") String finishDate);

    /** 半部件工序的日完成量中间库数据同步 **/
    List<DayFinishVo> selectTmByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<DayFinishVo> selectTcByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<DayFinishVo> selectNcByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<DayFinishVo> selectCd15ByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<DayFinishVo> selectCd90ByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<DayFinishVo> selectXwyyByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<DayFinishVo> selectTqByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
    List<DayFinishVo> selectGsqByScheduleDate(@Param("scheduleMonth") String scheduleMonth);
}
