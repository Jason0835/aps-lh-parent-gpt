package com.zlt.aps.tc.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITcStockRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:/tc}")
public interface ITcStockRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tcStock/list")
    TableDataInfo list(@RequestBody TcStock queryVO);

    @ApiOperation("保存")
    @PostMapping("/tcStock/save")
    AjaxResult save(TcStock tcStock);

    @ApiOperation("删除")
    @DeleteMapping("/tcStock/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tcStock/{id}")
    TcStock getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tcStock/checkUnique")
    String checkUnique(@RequestBody TcStock tcStockVO);

    @ApiOperation("导出列表")
    @PostMapping("/tcStock/exportData/{fileName}")
    byte[] exportData(@RequestBody TcStock queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tcStock/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}