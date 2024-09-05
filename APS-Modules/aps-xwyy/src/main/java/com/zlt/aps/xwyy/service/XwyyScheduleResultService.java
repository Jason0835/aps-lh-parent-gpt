package com.zlt.aps.xwyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.xwyy.api.domain.dto.XwyyScheduleResultDto;
import com.zlt.aps.xwyy.entity.XwyyScheduleResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


/**
 * 纤维压延排程结果Service接口
 *
 * @author chen
 * @date 2021-07-06
 */
public interface XwyyScheduleResultService extends IService<XwyyScheduleResult> {
    /**
     * 查询纤维压延排程结果信息维护列表
     *
     * @param scheduleResult 纤维压延排程结果信息维护
     * @return 纤维压延排程结果信息维护集合
     */
    public List<XwyyScheduleResultDto> selectScheduleResultList(XwyyScheduleResult scheduleResult);

    /**
     * 查询纤维压延排程结果信息维护列表
     *
     * @param id 要查询的id
     * @return 纤维压延排程结果信息维护集合
     */
    public XwyyScheduleResultDto selectScheduleResultById(Long id);

    /**
     * 保存纤维压延排程结果信息维护
     *
     * @param scheduleResult 纤维压延排程结果信息维护
     */
    @Transactional
    void saveScheduleResult(XwyyScheduleResult scheduleResult);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    void insetDispatcherLog(String operType, XwyyScheduleResult newSchedule);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量、2--插单
     */
    void insetDispatcherLogInsertOrder(String operType, List<XwyyScheduleResult> scheduleResults, XwyyScheduleResult newSchedule);

    /**
     * 根据排程日期和代码查询排程结果
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    List<XwyyScheduleResult> selectByScheduleDateAndCode(XwyyScheduleResult scheduleResult);

    /**
     * 保存钢丝圈排程结果选机台信息
     *
     * @param scheduleResult 钢丝圈排程结果信息
     */
    @Transactional
    void chooseMachine(XwyyScheduleResult scheduleResult);

    /**
     * 批量删除纤维压延排程结果信息维护
     *
     * @param ids 需要删除的纤维压延排程结果信息维护ID
     */
    @Transactional
    public void deleteScheduleResultByIds(Long[] ids, List<XwyyScheduleResult> list);

    /**
     * 导出excel表格
     *
     * @param list 要导出的数据集合
     * @return 字节数组
     */
    public byte[] export(List<XwyyScheduleResultDto> list);

	/**
	 * 发布排程结果
	 *
	 * @param scheduleResult 排程日期
	 * @param ids            要发布的排程结果id
	 * @param dataVersion    数据同步版本
	 */
	public void publish(XwyyScheduleResult scheduleResult, long[] ids, String dataVersion);

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

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    Boolean isPublish(Date scheduleDate);

    /**
     * 根据排程日期、物料编号、机台id校验唯一性
     * @param scheduleResult 要校验记录
     * @return 查询到的记录数
     */
    public Boolean checkUnique(XwyyScheduleResult scheduleResult);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<XwyyScheduleResultDto> list, Long importLogId, Date scheduleDate);


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
    public int changeReleaseStatus(XwyyScheduleResult entity);

    int checkXwyyCodeExist(XwyyScheduleResultDto dto);

    int isPublishByIds(Long[] ids);

    List<XwyyScheduleResultDto> selectByIds(List<Long> ids2);

    /**
     * 根据帘布大卷代号获取帘线大卷标准长度
     * @param bigRollCode 帘布大卷代号
     * @return 帘线大卷标准长度
     */
    BigDecimal getActClothLength(String bigRollCode);

    /**
     * 归并中夜班计划量，合并到同一个班次
     * @param ids id
     * @param classifiedShift 合并班次
     * @return 修改行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int combinationMiddleAndNight(long[] ids, String classifiedShift);
}
