package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.CxMachineGroupList;
import com.zlt.aps.cx.service.CxMachineGroupListService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组别机台列Controller
 *
 * @author zlt
 * @date 2021-12-16
 */
@RestController
@RequestMapping("/groupMachineList")
public class CxMachineGroupListController extends BaseController
{
    @Autowired
    private CxMachineGroupListService cxMachineGroupListService;

    /**
     * 查询组别机台列列表
     */
    @ApiOperation("查询组别机台列列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxMachineGroupList cxMachineGroupList)
    {
        startPage();
        cxMachineGroupList.setOrderStr(orderStr());
        List<CxMachineGroupList> list = cxMachineGroupListService.selectCxMachineGroupListList(cxMachineGroupList);
        return getDataTable(list);
    }

    /**
     * 获取组别机台列详细信息
     */
    @ApiOperation("获取组别机台列详细信息")
    @GetMapping(value = "/{id}")
    public CxMachineGroupList getInfo(@PathVariable("id") Long id){
        return cxMachineGroupListService.selectCxMachineGroupListById(id);
    }

    /**
     * 新增组别机台列
     */
    @Log(title = "ui.data.column.groupMachineList.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增组别机台列")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody CxMachineGroupList cxMachineGroupList){
        return toAjax(cxMachineGroupListService.insertCxMachineGroupList(cxMachineGroupList));
    }

    /**
     * 修改组别机台列
     */
    @Log(title = "ui.data.column.groupMachineList.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改组别机台列")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody CxMachineGroupList cxMachineGroupList){
        return toAjax(cxMachineGroupListService.updateCxMachineGroupList(cxMachineGroupList));
    }

    /**
     * 删除组别机台列
     */
    @Log(title = "ui.data.column.groupMachineList.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除组别机台列")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(cxMachineGroupListService.deleteCxMachineGroupListByIds(ids));
    }

    /**
     * 导出组别机台列列表
     */
    @Log(title = "ui.data.column.groupMachineList.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出组别机台列列表")
    @PostMapping("/getList")
    public List<CxMachineGroupList> getList(@RequestBody CxMachineGroupList cxMachineGroupList){
        startPage();
        cxMachineGroupList.setOrderStr(orderStr());
        return  cxMachineGroupListService.selectCxMachineGroupListList4MachineName(cxMachineGroupList);
    }

    /**
     * 校验组别机台列唯一性
     */
    @ApiOperation("校验组别机台列唯一性")
    @PostMapping("/checkCxMachineGroupListUnique")
    public List<CxMachineGroupList> checkCxMachineGroupListUnique(@RequestBody CxMachineGroupList cxMachineGroupList){
        return cxMachineGroupListService.checkCxMachineGroupListUnique(cxMachineGroupList);
    }

    /**
     * 根据集合导入组别机台列数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.groupMachineList.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入组别机台列数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxMachineGroupList> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cxMachineGroupListService.importData(list, updateSupport, importLogId);
    }
}
