package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.MixBadRubberStock;
import com.zlt.aps.lh.service.MixBadRubberStockService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 不合格胶库存Controller
 *
 * @author zlt
 * @date 2021-11-08
 */
@RestController
@RequestMapping("/badStock")
public class MixBadRubberStockController extends BaseController {
    @Autowired
    private MixBadRubberStockService mixBadRubberStockService;

    /**
     * 查询不合格胶库存列表
     */
    @ApiOperation("查询不合格胶库存列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MixBadRubberStock mixBadRubberStock) {
        startPage();
        mixBadRubberStock.setOrderStr(orderStr());
        List<MixBadRubberStock> list = mixBadRubberStockService.selectMixBadRubberStockList(mixBadRubberStock);
        return getDataTable(list);
    }

    /**
     * 获取不合格胶库存详细信息
     */
    @ApiOperation("获取不合格胶库存详细信息")
    @GetMapping(value = "/{id}")
    public MixBadRubberStock getInfo(@PathVariable("id") Long id) {
        return mixBadRubberStockService.selectMixBadRubberStockById(id);
    }

    /**
     * 新增不合格胶库存
     */
    @Log(title = "ui.data.column.badStock.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增不合格胶库存")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MixBadRubberStock mixBadRubberStock) {
        return toAjax(mixBadRubberStockService.insertMixBadRubberStock(mixBadRubberStock));
    }

    /**
     * 修改不合格胶库存
     */
    @Log(title = "ui.data.column.badStock.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改不合格胶库存")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MixBadRubberStock mixBadRubberStock) {
        return toAjax(mixBadRubberStockService.updateMixBadRubberStock(mixBadRubberStock));
    }

    /**
     * 删除不合格胶库存
     */
    @Log(title = "ui.data.column.badStock.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除不合格胶库存")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(mixBadRubberStockService.deleteMixBadRubberStockByIds(ids));
    }

    /**
     * 导出不合格胶库存列表
     */
    @Log(title = "ui.data.column.badStock.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出不合格胶库存列表")
    @PostMapping("/getList")
    public List<MixBadRubberStock> getList(@RequestBody MixBadRubberStock mixBadRubberStock) {
        startPage();
        mixBadRubberStock.setOrderStr(orderStr());
        return mixBadRubberStockService.selectMixBadRubberStockList(mixBadRubberStock);
    }

    /**
     * 校验不合格胶库存唯一性
     */
    @ApiOperation("校验不合格胶库存唯一性")
    @PostMapping("/checkMixBadRubberStockUnique")
    public String checkMixBadRubberStockUnique(@RequestBody MixBadRubberStock mixBadRubberStock) {
        return mixBadRubberStockService.checkMixBadRubberStockUnique(mixBadRubberStock);
    }

    /**
     * 根据集合导入不合格胶库存数据
     *
     * @param list          集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.badStock.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入不合格胶库存数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MixBadRubberStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return mixBadRubberStockService.importData(list, updateSupport, importLogId);
    }
}
