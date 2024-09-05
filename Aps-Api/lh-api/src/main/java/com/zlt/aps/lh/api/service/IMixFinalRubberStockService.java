package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.annotations.ApiOperation;

import com.zlt.aps.lh.api.domain.entity.MixFinalRubberStock;


/**
 * 终炼胶库存Service接口
 * @author zlt
 * @date 2021-11-09
 */
@FeignClient(contextId = "IMixFinalRubberStockService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface IMixFinalRubberStockService {

    /**
     * 查询终炼胶库存列表
     */
    @ApiOperation("查询终炼胶库存列表")
    @PostMapping("/finalRubberStock/list")
    TableDataInfo list(@RequestBody MixFinalRubberStock mixFinalRubberStock);

    /**
    * 新增终炼胶库存
    */
    @ApiOperation("新增终炼胶库存")
    @PostMapping("/finalRubberStock/add")
    AjaxResult add(@RequestBody MixFinalRubberStock mixFinalRubberStock);

    /**
     * 修改终炼胶库存
     */
    @ApiOperation("修改终炼胶库存")
    @PostMapping("/finalRubberStock/edit")
    AjaxResult edit(@RequestBody MixFinalRubberStock mixFinalRubberStock);

    /**
     * 删除终炼胶库存
     */
    @ApiOperation("删除终炼胶库存")
    @DeleteMapping("/finalRubberStock/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/finalRubberStock/{id}")
    MixFinalRubberStock getInfo(@PathVariable("id") Long id);

    /**
     * 校验终炼胶库存唯一性
     */
    @ApiOperation("校验终炼胶库存唯一性")
    @PostMapping("/finalRubberStock/checkMixFinalRubberStockUnique")
    String checkMixFinalRubberStockUnique(@RequestBody MixFinalRubberStock mixFinalRubberStock);

    /**
     * 导出终炼胶库存列表
     */
    @ApiOperation("导出终炼胶库存列表")
    @PostMapping("/finalRubberStock/getList")
    List<MixFinalRubberStock> getList(@RequestBody MixFinalRubberStock mixFinalRubberStock);

    /**
     * 导入终炼胶库存数据
     */
    @ApiOperation("导入终炼胶库存")
    @PostMapping("/finalRubberStock/importData")
    public AjaxResult importData(@RequestBody List<MixFinalRubberStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
