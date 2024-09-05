package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.annotations.ApiOperation;

import com.zlt.aps.lh.api.domain.entity.MixMasterMaterialStock;


/**
 * 母炼胶小料库存Service接口
 * @author zlt
 * @date 2021-11-09
 */
@FeignClient(contextId = "IMixMasterMaterialStockService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface IMixMasterMaterialStockService {

    /**
     * 查询母炼胶小料库存列表
     */
    @ApiOperation("查询母炼胶小料库存列表")
    @PostMapping("/masterMaterialStock/list")
    TableDataInfo list(@RequestBody MixMasterMaterialStock mixMasterMaterialStock);

    /**
    * 新增母炼胶小料库存
    */
    @ApiOperation("新增母炼胶小料库存")
    @PostMapping("/masterMaterialStock/add")
    AjaxResult add(@RequestBody MixMasterMaterialStock mixMasterMaterialStock);

    /**
     * 修改母炼胶小料库存
     */
    @ApiOperation("修改母炼胶小料库存")
    @PostMapping("/masterMaterialStock/edit")
    AjaxResult edit(@RequestBody MixMasterMaterialStock mixMasterMaterialStock);

    /**
     * 删除母炼胶小料库存
     */
    @ApiOperation("删除母炼胶小料库存")
    @DeleteMapping("/masterMaterialStock/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/masterMaterialStock/{id}")
    MixMasterMaterialStock getInfo(@PathVariable("id") Long id);

    /**
     * 校验母炼胶小料库存唯一性
     */
    @ApiOperation("校验母炼胶小料库存唯一性")
    @PostMapping("/masterMaterialStock/checkMixMasterMaterialStockUnique")
    String checkMixMasterMaterialStockUnique(@RequestBody MixMasterMaterialStock mixMasterMaterialStock);

    /**
     * 导出母炼胶小料库存列表
     */
    @ApiOperation("导出母炼胶小料库存列表")
    @PostMapping("/masterMaterialStock/getList")
    List<MixMasterMaterialStock> getList(@RequestBody MixMasterMaterialStock mixMasterMaterialStock);

    /**
     * 导入母炼胶小料库存数据
     */
    @ApiOperation("导入母炼胶小料库存")
    @PostMapping("/masterMaterialStock/importData")
    public AjaxResult importData(@RequestBody List<MixMasterMaterialStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
