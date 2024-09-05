package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.MixReturnRubberStock;
import com.zlt.aps.lh.service.MixReturnRubberStockService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 返回胶库存Controller
 *
 * @author zlt
 * @date 2021-11-09
 */
@RestController
@RequestMapping("/returnRubberStock")
public class MixReturnRubberStockController extends BaseController
{
    @Autowired
    private MixReturnRubberStockService mixReturnRubberStockService;

    /**
     * 查询返回胶库存列表
     */
    @ApiOperation("查询返回胶库存列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MixReturnRubberStock mixReturnRubberStock)
    {
        startPage();
        mixReturnRubberStock.setOrderStr(orderStr());
        List<MixReturnRubberStock> list = mixReturnRubberStockService.selectMixReturnRubberStockList(mixReturnRubberStock);
        return getDataTable(list);
    }

    /**
     * 获取返回胶库存详细信息
     */
    @ApiOperation("获取返回胶库存详细信息")
    @GetMapping(value = "/{id}")
    public MixReturnRubberStock getInfo(@PathVariable("id") Long id){
        return mixReturnRubberStockService.selectMixReturnRubberStockById(id);
    }

    /**
     * 新增返回胶库存
     */
    @Log(title = "ui.data.column.returnRubberStock.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增返回胶库存")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MixReturnRubberStock mixReturnRubberStock){
        return toAjax(mixReturnRubberStockService.insertMixReturnRubberStock(mixReturnRubberStock));
    }

    /**
     * 修改返回胶库存
     */
    @Log(title = "ui.data.column.returnRubberStock.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改返回胶库存")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MixReturnRubberStock mixReturnRubberStock){
        return toAjax(mixReturnRubberStockService.updateMixReturnRubberStock(mixReturnRubberStock));
    }

    /**
     * 删除返回胶库存
     */
    @Log(title = "ui.data.column.returnRubberStock.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除返回胶库存")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(mixReturnRubberStockService.deleteMixReturnRubberStockByIds(ids));
    }

    /**
     * 导出返回胶库存列表
     */
    @Log(title = "ui.data.column.returnRubberStock.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出返回胶库存列表")
    @PostMapping("/getList")
    public List<MixReturnRubberStock> getList(@RequestBody MixReturnRubberStock mixReturnRubberStock){
        startPage();
        mixReturnRubberStock.setOrderStr(orderStr());
        return  mixReturnRubberStockService.selectMixReturnRubberStockList(mixReturnRubberStock);
    }

    /**
     * 校验返回胶库存唯一性
     */
    @ApiOperation("校验返回胶库存唯一性")
    @PostMapping("/checkMixReturnRubberStockUnique")
    public String checkMixReturnRubberStockUnique(@RequestBody MixReturnRubberStock mixReturnRubberStock){
        return mixReturnRubberStockService.checkMixReturnRubberStockUnique(mixReturnRubberStock);
    }

    /**
     * 根据集合导入返回胶库存数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.returnRubberStock.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入返回胶库存数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MixReturnRubberStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return mixReturnRubberStockService.importData(list, updateSupport, importLogId);
    }
}
