package com.zlt.aps.cd90.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ICd90StockRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:/cd90}")
public interface ICd90StockRemoteService {
    @ApiOperation("查询列表")
    @PostMapping("/cd90Stock/list")
    TableDataInfo list(@RequestBody Cd90Stock queryVO);

    @ApiOperation("获取详情")
    @GetMapping("/cd90Stock/getInfo/{id}")
    Cd90Stock getInfo(@PathVariable("id") Long id);

    @ApiOperation("新增")
    @PostMapping("/cd90Stock/add")
    AjaxResult add(@RequestBody Cd90Stock entity);

    @ApiOperation("编辑")
    @PostMapping("/cd90Stock/edit")
    AjaxResult edit(@RequestBody Cd90Stock entity);

    @ApiOperation("删除")
    @PostMapping("/cd90Stock/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("校验唯一性")
    @PostMapping("/cd90Stock/checkUnique")
    String checkUnique(@RequestBody Cd90Stock entity);

    @ApiOperation("导出")
    @PostMapping("/cd90Stock/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd90Stock queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入")
    @PostMapping("/cd90Stock/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}