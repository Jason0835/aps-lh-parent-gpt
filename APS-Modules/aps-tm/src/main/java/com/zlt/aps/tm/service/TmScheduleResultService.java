package com.zlt.aps.tm.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.tm.api.domain.entity.TmDayFinishQty;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.bill.common.service.IBillService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 胎面排程结果Service接口
 *
 * @author zlt
 * @date 2021-06-17
 */
public interface TmScheduleResultService extends IBillService<TmScheduleResult> {
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
     * 新增胎面排程结果
     *
     * @param tmScheduleResult 胎面排程结果
     * @return 结果
     */
    public int insertTmScheduleResult(TmScheduleResult tmScheduleResult);

    int checkTmCodeExist(TmScheduleResult tmScheduleResult);

    /**
     * 修改胎面排程结果
     *
     * @param tmScheduleResult 胎面排程结果
     * @return 结果
     */
    public int updateTmScheduleResult(TmScheduleResult tmScheduleResult);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    void insetDispatcherLog(String operType, TmScheduleResult newSchedule);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量、2--插单
     */
    void insetDispatcherLogInsertOrder(String operType, List<TmScheduleResult> scheduleResults, TmScheduleResult newSchedule);

    /**
     * 根据排程日期和代码查询排程结果
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    List<TmScheduleResult> selectByScheduleDateAndCode(TmScheduleResult scheduleResult);

    /**
     * 批量删除胎面排程结果
     *
     * @param ids 需要删除的胎面排程结果ID
     * @return 结果
     */
    public int deleteTmScheduleResultByIds(Long[] ids);

    /**
     * 删除胎面排程结果信息
     *
     * @param id 胎面排程结果ID
     * @return 结果
     */
    public int deleteTmScheduleResultById(Long id);

    /**
     * 批量更新发布状态
     *
     * @param ids
     * @param status	发布状态
     */
    @Transactional
    public int batchUpdate(long[] ids, String status);


    /**
     * 查询排程日期是否已发布
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    Boolean isPublish(Date scheduleDate);

    /**
     * 唯一性校验
     */
    public List<TmScheduleResult> checkUnique(TmScheduleResult tmScheduleResult);

    /**
     * 导入数据，并保存记录
     */
    AjaxResult importData(List<TmScheduleResult> list, Long importLogId, String scheduleDate);

    /**
     * 排程发布
     */
    public AjaxResult publish(long[] ids, Date scheduleDate, String dataVersion, String factoryCode, String companyCode);

	/**
	 * 更新指定相关数据记录的发布状态
	 *
	 * @param dataVersion 数据版本
	 * @param ids         排程ID列表
	 * @param status      更新的状态
	 */
	void updateRelaseStatus(String dataVersion, long[] ids, String status);

    /**
     * 选机台
     */
    public AjaxResult chooseMachine(TmScheduleResult scheduleResult);

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
     * 更改发布状态
     * @param entity 排程日期
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public int changeReleaseStatus(TmScheduleResult entity);

    /**
     * 归并中夜班计划量，合并到同一个班次
     * @param ids id
     * @param classifiedShift 合并班次
     * @return 修改行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int combinationMiddleAndNight(Long[] ids, String classifiedShift);

    int isPublishByIds(Long[] ids);

    List<TmScheduleResult> selectByIds(List<Long> ids2);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importFinishQty(List<TmDayFinishQty> list, Long importLogId);

    /**
     * 查询出对应的施工信息字段
     *
     * @param embryoCodeList  施工代码
     * @param productionStage 仅投产阶段规格排产标识
     * @return 结果
     */
    List<EngineConstructionInfo> listConstruction(List<String> embryoCodeList, String productionStage);

    AjaxResult getSummaryVo(TmScheduleResult tmScheduleResult);

    /**
     * 批量转机台
     *
     * @param scheduleResult 排程结果
     * @return 结果
     */
    AjaxResult batchChangeMachine(TmScheduleResult scheduleResult);
}
