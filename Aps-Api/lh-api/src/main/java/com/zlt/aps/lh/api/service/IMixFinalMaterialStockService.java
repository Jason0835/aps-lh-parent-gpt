package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.annotations.ApiOperation;

import com.zlt.aps.lh.api.domain.entity.MixFinalMaterialStock;


/**
 * 终炼小料库存Service接口
 * @author zlt
 * @date 2021-11-09
 */
@FeignClient(contextId = "IMixFinalMaterialStockService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface IMixFinalMaterialStockService {

    /**
     * 查询终炼小料库存列表
     */
    @ApiOperation("查询终炼小料库存列表")
    @PostMapping("/finalMaterialStock/list")
    TableDataInfo list(@RequestBody MixFinalMaterialStock mixFinalMaterialStock);

    /**
    * 新增终炼小料库存
    */
    @ApiOperation("新增终炼小料库存")
    @PostMapping("/finalMaterialStock/add")
    AjaxResult add(@RequestBody MixFinalMaterialStock mixFinalMaterialStock);

    /**
     * 修改终炼小料库存
     */
    @ApiOperation("修改终炼小料库存")
    @PostMapping("/finalMaterialStock/edit")
    AjaxResult edit(@RequestBody MixFinalMaterialStock mixFinalMaterialStock);

    /**
     * 删除终炼小料库存
     */
    @ApiOperation("删除终炼小料库存")
    @DeleteMapping("/finalMaterialStock/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/finalMaterialStock/{id}")
    MixFinalMaterialStock getInfo(@PathVariable("id") Long id);

    /**
     * 校验终炼小料库存唯一性
     */
    @ApiOperation("校验终炼小料库存唯一性")
    @PostMapping("/finalMaterialStock/checkMixFinalMaterialStockUnique")
    String checkMixFinalMaterialStockUnique(@RequestBody MixFinalMaterialStock mixFinalMaterialStock);

    /**
     * 导出终炼小料库存列表
     */
    @ApiOperation("导出终炼小料库存列表")
    @PostMapping("/finalMaterialStock/getList")
    List<MixFinalMaterialStock> getList(@RequestBody MixFinalMaterialStock mixFinalMaterialStock);

    /**
     * 导入终炼小料库存数据
     */
    @ApiOperation("导入终炼小料库存")
    @PostMapping("/finalMaterialStock/importData")
    public AjaxResult importData(@RequestBody List<MixFinalMaterialStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
