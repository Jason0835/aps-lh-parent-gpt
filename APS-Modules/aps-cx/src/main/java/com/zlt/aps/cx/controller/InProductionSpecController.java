package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.InProductionSpec;
import com.zlt.aps.cx.service.InProductionSpecService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型机台当前生产规格Controller
 *
 * @author chen
 * @date 2022-02-25
 */
@RestController
@RequestMapping("/inProductionSpec")
public class InProductionSpecController extends BaseController {
    @Autowired
    private InProductionSpecService inProductionSpecService;

    /**
     * 查询成型机台当前生产规格列表
     */
    @ApiOperation("查询成型机台当前生产规格列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody InProductionSpec inProductionSpec) {
        inProductionSpec.setOrderStr(orderStr());
        List<InProductionSpec> list = inProductionSpecService.selectInProductionSpecList(inProductionSpec);
        return getDataTable(list);
    }

    /**
     * 获取成型机台当前生产规格详细信息
     */
    @ApiOperation("获取成型机台当前生产规格详细信息")
    @GetMapping(value = "/{id}")
    public InProductionSpec getInfo(@PathVariable("id") Long id) {
        return inProductionSpecService.selectInProductionSpecById(id);
    }

    /**
     * 新增成型机台当前生产规格
     */
    @Log(title = "ui.data.column.inProductionSpec.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增成型机台当前生产规格")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody InProductionSpec inProductionSpec) {
        return toAjax(inProductionSpecService.insertInProductionSpec(inProductionSpec));
    }

    /**
     * 修改成型机台当前生产规格
     */
    @Log(title = "ui.data.column.inProductionSpec.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改成型机台当前生产规格")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody InProductionSpec inProductionSpec) {
        return toAjax(inProductionSpecService.updateInProductionSpec(inProductionSpec));
    }

    /**
     * 删除成型机台当前生产规格
     */
    @Log(title = "ui.data.column.inProductionSpec.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除成型机台当前生产规格")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(inProductionSpecService.deleteInProductionSpecByIds(ids));
    }

    /**
     * 导出成型机台当前生产规格列表
     */
    @Log(title = "ui.data.column.inProductionSpec.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出成型机台当前生产规格列表")
    @PostMapping("/getList")
    public List<InProductionSpec> getList(@RequestBody InProductionSpec inProductionSpec) {
        inProductionSpec.setOrderStr(orderStr());
        return inProductionSpecService.selectInProductionSpecList(inProductionSpec);
    }

    /**
     * 校验成型机台当前生产规格唯一性
     */
    @ApiOperation("校验成型机台当前生产规格唯一性")
    @PostMapping("/checkInProductionSpecUnique")
    public String checkInProductionSpecUnique(@RequestBody InProductionSpec inProductionSpec) {
        return inProductionSpecService.checkInProductionSpecUnique(inProductionSpec);
    }

    /**
     * 根据集合导入成型机台当前生产规格数据
     *
     * @param list          集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.inProductionSpec.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入成型机台当前生产规格数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<InProductionSpec> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return inProductionSpecService.importData(list, updateSupport, importLogId);
    }
}
