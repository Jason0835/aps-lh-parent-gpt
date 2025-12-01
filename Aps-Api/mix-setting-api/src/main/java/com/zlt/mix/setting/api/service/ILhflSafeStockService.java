package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.LhflSafeStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 安全库存Service接口
 * @author hakimryan
 *
 */
@FeignClient(contextId = "ILhflSafeStockService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface ILhflSafeStockService {

    /**
     * 查询安全库存列表
     */
    @PostMapping("/lhflSafeStock/list")
    TableDataInfo listLhflSafeStock(@RequestBody LhflSafeStock lhflSafeStock);

    /**
    * 根据ID获取详细信息
    */
    @GetMapping(value = "/lhflSafeStock/{id}")
    LhflSafeStock getLhflSafeStockInfo(@PathVariable("id") Long id);

    /**
    * 保存安全库存信息（id为空则新增，id不为空则修改）
    */
    @PostMapping("/lhflSafeStock/save")
    AjaxResult saveLhflSafeStock(@RequestBody LhflSafeStock lhflSafeStock);

    /**
     * 批量删除安全库存
     */
    @PostMapping("/lhflSafeStock/delete/{ids}")
    AjaxResult deleteLhflSafeStock(@PathVariable("ids") Long[] ids);

    /**
     * 校验安全库存唯一性
     */
    @ApiOperation("校验安全库存唯一性")
    @PostMapping("/lhflSafeStock/checkLhflSafeStockUnique")
    String checkLhflSafeStockUnique(@RequestBody LhflSafeStock lhflSafeStock);

    /**
     * 导出安全库存列表
     */
    @PostMapping("/lhflSafeStock/exportData")
    List<LhflSafeStock> exportData(@RequestBody LhflSafeStock lhflSafeStock);

    /**
     * 导入安全库存数据
     */
    @ApiOperation("导入安全库存")
    @PostMapping("/lhflSafeStock/importData")
    public AjaxResult importData(@RequestBody List<LhflSafeStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 根据密炼区和胶料名称更改安全库存
     */
//    @PostMapping("/lhflSafeStock/updateSafeStockByMixAreaAndGlue")
//    AjaxResult updateSafeStockByMixAreaAndGlue(@RequestBody LhflSafeStock lhflSafeStock);
}
