package com.zlt.aps.xwyy.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.entity.XwyyStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "IXwyyStockRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.xwyy:xwyy}")
public interface IXwyyStockRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/xwyyStock/list")
    TableDataInfo list(@RequestBody XwyyStock queryVO);

    @ApiOperation("获取详情")
    @GetMapping("/xwyyStock/getInfo/{id}")
    XwyyStock getInfo(@PathVariable("id") Long id);

    @ApiOperation("新增")
    @PostMapping("/xwyyStock/add")
    AjaxResult add(@RequestBody XwyyStock entity);

    @ApiOperation("编辑")
    @PostMapping("/xwyyStock/edit")
    AjaxResult edit(@RequestBody XwyyStock entity);

    @ApiOperation("删除")
    @PostMapping("/xwyyStock/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("校验唯一性")
    @PostMapping("/xwyyStock/checkUnique")
    String checkUnique(@RequestBody XwyyStock entity);

    @ApiOperation("导出")
    @PostMapping("/xwyyStock/exportData/{fileName}")
    byte[] exportData(@RequestBody XwyyStock queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入")
    @PostMapping("/xwyyStock/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}