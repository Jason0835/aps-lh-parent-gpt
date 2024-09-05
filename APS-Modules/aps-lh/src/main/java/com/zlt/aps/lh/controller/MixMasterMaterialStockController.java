package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.MixMasterMaterialStock;
import com.zlt.aps.lh.service.MixMasterMaterialStockService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 母炼胶小料库存Controller
 *
 * @author zlt
 * @date 2021-11-09
 */
@RestController
@RequestMapping("/masterMaterialStock")
public class MixMasterMaterialStockController extends BaseController
{
    @Autowired
    private MixMasterMaterialStockService mixMasterMaterialStockService;

    /**
     * 查询母炼胶小料库存列表
     */
    @ApiOperation("查询母炼胶小料库存列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MixMasterMaterialStock mixMasterMaterialStock)
    {
        startPage();
        mixMasterMaterialStock.setOrderStr(orderStr());
        List<MixMasterMaterialStock> list = mixMasterMaterialStockService.selectMixMasterMaterialStockList(mixMasterMaterialStock);
        return getDataTable(list);
    }

    /**
     * 获取母炼胶小料库存详细信息
     */
    @ApiOperation("获取母炼胶小料库存详细信息")
    @GetMapping(value = "/{id}")
    public MixMasterMaterialStock getInfo(@PathVariable("id") Long id){
        return mixMasterMaterialStockService.selectMixMasterMaterialStockById(id);
    }

    /**
     * 新增母炼胶小料库存
     */
    @Log(title = "ui.data.column.masterMaterialStock.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增母炼胶小料库存")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MixMasterMaterialStock mixMasterMaterialStock){
        return toAjax(mixMasterMaterialStockService.insertMixMasterMaterialStock(mixMasterMaterialStock));
    }

    /**
     * 修改母炼胶小料库存
     */
    @Log(title = "ui.data.column.masterMaterialStock.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改母炼胶小料库存")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MixMasterMaterialStock mixMasterMaterialStock){
        return toAjax(mixMasterMaterialStockService.updateMixMasterMaterialStock(mixMasterMaterialStock));
    }

    /**
     * 删除母炼胶小料库存
     */
    @Log(title = "ui.data.column.masterMaterialStock.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除母炼胶小料库存")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(mixMasterMaterialStockService.deleteMixMasterMaterialStockByIds(ids));
    }

    /**
     * 导出母炼胶小料库存列表
     */
    @Log(title = "ui.data.column.masterMaterialStock.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出母炼胶小料库存列表")
    @PostMapping("/getList")
    public List<MixMasterMaterialStock> getList(@RequestBody MixMasterMaterialStock mixMasterMaterialStock){
        startPage();
        mixMasterMaterialStock.setOrderStr(orderStr());
        return  mixMasterMaterialStockService.selectMixMasterMaterialStockList(mixMasterMaterialStock);
    }

    /**
     * 校验母炼胶小料库存唯一性
     */
    @ApiOperation("校验母炼胶小料库存唯一性")
    @PostMapping("/checkMixMasterMaterialStockUnique")
    public String checkMixMasterMaterialStockUnique(@RequestBody MixMasterMaterialStock mixMasterMaterialStock){
        return mixMasterMaterialStockService.checkMixMasterMaterialStockUnique(mixMasterMaterialStock);
    }

    /**
     * 根据集合导入母炼胶小料库存数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.masterMaterialStock.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入母炼胶小料库存数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MixMasterMaterialStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return mixMasterMaterialStockService.importData(list, updateSupport, importLogId);
    }
}
