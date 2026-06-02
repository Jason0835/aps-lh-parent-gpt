package com.zlt.aps.tq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.entity.TqNewScheduleResult;
import com.zlt.aps.tq.api.domain.vo.TqScheduleShiftDateVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎圈排程结果Feign Service接口（新版）
 *
 * @author APS
 */
@FeignClient(contextId = "ITqNewScheduleResultService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tq:tq}")
public interface ITqNewScheduleResultService {

    @PostMapping("/tqNewScheduleResult/list")
    @ApiOperation("查询胎圈排程结果列表")
    TableDataInfo list(@RequestBody TqNewScheduleResult entity);

    @GetMapping(value = "/tqNewScheduleResult/{id}")
    @ApiOperation("获取胎圈排程结果详细信息")
    TqNewScheduleResult getInfo(@PathVariable("id") Long id);

    @PostMapping("/tqNewScheduleResult/save")
    @ApiOperation("保存胎圈排程结果（id为空则新增，id不为空则修改）")
    AjaxResult save(@RequestBody TqNewScheduleResult entity);

    @PostMapping("/tqNewScheduleResult/delete/{ids}")
    @ApiOperation("删除胎圈排程结果")
    AjaxResult removeByIds(@PathVariable("ids") List<Long> ids);

    @PostMapping("/tqNewScheduleResult/exportData/{fileName}")
    @ApiOperation("导出胎圈排程结果")
    byte[] exportData(@RequestBody TqNewScheduleResult entity, @PathVariable("fileName") String fileName);

    @PostMapping("/tqNewScheduleResult/exportList")
    @ApiOperation("导出胎圈排程结果列表")
    List<TqNewScheduleResult> exportList(@RequestBody TqNewScheduleResult entity);

    @PostMapping("/tqNewScheduleResult/importData")
    @ApiOperation("导入胎圈排程结果")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @PostMapping("/tqNewScheduleResult/add")
    @ApiOperation("新增胎圈排程结果")
    AjaxResult add(@RequestBody TqNewScheduleResult entity);

    @PostMapping("/tqNewScheduleResult/edit")
    @ApiOperation("修改胎圈排程结果")
    AjaxResult edit(@RequestBody TqNewScheduleResult entity);

    @PostMapping("/tqNewScheduleResult/remove")
    @ApiOperation("删除胎圈排程结果")
    AjaxResult remove(@RequestParam("ids") String ids);

    @PostMapping("/tqNewScheduleResult/listScheduleShiftDates")
    @ApiOperation("获取胎圈排程班次日期列表")
    List<TqScheduleShiftDateVO> listScheduleShiftDates(@RequestBody TqNewScheduleResult queryVO);
}
