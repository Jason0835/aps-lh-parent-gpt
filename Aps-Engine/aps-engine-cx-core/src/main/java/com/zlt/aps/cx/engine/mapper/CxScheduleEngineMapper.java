package com.zlt.aps.cx.engine.mapper;

import com.zlt.aps.cx.api.domain.dto.CxProductConstructionInfoDto;
import com.zlt.aps.cx.api.domain.entity.CxScheduleStopInfo;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleResult;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
  *  成型排程引擎获取排程结果数据mapper
  * @ClassName CxScheduleEngineMapper
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/22 18:22
  * @Version 1.0
**/
public interface CxScheduleEngineMapper {

    /**
     * 根据ID进行排程结果数据查询
     * @param id
     * @return
     */
    CxEngineScheduleResult selectCxEngineScheduleResultById(Long id);

    /**
     * 加载成型排程结果表数据
     * @param cxEngineScheduleResult
     * @return
     */
    List<CxEngineScheduleResult> selectCxScheduleResultList(CxEngineScheduleResult cxEngineScheduleResult);

    /**
     * 加载成型排程结果，不过滤收尾规格
     * @param cxEngineScheduleResult
     * @return
     */
    List<CxEngineScheduleResult> selectCxScheduleResultWithCloseOutList(CxEngineScheduleResult cxEngineScheduleResult);

    /**
     * 批量插入成型排程结果表数据
     * @param cxEngineScheduleResultList
     * @return
     */
    int batchInsertCxScheduleResult(@Param("scheduleResultList") List<CxEngineScheduleResult> cxEngineScheduleResultList);

    /**
     * 根据排程日期进行排程结果删除
     * @param scheduleDate
     * @return
     */
    int deleteCxScheduleResultByScheduleDate(@Param("scheduleDate") String scheduleDate);

    /**
     * 根据排程日期更新排程结果批次号
     * @param scheduleDate
     * @return
     */
    int updateCxScheduleResultBatchNoByScheduleDate(@Param("scheduleDate") String scheduleDate,@Param("cxBatchNo") String cxBatchNo);

    /**
     * 批量更新为已收尾
     * @param closeOutList
     * @return
     */
    int updateProductStatusToCloseOut(@Param("list") List<CxEngineScheduleResult> closeOutList);

    /**
     * 插单保存成型排程结果数据
     * @param cxEngineScheduleResult
     * @return
     */
    int insertCxScheduleResult(CxEngineScheduleResult cxEngineScheduleResult);

    /**
     * 根据SAP+胎胚代码+排程日期汇总已排的计划总量
     * @param cxEngineScheduleResult
     * @return
     */
    Integer selectSchedulePlanQtyByCondition(CxEngineScheduleResult cxEngineScheduleResult);

    /**
     * 根据条件限定查询排程结果是否已经存在
     * @param condition
     * @return
     */
    CxEngineScheduleResult selectScheduleResult(CxEngineScheduleResult condition);

    /**
     * 更新各个班次的计划量
     * @param scheduleResult
     * @return
     */
    int updateScheduleResultPlanQty(CxEngineScheduleResult scheduleResult);

    /**
     * 转机台更新结果表成型机台
     * @param cxEngineScheduleResult
     */
    void updateScheduleCxMachine(CxEngineScheduleResult cxEngineScheduleResult);

    /**
     * 删除指定日期的排程数据
     * @param scheduleDate
     */
    void deleteCxSchedule(@Param("scheduleDate") String scheduleDate, @Param("cxMachineCode") String cxMachineCode);

    /**
     * 把排程数据同步到log表
     * @param scheduleDate
     */
    void syncCxScheduleToLog(@Param("scheduleDate") String scheduleDate, @Param("cxMachineCode") String cxMachineCode);

    /**
     * 批量更新可硫化班次
     * @param updateList
     * @return
     */
    int updateAvailableBatch(@Param("list") List<CxEngineScheduleResult> updateList);

    /**
     * 加载成型状态收尾，硫化还未收尾的数据
     * @param cxEngineScheduleResult
     * @return
     */
    List<CxEngineScheduleResult> selectCxCloseOutScheduleResultList(CxEngineScheduleResult cxEngineScheduleResult);

    /**
     * 批量更新单班硫化量和留存单班硫化量栏位
     * @param updateList
     * @return
     */
    int updateSingleLhQtyBatch(@Param("list") List<CxEngineScheduleResult> updateList);

    /**
     * 自动排程时停排信息记录
     * @param cxScheduleStopInfoList
     * @return
     */
    int batchInsertScheduleStopInfo(@Param("stopList") List<CxScheduleStopInfo> cxScheduleStopInfoList);

    /**
     * 删除指定日期的停排数据
     * @param scheduleDate
     */
    void deleteScheduleStopInfoByScheduleDate(@Param("scheduleDate") String scheduleDate, @Param("cxMachineCode") String cxMachineCode);

	/**
	 * 查询多版本施工版本信息，如果7天内有成型排产记录，需要打标记
	 * 
	 * @param scheduleDate   排产日
	 * @param embryoCodeList 胎胚号
	 * @return
	 */
	List<CxProductConstructionInfoDto> listMultipleVersionConstruction(@Param("scheduleDate") Date scheduleDate,
			@Param("embryoCodeList") List<String> embryoCodeList);
}
