package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.MixFinalRubberStock;
import com.zlt.aps.lh.service.MixFinalRubberStockService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 终炼胶库存Controller
 *
 * @author zlt
 * @date 2021-11-09
 */
@RestController
@RequestMapping("/finalRubberStock")
public class MixFinalRubberStockController extends BaseController
{
    @Autowired
    private MixFinalRubberStockService mixFinalRubberStockService;

    /**
     * 查询终炼胶库存列表
     */
    @ApiOperation("查询终炼胶库存列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MixFinalRubberStock mixFinalRubberStock)
    {
        startPage();
        mixFinalRubberStock.setOrderStr(orderStr());
        List<MixFinalRubberStock> list = mixFinalRubberStockService.selectMixFinalRubberStockList(mixFinalRubberStock);
        return getDataTable(list);
    }

    /**
     * 获取终炼胶库存详细信息
     */
    @ApiOperation("获取终炼胶库存详细信息")
    @GetMapping(value = "/{id}")
    public MixFinalRubberStock getInfo(@PathVariable("id") Long id){
        return mixFinalRubberStockService.selectMixFinalRubberStockById(id);
    }

    /**
     * 新增终炼胶库存
     */
    @Log(title = "ui.data.column.finalRubberStock.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增终炼胶库存")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MixFinalRubberStock mixFinalRubberStock){
        return toAjax(mixFinalRubberStockService.insertMixFinalRubberStock(mixFinalRubberStock));
    }

    /**
     * 修改终炼胶库存
     */
    @Log(title = "ui.data.column.finalRubberStock.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改终炼胶库存")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MixFinalRubberStock mixFinalRubberStock){
        return toAjax(mixFinalRubberStockService.updateMixFinalRubberStock(mixFinalRubberStock));
    }

    /**
     * 删除终炼胶库存
     */
    @Log(title = "ui.data.column.finalRubberStock.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除终炼胶库存")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(mixFinalRubberStockService.deleteMixFinalRubberStockByIds(ids));
    }

    /**
     * 导出终炼胶库存列表
     */
    @Log(title = "ui.data.column.finalRubberStock.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出终炼胶库存列表")
    @PostMapping("/getList")
    public List<MixFinalRubberStock> getList(@RequestBody MixFinalRubberStock mixFinalRubberStock){
        startPage();
        mixFinalRubberStock.setOrderStr(orderStr());
        return  mixFinalRubberStockService.selectMixFinalRubberStockList(mixFinalRubberStock);
    }

    /**
     * 校验终炼胶库存唯一性
     */
    @ApiOperation("校验终炼胶库存唯一性")
    @PostMapping("/checkMixFinalRubberStockUnique")
    public String checkMixFinalRubberStockUnique(@RequestBody MixFinalRubberStock mixFinalRubberStock){
        return mixFinalRubberStockService.checkMixFinalRubberStockUnique(mixFinalRubberStock);
    }

    /**
     * 根据集合导入终炼胶库存数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.finalRubberStock.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入终炼胶库存数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MixFinalRubberStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return mixFinalRubberStockService.importData(list, updateSupport, importLogId);
    }
}
