package com.zlt.aps.xwyy.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.entity.XwyyShiftConfig;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "IXwyyShiftConfigRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.xwyy:xwyy}")
public interface IXwyyShiftConfigRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/xwyyShiftConfig/list")
    TableDataInfo list(@RequestBody XwyyShiftConfig queryVO);

    @ApiOperation("获取详情")
    @GetMapping("/xwyyShiftConfig/getInfo/{id}")
    XwyyShiftConfig getInfo(@PathVariable("id") Long id);

    @ApiOperation("新增")
    @PostMapping("/xwyyShiftConfig/add")
    AjaxResult add(@RequestBody XwyyShiftConfig entity);

    @ApiOperation("编辑")
    @PostMapping("/xwyyShiftConfig/edit")
    AjaxResult edit(@RequestBody XwyyShiftConfig entity);

    @ApiOperation("删除")
    @PostMapping("/xwyyShiftConfig/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("校验唯一性")
    @PostMapping("/xwyyShiftConfig/checkUnique")
    String checkUnique(@RequestBody XwyyShiftConfig entity);

    @ApiOperation("启用/禁用")
    @PostMapping("/xwyyShiftConfig/changeStatus")
    AjaxResult changeStatus(@RequestBody XwyyShiftConfig entity);

    @ApiOperation("导出")
    @PostMapping("/xwyyShiftConfig/exportData/{fileName}")
    byte[] exportData(@RequestBody XwyyShiftConfig queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入")
    @PostMapping("/xwyyShiftConfig/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
