package com.zlt.mix.setting.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.setting.api.domain.entity.BhgGlueStock;
import com.zlt.mix.setting.service.BhgGlueStockService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 不合格胶库存信息Controller
 *
 * @author Liam
 * @date 2022-04-12
 */
@RestController
@RequestMapping("/bhgstock")
public class BhgGlueStockController extends BaseController {
    @Resource
    private BhgGlueStockService bhgGlueStockService;

    /**
     * 查询不合格胶库存信息列表
     */
    @ApiOperation("查询不合格胶库存信息列表")
    @PostMapping("/list")
    public TableDataInfo listBhgGlueStock(@RequestBody BhgGlueStock bhgGlueStock) {
        startPage(false);
        bhgGlueStock.setOrderStr(orderStr());
        List<BhgGlueStock> list = bhgGlueStockService.selectBhgGlueStockList(bhgGlueStock);
        return getDataTable(list);
    }

    @ApiOperation("获取不合格胶库存信息详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public BhgGlueStock getBhgGlueStockInfo(@PathVariable("id") Long id) {
        return bhgGlueStockService.getById(id);
    }

    @Log(title = "setting.bhgstock.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存不合格胶库存信息信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveBhgGlueStock(@RequestBody BhgGlueStock bhgGlueStock) {
        //是否为编辑
        boolean isEdit=bhgGlueStock.getId()!=null;
        bhgGlueStockService.saveBhgGlueStock(bhgGlueStock);
        //如果为编辑，返回数据库数据，主要用于库存重量的显示，避免数据库向上取整导致和前台页面显示不一致
        if(isEdit){
            return AjaxResult.success(bhgGlueStockService.getById(bhgGlueStock.getId()));
        }
        return AjaxResult.success();
    }

    @Log(title = "setting.bhgstock.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除不合格胶库存信息")
    @PostMapping("/delete/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteBhgGlueStock(@PathVariable Long[] ids) {
        return toAjax(bhgGlueStockService.deleteBhgGlueStockByIds(ids));
    }

    @Log(title = "setting.bhgstock.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出不合格胶库存信息列表")
    @PostMapping("/exportData")
    public List<BhgGlueStock> exportData(@RequestBody BhgGlueStock bhgGlueStock) {
        startPage(false);
        bhgGlueStock.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return bhgGlueStockService.selectBhgGlueStockList(bhgGlueStock);
    }

    @ApiOperation("校验不合格胶库存信息唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkBhgGlueStockUnique")
    public String checkBhgGlueStockUnique(@RequestBody BhgGlueStock bhgGlueStock) {
        return bhgGlueStockService.checkBhgGlueStockUnique(bhgGlueStock);
    }

    @Log(title = "setting.bhgstock.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入不合格胶库存信息数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
            @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
            @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<BhgGlueStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return bhgGlueStockService.importData(list, updateSupport, importLogId);
    }
}
