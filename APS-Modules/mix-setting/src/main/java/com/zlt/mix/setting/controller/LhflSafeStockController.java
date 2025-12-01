package com.zlt.mix.setting.controller;

import java.util.Date;
import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import javax.annotation.Resource;

import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.DateUtil;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.zlt.mix.setting.api.domain.entity.LhflSafeStock;
import com.zlt.mix.setting.service.LhflSafeStockService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.util.CollectionUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;

/**
 * 硫磺辅料安全库存Controller
 * @author hakimryan
 *
 */
@RestController
@RequestMapping("/lhflSafeStock")
public class LhflSafeStockController extends BaseController {
    @Resource
    private LhflSafeStockService lhflSafeStockService;

    /**
     * 查询安全库存列表
     */
    @ApiOperation("查询安全库存列表")
    @PostMapping("/list")
    public TableDataInfo listLhflSafeStock(@RequestBody LhflSafeStock lhflSafeStock) {
        startPage(false);
        lhflSafeStock.setOrderStr(orderStr());
        lhflSafeStock.setTodayDate(DateUtil.thatDay(new Date()));
        List<LhflSafeStock> list = lhflSafeStockService.selectLhflSafeStockList(lhflSafeStock);
        return getDataTable(list);
    }

    @ApiOperation("获取安全库存详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public LhflSafeStock getLhflSafeStockInfo(@PathVariable("id") Long id){
        return lhflSafeStockService.getById(id);
    }

    @Log(title = "setting.safeStock.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存安全库存信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveLhflSafeStock(@RequestBody LhflSafeStock lhflSafeStock) {
        String unique = checkLhflSafeStockUnique(lhflSafeStock);
        if (unique.equals(ZltConstant.NOT_UNIQUE)) {
            return AjaxResult.error(I18nUtil.getMessage("setting.safeStock.database.unique"));
        }
        lhflSafeStockService.saveLhflSafeStock(lhflSafeStock);
        return AjaxResult.success();
    }

    @Log(title = "setting.safeStock.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除安全库存")
	@PostMapping("/delete/{ids}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteLhflSafeStock(@PathVariable Long[] ids){
        return toAjax(lhflSafeStockService.deleteLhflSafeStockByIds(ids));
    }

    @Log(title = "setting.safeStock.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出安全库存列表")
    @PostMapping("/exportData")
    public List<LhflSafeStock> exportData(@RequestBody LhflSafeStock lhflSafeStock){
        startPage(false);
        lhflSafeStock.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        lhflSafeStock.setTodayDate(DateUtil.thatDay(new Date()));
        return  lhflSafeStockService.selectLhflSafeStockList(lhflSafeStock);
    }

    @ApiOperation("校验安全库存唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkLhflSafeStockUnique")
    public String checkLhflSafeStockUnique(@RequestBody LhflSafeStock lhflSafeStock){
        return lhflSafeStockService.checkLhflSafeStockUnique(lhflSafeStock);
    }

    @Log(title = "setting.safeStock.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入安全库存数据")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
        @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
        @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<LhflSafeStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return lhflSafeStockService.importData(list, updateSupport, importLogId);
    }

//    /**
//     * 根据密炼区和胶料名称更改安全库存
//     */
//    @Log(title = "setting.safeStock.modelName", newBusinessType = BusinessConstant.UPDATE)
//    @ApiOperation("根据密炼区和胶料名称更改安全库存")
//    @PostMapping("/updateSafeStockByMixAreaAndLhfl")
//    public AjaxResult updateSafeStockByMixAreaAndLhfl(@RequestBody LhflSafeStock lhflSafeStock){
//        lhflSafeStockService.updateSafeStockByMixAreaAndLhfl(lhflSafeStock);
//        return AjaxResult.success();
//    }
}
