package com.zlt.aps.tm.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITmStockRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:/tm}")
public interface ITmStockRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tmStock/list")
    TableDataInfo list(@RequestBody TmStock queryVO);

    @ApiOperation("保存")
    @PostMapping("/tmStock/save")
    AjaxResult save(TmStock tmStock);

    @ApiOperation("删除")
    @DeleteMapping("/tmStock/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tmStock/{id}")
    TmStock getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tmStock/checkUnique")
    String checkUnique(@RequestBody TmStock tmStockVO);

    @ApiOperation("导出列表")
    @PostMapping("/tmStock/exportData/{fileName}")
    byte[] exportData(@RequestBody TmStock queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tmStock/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
