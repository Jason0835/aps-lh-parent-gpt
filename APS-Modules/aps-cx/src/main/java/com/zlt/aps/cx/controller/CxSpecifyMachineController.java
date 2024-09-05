package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.CxSpecifyMachine;
import com.zlt.aps.cx.service.CxSpecifyMachineService;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 定点机台Controller
 *
 * @author zlt
 * @date 2021-07-21
 */
@RestController
@RequestMapping("/cxSpecifyMachine")
public class CxSpecifyMachineController extends BaseController {
    @Autowired
    private CxSpecifyMachineService cxSpecifyMachine1Service;

    /**
     * 查询定点机台列表
     */
    //@PreAuthorize(hasPermi = "cx:cxSpecifyMachine:list")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxSpecifyMachine cxSpecifyMachine) {
        startPage();
        cxSpecifyMachine.setOrderStr(orderStr());
        List<CxSpecifyMachine> list = cxSpecifyMachine1Service.selectCxSpecifyMachine1List(cxSpecifyMachine);
        return getDataTable(list);
    }

    /**
     * 获取定点机台详细信息
     */
    //@PreAuthorize(hasPermi = "cx:cxSpecifyMachine:query")
    @GetMapping(value = "/{id}")
    public CxSpecifyMachine getInfo(@PathVariable("id") Long id) {
        return cxSpecifyMachine1Service.selectCxSpecifyMachine1ById(id);
    }

    /**
     * 新增定点机台
     */
    //@PreAuthorize(hasPermi = "cx:cxSpecifyMachine:add")
    @Log(title = "ui.cx.cxSpecifyMachine.export.fileName", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody CxSpecifyMachine cxSpecifyMachine) {
        return toAjax(cxSpecifyMachine1Service.insertCxSpecifyMachine1(cxSpecifyMachine));
    }

    /**
     * 修改定点机台
     */
    //@PreAuthorize(hasPermi = "cx:cxSpecifyMachine:edit")
    @Log(title = "ui.cx.cxSpecifyMachine.export.fileName", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody CxSpecifyMachine cxSpecifyMachine) {
        return toAjax(cxSpecifyMachine1Service.updateCxSpecifyMachine1(cxSpecifyMachine));
    }

    /**
     * 删除定点机台
     */
    //@PreAuthorize(hasPermi = "cx:cxSpecifyMachine:remove")
    @Log(title = "ui.cx.cxSpecifyMachine.export.fileName", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cxSpecifyMachine1Service.deleteCxSpecifyMachine1ByIds(ids));
    }

    /**
     * 导出定点机台列表
     */
    //@PreAuthorize(hasPermi = "cx:cxSpecifyMachine:export")
    @Log(title = "ui.cx.cxSpecifyMachine.export.fileName", businessType = BusinessType.EXPORT)
    @PostMapping("/getList")
    public List<CxSpecifyMachine> getList(@RequestBody CxSpecifyMachine cxSpecifyMachine) {
        startPage();
        cxSpecifyMachine.setOrderStr(orderStr());
        return cxSpecifyMachine1Service.selectCxSpecifyMachine1List(cxSpecifyMachine);
    }

    /**
     * 校验定点机台唯一性
     */
    @ApiOperation("校验定点机台唯一性")
    @PostMapping("/checkCxSpecifyMachine1Unique")
    public String checkCxSpecifyMachine1Unique(@RequestBody CxSpecifyMachine cxSpecifyMachine) {
        return cxSpecifyMachine1Service.checkCxSpecifyMachine1Unique(cxSpecifyMachine);
    }

    @Log(title = "ui.cx.cxSpecifyMachine.export.fileName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxSpecifyMachine> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cxSpecifyMachine1Service.importData(list, updateSupport, importLogId);
    }
}
