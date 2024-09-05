package com.zlt.aps.mps.mapper;
import java.util.Collection;
import java.util.Date;
import org.apache.ibatis.annotations.Param;

import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.mps.domain.MonthSurplusStatusVo;
import com.zlt.aps.mps.domain.TMesXwyyDayFinishQty;

import java.util.List;

/**
 * 成型排程结果Mapper接口
 *
 * @author zlt
 * @date 2021-07-12
 */
public interface MpsScheduleResultMapper {


    CxScheduleResult selectOneByOrderNoAndDelFlag(@Param("orderNo") String orderNo);

    int updateProductionStatusByOrderNoIn(@Param("orderNoList") List<String> orderNoList);

    int updateTaskTypeBySapCodeAndLhMachineCodeAndScheduleDate(@Param("sapCode") String sapCode, @Param("lhMachineCode") String lhMachineCode, @Param("scheduleDate") String scheduleDate);

    int updateLhProductionStatusByOrderNoIn(@Param("orderNoList") List<String> orderNoList);
    int updateTmProductionStatusByOrderNo(@Param("statusList") List<MonthSurplusStatusVo> statusList);
    int updateTcProductionStatusByOrderNo(@Param("statusList") List<MonthSurplusStatusVo> statusList);
    int updateNcProductionStatusByOrderNo(@Param("statusList") List<MonthSurplusStatusVo> statusList);
    int updateTqProductionStatusByOrderNo(@Param("statusList") List<MonthSurplusStatusVo> statusList);
    int updateGsqProductionStatusByOrderNo(@Param("statusList") List<MonthSurplusStatusVo> statusList);
    int updateCd15ProductionStatusByOrderNo(@Param("statusList") List<MonthSurplusStatusVo> statusList);
    int updateCd90ProductionStatusByOrderNo(@Param("statusList") List<MonthSurplusStatusVo> statusList);
    int updateXwyyProductionStatusByOrderNo(@Param("statusList") List<MonthSurplusStatusVo> statusList);

}
