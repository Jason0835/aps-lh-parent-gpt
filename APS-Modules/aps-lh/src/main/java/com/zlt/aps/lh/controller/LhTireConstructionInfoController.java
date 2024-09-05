package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.LhTireConstructionInfo;
import com.zlt.aps.lh.service.LhTireConstructionInfoService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 硫化外胎施工信息Controller
 *
 * @author zlt
 * @date 2021-11-15
 */
@RestController
@RequestMapping("/lhTireConstructionInfo")
public class LhTireConstructionInfoController extends BaseController
{
    @Autowired
    private LhTireConstructionInfoService lhTireConstructionInfoService;

    /**
     * 查询硫化外胎施工信息列表
     */
    @ApiOperation("查询硫化外胎施工信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody LhTireConstructionInfo lhTireConstructionInfo)
    {
        startPage();
        lhTireConstructionInfo.setOrderStr(orderStr());
        List<LhTireConstructionInfo> list = lhTireConstructionInfoService.selectLhTireConstructionInfoList(lhTireConstructionInfo);
        return getDataTable(list);
    }

    /**
     * 获取硫化外胎施工信息详细信息
     */
    @ApiOperation("获取硫化外胎施工信息详细信息")
    @GetMapping(value = "/{id}")
    public LhTireConstructionInfo getInfo(@PathVariable("id") Long id){
        return lhTireConstructionInfoService.selectLhTireConstructionInfoById(id);
    }

    /**
     * 新增硫化外胎施工信息
     */
    @Log(title = "ui.data.column.lhTireConstructionInfo.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增硫化外胎施工信息")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody LhTireConstructionInfo lhTireConstructionInfo){
        return toAjax(lhTireConstructionInfoService.insertLhTireConstructionInfo(lhTireConstructionInfo));
    }

    /**
     * 修改硫化外胎施工信息
     */
    @Log(title = "ui.data.column.lhTireConstructionInfo.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改硫化外胎施工信息")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody LhTireConstructionInfo lhTireConstructionInfo){
        return toAjax(lhTireConstructionInfoService.updateLhTireConstructionInfo(lhTireConstructionInfo));
    }

    /**
     * 删除硫化外胎施工信息
     */
    @Log(title = "ui.data.column.lhTireConstructionInfo.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除硫化外胎施工信息")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(lhTireConstructionInfoService.deleteLhTireConstructionInfoByIds(ids));
    }

    /**
     * 导出硫化外胎施工信息列表
     */
    @Log(title = "ui.data.column.lhTireConstructionInfo.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出硫化外胎施工信息列表")
    @PostMapping("/getList")
    public List<LhTireConstructionInfo> getList(@RequestBody LhTireConstructionInfo lhTireConstructionInfo){
        startPage();
        lhTireConstructionInfo.setOrderStr(orderStr());
        return  lhTireConstructionInfoService.selectLhTireConstructionInfoList(lhTireConstructionInfo);
    }

    /**
     * 校验硫化外胎施工信息唯一性
     */
    @ApiOperation("校验硫化外胎施工信息唯一性")
    @PostMapping("/checkLhTireConstructionInfoUnique")
    public String checkLhTireConstructionInfoUnique(@RequestBody LhTireConstructionInfo lhTireConstructionInfo){
        return lhTireConstructionInfoService.checkLhTireConstructionInfoUnique(lhTireConstructionInfo);
    }

    /**
     * 根据集合导入硫化外胎施工信息数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.lhTireConstructionInfo.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入硫化外胎施工信息数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<LhTireConstructionInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return lhTireConstructionInfoService.importData(list, updateSupport, importLogId);
    }

    /**
     * 根据sap查询对应的胎胚代码
     * @param lhTireConstructionInfo sap品号
     * @return 查询到的胎胚代码
     */
    @ApiOperation("根据sap查询对应的胎胚代码")
    @PostMapping("/getEmbryoCodeListBySapCode")
    public List<LhTireConstructionInfo> getEmbryoCodeListBySapCode(@RequestBody LhTireConstructionInfo lhTireConstructionInfo) {
        return lhTireConstructionInfoService.getEmbryoCodeListBySapCode(lhTireConstructionInfo);
    }
}
