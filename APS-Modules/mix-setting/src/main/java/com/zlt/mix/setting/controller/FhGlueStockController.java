package com.zlt.mix.setting.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.setting.api.domain.entity.FhGlueStock;
import com.zlt.mix.setting.service.FhGlueStockService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 返回胶库存信息Controller
 *
 * @author Liam
 * @date 2022-04-12
 */
@RestController
@RequestMapping("/fhstock")
public class FhGlueStockController extends BaseController {
    @Resource
    private FhGlueStockService fhGlueStockService;

    /**
     * 查询返回胶库存信息列表
     */
    @ApiOperation("查询返回胶库存信息列表")
    @PostMapping("/list")
    public TableDataInfo listFhGlueStock(@RequestBody FhGlueStock fhGlueStock) {
        startPage(false);
        fhGlueStock.setOrderStr(orderStr());
        List<FhGlueStock> list = fhGlueStockService.selectFhGlueStockList(fhGlueStock);
        return getDataTable(list);
    }

    @ApiOperation("获取返回胶库存信息详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public FhGlueStock getFhGlueStockInfo(@PathVariable("id") Long id) {
        return fhGlueStockService.getById(id);
    }

    @Log(title = "setting.fhstock.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存返回胶库存信息信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveFhGlueStock(@RequestBody FhGlueStock fhGlueStock) {
        //是否为编辑
        boolean isEdit=fhGlueStock.getId()!=null;
        fhGlueStockService.saveFhGlueStock(fhGlueStock);
        //如果为编辑，返回数据库数据，主要用于库存重量的显示，避免数据库向上取整导致和前台页面显示不一致
        if(isEdit){
            return AjaxResult.success(fhGlueStockService.getById(fhGlueStock.getId()));
        }
        return AjaxResult.success();
    }

    @Log(title = "setting.fhstock.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除返回胶库存信息")
    @PostMapping("/delete/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteFhGlueStock(@PathVariable Long[] ids) {
        return toAjax(fhGlueStockService.deleteFhGlueStockByIds(ids));
    }

    @Log(title = "setting.fhstock.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出返回胶库存信息列表")
    @PostMapping("/exportData")
    public List<FhGlueStock> exportData(@RequestBody FhGlueStock fhGlueStock) {
        startPage(false);
        fhGlueStock.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return fhGlueStockService.selectFhGlueStockList(fhGlueStock);
    }

    @ApiOperation("校验返回胶库存信息唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkFhGlueStockUnique")
    public String checkFhGlueStockUnique(@RequestBody FhGlueStock fhGlueStock) {
        return fhGlueStockService.checkFhGlueStockUnique(fhGlueStock);
    }

    @Log(title = "setting.fhstock.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入返回胶库存信息数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
            @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
            @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<FhGlueStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return fhGlueStockService.importData(list, updateSupport, importLogId);
    }
}
