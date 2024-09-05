package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.CxMachineGroup;
import com.zlt.aps.cx.api.domain.entity.CxMachineGroupForExcel;
import com.zlt.aps.cx.api.domain.entity.CxMachineGroupList;
import com.zlt.aps.cx.service.CxMachineGroupService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * 成型机组Controller
 *
 * @author zlt
 * @date 2021-12-16
 */
@RestController
@RequestMapping("/machineGroup")
public class CxMachineGroupController extends BaseController
{
    @Autowired
    private CxMachineGroupService cxMachineGroupService;

    /**
     * 查询成型机组列表
     */
    @ApiOperation("查询成型机组列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxMachineGroup cxMachineGroup)
    {
        startPage();
        cxMachineGroup.setOrderStr(orderStr());
        List<CxMachineGroup> list = cxMachineGroupService.selectCxMachineGroupList(cxMachineGroup);
        return getDataTable(list);
    }

    /**
     * 获取成型机组详细信息
     */
    @ApiOperation("获取成型机组详细信息")
    @GetMapping(value = "/{id}")
    public CxMachineGroup getInfo(@PathVariable("id") Long id){
        return cxMachineGroupService.selectCxMachineGroupById(id);
    }

    /**
     * 新增成型机组
     */
    @Log(title = "ui.data.column.machineGroup.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增成型机组")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody CxMachineGroup cxMachineGroup){
        return toAjax(cxMachineGroupService.insertCxMachineGroup(cxMachineGroup));
    }

    /**
     * 修改成型机组
     */
    @Log(title = "ui.data.column.machineGroup.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改成型机组")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody CxMachineGroup cxMachineGroup){
        return toAjax(cxMachineGroupService.updateCxMachineGroup(cxMachineGroup));
    }

    /**
     * 删除成型机组
     */
    @Log(title = "ui.data.column.machineGroup.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除成型机组")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(cxMachineGroupService.deleteCxMachineGroupByIds(ids));
    }

    /**
     * 导出成型机组列表
     */
    @Log(title = "ui.data.column.machineGroup.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出成型机组列表")
    @PostMapping("/selectCxMachineGroup4Excel")
    public List<CxMachineGroupForExcel> selectCxMachineGroup4Excel(@RequestBody CxMachineGroup cxMachineGroup){
        startPage();
        cxMachineGroup.setOrderStr(orderStr());
        return  cxMachineGroupService.selectCxMachineGroup4Excel(cxMachineGroup);
    }

    /**
     * 校验成型机组唯一性
     */
    @ApiOperation("校验成型机组唯一性")
    @PostMapping("/checkCxMachineGroupUnique")
    public String checkCxMachineGroupUnique(@RequestBody CxMachineGroup cxMachineGroup){
        return cxMachineGroupService.checkCxMachineGroupUnique(cxMachineGroup);
    }

    /**
     * 根据集合导入成型机组数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.machineGroup.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入成型机组数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxMachineGroupForExcel> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cxMachineGroupService.importData(list, updateSupport, importLogId);
    }

    @PostMapping("/getDetailById")
    public TableDataInfo getDetailById(@RequestBody CxMachineGroup cxMachineGroup) throws IOException {
        CxMachineGroup cxMachineGroup2 = cxMachineGroupService.selectCxMachineGroupById(cxMachineGroup.getId());
        List<CxMachineGroupList> list = cxMachineGroup2.getCxMachineGroupListList();
        return getDataTable(list);
    }
}
