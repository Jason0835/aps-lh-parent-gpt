package com.zlt.aps.cd90.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90DayFinishQty;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;


/**
 * 90度裁断排程结果Service接口
 *
 * @author zlt
 * @date 2021-07-06
 */
public interface Cd90ScheduleResultService {
    /**
     * 查询90度裁断排程结果
     *
     * @param id 90度裁断排程结果ID
     * @return 90度裁断排程结果
     */
    public Cd90ScheduleResult selectCd90ScheduleResultById(Long id);

    /**
     * 查询90度裁断排程结果列表
     *
     * @param cd90ScheduleResult 90度裁断排程结果
     * @return 90度裁断排程结果集合
     */
    public List<Cd90ScheduleResult> selectCd90ScheduleResultList(Cd90ScheduleResult cd90ScheduleResult);

    /**
     * 新增90度裁断排程结果
     *
     * @param cd90ScheduleResult 90度裁断排程结果
     * @return 结果
     */
    public int insertCd90ScheduleResult(Cd90ScheduleResult cd90ScheduleResult);

    /**
     * 修改90度裁断排程结果
     *
     * @param cd90ScheduleResult 90度裁断排程结果
     * @return 结果
     */
    public int updateCd90ScheduleResult(Cd90ScheduleResult cd90ScheduleResult);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    void insetDispatcherLog(String operType, Cd90ScheduleResult newSchedule);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量、2--插单
     */
    void insetDispatcherLogInsertOrder(String operType, List<Cd90ScheduleResult> scheduleResults, Cd90ScheduleResult newSchedule);

    /**
     * 修改90度裁断排程结果机台
     *
     * @param scheduleResult 90度裁断排程结果
     * @return 结果
     */
    @Transactional
    public void chooseMachine(Cd90ScheduleResult scheduleResult);

    /**
     * 批量删除90度裁断排程结果
     *
     * @param ids 需要删除的90度裁断排程结果ID
     * @return 结果
     */
    public int deleteCd90ScheduleResultByIds(Long[] ids);

    /**
     * 删除90度裁断排程结果信息
     *
     * @param id 90度裁断排程结果ID
     * @return 结果
     */
    public int deleteCd90ScheduleResultById(Long id);

    @Transactional(rollbackFor = Exception.class)
    public int batchUpdate(long[] ids, Date scheduleDate, String dataVersion);
    
	/**
	 * 给mes发送排程下发通知
	 * 
	 * @param scheduleDate 排产日
	 * @param dataVersion  数据版本
	 * @param rowCount  同步记录数据
	 */
	void publishNoticeMes(Date scheduleDate, String dataVersion, int rowCount);
    
	/**
	 * 更新指定相关数据记录的发布状态
	 * 
	 * @param dataVersion 数据版本
	 * @param ids         排程ID列表
	 * @param status      更新的状态
	 */
	void updateRelaseStatus(String dataVersion, long[] ids, String status);

    public List<Cd90ScheduleResult> checkScheduleResultUnique(Cd90ScheduleResult cd90ScheduleResult);

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    Boolean isPublish(Date scheduleDate);

    /**
     * 导入数据
     */
    AjaxResult importData(List<Cd90ScheduleResult> list, Long importLogId, String scheduleDate);


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
    public int changeReleaseStatus(Cd90ScheduleResult entity);

    /**
     * 归并中夜班计划量，合并到同一个班次
     * @param ids id
     * @param classifiedShift 合并班次
     * @return 修改行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int combinationMiddleAndNight(Long[] ids, String classifiedShift);

    int checkCd90CodeExist(Cd90ScheduleResult cd90ScheduleResult);

    int isPublishByIds(Long[] ids);

    List<Cd90ScheduleResult> selectByIds(List<Long> ids2);

    /**
     * 根据排程日期和代码查询排程结果
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    List<Cd90ScheduleResult> selectByScheduleDateAndCode(Cd90ScheduleResult scheduleResult);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importFinishQty(List<Cd90DayFinishQty> list, Long importLogId);

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    AjaxResult getSummaryVo(Cd90ScheduleResult scheduleResult);
}
