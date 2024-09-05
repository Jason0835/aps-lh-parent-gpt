package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.annotations.ApiOperation;

import com.zlt.aps.lh.api.domain.entity.MixMasterRubberStock;


/**
 * 母炼胶库存Service接口
 * @author zlt
 * @date 2021-11-09
 */
@FeignClient(contextId = "IMixMasterRubberStockService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface IMixMasterRubberStockService {

    /**
     * 查询母炼胶库存列表
     */
    @ApiOperation("查询母炼胶库存列表")
    @PostMapping("/masterRubberStock/list")
    TableDataInfo list(@RequestBody MixMasterRubberStock mixMasterRubberStock);

    /**
    * 新增母炼胶库存
    */
    @ApiOperation("新增母炼胶库存")
    @PostMapping("/masterRubberStock/add")
    AjaxResult add(@RequestBody MixMasterRubberStock mixMasterRubberStock);

    /**
     * 修改母炼胶库存
     */
    @ApiOperation("修改母炼胶库存")
    @PostMapping("/masterRubberStock/edit")
    AjaxResult edit(@RequestBody MixMasterRubberStock mixMasterRubberStock);

    /**
     * 删除母炼胶库存
     */
    @ApiOperation("删除母炼胶库存")
    @DeleteMapping("/masterRubberStock/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/masterRubberStock/{id}")
    MixMasterRubberStock getInfo(@PathVariable("id") Long id);

    /**
     * 校验母炼胶库存唯一性
     */
    @ApiOperation("校验母炼胶库存唯一性")
    @PostMapping("/masterRubberStock/checkMixMasterRubberStockUnique")
    String checkMixMasterRubberStockUnique(@RequestBody MixMasterRubberStock mixMasterRubberStock);

    /**
     * 导出母炼胶库存列表
     */
    @ApiOperation("导出母炼胶库存列表")
    @PostMapping("/masterRubberStock/getList")
    List<MixMasterRubberStock> getList(@RequestBody MixMasterRubberStock mixMasterRubberStock);

    /**
     * 导入母炼胶库存数据
     */
    @ApiOperation("导入母炼胶库存")
    @PostMapping("/masterRubberStock/importData")
    public AjaxResult importData(@RequestBody List<MixMasterRubberStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
