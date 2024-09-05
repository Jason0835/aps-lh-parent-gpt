package com.zlt.aps.tc.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 胎侧排程结果Service接口
 *
 * @author zlt
 * @date 2021-06-21
 */
public interface TcScheduleResultService {
    /**
     * 查询胎侧排程结果
     *
     * @param id 胎侧排程结果ID
     * @return 胎侧排程结果
     */
    public TcScheduleResult selectTcScheduleResultById(Long id);

    /**
     * 查询胎侧排程结果列表
     *
     * @param tcScheduleResult 胎侧排程结果
     * @return 胎侧排程结果集合
     */
    public List<TcScheduleResult> selectTcScheduleResultList(TcScheduleResult tcScheduleResult);

    /**
     * 新增胎侧排程结果
     *
     * @param tcScheduleResult 胎侧排程结果
     * @return 结果
     */
    public int insertTcScheduleResult(TcScheduleResult tcScheduleResult);

    /**
     * 修改胎侧排程结果
     *
     * @param tcScheduleResult 胎侧排程结果
     * @return 结果
     */
    public int updateTcScheduleResult(TcScheduleResult tcScheduleResult);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    void insetDispatcherLog(String operType, TcScheduleResult newSchedule);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量、2--插单
     */
    void insetDispatcherLogInsertOrder(String operType, List<TcScheduleResult> scheduleResults, TcScheduleResult newSchedule);

    /**
     * 根据排程日期和代码查询排程结果
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    List<TcScheduleResult> selectByScheduleDateAndCode(TcScheduleResult scheduleResult);

    /**
     * 批量删除胎侧排程结果
     *
     * @param ids 需要删除的胎侧排程结果ID
     * @return 结果
     */
    public int deleteTcScheduleResultByIds(Long[] ids);

    /**
     * 删除胎侧排程结果信息
     *
     * @param id 胎侧排程结果ID
     * @return 结果
     */
    public int deleteTcScheduleResultById(Long id);

    /**
     * 批量更新发布状态
     *
     * @param ids
     * @param status	发布状态
     */
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
    public List<TcScheduleResult> checkUnique(TcScheduleResult entity);

    /**
     * 导入数据，并保存记录
     */
    AjaxResult importData(List<TcScheduleResult> list, Long importLogId, String scheduleDate);

    /**
     * 排程发布
     */
    public AjaxResult publish(long[] ids,Date scheduleDate, String dataVersion, String factoryCode, String companyCode);
    
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
    public AjaxResult chooseMachine(TcScheduleResult scheduleResult);

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
    public int changeReleaseStatus(TcScheduleResult entity);

    /**
     * 归并中夜班计划量，合并到同一个班次
     * @param ids id
     * @param classifiedShift 合并班次
     * @return 修改行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int combinationMiddleAndNight(Long[] ids, String classifiedShift);

    int checkTcCodeExist(TcScheduleResult tcScheduleResult);

    int isPublishByIds(Long[] ids);

    List<TcScheduleResult> selectByIds(List<Long> ids2);
}
