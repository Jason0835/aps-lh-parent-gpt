package com.zlt.aps.lh.engine.mapper;

import com.zlt.aps.cx.api.domain.dto.CxParamsDto;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.lh.engine.domain.LhSapEmbryoTime;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通用Mapper接口
 */
public interface CommonCxEngineMapper {
    /**
     * 查询成型排程结果表数据
     * @param cxScheduleResult
     * @return
     */
    List<CxScheduleResult> selectCxScheduleResultList(CxScheduleResult cxScheduleResult);

    /**
     * 根据排程日期获取对应的抓取记录中的成型批次号
     * @param scheduleDate
     * @return
     */
    String selectCxBatchNoByScheduleDate(@Param("scheduleDate") String scheduleDate);

    /**
     * 成型库存列表
     * @param cxStock
     * @return
     */
    List<CxStock> selectCxStockList(CxStock cxStock);

    /**
     * 相同胎胚进行合并
     * @param cxStock
     * @return
     */
    List<CxStock> selectMergeCxStockList(CxStock cxStock);

    /**
     * 查询成型参数集合
     * @return
     */
    List<CxParamsDto> selectCxParamsList(CxParamsDto cxParamsDto);

    /**
     * 根据日期查询排程日期对应的成型所有任务开始时间结束时间
     * @param scheduleDate
     * @return
     */
    List<LhSapEmbryoTime> selectEmbryoTimeList(@Param("scheduleDate") String scheduleDate);
}
