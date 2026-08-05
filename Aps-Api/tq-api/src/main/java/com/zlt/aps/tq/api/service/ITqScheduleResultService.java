package com.zlt.aps.tq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.dto.TqScheduleResultImportDTO;
import com.zlt.aps.tq.api.domain.entity.TqScheduleResult;
import com.zlt.aps.tq.api.domain.vo.TqScheduleShiftDateVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎圈排程结果Feign Service接口
 *
 * @author APS
 */
@FeignClient(contextId = "ITqScheduleResultService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tq:tq}")
public interface ITqScheduleResultService {

    @PostMapping("/tq/scheduleResult/list")
    @ApiOperation("查询胎圈排程结果列表")
    TableDataInfo list(@RequestBody TqScheduleResult entity);

    @GetMapping(value = "/tq/scheduleResult/{id}")
    @ApiOperation("获取胎圈排程结果详细信息")
    TqScheduleResult getInfo(@PathVariable("id") Long id);

    @PostMapping("/tq/scheduleResult/save")
    @ApiOperation("保存胎圈排程结果（id为空则新增，id不为空则修改）")
    AjaxResult save(@RequestBody TqScheduleResult entity);

    @PostMapping("/tq/scheduleResult/delete/{ids}")
    @ApiOperation("删除胎圈排程结果")
    AjaxResult removeByIds(@PathVariable("ids") List<Long> ids);

    @PostMapping("/tq/scheduleResult/exportData/{fileName}")
    @ApiOperation("导出胎圈排程结果")
    byte[] exportData(@RequestBody TqScheduleResult entity, @PathVariable("fileName") String fileName);

    @PostMapping("/tq/scheduleResult/exportList")
    @ApiOperation("导出胎圈排程结果列表")
    List<TqScheduleResult> exportList(@RequestBody TqScheduleResult entity);

    @PostMapping("/tq/scheduleResult/importData")
    @ApiOperation("导入胎圈排程结果")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @PostMapping("/tq/scheduleResult/exportDataScheduleResult")
    @ApiOperation("按专用模板导出胎圈排程结果")
    byte[] exportDataScheduleResult(@RequestBody TqScheduleResult entity, @RequestParam("fileName") String fileName);

    @PostMapping("/tq/scheduleResult/importDataScheduleResult")
    @ApiOperation("按专用模板导入胎圈排程结果")
    AjaxResult importDataScheduleResult(@RequestBody TqScheduleResultImportDTO importDTO,
                                        @RequestParam("updateSupport") boolean updateSupport);

    @PostMapping("/tq/scheduleResult/downloadTemplate")
    @ApiOperation("下载胎圈排程结果空白导入模板")
    byte[] downloadTemplate(@RequestBody TqScheduleResult entity);

    @PostMapping("/tq/scheduleResult/add")
    @ApiOperation("新增胎圈排程结果")
    AjaxResult add(@RequestBody TqScheduleResult entity);

    @PostMapping("/tq/scheduleResult/edit")
    @ApiOperation("修改胎圈排程结果")
    AjaxResult edit(@RequestBody TqScheduleResult entity);

    @PostMapping("/tq/scheduleResult/remove")
    @ApiOperation("删除胎圈排程结果")
    AjaxResult remove(@RequestParam("ids") String ids);

    @PostMapping("/tq/scheduleResult/logicDelete")
    @ApiOperation("逻辑删除排程记录（已发布成功的计划不允许删除）")
    AjaxResult logicDelete(@RequestBody List<Long> ids);

    @PostMapping("/tq/scheduleResult/listScheduleShiftDates")
    @ApiOperation("获取胎圈排程班次日期列表")
    List<TqScheduleShiftDateVO> listScheduleShiftDates(@RequestBody TqScheduleResult queryVO);

    @PostMapping("/tq/scheduleResult/autoPlan")
    @ApiOperation("自动排程")
    AjaxResult autoPlan(@RequestBody TqScheduleResult entity);

    @PostMapping("/tq/scheduleResult/insertOrder")
    @ApiOperation("插单")
    AjaxResult insertOrder(@RequestBody TqScheduleResult entity);

    @PostMapping("/tq/scheduleResult/changeMachine")
    @ApiOperation("转机台")
    AjaxResult changeMachine(@RequestBody TqScheduleResult entity);

    @PostMapping("/tq/scheduleResult/listCandidateMachines/{id}")
    @ApiOperation("获取转机台候选机台列表（按寸口/口型板/定点约束过滤）")
    AjaxResult listCandidateMachines(@PathVariable("id") Long id);

    @PostMapping("/tq/scheduleResult/changeQty")
    @ApiOperation("调量")
    AjaxResult changeQty(@RequestBody TqScheduleResult entity);

    @PostMapping("/tq/scheduleResult/publish")
    @ApiOperation("发布排程")
    AjaxResult publish(@RequestBody TqScheduleResult entity);

    @PostMapping("/tq/scheduleResult/isPublish")
    @ApiOperation("查询排程日期是否已发布")
    Boolean isPublish(@RequestBody TqScheduleResult entity);

    @PostMapping("/tq/scheduleResult/checkUnique")
    @ApiOperation("唯一性校验")
    Boolean checkUnique(@RequestBody TqScheduleResult entity);
}
