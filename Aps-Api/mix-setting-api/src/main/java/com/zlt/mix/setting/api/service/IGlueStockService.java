package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.dto.GlueStockDto;
import com.zlt.mix.setting.api.domain.entity.GlueStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 终炼胶库存信息Service接口
 *
 * @author Gim
 * @date 2022-03-18
 */
@FeignClient(contextId = "IGlueStockService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IGlueStockService {

    /**
     * 查询库存信息列表
     */
    @PostMapping("/stock/list")
    TableDataInfo listGlueStock(@RequestBody GlueStock glueStock);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/stock/{id}")
    GlueStockDto getGlueStockInfo(@PathVariable("id") Long id);

    /**
     * 保存库存信息信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/stock/save")
    AjaxResult saveGlueStock(@RequestBody GlueStockDto glueStockDto);

    /**
     * 批量删除库存信息
     */
    @PostMapping("/stock/delete/{ids}")
    AjaxResult deleteGlueStock(@PathVariable("ids") Long[] ids);

    /**
     * 校验库存信息唯一性
     */
    @ApiOperation("校验库存信息唯一性")
    @PostMapping("/stock/checkTGlueStockUnique")
    String checkGlueStockUnique(@RequestBody GlueStock glueStock);

    /**
     * 导出库存信息列表
     */
    @PostMapping("/stock/exportData")
    List<GlueStockDto> exportData(@RequestBody GlueStock glueStock);

    /**
     * 导入库存信息数据
     */
    @ApiOperation("导入库存信息")
    @PostMapping("/stock/importData")
    public AjaxResult importData(@RequestBody List<GlueStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
