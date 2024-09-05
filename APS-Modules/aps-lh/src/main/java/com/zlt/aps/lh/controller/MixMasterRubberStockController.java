package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.MixMasterRubberStock;
import com.zlt.aps.lh.service.MixMasterRubberStockService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 母炼胶库存Controller
 *
 * @author zlt
 * @date 2021-11-09
 */
@RestController
@RequestMapping("/masterRubberStock")
public class MixMasterRubberStockController extends BaseController
{
    @Autowired
    private MixMasterRubberStockService mixMasterRubberStockService;

    /**
     * 查询母炼胶库存列表
     */
    @ApiOperation("查询母炼胶库存列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MixMasterRubberStock mixMasterRubberStock)
    {
        startPage();
        mixMasterRubberStock.setOrderStr(orderStr());
        List<MixMasterRubberStock> list = mixMasterRubberStockService.selectMixMasterRubberStockList(mixMasterRubberStock);
        return getDataTable(list);
    }

    /**
     * 获取母炼胶库存详细信息
     */
    @ApiOperation("获取母炼胶库存详细信息")
    @GetMapping(value = "/{id}")
    public MixMasterRubberStock getInfo(@PathVariable("id") Long id){
        return mixMasterRubberStockService.selectMixMasterRubberStockById(id);
    }

    /**
     * 新增母炼胶库存
     */
    @Log(title = "ui.data.column.masterRubberStock.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增母炼胶库存")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MixMasterRubberStock mixMasterRubberStock){
        return toAjax(mixMasterRubberStockService.insertMixMasterRubberStock(mixMasterRubberStock));
    }

    /**
     * 修改母炼胶库存
     */
    @Log(title = "ui.data.column.masterRubberStock.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改母炼胶库存")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MixMasterRubberStock mixMasterRubberStock){
        return toAjax(mixMasterRubberStockService.updateMixMasterRubberStock(mixMasterRubberStock));
    }

    /**
     * 删除母炼胶库存
     */
    @Log(title = "ui.data.column.masterRubberStock.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除母炼胶库存")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(mixMasterRubberStockService.deleteMixMasterRubberStockByIds(ids));
    }

    /**
     * 导出母炼胶库存列表
     */
    @Log(title = "ui.data.column.masterRubberStock.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出母炼胶库存列表")
    @PostMapping("/getList")
    public List<MixMasterRubberStock> getList(@RequestBody MixMasterRubberStock mixMasterRubberStock){
        startPage();
        mixMasterRubberStock.setOrderStr(orderStr());
        return  mixMasterRubberStockService.selectMixMasterRubberStockList(mixMasterRubberStock);
    }

    /**
     * 校验母炼胶库存唯一性
     */
    @ApiOperation("校验母炼胶库存唯一性")
    @PostMapping("/checkMixMasterRubberStockUnique")
    public String checkMixMasterRubberStockUnique(@RequestBody MixMasterRubberStock mixMasterRubberStock){
        return mixMasterRubberStockService.checkMixMasterRubberStockUnique(mixMasterRubberStock);
    }

    /**
     * 根据集合导入母炼胶库存数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.masterRubberStock.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入母炼胶库存数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MixMasterRubberStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return mixMasterRubberStockService.importData(list, updateSupport, importLogId);
    }
}
