package com.zlt.aps.cd15.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 斜裁机台基础信息 Feign 接口。
 */
@FeignClient(contextId = "ICd15MachineInfoRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:/cd15}")
public interface ICd15MachineInfoRemoteService {

    /** 查询列表 */
    @ApiOperation("查询斜裁机台列表")
    @PostMapping("/cd15MachineInfo/list")
    TableDataInfo list(@RequestBody Cd15MachineInfo queryVO);

    /** 获取详情 */
    @ApiOperation("获取斜裁机台详情")
    @GetMapping("/cd15MachineInfo/getInfo/{id}")
    Cd15MachineInfo getInfo(@PathVariable("id") Long id);

    /** 新增 */
    @ApiOperation("新增斜裁机台")
    @PostMapping("/cd15MachineInfo/add")
    AjaxResult add(@RequestBody Cd15MachineInfo machineInfo);

    /** 编辑 */
    @ApiOperation("编辑斜裁机台")
    @PostMapping("/cd15MachineInfo/edit")
    AjaxResult edit(@RequestBody Cd15MachineInfo machineInfo);

    /** 删除 */
    @ApiOperation("删除斜裁机台")
    @PostMapping("/cd15MachineInfo/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /** 校验同工厂机台编号唯一 */
    @ApiOperation("校验斜裁机台唯一性")
    @PostMapping("/cd15MachineInfo/checkUnique")
    String checkUnique(@RequestBody Cd15MachineInfo machineInfo);

    /** 导出数据 */
    @ApiOperation("导出斜裁机台")
    @PostMapping("/cd15MachineInfo/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd15MachineInfo queryVO, @PathVariable("fileName") String fileName);

    /** 导入数据 */
    @ApiOperation("导入斜裁机台")
    @PostMapping("/cd15MachineInfo/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /** 启用机台下拉 */
    @ApiOperation("启用斜裁机台下拉")
    @PostMapping("/cd15MachineInfo/enableOptions")
    AjaxResult enableOptions(@RequestBody Cd15MachineInfo queryVO);

    /** 修改机台状态 */
    @ApiOperation("修改机台状态")
    @PostMapping("/cd15MachineInfo/changeStatus")
    AjaxResult changeStatus(@RequestBody Cd15MachineInfo machineInfo);
}
