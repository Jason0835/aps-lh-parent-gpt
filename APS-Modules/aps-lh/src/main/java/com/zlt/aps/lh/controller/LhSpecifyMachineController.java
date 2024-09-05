package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.LhSpecifyMachine;
import com.zlt.aps.lh.service.LhSpecifyMachineService;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 硫化定点机台信息Controller
 *
 * @author zlt
 * @date 2021-07-21
 */
@RestController
@RequestMapping("/lhSpecifyMachine")
public class LhSpecifyMachineController extends BaseController {
    @Autowired
    private LhSpecifyMachineService lhSpecifyMachineService;

    /**
     * 查询硫化定点机台信息列表
     */
    @ApiOperation("查询硫化定点机台信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody LhSpecifyMachine lhSpecifyMachine) {
        startPage();
        lhSpecifyMachine.setOrderStr(orderStr());
        List<LhSpecifyMachine> list = lhSpecifyMachineService.selectLhSpecifyMachineList(lhSpecifyMachine);
        return getDataTable(list);
    }

    /**
     * 获取硫化定点机台信息详细信息
     */
    @ApiOperation("获取硫化定点机台信息详细信息")
    @GetMapping(value = "/{id}")
    public LhSpecifyMachine getInfo(@PathVariable("id") Long id) {
        return lhSpecifyMachineService.selectLhSpecifyMachineById(id);
    }

    /**
     * 新增硫化定点机台信息
     */
    @Log(title = "ui.data.lh.column.machine.info", businessType = BusinessType.INSERT)
    @ApiOperation("新增硫化定点机台信息")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody LhSpecifyMachine lhSpecifyMachine) {
        return toAjax(lhSpecifyMachineService.insertLhSpecifyMachine(lhSpecifyMachine));
    }

    /**
     * 修改硫化定点机台信息
     */
    @Log(title = "ui.data.lh.column.machine.info", businessType = BusinessType.UPDATE)
    @ApiOperation("修改硫化定点机台信息")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody LhSpecifyMachine lhSpecifyMachine) {
        return toAjax(lhSpecifyMachineService.updateLhSpecifyMachine(lhSpecifyMachine));
    }

    /**
     * 删除硫化定点机台信息
     */
    @Log(title = "ui.data.lh.column.machine.info", businessType = BusinessType.DELETE)
    @ApiOperation("删除硫化定点机台信息")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(lhSpecifyMachineService.deleteLhSpecifyMachineByIds(ids));
    }

    /**
     * 导出硫化定点机台信息列表
     */
    @Log(title = "ui.data.lh.column.machine.info", businessType = BusinessType.EXPORT)
    @ApiOperation("导出硫化定点机台信息列表")
    @PostMapping("/getList")
    public List<LhSpecifyMachine> getList(@RequestBody LhSpecifyMachine lhSpecifyMachine) {
        startPage();
        lhSpecifyMachine.setOrderStr(orderStr());
        return lhSpecifyMachineService.selectLhSpecifyMachineList(lhSpecifyMachine);
    }

    /**
     * 校验硫化定点机台信息唯一性
     */
    @ApiOperation("校验硫化定点机台信息唯一性")
    @PostMapping("/checkLhSpecifyMachineUnique")
    public String checkLhSpecifyMachineUnique(@RequestBody LhSpecifyMachine lhSpecifyMachine) {
        return lhSpecifyMachineService.checkLhSpecifyMachineUnique(lhSpecifyMachine);
    }

    @Log(title = "ui.data.lh.column.machine.info", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<LhSpecifyMachine> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return lhSpecifyMachineService.importData(list, updateSupport, importLogId);
    }
}
