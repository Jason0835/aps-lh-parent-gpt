package com.zlt.aps.cd90.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftConfig;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 班次配置 Feign 接口。
 */
@FeignClient(contextId = "ICd90ShiftConfigRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:/cd90}")
public interface ICd90ShiftConfigRemoteService {

    /** 查询列表 */
    @ApiOperation("查询班次配置列表")
    @PostMapping("/cd90ShiftConfig/list")
    TableDataInfo list(@RequestBody Cd90ShiftConfig queryVO);

    /** 获取详情 */
    @ApiOperation("获取班次配置详情")
    @GetMapping("/cd90ShiftConfig/getInfo/{id}")
    Cd90ShiftConfig getInfo(@PathVariable("id") Long id);

    /** 新增 */
    @ApiOperation("新增班次配置")
    @PostMapping("/cd90ShiftConfig/add")
    AjaxResult add(@RequestBody Cd90ShiftConfig shiftConfig);

    /** 编辑 */
    @ApiOperation("编辑班次配置")
    @PostMapping("/cd90ShiftConfig/edit")
    AjaxResult edit(@RequestBody Cd90ShiftConfig shiftConfig);

    /** 删除 */
    @ApiOperation("删除班次配置")
    @PostMapping("/cd90ShiftConfig/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /** 校验同工厂班次编码唯一 */
    @ApiOperation("校验班次配置唯一性")
    @PostMapping("/cd90ShiftConfig/checkUnique")
    String checkUnique(@RequestBody Cd90ShiftConfig shiftConfig);

    /** 导出数据 */
    @ApiOperation("导出班次配置")
    @PostMapping("/cd90ShiftConfig/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd90ShiftConfig queryVO, @PathVariable("fileName") String fileName);

    /** 导入数据 */
    @ApiOperation("导入班次配置")
    @PostMapping("/cd90ShiftConfig/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /** 修改班次启用状态 */
    @ApiOperation("修改班次启用状态")
    @PostMapping("/cd90ShiftConfig/changeStatus")
    AjaxResult changeStatus(@RequestBody Cd90ShiftConfig shiftConfig);
}