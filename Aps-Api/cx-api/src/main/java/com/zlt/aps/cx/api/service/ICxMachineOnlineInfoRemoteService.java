package com.zlt.aps.cx.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型在机信息前端远程接口
 */
@FeignClient(contextId = "ICxMachineOnlineInfoRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:/cx}")
public interface ICxMachineOnlineInfoRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/cxMachineOnlineInfo/list")
    TableDataInfo list(@RequestBody CxMachineOnlineInfo queryVO);

    @ApiOperation("保存")
    @PostMapping("/cxMachineOnlineInfo/save")
    AjaxResult save(@RequestBody CxMachineOnlineInfo entity);

    @ApiOperation("删除")
    @DeleteMapping("/cxMachineOnlineInfo/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详情")
    @GetMapping(value = "/cxMachineOnlineInfo/{id}")
    CxMachineOnlineInfo getInfo(@PathVariable("id") Long id);

    @ApiOperation("\u6821\u9A8C\u552F\u4E00\u6027")
    @PostMapping("/cxMachineOnlineInfo/checkUnique")
    String checkUnique(@RequestBody CxMachineOnlineInfo entity);

    @ApiOperation("导出数据")
    @PostMapping("/cxMachineOnlineInfo/exportData/{fileName}")
    byte[] exportData(@RequestBody CxMachineOnlineInfo queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/cxMachineOnlineInfo/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}

