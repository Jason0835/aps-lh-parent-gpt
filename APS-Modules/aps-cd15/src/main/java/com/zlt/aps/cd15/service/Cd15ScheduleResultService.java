package com.zlt.aps.cd15.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;


/**
 * 15度裁断排程结果Service接口
 *
 * @author zlt
 * @date 2021-07-05
 */
public interface Cd15ScheduleResultService {
    /**
     * 查询15度裁断排程结果
     *
     * @param id 15度裁断排程结果ID
     * @return 15度裁断排程结果
     */
    public Cd15ScheduleResult selectCd15ScheduleResultById(Long id);
    public List<Cd15ScheduleResult> selectCd15ScheduleResultByIds(List<Long> ids);

    /**
     * 查询15度裁断排程结果列表
     *
     * @param cd15ScheduleResult 15度裁断排程结果
     * @return 15度裁断排程结果集合
     */
    public List<Cd15ScheduleResult> selectCd15ScheduleResultList(Cd15ScheduleResult cd15ScheduleResult);

    /**
     * 新增15度裁断排程结果
     *
     * @param cd15ScheduleResult 15度裁断排程结果
     * @return 结果
     */
    public int insertCd15ScheduleResult(Cd15ScheduleResult cd15ScheduleResult);

    /**
     * 修改15度裁断排程结果
     *
     * @param cd15ScheduleResult 15度裁断排程结果
     * @return 结果
     */
    public int updateCd15ScheduleResult(Cd15ScheduleResult cd15ScheduleResult);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    void insetDispatcherLog(String operType, Cd15ScheduleResult newSchedule);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量、2--插单
     */
    void insetDispatcherLogInsertOrder(String operType, List<Cd15ScheduleResult> scheduleResults, Cd15ScheduleResult newSchedule);

    /**
     * 修改15度裁断排程结果机台
     *
     * @param cd15ScheduleResult 15度裁断排程结果
     * @return 结果
     */
    @Transactional
    public void chooseMachine(Cd15ScheduleResult cd15ScheduleResult);

    /**
     * 批量删除15度裁断排程结果
     *
     * @param ids 需要删除的15度裁断排程结果ID
     * @return 结果
     */
    public int deleteCd15ScheduleResultByIds(Long[] ids);

    /**
     * 删除15度裁断排程结果信息
     *
     * @param id 15度裁断排程结果ID
     * @return 结果
     */
    public int deleteCd15ScheduleResultById(Long id);


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

    public List<Cd15ScheduleResult> checkScheduleResultUnique(Cd15ScheduleResult cd15ScheduleResult);

    /**
     * 查询排程日期是否已发布
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    Boolean isPublish(Date scheduleDate);

    /**
     * 导入数据
     */
    AjaxResult importData(List<Cd15ScheduleResult> list, Long importLogId, String scheduleDate);

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
    public int changeReleaseStatus(Cd15ScheduleResult entity);

    /**
     * 归并中夜班计划量，合并到同一个班次
     * @param ids id
     * @param classifiedShift 合并班次
     * @return 修改行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int combinationMiddleAndNight(Long[] ids, String classifiedShift);

    int checkCd15CodeExist(Cd15ScheduleResult cd15ScheduleResult);

    public int isPublishByIds(Long[] ids);

    /**
     * 根据排程日期和钢带代码查询排程结果
     * @param cd15ScheduleResult 排程日期、钢带代码
     * @return 查询到的数据
     */
    List<Cd15ScheduleResult> selectByScheduleDateAndBigRollCode(Cd15ScheduleResult cd15ScheduleResult);
}
