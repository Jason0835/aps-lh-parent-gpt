package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.MixFinalMaterialStock;
import com.zlt.aps.lh.service.MixFinalMaterialStockService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 终炼小料库存Controller
 *
 * @author zlt
 * @date 2021-11-09
 */
@RestController
@RequestMapping("/finalMaterialStock")
public class MixFinalMaterialStockController extends BaseController
{
    @Autowired
    private MixFinalMaterialStockService mixFinalMaterialStockService;

    /**
     * 查询终炼小料库存列表
     */
    @ApiOperation("查询终炼小料库存列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MixFinalMaterialStock mixFinalMaterialStock)
    {
        startPage();
        mixFinalMaterialStock.setOrderStr(orderStr());
        List<MixFinalMaterialStock> list = mixFinalMaterialStockService.selectMixFinalMaterialStockList(mixFinalMaterialStock);
        return getDataTable(list);
    }

    /**
     * 获取终炼小料库存详细信息
     */
    @ApiOperation("获取终炼小料库存详细信息")
    @GetMapping(value = "/{id}")
    public MixFinalMaterialStock getInfo(@PathVariable("id") Long id){
        return mixFinalMaterialStockService.selectMixFinalMaterialStockById(id);
    }

    /**
     * 新增终炼小料库存
     */
    @Log(title = "ui.data.column.finalMaterialStock.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增终炼小料库存")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MixFinalMaterialStock mixFinalMaterialStock){
        return toAjax(mixFinalMaterialStockService.insertMixFinalMaterialStock(mixFinalMaterialStock));
    }

    /**
     * 修改终炼小料库存
     */
    @Log(title = "ui.data.column.finalMaterialStock.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改终炼小料库存")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MixFinalMaterialStock mixFinalMaterialStock){
        return toAjax(mixFinalMaterialStockService.updateMixFinalMaterialStock(mixFinalMaterialStock));
    }

    /**
     * 删除终炼小料库存
     */
    @Log(title = "ui.data.column.finalMaterialStock.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除终炼小料库存")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(mixFinalMaterialStockService.deleteMixFinalMaterialStockByIds(ids));
    }

    /**
     * 导出终炼小料库存列表
     */
    @Log(title = "ui.data.column.finalMaterialStock.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出终炼小料库存列表")
    @PostMapping("/getList")
    public List<MixFinalMaterialStock> getList(@RequestBody MixFinalMaterialStock mixFinalMaterialStock){
        startPage();
        mixFinalMaterialStock.setOrderStr(orderStr());
        return  mixFinalMaterialStockService.selectMixFinalMaterialStockList(mixFinalMaterialStock);
    }

    /**
     * 校验终炼小料库存唯一性
     */
    @ApiOperation("校验终炼小料库存唯一性")
    @PostMapping("/checkMixFinalMaterialStockUnique")
    public String checkMixFinalMaterialStockUnique(@RequestBody MixFinalMaterialStock mixFinalMaterialStock){
        return mixFinalMaterialStockService.checkMixFinalMaterialStockUnique(mixFinalMaterialStock);
    }

    /**
     * 根据集合导入终炼小料库存数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.finalMaterialStock.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入终炼小料库存数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MixFinalMaterialStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return mixFinalMaterialStockService.importData(list, updateSupport, importLogId);
    }
}
