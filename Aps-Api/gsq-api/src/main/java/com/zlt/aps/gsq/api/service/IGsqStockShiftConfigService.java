package com.zlt.aps.gsq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqStockShiftConfig;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈备库班数配置 Feign接口
 *
 * @author zlt
 * @date 2026-08-06
 */
@FeignClient(contextId = "iGsqStockShiftConfigService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqStockShiftConfigService {

    @PostMapping("/gsqStockShiftConfig/list")
    @ApiOperation("查询钢丝圈备库班数配置列表")
    TableDataInfo list(@RequestBody GsqStockShiftConfig entity);

    @GetMapping(value = "/gsqStockShiftConfig/{id}")
    @ApiOperation("获取钢丝圈备库班数配置详细信息")
    GsqStockShiftConfig getInfo(@PathVariable("id") Long id);

    @PostMapping("/gsqStockShiftConfig/save")
    @ApiOperation("保存钢丝圈备库班数配置（id为空则新增，id不为空则修改）")
    AjaxResult save(@Validated @RequestBody GsqStockShiftConfig entity);

    @PostMapping("/gsqStockShiftConfig/delete/{ids}")
    @ApiOperation("删除钢丝圈备库班数配置")
    AjaxResult removeByIds(@PathVariable("ids") List<Long> ids);

    @PostMapping("/gsqStockShiftConfig/checkUnique")
    @ApiOperation("校验钢丝圈备库班数配置唯一性")
    String checkUnique(@Validated @RequestBody GsqStockShiftConfig entity);

    @PostMapping("/gsqStockShiftConfig/checkRangeCross")
    @ApiOperation("校验配置规则交叉（确保新增/修改的规则不与现有规则有范围交叉）")
    String checkRangeCross(@RequestBody GsqStockShiftConfig entity);

    @PostMapping("/gsqStockShiftConfig/exportData/{fileName}")
    @ApiOperation("导出钢丝圈备库班数配置")
    byte[] exportData(@RequestBody GsqStockShiftConfig entity, @PathVariable("fileName") String fileName);

    @PostMapping("/gsqStockShiftConfig/exportList")
    @ApiOperation("导出钢丝圈备库班数配置列表")
    List<GsqStockShiftConfig> exportList(@RequestBody GsqStockShiftConfig entity);

    @PostMapping("/gsqStockShiftConfig/importData")
    @ApiOperation("导入钢丝圈备库班数配置信息")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @PostMapping("/gsqStockShiftConfig/deleteAll")
    @ApiOperation("删除全部(逻辑删)")
    AjaxResult deleteAll();
}