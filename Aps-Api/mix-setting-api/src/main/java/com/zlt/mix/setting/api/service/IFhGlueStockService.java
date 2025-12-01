package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.FhGlueStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 返回胶库存信息Service接口
 *
 * @author Liam
 * @date 2022-04-12
 */
@FeignClient(contextId = "IFhGlueStockService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IFhGlueStockService {

    /**
     * 查询返回胶库存信息列表
     */
    @PostMapping("/fhstock/list")
    TableDataInfo listFhGlueStock(@RequestBody FhGlueStock fhGlueStock);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/fhstock/{id}")
    FhGlueStock getFhGlueStockInfo(@PathVariable("id") Long id);

    /**
     * 保存返回胶库存信息信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/fhstock/save")
    AjaxResult saveFhGlueStock(@RequestBody FhGlueStock fhGlueStock);

    /**
     * 批量删除返回胶库存信息
     */
    @PostMapping("/fhstock/delete/{ids}")
    AjaxResult deleteFhGlueStock(@PathVariable("ids") Long[] ids);

    /**
     * 校验返回胶库存信息唯一性
     */
    @ApiOperation("校验返回胶库存信息唯一性")
    @PostMapping("/fhstock/checkFhGlueStockUnique")
    String checkFhGlueStockUnique(@RequestBody FhGlueStock fhGlueStock);

    /**
     * 导出返回胶库存信息列表
     */
    @PostMapping("/fhstock/exportData")
    List<FhGlueStock> exportData(@RequestBody FhGlueStock fhGlueStock);

    /**
     * 导入返回胶库存信息数据
     */
    @ApiOperation("导入返回胶库存信息")
    @PostMapping("/fhstock/importData")
    public AjaxResult importData(@RequestBody List<FhGlueStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
