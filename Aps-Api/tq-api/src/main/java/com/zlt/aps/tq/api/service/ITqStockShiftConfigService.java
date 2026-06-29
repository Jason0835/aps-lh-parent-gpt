package com.zlt.aps.tq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.entity.TqStockShiftConfig;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎圈备库班数配置 Feign接口
 *
 * @author zlt
 * @date 2026-06-25
 */
@FeignClient(contextId = "iTqStockShiftConfigService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tq:tq}")
public interface ITqStockShiftConfigService {

    @PostMapping("/tqStockShiftConfig/list")
    @ApiOperation("查询胎圈备库班数配置列表")
    TableDataInfo list(@RequestBody TqStockShiftConfig entity);

    @GetMapping(value = "/tqStockShiftConfig/{id}")
    @ApiOperation("获取胎圈备库班数配置详细信息")
    TqStockShiftConfig getInfo(@PathVariable("id") Long id);

    @PostMapping("/tqStockShiftConfig/save")
    @ApiOperation("保存胎圈备库班数配置（id为空则新增，id不为空则修改）")
    AjaxResult save(@Validated @RequestBody TqStockShiftConfig entity);

    @PostMapping("/tqStockShiftConfig/delete/{ids}")
    @ApiOperation("删除胎圈备库班数配置")
    AjaxResult removeByIds(@PathVariable("ids") List<Long> ids);

    @PostMapping("/tqStockShiftConfig/checkUnique")
    @ApiOperation("校验胎圈备库班数配置唯一性")
    String checkUnique(@Validated @RequestBody TqStockShiftConfig entity);

    @PostMapping("/tqStockShiftConfig/checkRangeCross")
    @ApiOperation("校验配置规则交叉（确保新增/修改的规则不与现有规则有范围交叉）")
    String checkRangeCross(@RequestBody TqStockShiftConfig entity);

    @PostMapping("/tqStockShiftConfig/exportData/{fileName}")
    @ApiOperation("导出胎圈备库班数配置")
    byte[] exportData(@RequestBody TqStockShiftConfig entity, @PathVariable("fileName") String fileName);

    @PostMapping("/tqStockShiftConfig/exportList")
    @ApiOperation("导出胎圈备库班数配置列表")
    List<TqStockShiftConfig> exportList(@RequestBody TqStockShiftConfig entity);

    @PostMapping("/tqStockShiftConfig/importData")
    @ApiOperation("导入胎圈备库班数配置信息")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @PostMapping("/tqStockShiftConfig/deleteAll")
    @ApiOperation("删除全部(逻辑删)")
    AjaxResult deleteAll();
}
