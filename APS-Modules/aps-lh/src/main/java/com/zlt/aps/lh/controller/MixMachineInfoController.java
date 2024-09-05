package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.MixMachineInfo;
import com.zlt.aps.lh.service.MixMachineInfoService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 密炼机台信息Controller
 *
 * @author zlt
 * @date 2021-11-09
 */
@RestController
@RequestMapping("/mixMachine")
public class MixMachineInfoController extends BaseController
{
    @Autowired
    private MixMachineInfoService mixMachineInfoService;

    /**
     * 查询密炼机台信息列表
     */
    @ApiOperation("查询密炼机台信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MixMachineInfo mixMachineInfo)
    {
        startPage();
        mixMachineInfo.setOrderStr(orderStr());
        List<MixMachineInfo> list = mixMachineInfoService.selectMixMachineInfoList(mixMachineInfo);
        return getDataTable(list);
    }

    /**
     * 获取密炼机台信息详细信息
     */
    @ApiOperation("获取密炼机台信息详细信息")
    @GetMapping(value = "/{id}")
    public MixMachineInfo getInfo(@PathVariable("id") Long id){
        return mixMachineInfoService.selectMixMachineInfoById(id);
    }

    /**
     * 新增密炼机台信息
     */
    @Log(title = "ui.data.column.mixMachine.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增密炼机台信息")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MixMachineInfo mixMachineInfo){
        return toAjax(mixMachineInfoService.insertMixMachineInfo(mixMachineInfo));
    }

    /**
     * 修改密炼机台信息
     */
    @Log(title = "ui.data.column.mixMachine.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改密炼机台信息")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MixMachineInfo mixMachineInfo){
        return toAjax(mixMachineInfoService.updateMixMachineInfo(mixMachineInfo));
    }

    /**
     * 删除密炼机台信息
     */
    @Log(title = "ui.data.column.mixMachine.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除密炼机台信息")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(mixMachineInfoService.deleteMixMachineInfoByIds(ids));
    }

    /**
     * 导出密炼机台信息列表
     */
    @Log(title = "ui.data.column.mixMachine.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出密炼机台信息列表")
    @PostMapping("/getList")
    public List<MixMachineInfo> getList(@RequestBody MixMachineInfo mixMachineInfo){
        startPage();
        mixMachineInfo.setOrderStr(orderStr());
        return  mixMachineInfoService.selectMixMachineInfoList(mixMachineInfo);
    }

    /**
     * 校验密炼机台信息唯一性
     */
    @ApiOperation("校验密炼机台信息唯一性")
    @PostMapping("/checkMixMachineInfoUnique")
    public String checkMixMachineInfoUnique(@RequestBody MixMachineInfo mixMachineInfo){
        return mixMachineInfoService.checkMixMachineInfoUnique(mixMachineInfo);
    }

    /**
     * 根据集合导入密炼机台信息数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.mixMachine.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入密炼机台信息数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MixMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return mixMachineInfoService.importData(list, updateSupport, importLogId);
    }
}
