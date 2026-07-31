package com.zlt.aps.gdyy.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gdyy.api.domain.dto.GdyyScheduleImportDTO;
import com.zlt.aps.gdyy.api.domain.entity.GdyyScheduleResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢带压延排程结果 Feign 接口。
 */
@FeignClient(contextId = "IGdyyScheduleResultRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gdyy:gdyy}")
public interface IGdyyScheduleResultRemoteService {

    @ApiOperation("查询钢带压延排程结果列表")
    @PostMapping("/gdyy/scheduleResult/list")
    TableDataInfo list(@RequestBody GdyyScheduleResult queryVO);

    @ApiOperation("获取钢带压延排程结果详情")
    @GetMapping("/gdyy/scheduleResult/getInfo/{id}")
    GdyyScheduleResult getInfo(@PathVariable("id") Long id);

    @ApiOperation("新增/编辑钢带压延排程结果")
    @PostMapping("/gdyy/scheduleResult/edit")
    AjaxResult edit(@RequestBody GdyyScheduleResult entity);

    @ApiOperation("删除钢带压延排程结果")
    @PostMapping("/gdyy/scheduleResult/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("校验钢带压延排程结果唯一性")
    @PostMapping("/gdyy/scheduleResult/checkUnique")
    String checkUnique(@RequestBody GdyyScheduleResult entity);

    @ApiOperation("导出钢带压延排程结果")
    @PostMapping("/gdyy/scheduleResult/exportData/{fileName}")
    byte[] exportData(@RequestBody GdyyScheduleResult queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入钢带压延排程结果")
    @PostMapping("/gdyy/scheduleResult/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @ApiOperation("按固定模板导入钢带压延排程结果")
    @PostMapping("/gdyy/scheduleResult/importDataByCust/{updateSupport}")
    AjaxResult importDataByCust(@PathVariable("updateSupport") boolean updateSupport,
                                @RequestBody GdyyScheduleImportDTO importDTO);

    @ApiOperation("调量")
    @PostMapping("/gdyy/scheduleResult/changeQty")
    AjaxResult changeQty(@RequestBody GdyyScheduleResult entity);

    @ApiOperation("转机台")
    @PostMapping("/gdyy/scheduleResult/changeMachine")
    AjaxResult changeMachine(@RequestBody GdyyScheduleResult entity);

    @ApiOperation("发布")
    @PostMapping("/gdyy/scheduleResult/publish")
    AjaxResult publish(@RequestBody GdyyScheduleResult entity);

    @ApiOperation("更改发布状态")
    @PostMapping("/gdyy/scheduleResult/changeReleaseStatus")
    AjaxResult changeReleaseStatus(@RequestBody GdyyScheduleResult entity);

    @ApiOperation("获取合计信息")
    @PostMapping("/gdyy/scheduleResult/getSummaryVo")
    AjaxResult getSummaryVo(@RequestBody GdyyScheduleResult queryVO);

    @ApiOperation("导入完成量")
    @PostMapping("/gdyy/scheduleResult/importFinishQty")
    AjaxResult importFinishQty(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
