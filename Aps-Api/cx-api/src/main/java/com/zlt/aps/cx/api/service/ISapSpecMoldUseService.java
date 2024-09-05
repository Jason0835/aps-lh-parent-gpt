package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.annotations.ApiOperation;

import com.zlt.aps.cx.api.domain.entity.SapSpecMoldUse;


/**
 * 规格使用模数Service接口
 * @author zlt
 * @date 2022-01-18
 */
@FeignClient(contextId = "ISapSpecMoldUseService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ISapSpecMoldUseService {

    /**
     * 查询规格使用模数列表
     */
    @ApiOperation("查询规格使用模数列表")
    @PostMapping("/sapSpecMoldUse/list")
    TableDataInfo list(@RequestBody SapSpecMoldUse sapSpecMoldUse);

    /**
    * 新增规格使用模数
    */
    @ApiOperation("新增规格使用模数")
    @PostMapping("/sapSpecMoldUse/add")
    AjaxResult add(@RequestBody SapSpecMoldUse sapSpecMoldUse);

    /**
     * 修改规格使用模数
     */
    @ApiOperation("修改规格使用模数")
    @PostMapping("/sapSpecMoldUse/edit")
    AjaxResult edit(@RequestBody SapSpecMoldUse sapSpecMoldUse);

    /**
     * 删除规格使用模数
     */
    @ApiOperation("删除规格使用模数")
    @DeleteMapping("/sapSpecMoldUse/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/sapSpecMoldUse/{id}")
    SapSpecMoldUse getInfo(@PathVariable("id") Long id);

    /**
     * 校验规格使用模数唯一性
     */
    @ApiOperation("校验规格使用模数唯一性")
    @PostMapping("/sapSpecMoldUse/checkSapSpecMoldUseUnique")
    String checkSapSpecMoldUseUnique(@RequestBody SapSpecMoldUse sapSpecMoldUse);

    /**
     * 导出规格使用模数列表
     */
    @ApiOperation("导出规格使用模数列表")
    @PostMapping("/sapSpecMoldUse/getList")
    List<SapSpecMoldUse> getList(@RequestBody SapSpecMoldUse sapSpecMoldUse);

    @ApiOperation("根据SAP查找规格信息")
    @PostMapping("/sapSpecMoldUse/getSpecDesc")
    List<SapSpecMoldUse> getSpecDesc(@RequestBody SapSpecMoldUse sapSpecMoldUse);

    /**
     * 导入规格使用模数数据
     */
    @ApiOperation("导入规格使用模数")
    @PostMapping("/sapSpecMoldUse/importData")
    public AjaxResult importData(@RequestBody List<SapSpecMoldUse> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
