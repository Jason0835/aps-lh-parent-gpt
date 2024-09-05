package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.MidNightShiftFinish;
import com.zlt.aps.cx.service.MidNightShiftFinishService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型排程中夜班完成量Controller
 *
 * @author chen
 * @date 2022-02-25
 */
@RestController
@RequestMapping("/midNightFinish")
public class MidNightShiftFinishController extends BaseController {
    @Autowired
    private MidNightShiftFinishService midNightShiftFinishService;

    /**
     * 查询成型排程中夜班完成量列表
     */
    @ApiOperation("查询成型排程中夜班完成量列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MidNightShiftFinish midNightShiftFinish) {
        midNightShiftFinish.setOrderStr(orderStr());
        List<MidNightShiftFinish> list = midNightShiftFinishService.selectMidNightShiftFinishList(midNightShiftFinish);
        return getDataTable(list);
    }

    /**
     * 获取成型排程中夜班完成量详细信息
     */
    @ApiOperation("获取成型排程中夜班完成量详细信息")
    @GetMapping(value = "/{id}")
    public MidNightShiftFinish getInfo(@PathVariable("id") Long id) {
        return midNightShiftFinishService.selectMidNightShiftFinishById(id);
    }

    /**
     * 新增成型排程中夜班完成量
     */
    @Log(title = "ui.data.column.midNightFinish.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增成型排程中夜班完成量")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MidNightShiftFinish midNightShiftFinish) {
        return toAjax(midNightShiftFinishService.insertMidNightShiftFinish(midNightShiftFinish));
    }

    /**
     * 修改成型排程中夜班完成量
     */
    @Log(title = "ui.data.column.midNightFinish.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改成型排程中夜班完成量")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MidNightShiftFinish midNightShiftFinish) {
        return toAjax(midNightShiftFinishService.updateMidNightShiftFinish(midNightShiftFinish));
    }

    /**
     * 删除成型排程中夜班完成量
     */
    @Log(title = "ui.data.column.midNightFinish.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除成型排程中夜班完成量")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(midNightShiftFinishService.deleteMidNightShiftFinishByIds(ids));
    }

    /**
     * 导出成型排程中夜班完成量列表
     */
    @Log(title = "ui.data.column.midNightFinish.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出成型排程中夜班完成量列表")
    @PostMapping("/getList")
    public List<MidNightShiftFinish> getList(@RequestBody MidNightShiftFinish midNightShiftFinish) {
        midNightShiftFinish.setOrderStr(orderStr());
        return midNightShiftFinishService.selectMidNightShiftFinishList(midNightShiftFinish);
    }

    /**
     * 校验成型排程中夜班完成量唯一性
     */
    @ApiOperation("校验成型排程中夜班完成量唯一性")
    @PostMapping("/checkMidNightShiftFinishUnique")
    public String checkMidNightShiftFinishUnique(@RequestBody MidNightShiftFinish midNightShiftFinish) {
        return midNightShiftFinishService.checkMidNightShiftFinishUnique(midNightShiftFinish);
    }

    /**
     * 根据集合导入成型排程中夜班完成量数据
     *
     * @param list          集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.midNightFinish.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入成型排程中夜班完成量数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MidNightShiftFinish> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return midNightShiftFinishService.importData(list, updateSupport, importLogId);
    }
}
