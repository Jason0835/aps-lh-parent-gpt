package com.zlt.aps.tq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.entity.TqLossSetting;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "iTqLossSettingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tq:tq}")
public interface ITqLossSettingService {

    @PostMapping("/tqLossSetting/list")
    @ApiOperation("查询胎圈损耗率设定列表")
    TableDataInfo list(@RequestBody TqLossSetting entity);

    @GetMapping(value = "/tqLossSetting/{id}")
    @ApiOperation("获取胎圈损耗率设定详细信息")
    TqLossSetting getInfo(@PathVariable("id") Long id);

    @PostMapping("/tqLossSetting/save")
    @ApiOperation("保存胎圈损耗率设定（id为空则新增，id不为空则修改）")
    AjaxResult save(@Validated @RequestBody TqLossSetting entity);

    @PostMapping("/tqLossSetting/delete/{ids}")
    @ApiOperation("删除胎圈损耗率设定")
    AjaxResult removeByIds(@PathVariable("ids") List<Long> ids);

    @PostMapping("/tqLossSetting/checkUnique")
    @ApiOperation("校验胎圈损耗率设定唯一性")
    String checkUnique(@Validated @RequestBody TqLossSetting entity);

    @PostMapping("/tqLossSetting/exportData/{fileName}")
    @ApiOperation("导出胎圈损耗率设定")
    byte[] exportData(@RequestBody TqLossSetting entity, @PathVariable("fileName") String fileName);

    @PostMapping("/tqLossSetting/exportList")
    @ApiOperation("导出胎圈损耗率列表")
    List<TqLossSetting> exportList(@RequestBody TqLossSetting entity);

    @PostMapping("/tqLossSetting/importData")
    @ApiOperation("导入胎圈损耗率信息")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @PostMapping("/tqLossSetting/deleteAll")
    @ApiOperation("删除全部(逻辑删)")
    AjaxResult deleteAll();
}
