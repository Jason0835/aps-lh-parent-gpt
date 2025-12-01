package com.zlt.mix.setting.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.setting.api.domain.dto.MlGlueStockDto;
import com.zlt.mix.setting.api.domain.entity.MlGlueStock;
import com.zlt.mix.setting.service.GlueSafeStockService;
import com.zlt.mix.setting.service.MlGlueStockService;
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
 * 母炼库存信息Controller
 *
 * @author Liam
 * @date 2022-04-12
 */
@RestController
@RequestMapping("/mlstock")
public class MlGlueStockController extends BaseController {
    @Resource
    private MlGlueStockService mlGlueStockService;
    @Autowired
    private GlueSafeStockService glueSafeStockService;

    /**
     * 查询母炼库存信息列表
     */
    @ApiOperation("查询母炼库存信息列表")
    @PostMapping("/list")
    public TableDataInfo listMlGlueStock(@RequestBody MlGlueStock mlGlueStock) {
        startPage(false);
        mlGlueStock.setOrderStr(orderStr());
        List<MlGlueStockDto> list = mlGlueStockService.selectMlGlueStockList(mlGlueStock);
        return getDataTable(list);
    }

    @ApiOperation("获取母炼库存信息详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public MlGlueStockDto getMlGlueStockInfo(@PathVariable("id") Long id) {
        MlGlueStock mlGlueStock = mlGlueStockService.getById(id);
        MlGlueStockDto mlGlueStockDto = new MlGlueStockDto();
        BeanUtils.copyProperties(mlGlueStock, mlGlueStockDto);
        //获取安全库存
        BigDecimal safeStock = glueSafeStockService.selectGlueSafeStock(mlGlueStock.getMixArea(), mlGlueStock.getGlue());
        mlGlueStockDto.setSafeStock(safeStock);
        return mlGlueStockDto;
    }

    @Log(title = "setting.mlstock.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存母炼库存信息信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    @Transactional
    public AjaxResult saveMlGlueStock(@RequestBody MlGlueStockDto mlGlueStockDto) {
        //记录是否编辑
        boolean isEdit=mlGlueStockDto.getId()!=null;

        MlGlueStock mlGlueStock = new MlGlueStock();
        BeanUtils.copyProperties(mlGlueStockDto, mlGlueStock);
        mlGlueStockService.saveMlGlueStock(mlGlueStock);
        //如果安全库存不等于空，更新安全库存
        if (mlGlueStockDto.getSafeStock() != null) {
            //如果安全库存存在更新，则更新相同密炼区和胶料名称的其他列表数据的安全库存
            BigDecimal safeStock = glueSafeStockService.selectGlueSafeStock(mlGlueStockDto.getMixArea(), mlGlueStockDto.getGlue());
            if(!mlGlueStockDto.getSafeStock().equals(safeStock)){
                //更新安全库存
                glueSafeStockService.saveOrUpdateGlueSafeStock(mlGlueStockDto.getMixArea(), mlGlueStockDto.getGlue(), mlGlueStockDto.getSafeStock());

                //如果为编辑，根据密炼区和胶料名称，去更新页面上的相同胶料安全库存的列表显示
                if(isEdit){
                    MlGlueStock mlGlueStockParam = new MlGlueStock();
                    mlGlueStockParam.setMixArea(mlGlueStockDto.getMixArea());
                    mlGlueStockParam.setGlue(mlGlueStockDto.getGlue());
                    List<MlGlueStockDto> mlGlueStockDtoList = mlGlueStockService.selectMlGlueStockList(mlGlueStockParam);
                    return AjaxResult.success(mlGlueStockDtoList);
                }
            }
        }

        //如果为编辑，返回数据库数据，主要用于库存重量的显示，避免数据库向上取整导致和前台页面显示不一致
        if(isEdit){
            List<MlGlueStock> glueStockList = Collections.singletonList(mlGlueStockService.getById(mlGlueStockDto.getId()));
            return AjaxResult.success(glueStockList);
        }

        return AjaxResult.success();
    }

    @Log(title = "setting.mlstock.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除母炼库存信息")
    @PostMapping("/delete/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteMlGlueStock(@PathVariable Long[] ids) {
        return toAjax(mlGlueStockService.deleteMlGlueStockByIds(ids));
    }

    @Log(title = "setting.mlstock.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出母炼库存信息列表")
    @PostMapping("/exportData")
    public List<MlGlueStockDto> exportData(@RequestBody MlGlueStock mlGlueStock) {
        startPage(false);
        mlGlueStock.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return mlGlueStockService.selectMlGlueStockList(mlGlueStock);
    }

    @ApiOperation("校验母炼库存信息唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkMlGlueStockUnique")
    public String checkMlGlueStockUnique(@RequestBody MlGlueStock mlGlueStock) {
        return mlGlueStockService.checkMlGlueStockUnique(mlGlueStock);
    }

    @Log(title = "setting.mlstock.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入母炼库存信息数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
            @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
            @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MlGlueStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return mlGlueStockService.importData(list, updateSupport, importLogId);
    }
}
