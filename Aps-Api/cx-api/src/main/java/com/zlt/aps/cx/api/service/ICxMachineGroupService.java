package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.CxMachineGroupForExcel;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.annotations.ApiOperation;

import com.zlt.aps.cx.api.domain.entity.CxMachineGroup;


/**
 * 成型机组Service接口
 * @author zlt
 * @date 2021-12-16
 */
@FeignClient(contextId = "ICxMachineGroupService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxMachineGroupService {

    /**
     * 查询成型机组列表
     */
    @ApiOperation("查询成型机组列表")
    @PostMapping("/machineGroup/list")
    TableDataInfo list(@RequestBody CxMachineGroup cxMachineGroup);

    /**
    * 新增成型机组
    */
    @ApiOperation("新增成型机组")
    @PostMapping("/machineGroup/add")
    AjaxResult add(@RequestBody CxMachineGroup cxMachineGroup);

    /**
     * 修改成型机组
     */
    @ApiOperation("修改成型机组")
    @PostMapping("/machineGroup/edit")
    AjaxResult edit(@RequestBody CxMachineGroup cxMachineGroup);

    /**
     * 删除成型机组
     */
    @ApiOperation("删除成型机组")
    @DeleteMapping("/machineGroup/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/machineGroup/{id}")
    CxMachineGroup getInfo(@PathVariable("id") Long id);

    /**
     * 校验成型机组唯一性
     */
    @ApiOperation("校验成型机组唯一性")
    @PostMapping("/machineGroup/checkCxMachineGroupUnique")
    String checkCxMachineGroupUnique(@RequestBody CxMachineGroup cxMachineGroup);

    /**
     * 导出成型机组列表
     */
    @ApiOperation("导出成型机组列表")
    @PostMapping("/machineGroup/selectCxMachineGroup4Excel")
    List<CxMachineGroupForExcel> selectCxMachineGroup4Excel(@RequestBody CxMachineGroup cxMachineGroup);

    /**
     * 导入成型机组数据
     */
    @ApiOperation("导入成型机组")
    @PostMapping("/machineGroup/importData")
    public AjaxResult importData(@RequestBody List<CxMachineGroupForExcel> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);


    @PostMapping("/machineGroup/getDetailById")
    TableDataInfo getDetailById(@RequestBody CxMachineGroup machineInfo);
}
