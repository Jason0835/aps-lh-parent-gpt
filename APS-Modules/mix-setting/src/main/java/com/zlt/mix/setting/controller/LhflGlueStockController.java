package com.zlt.mix.setting.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.setting.api.domain.entity.LhflGlueStock;
import com.zlt.mix.setting.service.LhflGlueStockService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 硫化辅料终炼库存信息Controller
 *
 * @author Liam
 * @date 2022-04-18
 */
@RestController
@RequestMapping("/lhflGlueStock")
public class LhflGlueStockController extends BaseController {
    @Resource
    private LhflGlueStockService lhflGlueStockService;

    /**
     * 查询硫化辅料终炼库存信息列表
     */
    @ApiOperation("查询硫化辅料终炼库存信息列表")
    @PostMapping("/list")
    public TableDataInfo listLhflGlueStock(@RequestBody LhflGlueStock lhflGlueStock) {
        startPage(false);
        lhflGlueStock.setOrderStr(orderStr());
        List<LhflGlueStock> list = lhflGlueStockService.selectLhflGlueStockList(lhflGlueStock);
        return getDataTable(list);
    }
    
    /**
     * 查询硫磺辅料终炼库存信息列表（不分页）
     */
    @ApiOperation("查询硫磺辅料终炼库存信息列表（不分页）")
    @PostMapping("/selectLhflGlueStock")
    public List<LhflGlueStock> selectLhflGlueStock(@RequestBody LhflGlueStock lhflGlueStock) {
        startPage(false);
        return lhflGlueStockService.selectLhflGlueStockList(lhflGlueStock);
    }

    @ApiOperation("获取硫化辅料终炼库存信息详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public LhflGlueStock getLhflGlueStockInfo(@PathVariable("id") Long id) {
        return lhflGlueStockService.getById(id);
    }

    @Log(title = "setting.lhflGlueStock.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存硫化辅料终炼库存信息信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveLhflGlueStock(@RequestBody LhflGlueStock lhflGlueStock) {
        //是否为编辑
        boolean isEdit=lhflGlueStock.getId()!=null;
        lhflGlueStockService.saveLhflGlueStock(lhflGlueStock);
        //如果为编辑，返回数据库数据，主要用于库存重量的显示，避免数据库向上取整导致和前台页面显示不一致
        if(isEdit){
            return AjaxResult.success(lhflGlueStockService.getById(lhflGlueStock.getId()));
        }
        return AjaxResult.success();
    }

    @Log(title = "setting.lhflGlueStock.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除硫化辅料终炼库存信息")
    @PostMapping("/delete/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteLhflGlueStock(@PathVariable Long[] ids) {
        return toAjax(lhflGlueStockService.deleteLhflGlueStockByIds(ids));
    }

    @Log(title = "setting.lhflGlueStock.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出硫化辅料终炼库存信息列表")
    @PostMapping("/exportData")
    public List<LhflGlueStock> exportData(@RequestBody LhflGlueStock lhflGlueStock) {
        startPage(false);
        lhflGlueStock.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return lhflGlueStockService.selectLhflGlueStockList(lhflGlueStock);
    }

    @ApiOperation("校验硫化辅料终炼库存信息唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkLhflGlueStockUnique")
    public String checkLhflGlueStockUnique(@RequestBody LhflGlueStock lhflGlueStock) {
        return lhflGlueStockService.checkLhflGlueStockUnique(lhflGlueStock);
    }

    @Log(title = "setting.lhflGlueStock.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入硫化辅料终炼库存信息数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
            @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
            @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<LhflGlueStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return lhflGlueStockService.importData(list, updateSupport, importLogId);
    }
}
