package com.zlt.aps.tq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.dto.TqScheduleResultDto;
import com.zlt.aps.tq.entity.TqScheduleResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 胎圈排程结果Service接口
 *
 * @author chen
 * @date 2021-06-21
 */
public interface TqScheduleResultService extends IService<TqScheduleResult> {
    /**
     * 查询胎圈排程结果信息维护列表
     *
     * @param scheduleResult 胎圈排程结果信息维护
     * @return 胎圈排程结果信息维护集合
     */
    public List<TqScheduleResultDto> selectScheduleResultList(TqScheduleResult scheduleResult);

    /**
     * 查询胎圈排程结果信息维护列表
     *
     * @param id 要查询的id
     * @return 胎圈排程结果信息维护集合
     */
    public TqScheduleResultDto selectScheduleResultById(Long id);

    /**
     * 保存胎圈排程结果信息维护
     *
     * @param scheduleResult 胎圈排程结果信息维护
     */
    @Transactional
    void saveScheduleResult(TqScheduleResult scheduleResult);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    void insetDispatcherLog(String operType, TqScheduleResult newSchedule);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量、2--插单
     */
    void insetDispatcherLogInsertOrder(String operType, List<TqScheduleResult> scheduleResults, TqScheduleResult newSchedule);

    /**
     * 根据排程日期和代码查询排程结果
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    List<TqScheduleResult> selectByScheduleDateAndCode(TqScheduleResult scheduleResult);

    /**
     * 批量删除胎圈排程结果信息维护
     *
     * @param ids 需要删除的胎圈排程结果信息维护ID
     */
    @Transactional
    public void deleteScheduleResultByIds(long[] ids);

    /**
     * 导出excel表格
     *
     * @param list 要导出的数据集合
     * @return 字节数组
     */
    public byte[] export(List<TqScheduleResultDto> list);

    /**
     * 发布排程结果
     *
     * @param scheduleResult 排程日期
     * @param ids            要发布的排程结果id
     */
    @Transactional(rollbackFor = Exception.class)
    public void publish(TqScheduleResult scheduleResult, long[] ids, String dataVersion, String factoryCode, String companyCode);
    
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
    Boolean checkUnique(TqScheduleResult scheduleResult);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    @Transactional
    AjaxResult importData(List<TqScheduleResultDto> list, Long importLogId, Date scheduleDate);

    /**
     * 选机台
     */
    AjaxResult chooseMachine(TqScheduleResultDto scheduleResult);


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
    public int changeReleaseStatus(TqScheduleResult entity);

    int checkTqCodeExist(TqScheduleResultDto dto);

    int isPublishByIds(long[] ids);

    List<TqScheduleResultDto> selectByIds(List<Long> ids2);
}
