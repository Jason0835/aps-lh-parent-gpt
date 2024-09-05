package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.annotations.ApiOperation;

import com.zlt.aps.lh.api.domain.entity.MixReturnRubberStock;


/**
 * 返回胶库存Service接口
 * @author zlt
 * @date 2021-11-09
 */
@FeignClient(contextId = "IMixReturnRubberStockService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface IMixReturnRubberStockService {

    /**
     * 查询返回胶库存列表
     */
    @ApiOperation("查询返回胶库存列表")
    @PostMapping("/returnRubberStock/list")
    TableDataInfo list(@RequestBody MixReturnRubberStock mixReturnRubberStock);

    /**
    * 新增返回胶库存
    */
    @ApiOperation("新增返回胶库存")
    @PostMapping("/returnRubberStock/add")
    AjaxResult add(@RequestBody MixReturnRubberStock mixReturnRubberStock);

    /**
     * 修改返回胶库存
     */
    @ApiOperation("修改返回胶库存")
    @PostMapping("/returnRubberStock/edit")
    AjaxResult edit(@RequestBody MixReturnRubberStock mixReturnRubberStock);

    /**
     * 删除返回胶库存
     */
    @ApiOperation("删除返回胶库存")
    @DeleteMapping("/returnRubberStock/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/returnRubberStock/{id}")
    MixReturnRubberStock getInfo(@PathVariable("id") Long id);

    /**
     * 校验返回胶库存唯一性
     */
    @ApiOperation("校验返回胶库存唯一性")
    @PostMapping("/returnRubberStock/checkMixReturnRubberStockUnique")
    String checkMixReturnRubberStockUnique(@RequestBody MixReturnRubberStock mixReturnRubberStock);

    /**
     * 导出返回胶库存列表
     */
    @ApiOperation("导出返回胶库存列表")
    @PostMapping("/returnRubberStock/getList")
    List<MixReturnRubberStock> getList(@RequestBody MixReturnRubberStock mixReturnRubberStock);

    /**
     * 导入返回胶库存数据
     */
    @ApiOperation("导入返回胶库存")
    @PostMapping("/returnRubberStock/importData")
    public AjaxResult importData(@RequestBody List<MixReturnRubberStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
