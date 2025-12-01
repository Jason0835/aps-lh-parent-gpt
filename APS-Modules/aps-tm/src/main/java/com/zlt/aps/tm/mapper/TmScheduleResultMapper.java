package com.zlt.aps.tm.mapper;

import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.common.engine.domain.ScheduleSummaryVo;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 胎面排程结果Mapper接口
 *
 * @author zlt
 * @date 2021-06-17
 */
public interface TmScheduleResultMapper extends CommBaseMapper<TmScheduleResult> {
    /**
     * 查询胎面排程结果
     *
     * @param id 胎面排程结果ID
     * @return 胎面排程结果
     */
    public TmScheduleResult selectTmScheduleResultById(Long id);

    /**
     * 查询胎面排程结果列表
     *
     * @param tmScheduleResult 胎面排程结果
     * @return 胎面排程结果集合
     */
    public List<TmScheduleResult> selectTmScheduleResultList(TmScheduleResult tmScheduleResult);

    /**
     * 唯一性校验
     */
    public List<TmScheduleResult> checkUnique(TmScheduleResult tmScheduleResult);

    /**
     * 新增胎面排程结果
     *
     * @param tmScheduleResult 胎面排程结果
     * @return 结果
     */
    public int insertTmScheduleResult(TmScheduleResult tmScheduleResult);

    /**
     * 修改胎面排程结果
     *
     * @param tmScheduleResult 胎面排程结果
     * @return 结果
     */
    public int updateTmScheduleResult(TmScheduleResult tmScheduleResult);

    /**
     * 删除胎面排程结果
     *
     * @param id 胎面排程结果ID
     * @return 结果
     */
    public int deleteTmScheduleResultById(Long id);

    /**
     * 批量删除胎面排程结果
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTmScheduleResultByIds(Long[] ids);

    /**
     * 批量更新发布状态
     *
     * @param ids
     */
    public int batchUpdate(@Param("list") List<Long> ids, @Param("status") String status);

    /**
     * 保存发布日志
     * @param schedulePublishRecord 要保存的发布日志
     * @return 结果
     */
    public int insertPublishRecord(SchedulePublishRecord schedulePublishRecord);

    /**
     * 查询指定日期的排程结果是否已经发布
     * @param schedulePublishRecord 要查询的日期及工序参数
     * @return 查询到的记录条数
     */
    public int isPublish(SchedulePublishRecord schedulePublishRecord);

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    public int isReleasingOrTimeoutByDate(Date scheduleDate);

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param ids id
     * @return 查询到的记录数
     */
    public int isReleasingOrTimeoutByIds(Long[] ids);

    /**
     * 把排程数据发布到中间库
     * @param dataVersion 接口发布版本号
     * @param ids  排程发布的ids
	 * @param factoryCode 厂别
	 * @param companyCode 分公司编号
     */
    public void deployScheduleToMes(@Param("dataVersion") String dataVersion, @Param("ids") long[] ids,
			@Param("factoryCode") String factoryCode, @Param("companyCode") String companyCode);

    /**
     * 更改发布状态
     * @param entity 排程日期
     * @return 结果
     */
    public int changeReleaseStatus(TmScheduleResult entity);

    /**
     * 更新发布记录发布状态
     * @param schedulePublishRecord 发布记录
     * @return 影响行数
     */
    public int updatePublishRecord(SchedulePublishRecord schedulePublishRecord);

	/**
	 * 更新发布日志状态
	 *
	 * @param dataVersion 数据版本
	 * @param status      状态
	 */
	public int updatePublishRecordVersion(@Param("dataVersion") String dataVersion, @Param("status") String status);

    /**
     * 归并中夜班计划量，合并到同一个班次
     * @param map 要合并的id及合并的班次(type = 1合并到中班，2 合并到夜班)
     * @return 修改行数
     */
	public int combinationMiddleAndNight(Map<String, Object> map);

    int checkTmCodeExist(TmScheduleResult tmScheduleResult);

    /**
     * 根据id查询未发布记录的条数
     * @param ids id
     * @return 未发布的记录条数
     */
    public int isPublishByIds(Long[] ids);

    List<TmScheduleResult> selectByIds(@Param("list") List<Long> ids2);

    /**
     * 根据排程日期和帘布代码查询记录
     * @return 查询到的记录
     */
    List<TmScheduleResult> selectByScheduleDateAndCode(TmScheduleResult scheduleResult);

    /**
     * 查询出对应的施工信息字段
     *
     * @param embryoCodeList  施工代码
     * @param productionStage 仅投产阶段规格排产标识
     * @return 结果
     */
    List<EngineConstructionInfo> listConstruction(@Param("embryoCodeList") List<String> embryoCodeList, @Param("productionStage") String productionStage);

    /**
     * 获取排程结果统计信息
     *
     * @param tmScheduleResult 排程日期
     * @return 结果
     */
    ScheduleSummaryVo getSummaryVo(TmScheduleResult tmScheduleResult);

    /**
     * 获取昨日早班计划量
     *
     * @param tmScheduleResult 排程日期
     * @return 结果
     */
    ScheduleSummaryVo getLastDayPlanQty(TmScheduleResult tmScheduleResult);

    /**
     * 获取昨日早班计划量-具体到每个规格
     *
     * @param tmScheduleResult 排程日期
     * @return 结果
     */
    List<TmScheduleResult> getLastDayPlanQty4List(TmScheduleResult tmScheduleResult);

    /**
     * 获取成型消耗量
     *
     * @param tmScheduleResult 排程日期
     * @return 结果
     */
    ScheduleSummaryVo getCxConsume(TmScheduleResult tmScheduleResult);

    /**
     * 获取成型消耗量-具体到每个规格
     *
     * @param tmScheduleResult 排程日期
     * @return 结果
     */
    List<TmScheduleResult> getCxConsume4List(TmScheduleResult tmScheduleResult);

    /**
     * 根据原机台id和班次计划量查询排程结果
     *
     * @param scheduleResult 机台ID、班次
     * @return 排程结果
     */
    List<TmScheduleResult> selectBySourceMachineIdAndShiftPlanQty(TmScheduleResult scheduleResult);

    /**
     * 根据目标机台id查询排程结果
     *
     * @param scheduleResult 目标机台ID、排程日期
     * @return 排程结果
     */
    List<TmScheduleResult> selectByTargetMachineIdAndShiftPlanQty(TmScheduleResult scheduleResult);
}
