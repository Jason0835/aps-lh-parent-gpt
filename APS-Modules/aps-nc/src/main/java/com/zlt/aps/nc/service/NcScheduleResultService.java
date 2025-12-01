package com.zlt.aps.nc.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.nc.api.domain.entity.NcDayFinishQty;
import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;
import com.zlt.bill.common.service.IBillService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 内衬胶排程结果Service接口
 *
 * @author zlt
 * @date 2021-06-24
 */
public interface NcScheduleResultService extends IBillService<NcScheduleResult> {
    /**
     * 查询内衬排程结果
     *
     * @param id 内衬排程结果ID
     * @return 内衬排程结果
     */
    public NcScheduleResult selectNcScheduleResultById(Long id);

    /**
     * 查询内衬排程结果列表
     *
     * @param tcScheduleResult 内衬排程结果
     * @return 内衬排程结果集合
     */
    public List<NcScheduleResult> selectNcScheduleResultList(NcScheduleResult tcScheduleResult);

    /**
     * 新增内衬排程结果
     *
     * @param tcScheduleResult 内衬排程结果
     * @return 结果
     */
    public int insertNcScheduleResult(NcScheduleResult tcScheduleResult);

    /**
     * 修改内衬排程结果
     *
     * @param tcScheduleResult 内衬排程结果
     * @return 结果
     */
    public int updateNcScheduleResult(NcScheduleResult tcScheduleResult);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    void insetDispatcherLog(String operType, NcScheduleResult newSchedule);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量、2--插单
     */
    void insetDispatcherLogInsertOrder(String operType, List<NcScheduleResult> scheduleResults, NcScheduleResult newSchedule);

    /**
     * 根据排程日期和代码查询排程结果
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    List<NcScheduleResult> selectByScheduleDateAndCode(NcScheduleResult scheduleResult);

    /**
     * 批量删除内衬排程结果
     *
     * @param ids 需要删除的内衬排程结果ID
     * @return 结果
     */
    public int deleteNcScheduleResultByIds(Long[] ids);

    /**
     * 删除内衬排程结果信息
     *
     * @param id 内衬排程结果ID
     * @return 结果
     */
    public int deleteNcScheduleResultById(Long id);

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
    public List<NcScheduleResult> checkUnique(NcScheduleResult entity);

    /**
     * 导入数据
     */
    AjaxResult importData(List<NcScheduleResult> list, Long importLogId,String scheduleDate);

    /**
     * 选机台
     */
    public AjaxResult chooseMachine(NcScheduleResult scheduleResult);

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
    public int changeReleaseStatus(NcScheduleResult entity);

    /**
     * 归并中夜班计划量，合并到同一个班次
     * @param ids id
     * @param classifiedShift 合并班次
     * @return 修改行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int combinationMiddleAndNight(Long[] ids, String classifiedShift);

    int checkNcCodeExist(NcScheduleResult ncScheduleResult);

    int isPublishByIds(Long[] ids);

    List<NcScheduleResult> selectByIds(List<Long> ids2);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importFinishQty(List<NcDayFinishQty> list, Long importLogId);

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    AjaxResult getSummaryVo(NcScheduleResult scheduleResult);
}
