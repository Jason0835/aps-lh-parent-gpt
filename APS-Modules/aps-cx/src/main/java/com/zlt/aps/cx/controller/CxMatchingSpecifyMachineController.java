package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.CxMatchingSpecifyMachine;
import com.zlt.aps.cx.api.domain.entity.CxMatchingSpecifyMachineList;
import com.zlt.aps.cx.service.CxMatchingSpecifyMachineService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 定点机台Controller
 *
 * @author zlt
 * @date 2021-06-11
 */
@Api(tags = "成型定点机台接口")
@RestController
@RequestMapping("/specifyMachine")
public class CxMatchingSpecifyMachineController extends BaseController {
    @Autowired
    private CxMatchingSpecifyMachineService tSpecifyMachineService;

    /**
     * 查询定点机台列表
     */
    @ApiOperation("根据条件查询成型定点机台信息")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxMatchingSpecifyMachine cxSpecifyMachine) {
        startPage();
        cxSpecifyMachine.setOrderStr(orderStr());
        List<CxMatchingSpecifyMachine> list = tSpecifyMachineService.selectTSpecifyMachineList(cxSpecifyMachine);
        return getDataTable(list);
    }

    /**
     * 获取定点机台详细信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation("根据id查询成型定点机台信息")
    @ApiImplicitParams({@ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")})
    public CxMatchingSpecifyMachine getInfo(@PathVariable("id") Long id) {
        return tSpecifyMachineService.selectTSpecifyMachineById(id);
    }

    /**
     * 新增定点机台
     */
    @Log(title = "ui.cx.specifyMachine.export.fileName", businessType = BusinessType.INSERT)
    @ApiOperation("新增成型定点机台信息（id不为空）")
    @PostMapping
    public AjaxResult add(@RequestBody CxMatchingSpecifyMachine cxSpecifyMachine) {
        //唯一性校验
        List<CxMatchingSpecifyMachine> list = tSpecifyMachineService.checkCxSpecifyMachineUnic(cxSpecifyMachine);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.specifyMachine.message.unique"));
        } else {
            return toAjax(tSpecifyMachineService.insertTSpecifyMachine(cxSpecifyMachine));
        }
    }

    /**
     * 修改定点机台
     */
    @Log(title = "ui.cx.specifyMachine.export.fileName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改成型定点机台信息（id不为空）")
    @PutMapping
    public AjaxResult edit(@RequestBody CxMatchingSpecifyMachine cxSpecifyMachine) {
        //唯一性校验
        List<CxMatchingSpecifyMachine> list = tSpecifyMachineService.checkCxSpecifyMachineUnic(cxSpecifyMachine);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.specifyMachine.message.unique"));
        } else {
            return toAjax(tSpecifyMachineService.updateTSpecifyMachine(cxSpecifyMachine));
        }
    }

    /**
     * 删除定点机台
     */
    @Log(title = "ui.cx.specifyMachine.export.fileName", businessType = BusinessType.DELETE)
    @ApiOperation("删除成型定点机台信息（id不为空）")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(tSpecifyMachineService.deleteTSpecifyMachineByIds(ids));
    }

    /**
     * 导出定点机台列表
     */
    @Log(title = "ui.cx.specifyMachine.export.fileName", businessType = BusinessType.EXPORT)
    @PostMapping("/exportList")
    public List<CxMatchingSpecifyMachine> exportList(@RequestBody CxMatchingSpecifyMachine cxSpecifyMachine) {
        startPage();
        cxSpecifyMachine.setOrderStr(orderStr());
        List<CxMatchingSpecifyMachine> list = tSpecifyMachineService.selectTSpecifyMachineList(cxSpecifyMachine);
        return list;
    }

    @Log(title = "ui.cx.specifyMachine.export.fileName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxMatchingSpecifyMachine> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tSpecifyMachineService.importData(list, updateSupport, importLogId);
    }

    @PostMapping("/getDetailById")
    public TableDataInfo getDetailById(@RequestBody CxMatchingSpecifyMachine cxSpecifyMachine) throws IOException {
        CxMatchingSpecifyMachine cxSpecifyMachine2 = tSpecifyMachineService.selectTSpecifyMachineById(cxSpecifyMachine.getId());
        List<CxMatchingSpecifyMachineList> list = cxSpecifyMachine2.getTSpecifyMachineListList();
        return getDataTable(list);
    }

    @PostMapping("/detailList")
    public TableDataInfo detailList(@RequestBody CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList) throws IOException {
        startPage();
        cxMatchingSpecifyMachineList.setOrderStr(orderStr());
        List<CxMatchingSpecifyMachineList> list = tSpecifyMachineService.detailList(cxMatchingSpecifyMachineList);
        if (CollectionUtils.isEmpty(list)) {
            list = new ArrayList<CxMatchingSpecifyMachineList>();
        }
        return getDataTable(list);
    }

    @GetMapping(value = "/detail/{id}")
    @ApiOperation("根据id查询成型定点机台信息")
    public CxMatchingSpecifyMachineList getDetailInfo(@PathVariable("id") Long id) {
        return tSpecifyMachineService.selectCxSpecifyMachineListById(id);
    }

    /**
     * 新增定点机台
     */
    @Log(title = "ui.cx.specifyMachine.detailExport.fileName", businessType = BusinessType.INSERT)
    @PostMapping("/detail/add")
    public AjaxResult detailAdd(@RequestBody CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList) {
        //唯一性校验
        List<CxMatchingSpecifyMachineList> list = tSpecifyMachineService.checkCxSpecifyMachineDetailUnic(cxMatchingSpecifyMachineList);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.specifyMachine.detail.message.unique"));
        } else {
            return toAjax(tSpecifyMachineService.detailAdd(cxMatchingSpecifyMachineList));
        }
    }

    /**
     * 修改定点机台
     */
    @Log(title = "ui.cx.specifyMachine.detailExport.fileName", businessType = BusinessType.UPDATE)
    @PostMapping("/detail/edit")
    public AjaxResult detailEdit(@RequestBody CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList) {
        //唯一性校验
        List<CxMatchingSpecifyMachineList> list = tSpecifyMachineService.checkCxSpecifyMachineDetailUnic(cxMatchingSpecifyMachineList);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.specifyMachine.detail.message.unique"));
        } else {
            return toAjax(tSpecifyMachineService.detailEdit(cxMatchingSpecifyMachineList));
        }
    }

    @Log(title = "ui.cx.specifyMachine.detailExport.fileName", businessType = BusinessType.DELETE)
    @DeleteMapping("detail/{ids}")
    public AjaxResult detailRemove(@PathVariable Long[] ids) {
        return toAjax(tSpecifyMachineService.deleteDetailByIds(ids));
    }

    @Log(title = "ui.cx.specifyMachine.detailExport.fileName", businessType = BusinessType.EXPORT)
    @PostMapping("detail/exportList")
    public List<CxMatchingSpecifyMachineList> detailExportList(@RequestBody CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList) {
        startPage();
        cxMatchingSpecifyMachineList.setOrderStr(orderStr());
        List<CxMatchingSpecifyMachineList> list = tSpecifyMachineService.detailList(cxMatchingSpecifyMachineList);
        return list;
    }

    @Log(title = "ui.cx.specifyMachine.detailExport.fileName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("detail/detailImportData")
    public AjaxResult detailImportData(@RequestBody List<CxMatchingSpecifyMachineList> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tSpecifyMachineService.detailImportData(list, updateSupport, importLogId);
    }

    @PostMapping("detail/viewList")
    public List<CxMatchingSpecifyMachineList> viewList(@RequestBody CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList) {
        List<CxMatchingSpecifyMachineList> list = tSpecifyMachineService.viewList(cxMatchingSpecifyMachineList);
        return list;
    }


}
