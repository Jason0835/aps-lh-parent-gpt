package com.zlt.aps.dj.service;

import java.util.Date;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.dj.api.domain.entity.DjDayFinishQty;
import com.zlt.aps.dj.api.domain.entity.DjScheduleResult;
import com.zlt.bill.common.service.IBillService;

/**
 * 垫胶胶排程结果Service接口
 *
 * @author zlt
 * @date 2026-06-13
 */
public interface DjScheduleResultService extends IBillService<DjScheduleResult> {
    /**
     * 查询垫胶排程结果
     *
     * @param id 垫胶排程结果ID
     * @return 垫胶排程结果
     */
    public DjScheduleResult selectDjScheduleResultById(Long id);

    /**
     * 查询垫胶排程结果列表
     *
     * @param djScheduleResult 垫胶排程结果
     * @return 垫胶排程结果集合
     */
    public List<DjScheduleResult> selectDjScheduleResultList(DjScheduleResult djScheduleResult);

    /**
     * 新增垫胶排程结果
     *
     * @param djScheduleResult 垫胶排程结果
     * @return 结果
     */
    public int insertDjScheduleResult(DjScheduleResult djScheduleResult);

    /**
     * 修改垫胶排程结果
     *
     * @param djScheduleResult 垫胶排程结果
     * @return 结果
     */
    public int updateDjScheduleResult(DjScheduleResult djScheduleResult);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    void insertDispatcherLog(String operType, DjScheduleResult newSchedule);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量、2--插单
     */
    void insertDispatcherLogInsertOrder(String operType, List<DjScheduleResult> scheduleResults, DjScheduleResult newSchedule);

    /**
     * 根据排程日期和代码查询排程结果
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    List<DjScheduleResult> selectByScheduleDateAndCode(DjScheduleResult scheduleResult);

    /**
     * 批量删除垫胶排程结果
     *
     * @param ids 需要删除的垫胶排程结果ID
     * @return 结果
     */
    public int deleteDjScheduleResultByIds(Long[] ids);

    /**
     * 删除垫胶排程结果信息
     *
     * @param id 垫胶排程结果ID
     * @return 结果
     */
    public int deleteDjScheduleResultById(Long id);

    /**
     * 批量更新发布状态
     *
     * @param ids
     */
    public int batchUpdate(long[] ids, Date scheduleDate, String dataVersion, String factoryCode, String companyCode);

	/**
	 * 更新指定相关数据记录的发布状态
	 *
	 * @param dataVersion 数据版本
	 * @param ids         排程ID列表
	 * @param status      更新的状态
	 */
	void updateRelaseStatus(String dataVersion, long[] ids, String status);

    /**
     * 查询排程日期是否已发布
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    Boolean isPublish(Date scheduleDate);

    /**
     * 唯一性校验
     */
    public List<DjScheduleResult> checkUnique(DjScheduleResult entity);

    /**
     * 导入数据
     */
    AjaxResult importData(List<DjScheduleResult> list, Long importLogId,String scheduleDate);

    /**
     * 选机台
     */
    public AjaxResult chooseMachine(DjScheduleResult scheduleResult);

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
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public int changeReleaseStatus(DjScheduleResult entity);

    /**
     * 归并中夜班计划量，合并到同一个班次
     * @param ids id
     * @param classifiedShift 合并班次
     * @return 修改行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int combinationMiddleAndNight(Long[] ids, String classifiedShift);

    int checkDjCodeExist(DjScheduleResult djScheduleResult);

    int isPublishByIds(Long[] ids);

    List<DjScheduleResult> selectByIds(List<Long> ids2);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importFinishQty(List<DjDayFinishQty> list, Long importLogId);

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    AjaxResult getSummaryVo(DjScheduleResult scheduleResult);

    /**
     * 填充 T-1 日早班数据（前日排产结果中 class3 相关字段）
     *
     * @param list 当前排程结果列表
     * @param scheduleDate 排程日期
     */
    void fillPrevDayClass3Plan(List<DjScheduleResult> list, Date scheduleDate);
}