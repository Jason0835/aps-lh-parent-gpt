package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.GlueSafeStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 安全库存Service接口
 * @author Gim
 * @date 2022-03-21
 */
@FeignClient(contextId = "IGlueSafeStockService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IGlueSafeStockService {

    /**
     * 查询安全库存列表
     */
    @PostMapping("/safeStock/list")
    TableDataInfo listGlueSafeStock(@RequestBody GlueSafeStock glueSafeStock);

    /**
    * 根据ID获取详细信息
    */
    @GetMapping(value = "/safeStock/{id}")
    GlueSafeStock getGlueSafeStockInfo(@PathVariable("id") Long id);

    /**
    * 保存安全库存信息（id为空则新增，id不为空则修改）
    */
    @PostMapping("/safeStock/save")
    AjaxResult saveGlueSafeStock(@RequestBody GlueSafeStock glueSafeStock);

    /**
     * 批量删除安全库存
     */
    @PostMapping("/safeStock/delete/{ids}")
    AjaxResult deleteGlueSafeStock(@PathVariable("ids") Long[] ids);

    /**
     * 校验安全库存唯一性
     */
    @ApiOperation("校验安全库存唯一性")
    @PostMapping("/safeStock/checkGlueSafeStockUnique")
    String checkGlueSafeStockUnique(@RequestBody GlueSafeStock glueSafeStock);

    /**
     * 导出安全库存列表
     */
    @PostMapping("/safeStock/exportData")
    List<GlueSafeStock> exportData(@RequestBody GlueSafeStock glueSafeStock);

    /**
     * 导入安全库存数据
     */
    @ApiOperation("导入安全库存")
    @PostMapping("/safeStock/importData")
    public AjaxResult importData(@RequestBody List<GlueSafeStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 根据密炼区和胶料名称更改安全库存
     */
    @PostMapping("/safeStock/updateSafeStockByMixAreaAndGlue")
    AjaxResult updateSafeStockByMixAreaAndGlue(@RequestBody GlueSafeStock glueSafeStock);
}
