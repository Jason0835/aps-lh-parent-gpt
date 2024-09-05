package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.LhInProductionSpec;
import com.zlt.aps.lh.service.LhInProductionSpecService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 硫化机台当前生产规格Controller
 *
 * @author chen
 * @date 2022-03-23
 */
@RestController
@RequestMapping("/inProductionSpec")
public class LhInProductionSpecController extends BaseController {
    @Autowired
    private LhInProductionSpecService lhInProductionSpecService;

    /**
     * 查询硫化机台当前生产规格列表
     */
    @ApiOperation("查询硫化机台当前生产规格列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody LhInProductionSpec lhInProductionSpec) {
        startPage();
        lhInProductionSpec.setOrderStr(orderStr());
        List<LhInProductionSpec> list = lhInProductionSpecService.selectLhInProductionSpecList(lhInProductionSpec);
        return getDataTable(list);
    }

    /**
     * 获取硫化机台当前生产规格详细信息
     */
    @ApiOperation("获取硫化机台当前生产规格详细信息")
    @GetMapping(value = "/{id}")
    public LhInProductionSpec getInfo(@PathVariable("id") Long id) {
        return lhInProductionSpecService.selectLhInProductionSpecById(id);
    }

    /**
     * 新增硫化机台当前生产规格
     */
    @Log(title = "ui.data.column.inProductionSpec.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增硫化机台当前生产规格")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody LhInProductionSpec lhInProductionSpec) {
        return toAjax(lhInProductionSpecService.insertLhInProductionSpec(lhInProductionSpec));
    }

    /**
     * 修改硫化机台当前生产规格
     */
    @Log(title = "ui.data.column.inProductionSpec.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改硫化机台当前生产规格")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody LhInProductionSpec lhInProductionSpec) {
        return toAjax(lhInProductionSpecService.updateLhInProductionSpec(lhInProductionSpec));
    }

    /**
     * 删除硫化机台当前生产规格
     */
    @Log(title = "ui.data.column.inProductionSpec.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除硫化机台当前生产规格")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(lhInProductionSpecService.deleteLhInProductionSpecByIds(ids));
    }

    /**
     * 导出硫化机台当前生产规格列表
     */
    @Log(title = "ui.data.column.inProductionSpec.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出硫化机台当前生产规格列表")
    @PostMapping("/getList")
    public List<LhInProductionSpec> getList(@RequestBody LhInProductionSpec lhInProductionSpec) {
        startPage();
        lhInProductionSpec.setOrderStr(orderStr());
        return lhInProductionSpecService.selectLhInProductionSpecList(lhInProductionSpec);
    }

    /**
     * 校验硫化机台当前生产规格唯一性
     */
    @ApiOperation("校验硫化机台当前生产规格唯一性")
    @PostMapping("/checkLhInProductionSpecUnique")
    public String checkLhInProductionSpecUnique(@RequestBody LhInProductionSpec lhInProductionSpec) {
        return lhInProductionSpecService.checkLhInProductionSpecUnique(lhInProductionSpec);
    }

    /**
     * 根据集合导入硫化机台当前生产规格数据
     *
     * @param list          集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.inProductionSpec.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入硫化机台当前生产规格数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<LhInProductionSpec> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return lhInProductionSpecService.importData(list, updateSupport, importLogId);
    }
}
