package com.zlt.aps.cx.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.vo.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型排程结果对外暴露接口
 *
 * @author APS Team
 */
@FeignClient(contextId = "cxScheduleResultService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxScheduleResultService {

    /**
     * 获取成型排程结果列表
     *
     * @param cxScheduleResult 查询条件
     * @return 分页结果
     */
    @PostMapping("/cxScheduleResult/list")
    TableDataInfo list(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 生成排程
     *
     * @param scheduleGenerateVo 排程生成参数
     * @return 操作结果
     */
    @PostMapping("/cxScheduleResult/generate")
    AjaxResult generateSchedule(@RequestBody ScheduleGenerateVo scheduleGenerateVo);

    /**
     * 成型排程结果下发到MES
     *
     * @return 下发结果
     */
    @PostMapping("/cxScheduleResult/issueToMes")
    AjaxResult issueCxScheduleResultToMes();

    /**
     * 【调量】调整各班计划量
     *
     * @param scheduleAdjustVo 调量参数
     * @return 操作结果
     */
    @PostMapping("/cxScheduleResult/adjustQty")
    AjaxResult adjustQty(@RequestBody ScheduleAdjustVo scheduleAdjustVo);

    /**
     * 【插单】插入新的排程记录
     *
     * @param scheduleInsertVo 插单参数
     * @return 操作结果
     */
    @PostMapping("/cxScheduleResult/insertOrder")
    AjaxResult insertOrder(@RequestBody ScheduleInsertVo scheduleInsertVo);

    /**
     * 【修改】修改备注和原因分析
     *
     * @param scheduleUpdateRemarkVo 修改参数
     * @return 操作结果
     */
    @PostMapping("/cxScheduleResult/updateRemark")
    AjaxResult updateRemark(@RequestBody ScheduleUpdateRemarkVo scheduleUpdateRemarkVo);

    /**
     * 【转机台】转换机台
     *
     * @param scheduleTransferMachineVo 转机台参数
     * @return 操作结果
     */
    @PostMapping("/cxScheduleResult/transferMachine")
    AjaxResult transferMachine(@RequestBody ScheduleTransferMachineVo scheduleTransferMachineVo);

    /**
     * 【排程发布】发布排程数据
     *
     * @param ids ID列表
     * @return 操作结果
     */
    @PostMapping("/cxScheduleResult/publish")
    AjaxResult publish(@RequestBody List<Long> ids);

    // ==================== UI Controller 需要的方法 ====================

    /**
     * 保存（新增或修改）
     */
    @PostMapping("/cxScheduleResult/save")
    AjaxResult save(@Validated @RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 删除（批量，UI标准格式）
     */
    @PostMapping("/cxScheduleResult/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 校验唯一性
     */
    @PostMapping("/cxScheduleResult/checkUnique")
    String checkUnique(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 导出数据（UI标准格式）
     */
    @PostMapping("/cxScheduleResult/exportData/{fileName}")
    byte[] exportData(@RequestBody CxScheduleResult cxScheduleResult, @PathVariable("fileName") String fileName);

    /**
     * 导入数据（UI标准格式）
     */
    @PostMapping("/cxScheduleResult/importData")
    AjaxResult importData(@RequestBody ImportContext context, @RequestParam("updateSupport") boolean updateSupport);
}
