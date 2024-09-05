package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.annotations.ApiOperation;

import com.zlt.aps.cx.api.domain.entity.CxProductDimensionLimit;


/**
 * 成型投产班次同寸口硫化班次限定设置Service接口
 * @author zlt
 * @date 2022-01-08
 */
@FeignClient(contextId = "ICxProductDimensionLimitService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxProductDimensionLimitService {

    /**
     * 查询成型投产班次同寸口硫化班次限定设置列表
     */
    @ApiOperation("查询成型投产班次同寸口硫化班次限定设置列表")
    @PostMapping("/dimensionLimit/list")
    TableDataInfo list(@RequestBody CxProductDimensionLimit cxProductDimensionLimit);

    /**
    * 新增成型投产班次同寸口硫化班次限定设置
    */
    @ApiOperation("新增成型投产班次同寸口硫化班次限定设置")
    @PostMapping("/dimensionLimit/add")
    AjaxResult add(@RequestBody CxProductDimensionLimit cxProductDimensionLimit);

    /**
     * 修改成型投产班次同寸口硫化班次限定设置
     */
    @ApiOperation("修改成型投产班次同寸口硫化班次限定设置")
    @PostMapping("/dimensionLimit/edit")
    AjaxResult edit(@RequestBody CxProductDimensionLimit cxProductDimensionLimit);

    /**
     * 删除成型投产班次同寸口硫化班次限定设置
     */
    @ApiOperation("删除成型投产班次同寸口硫化班次限定设置")
    @DeleteMapping("/dimensionLimit/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/dimensionLimit/{id}")
    CxProductDimensionLimit getInfo(@PathVariable("id") Long id);

    /**
     * 校验成型投产班次同寸口硫化班次限定设置唯一性
     */
    @ApiOperation("校验成型投产班次同寸口硫化班次限定设置唯一性")
    @PostMapping("/dimensionLimit/checkCxProductDimensionLimitUnique")
    String checkCxProductDimensionLimitUnique(@RequestBody CxProductDimensionLimit cxProductDimensionLimit);

    /**
     * 导出成型投产班次同寸口硫化班次限定设置列表
     */
    @ApiOperation("导出成型投产班次同寸口硫化班次限定设置列表")
    @PostMapping("/dimensionLimit/getList")
    List<CxProductDimensionLimit> getList(@RequestBody CxProductDimensionLimit cxProductDimensionLimit);

    /**
     * 导入成型投产班次同寸口硫化班次限定设置数据
     */
    @ApiOperation("导入成型投产班次同寸口硫化班次限定设置")
    @PostMapping("/dimensionLimit/importData")
    public AjaxResult importData(@RequestBody List<CxProductDimensionLimit> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
