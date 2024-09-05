package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.annotations.ApiOperation;

import com.zlt.aps.cx.api.domain.entity.CxProductMachineLimit;


/**
 * 成型投产班次同机台硫化班次限定设置Service接口
 * @author zlt
 * @date 2022-01-08
 */
@FeignClient(contextId = "ICxProductMachineLimitService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxProductMachineLimitService {

    /**
     * 查询成型投产班次同机台硫化班次限定设置列表
     */
    @ApiOperation("查询成型投产班次同机台硫化班次限定设置列表")
    @PostMapping("/machineLimit/list")
    TableDataInfo list(@RequestBody CxProductMachineLimit cxProductMachineLimit);

    /**
    * 新增成型投产班次同机台硫化班次限定设置
    */
    @ApiOperation("新增成型投产班次同机台硫化班次限定设置")
    @PostMapping("/machineLimit/add")
    AjaxResult add(@RequestBody CxProductMachineLimit cxProductMachineLimit);

    /**
     * 修改成型投产班次同机台硫化班次限定设置
     */
    @ApiOperation("修改成型投产班次同机台硫化班次限定设置")
    @PostMapping("/machineLimit/edit")
    AjaxResult edit(@RequestBody CxProductMachineLimit cxProductMachineLimit);

    /**
     * 删除成型投产班次同机台硫化班次限定设置
     */
    @ApiOperation("删除成型投产班次同机台硫化班次限定设置")
    @DeleteMapping("/machineLimit/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/machineLimit/{id}")
    CxProductMachineLimit getInfo(@PathVariable("id") Long id);

    /**
     * 校验成型投产班次同机台硫化班次限定设置唯一性
     */
    @ApiOperation("校验成型投产班次同机台硫化班次限定设置唯一性")
    @PostMapping("/machineLimit/checkCxProductMachineLimitUnique")
    String checkCxProductMachineLimitUnique(@RequestBody CxProductMachineLimit cxProductMachineLimit);

    /**
     * 导出成型投产班次同机台硫化班次限定设置列表
     */
    @ApiOperation("导出成型投产班次同机台硫化班次限定设置列表")
    @PostMapping("/machineLimit/getList")
    List<CxProductMachineLimit> getList(@RequestBody CxProductMachineLimit cxProductMachineLimit);

    /**
     * 导入成型投产班次同机台硫化班次限定设置数据
     */
    @ApiOperation("导入成型投产班次同机台硫化班次限定设置")
    @PostMapping("/machineLimit/importData")
    public AjaxResult importData(@RequestBody List<CxProductMachineLimit> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
