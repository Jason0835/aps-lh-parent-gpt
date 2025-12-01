package com.zlt.mix.setting.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.mix.setting.api.domain.entity.FactoryGlueAreaRelation;
import com.zlt.mix.setting.service.FactoryGlueAreaRelationService;

import io.swagger.annotations.ApiOperation;

/**
 * 分厂胶料与密炼区对应关系Controller
 *
 * @author zlt
 * @date 2022-11-22
 */
@RestController
@RequestMapping("/factoryGlueAreaRelation")
public class FactoryGlueAreaRelationController extends BaseController
{
    @Autowired
    private FactoryGlueAreaRelationService factoryGlueAreaRelationService;

    /**
     * 查询分厂胶料与密炼区对应关系列表
     */
    @ApiOperation("查询分厂胶料与密炼区对应关系列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody FactoryGlueAreaRelation tFactoryGlueAreaRelation)
    {
        startPage("create_time desc");
        List<FactoryGlueAreaRelation> list = factoryGlueAreaRelationService.selectFactoryGlueAreaRelationList(tFactoryGlueAreaRelation);
        return getDataTable(list);
    }

    /**
     * 获取分厂胶料与密炼区对应关系详细信息
     */
    @ApiOperation("获取分厂胶料与密炼区对应关系详细信息")
    @GetMapping(value = "/{id}")
    public FactoryGlueAreaRelation getInfo(@PathVariable("id") Long id){
        return factoryGlueAreaRelationService.selectFactoryGlueAreaRelationById(id);
    }

    /**
     * 新增分厂胶料与密炼区对应关系
     */
    @Log(title = "ui.data.column.factoryGlueAreaRelation.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增分厂胶料与密炼区对应关系")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody FactoryGlueAreaRelation tFactoryGlueAreaRelation){
        return toAjax(factoryGlueAreaRelationService.insertFactoryGlueAreaRelation(tFactoryGlueAreaRelation));
    }

    /**
     * 修改分厂胶料与密炼区对应关系
     */
    @Log(title = "ui.data.column.factoryGlueAreaRelation.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改分厂胶料与密炼区对应关系")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody FactoryGlueAreaRelation tFactoryGlueAreaRelation){
        return toAjax(factoryGlueAreaRelationService.updateFactoryGlueAreaRelation(tFactoryGlueAreaRelation));
    }

    /**
     * 删除分厂胶料与密炼区对应关系
     */
    @Log(title = "ui.data.column.factoryGlueAreaRelation.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除分厂胶料与密炼区对应关系")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(factoryGlueAreaRelationService.deleteFactoryGlueAreaRelationByIds(ids));
    }

    /**
     * 导出分厂胶料与密炼区对应关系列表
     */
    @Log(title = "ui.data.column.factoryGlueAreaRelation.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出分厂胶料与密炼区对应关系列表")
    @PostMapping("/getList")
    public List<FactoryGlueAreaRelation> getList(@RequestBody FactoryGlueAreaRelation tFactoryGlueAreaRelation){
        startPage("create_time desc");
        return  factoryGlueAreaRelationService.selectFactoryGlueAreaRelationList(tFactoryGlueAreaRelation);
    }

    /**
     * 校验分厂胶料与密炼区对应关系唯一性
     */
    @ApiOperation("校验分厂胶料与密炼区对应关系唯一性")
    @PostMapping("/checkFactoryGlueAreaRelationUnique")
    public String checkFactoryGlueAreaRelationUnique(@RequestBody FactoryGlueAreaRelation tFactoryGlueAreaRelation){
        return factoryGlueAreaRelationService.checkFactoryGlueAreaRelationUnique(tFactoryGlueAreaRelation);
    }

    /**
     * 根据集合导入分厂胶料与密炼区对应关系数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.factoryGlueAreaRelation.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入分厂胶料与密炼区对应关系数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<FactoryGlueAreaRelation> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return factoryGlueAreaRelationService.importData(list, updateSupport, importLogId);
    }
}
