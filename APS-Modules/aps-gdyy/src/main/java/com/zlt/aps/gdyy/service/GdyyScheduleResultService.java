package com.zlt.aps.gdyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gdyy.api.domain.dto.GdyyScheduleResultDto;
import com.zlt.aps.gdyy.entity.GdyyScheduleResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;


/**
 * 钢带压延排程结果Service接口
 *
 * @author chen
 * @date 2021-07-05
 */
public interface GdyyScheduleResultService extends IService<GdyyScheduleResult> {
    /**
     * 查询钢带压延排程结果信息维护列表
     *
     * @param scheduleResult 钢带压延排程结果信息维护
     * @return 钢带压延排程结果信息维护集合
     */
    public List<GdyyScheduleResultDto> selectScheduleResultList(GdyyScheduleResult scheduleResult);

    /**
     * 查询钢带压延排程结果信息维护列表
     *
     * @param id 要查询的id
     * @return 钢带压延排程结果信息维护集合
     */
    public GdyyScheduleResultDto selectScheduleResultById(Long id);

    /**
     * 保存钢带压延排程结果信息维护
     *
     * @param scheduleResult 钢带压延排程结果信息维护
     */
    @Transactional
    void saveScheduleResult(GdyyScheduleResult scheduleResult);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    void insetDispatcherLog(String operType, GdyyScheduleResult newSchedule);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量、2--插单
     */
    void insetDispatcherLogInsertOrder(String operType, List<GdyyScheduleResult> scheduleResults, GdyyScheduleResult newSchedule);

    /**
     * 批量删除钢带压延排程结果信息维护
     *
     * @param ids 需要删除的钢带压延排程结果信息维护ID
     */
    @Transactional
    public void deleteScheduleResultByIds(long[] ids);

    /**
     * 导出excel表格
     *
     * @param list 要导出的数据集合
     * @return 字节数组
     */
    public byte[] export(List<GdyyScheduleResultDto> list);

    /**
     * 发布排程结果
     *
     * @param ids 要发布的排程结果id
     */
    public void publish(GdyyScheduleResult scheduleResult, long[] ids);

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
    Boolean checkUnique(GdyyScheduleResult scheduleResult);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GdyyScheduleResultDto> list, Long importLogId, Date scheduleDate);


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
    public int changeReleaseStatus(GdyyScheduleResult entity);

    int checkGdyyCodeExist(GdyyScheduleResult scheduleResult);

    int isPublishByIds(long[] ids);

    /**
     * 根据排程日期和代码查询排程结果
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    List<GdyyScheduleResult> selectByScheduleDateAndCode(GdyyScheduleResult scheduleResult);
}
