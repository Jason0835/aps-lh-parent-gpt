package com.zlt.aps.cd90.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 直裁机台基础信息 Feign 接口。
 */
@FeignClient(contextId = "ICd90MachineInfoRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:/cd90}")
public interface ICd90MachineInfoRemoteService {

    /** 查询列表 */
    @ApiOperation("查询直裁机台列表")
    @PostMapping("/cd90MachineInfo/list")
    TableDataInfo list(@RequestBody Cd90MachineInfo queryVO);

    /** 获取详情 */
    @ApiOperation("获取直裁机台详情")
    @GetMapping("/cd90MachineInfo/getInfo/{id}")
    Cd90MachineInfo getInfo(@PathVariable("id") Long id);

    /** 新增 */
    @ApiOperation("新增直裁机台")
    @PostMapping("/cd90MachineInfo/add")
    AjaxResult add(@RequestBody Cd90MachineInfo machineInfo);

    /** 编辑 */
    @ApiOperation("编辑直裁机台")
    @PostMapping("/cd90MachineInfo/edit")
    AjaxResult edit(@RequestBody Cd90MachineInfo machineInfo);

    /** 删除 */
    @ApiOperation("删除直裁机台")
    @PostMapping("/cd90MachineInfo/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /** 校验同工厂机台编号唯一 */
    @ApiOperation("校验直裁机台唯一性")
    @PostMapping("/cd90MachineInfo/checkUnique")
    String checkUnique(@RequestBody Cd90MachineInfo machineInfo);

    /** 导出数据 */
    @ApiOperation("导出直裁机台")
    @PostMapping("/cd90MachineInfo/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd90MachineInfo queryVO, @PathVariable("fileName") String fileName);

    /** 导入数据 */
    @ApiOperation("导入直裁机台")
    @PostMapping("/cd90MachineInfo/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /** 启用机台下拉 */
    @ApiOperation("启用直裁机台下拉")
    @PostMapping("/cd90MachineInfo/enableOptions")
    AjaxResult enableOptions(@RequestBody Cd90MachineInfo queryVO);
}
