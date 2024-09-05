package com.zlt.aps.gsq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.dto.GsqScheduleResultDto;
import com.zlt.aps.gsq.entity.GsqScheduleResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 钢丝圈排程结果Service接口
 *
 * @author chen
 * @date 2021-06-21
 */
public interface GsqScheduleResultService extends IService<GsqScheduleResult> {
    /**
     * 查询钢丝圈排程结果信息维护列表
     *
     * @param scheduleResult 钢丝圈排程结果信息维护
     * @return 钢丝圈排程结果信息维护集合
     */
    public List<GsqScheduleResultDto> selectScheduleResultList(GsqScheduleResult scheduleResult);

    /**
     * 查询钢丝圈排程结果信息维护列表
     *
     * @param id 要查询的id
     * @return 钢丝圈排程结果信息维护集合
     */
    public GsqScheduleResultDto selectScheduleResultById(Long id);

    /**
     * 保存钢丝圈排程结果信息维护
     *
     * @param scheduleResult 钢丝圈排程结果信息维护
     */
    @Transactional
    void editScheduleResult(GsqScheduleResult scheduleResult);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    void insetDispatcherLog(String operType, GsqScheduleResult newSchedule);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量、2--插单
     */
    void insetDispatcherLogInsertOrder(String operType, List<GsqScheduleResult> scheduleResults, GsqScheduleResult newSchedule);

    /**
     * 根据排程日期和代码查询排程结果
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    List<GsqScheduleResult> selectByScheduleDateAndCode(GsqScheduleResult scheduleResult);

    /**
     * 保存钢丝圈排程结果选机台信息
     *
     * @param scheduleResult 钢丝圈排程结果信息
     */
    @Transactional
    void chooseMachine(GsqScheduleResult scheduleResult);

    /**
     * 插单
     * @param scheduleResult 排程结果
     */
    @Transactional
    void addScheduleResult(GsqScheduleResult scheduleResult);

    /**
     * 批量删除钢丝圈排程结果信息维护
     *
     * @param ids 需要删除的钢丝圈排程结果信息维护ID
     */
    @Transactional
    public void deleteScheduleResultByIds(long[] ids);

    /**
     * 导出excel表格
     *
     * @param list 要导出的数据集合
     * @return 字节数组
     */
    public byte[] export(List<GsqScheduleResultDto> list);

    /**
     * 发布排程结果
     *
     * @param ids 要发布的排程结果id
     */
    @Transactional(rollbackFor = Exception.class)
    public void publish(GsqScheduleResult scheduleResult, long[] ids, String dataVersion, String factoryCode, String companyCode);
    
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
     * 根据排程日期、物料编号、机台id校验唯一性
     * @param scheduleResult 要校验记录
     * @return 查询到的记录数
     */
    Boolean checkUnique(GsqScheduleResult scheduleResult);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GsqScheduleResultDto> list, Long importLogId, Date scheduleDate);

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
    public int isReleasingOrTimeoutByIds(long[] ids);

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public int changeReleaseStatus(GsqScheduleResult entity);

    int checkGsqCodeExist(GsqScheduleResult scheduleResult);

    int isPublishByIds(long[] ids);

    List<GsqScheduleResultDto> selectByIds(List<Long> ids2);
}
