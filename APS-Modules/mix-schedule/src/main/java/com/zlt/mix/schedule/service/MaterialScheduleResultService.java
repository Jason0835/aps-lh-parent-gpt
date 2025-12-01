package com.zlt.mix.schedule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.schedule.api.domain.dto.*;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleResult;
import com.zlt.mix.schedule.api.domain.entity.MaterialScheduleResult;
import com.zlt.mix.schedule.api.domain.entity.MaterialSpanReceive;
import com.zlt.mix.schedule.api.domain.entity.MaterialSpanSend;
import com.zlt.mix.setting.api.domain.entity.LhflMachine;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * 硫磺辅料日计划排程Service接口
 *
 * @author chen
 * @date 2022-05-24
 */
public interface MaterialScheduleResultService extends IService<MaterialScheduleResult> {
    /**
     * 查询硫磺辅料日计划排程列表
     *
     * @param materialScheduleResult 硫磺辅料日计划排程
     * @return 硫磺辅料日计划排程集合
     */
    List<MaterialScheduleResult> selectMaterialScheduleResultList(MaterialScheduleResult materialScheduleResult);

    /**
     * 保存硫磺辅料日计划排程信息（id为空则新增，id不为空则修改）
     *
     * @param materialScheduleResult
     */
    List<MaterialScheduleResult> saveMaterialScheduleResult(MaterialScheduleResult materialScheduleResult);

    /**
     * 批量删除硫磺辅料日计划排程
     *
     * @param ids 需要删除的硫磺辅料日计划排程ID
     * @return 结果
     */
    int deleteMaterialScheduleResultByIds(Long[] ids);

    /**
     * 校验硫化辅料日计划排程唯一性
     */
    String checkMaterialScheduleResultUnique(MaterialScheduleResult materialScheduleResult);

    /**
     * 导入硫化辅料日计划排程数据
     */
    AjaxResult importData(List<MaterialScheduleResult> list, Date scheduleDate, String mixArea, Long importLogId);

    /**
     * 发布硫磺辅料日计划
     */
    @Transactional(rollbackFor = Exception.class)
    AjaxResult publish(MaterialScheduleResult scheduleResult);

    /**
     * 批量转机台
     */
    @Transactional(rollbackFor = Exception.class)
    AjaxResult batchChangeMachine(String machineCode, Long[] ids);

    /**
     * 转机台（新）：转机台后重新创建一个新的工单号
     * @param scheduleResult
     */
    void changeMachine(MaterialScheduleResult scheduleResult);

    /**
     * 根据模板文件导出到Excel
     *
     * @param dto 参数
     * @return Excel字节数组
     */
    byte[] exportData(MaterialScheduleResultExportDictDto dto);

    /**
     * 检测对应日期和密炼区的数据是否存在
     *
     * @param scheduleResult 日期和密炼区
     * @return 是否唯一的常量值
     */
    String checkScheduleDateAndMixAreaExist(MaterialScheduleResult scheduleResult);

    /**
     * 更改配方信息
     * @param materialScheduleResult id、配方阶段、配方版本号、配方类型
     * @return 结果
     */
    AjaxResult changeRecipe(MaterialScheduleResult materialScheduleResult);

    /**
     * 根据id查询排程信息
     * @param id id
     * @return 结果
     */
    MaterialScheduleResult getById(Long id);

    /**
     * 根据ids查询发布状态是否有不是【未发布】的记录
     * @param ids ids
     * @return 不是未发布的记录数
     */
    int isNoReleaseByIds(Long[] ids);

    /**
     * 根据参数查询机台信息
     */
    List<LhflMachine> getMachineInfo(LhflMachine param);

    /**
     * 获取统计信息
     * @param materialScheduleResult 日期、密炼区、机台编号
     * @return 统计好的信息列表
     */
    List<MaterialScheduleResultStatisticsDto> statistics(MaterialScheduleResult materialScheduleResult);
    
    /**
     * 获取超期预警信息
     * @param materialScheduleResult
     * @return
     */
    List<MaterialExpireWarningDto> expireWarning(MaterialScheduleResult materialScheduleResult);

    /**
     * 根据条件查询终炼母炼日计划跨区发送列表
     * @param entity 查询条件
     * @return 结果
     */
    List<MaterialSpanSend> listMaterialSpanSend(MaterialSpanSend entity);

    /**
     * 发送跨区请求
     * @param dto 跨区请求集合
     * @return 结果
     */
    @Transactional
    AjaxResult sendMaterialSpan(MaterialSpanSendDto dto) throws ParseException;

    /**
     * 根据条件查询终炼母炼日计划跨区接收列表
     * @param entity 查询条件
     * @return 结果
     */
    List<MaterialSpanReceive> listMaterialSpanReceive(MaterialSpanReceive entity);

    /**
     * 接收跨区请求
     * @param dto 要接收的跨区请求
     * @return 结果
     */
    @Transactional
    AjaxResult receiveMaterialSpanReceive(MaterialSpanReceiveDto dto);

    /**
     * 根据排程日期、密炼区、机台，查询机台的各班次总计划量
     * @param scheduleResult 参数
     * @return 结果
     */
    MaterialSpanReceiveQtyDto getSumQtyByMachineCode(MaterialScheduleResult scheduleResult);

    /**
     * 删除发送的跨区请求
     * @return 结果
     */
    @Transactional
    AjaxResult deleteMaterialSpanSend(Long[] ids);

    /**
     * 自动排程后，根据跨区设置表，自动生产相应的跨区发送和接收记录
     * @param mixArea  密炼区
     * @param scheduleDate  排程日期
     */
    void autoCreateMaterialSpanRecord(String mixArea, Date scheduleDate);

    /**
     * 根据选中的ids查询跨区发送时要携带的字段
     * @param ids 选中的id
     * @return 查询结果
     */
    List<MaterialScheduleResult> selectSpanSendNeedFieldByIds(Long[] ids);

    /**
     * 保存自动排程日志
     * @param scheduleResult 参数
     * @return 结果
     */
    void saveAutoScheduleLog(MaterialScheduleResult scheduleResult);
}
