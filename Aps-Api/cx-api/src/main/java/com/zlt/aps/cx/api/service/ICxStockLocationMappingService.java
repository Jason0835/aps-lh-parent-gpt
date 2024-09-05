package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.annotations.ApiOperation;

import com.zlt.aps.cx.api.domain.entity.CxStockLocationMapping;


/**
 * 库存地点映射Service接口
 * @author zlt
 * @date 2021-11-15
 */
@FeignClient(contextId = "ICxStockLocationMappingService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxStockLocationMappingService {

    /**
     * 查询库存地点映射列表
     */
    @ApiOperation("查询库存地点映射列表")
    @PostMapping("/stockLocationMapping/list")
    TableDataInfo list(@RequestBody CxStockLocationMapping cxStockLocationMapping);

    /**
    * 新增库存地点映射
    */
    @ApiOperation("新增库存地点映射")
    @PostMapping("/stockLocationMapping/add")
    AjaxResult add(@RequestBody CxStockLocationMapping cxStockLocationMapping);

    /**
     * 修改库存地点映射
     */
    @ApiOperation("修改库存地点映射")
    @PostMapping("/stockLocationMapping/edit")
    AjaxResult edit(@RequestBody CxStockLocationMapping cxStockLocationMapping);

    /**
     * 删除库存地点映射
     */
    @ApiOperation("删除库存地点映射")
    @DeleteMapping("/stockLocationMapping/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/stockLocationMapping/{id}")
    CxStockLocationMapping getInfo(@PathVariable("id") Long id);

    /**
     * 校验库存地点映射唯一性
     */
    @ApiOperation("校验库存地点映射唯一性")
    @PostMapping("/stockLocationMapping/checkCxStockLocationMappingUnique")
    String checkCxStockLocationMappingUnique(@RequestBody CxStockLocationMapping cxStockLocationMapping);

    /**
     * 导出库存地点映射列表
     */
    @ApiOperation("导出库存地点映射列表")
    @PostMapping("/stockLocationMapping/getList")
    List<CxStockLocationMapping> getList(@RequestBody CxStockLocationMapping cxStockLocationMapping);

    /**
     * 导入库存地点映射数据
     */
    @ApiOperation("导入库存地点映射")
    @PostMapping("/stockLocationMapping/importData")
    public AjaxResult importData(@RequestBody List<CxStockLocationMapping> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
