package com.zlt.mix.setting.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.setting.api.domain.dto.GlueStockDto;
import com.zlt.mix.setting.api.domain.entity.GlueStock;
import com.zlt.mix.setting.service.GlueSafeStockService;
import com.zlt.mix.setting.service.GlueStockService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 终炼胶库存信息Controller
 *
 * @author Gim
 * @date 2022-03-18
 */
@RestController
@RequestMapping("/stock")
public class GlueStockController extends BaseController {
    @Resource
    private GlueStockService glueStockService;

    @Autowired
    private GlueSafeStockService glueSafeStockService;

    /**
     * 查询库存信息列表
     */
    @ApiOperation("查询库存信息列表")
    @PostMapping("/list")
    public TableDataInfo listGlueStock(@RequestBody GlueStock glueStock) {
        startPage(false);
        glueStock.setOrderStr(orderStr());
        List<GlueStockDto> list = glueStockService.selectGlueStockList(glueStock);
        return getDataTable(list);
    }

    @ApiOperation("获取库存信息详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GlueStockDto getGlueStockInfo(@PathVariable("id") Long id) {
        GlueStock glueStock = glueStockService.getById(id);
        GlueStockDto glueStockDto = new GlueStockDto();
        BeanUtils.copyProperties(glueStock, glueStockDto);
        //查询安全库存
        BigDecimal safeStock = glueSafeStockService.selectGlueSafeStock(glueStock.getMixArea(), glueStock.getGlue());
        glueStockDto.setSafeStock(safeStock);
        return glueStockDto;
    }

    @Log(title = "setting.stock.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存库存信息信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    @Transactional
    public AjaxResult saveGlueStock(@RequestBody GlueStockDto glueStockDto) {
        //记录是否编辑
        boolean isEdit=glueStockDto.getId()!=null;

        GlueStock glueStock = new GlueStock();
        BeanUtils.copyProperties(glueStockDto, glueStock);
        String unique = this.checkTGlueStockUnique(glueStock);
        if (unique.equals(ZltConstant.NOT_UNIQUE)) {
            return AjaxResult.error(I18nUtil.getMessage("setting.stock.database.unique"));
        }
        glueStockService.saveGlueStock(glueStock);

        //如果安全库存不为空，更新安全库存
        if (glueStockDto.getSafeStock() != null) {

            //如果安全库存存在更新，则更新相同密炼区和胶料名称的其他列表数据的安全库存
            BigDecimal safeStock = glueSafeStockService.selectGlueSafeStock(glueStockDto.getMixArea(), glueStockDto.getGlue());
            if(!glueStockDto.getSafeStock().equals(safeStock)){
                //更新安全库存
                glueSafeStockService.saveOrUpdateGlueSafeStock(glueStockDto.getMixArea(), glueStockDto.getGlue(), glueStockDto.getSafeStock());

                //如果为编辑，根据密炼区和胶料名称，去更新页面上的相同胶料安全库存的列表显示
                if(isEdit){
                    GlueStock glueStockParam=new GlueStock();
                    glueStockParam.setMixArea(glueStockDto.getMixArea());
                    glueStockParam.setGlue(glueStockDto.getGlue());
                    List<GlueStockDto> glueStockDtoList = glueStockService.selectGlueStockList(glueStockParam);
                    return AjaxResult.success(glueStockDtoList);
                }
            }

        }

        //如果为编辑，返回数据库数据，主要用于库存重量的显示，避免数据库向上取整导致和前台页面显示不一致
        if(isEdit){
            List<GlueStock> glueStockList = Collections.singletonList(glueStockService.getById(glueStock.getId()));
            return AjaxResult.success(glueStockList);
        }

        return AjaxResult.success();
    }

    @Log(title = "setting.stock.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除库存信息")
    @PostMapping("/delete/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteGlueStock(@PathVariable Long[] ids) {
        return toAjax(glueStockService.deleteGlueStockByIds(ids));
    }

    @Log(title = "setting.stock.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出库存信息列表")
    @PostMapping("/exportData")
    public List<GlueStockDto> exportData(@RequestBody GlueStock glueStock) {
        startPage(false);
        glueStock.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<GlueStockDto> list = glueStockService.selectGlueStockList(glueStock);
        // TODO 转换密炼区
        return list;
    }

    @ApiOperation("校验库存信息唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkTGlueStockUnique")
    public String checkTGlueStockUnique(@RequestBody GlueStock glueStock) {
        return glueStockService.checkGlueStockUnique(glueStock);
    }

    @Log(title = "setting.stock.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入库存信息数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
            @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
            @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<GlueStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return glueStockService.importData(list, updateSupport, importLogId);
    }
}
