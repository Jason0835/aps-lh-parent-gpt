package com.ruoyi.system.controller;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.api.gateway.system.domain.SysDictType;
import com.ruoyi.api.gateway.system.domain.Ztree;
import com.ruoyi.api.gateway.system.domain.SysDictData;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.ruoyi.system.service.ISysDictTypeService;

/**
 * 数据字典信息
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/dict/type")
public class SysDictTypeController extends BaseController
{
    @Autowired
    private ISysDictTypeService dictTypeService;

    @PreAuthorize(hasPermi = "system:dict:list")
    @GetMapping("/list")
    public TableDataInfo list(SysDictType dictType)
    {
        startPage();
        List<SysDictType> list = dictTypeService.selectDictTypeList(dictType);
        return getDataTable(list);
    }


    @PreAuthorize(hasPermi = "system:dict:list")
    @GetMapping("/totalList")
    public List<SysDictType> totalList(SysDictType role)
    {
        List<SysDictType> list = dictTypeService.selectDictTypeList(role);
        return list;
    }

    @Log(title = "system.title.dicttype", businessType = BusinessType.EXPORT)
    @PreAuthorize(hasPermi = "system:dict:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysDictType dictType) throws IOException
    {
        List<SysDictType> list = dictTypeService.selectDictTypeList(dictType);
        ExcelUtil<SysDictType> util = new ExcelUtil<SysDictType>(SysDictType.class);
        util.exportExcel(response, list, I18nUtil.getMessage("system.title.dicttype"));
    }

    /**
     * 查询字典类型详细
     */
    @PreAuthorize(hasPermi = "system:dict:query")
    @GetMapping(value = "/{dictId}")
    public AjaxResult getInfo(@PathVariable Long dictId)
    {
        return AjaxResult.success(dictTypeService.selectDictTypeById(dictId));
    }

    /**
     * 新增字典类型
     */
    @PreAuthorize(hasPermi = "system:dict:add")
    @Log(title = "system.title.dicttype", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysDictType dict)
    {
        if (UserConstants.NOT_UNIQUE.equals(dictTypeService.checkDictTypeUnique(dict)))
        {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.dict.exist.noadd") , dict.getDictName());
            return AjaxResult.error(errMsg);
        }
        dict.setCreateBy(SecurityUtils.getUsername());
        return toAjax(dictTypeService.insertDictType(dict));
    }

    /**
     * 修改字典类型
     */
    @PreAuthorize(hasPermi = "system:dict:edit")
    @Log(title = "system.title.dicttype", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysDictType dict)
    {
        if (UserConstants.NOT_UNIQUE.equals(dictTypeService.checkDictTypeUnique(dict)))
        {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.dict.exist.noupdate") , dict.getDictName());
            return AjaxResult.error(errMsg);
        }
        dict.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(dictTypeService.updateDictType(dict));
    }

    /**
     * 删除字典类型
     */
    @PreAuthorize(hasPermi = "system:dict:remove")
    @Log(title = "system.title.dicttype", businessType = BusinessType.DELETE)
    @DeleteMapping("/{dictIds}")
    public AjaxResult remove(@PathVariable Long[] dictIds)
    {
        return toAjax(dictTypeService.deleteDictTypeByIds(dictIds));
    }

    /**
     * 清空缓存
     */
    @PreAuthorize(hasPermi = "system:dict:remove")
    @Log(title = "system.title.dicttype", businessType = BusinessType.CLEAN)
    @DeleteMapping("/clearCache")
    public AjaxResult clearCache()
    {
        dictTypeService.clearCache();
        return AjaxResult.success();
    }

    /**
     * 获取字典选择框列表
     */
    @GetMapping("/optionselect")
    public AjaxResult optionselect()
    {
        List<SysDictType> dictTypes = dictTypeService.selectDictTypeAll();
        return AjaxResult.success(dictTypes);
    }

    /**
     * 根据类型ID获取数据字典
     * @param dictId
     * @return
     */
    @PostMapping("/selectDictTypeById")
    public SysDictType selectDictTypeById(Long dictId){
        return  dictTypeService.selectDictTypeById(dictId);
    }

    /**
     * 获取所有字典类型
     * @return
     */
    @PostMapping("/selectDictTypeAll")
    public  List<SysDictType>  selectDictTypeAll(){
        return  dictTypeService.selectDictTypeAll();
    }

    /**
     * 根据类型获取数据字典类型
     * @param dictType
     * @return
     */
    @PostMapping("/selectDictTypeByType")
    public SysDictType selectDictTypeByType(String dictType){
        return  dictTypeService.selectDictTypeByType(dictType);
    }

    /**
     * 根据数据字典类型获取数据字典项
     * @param dictType
     * @return
     */
    @PostMapping("/selectDictDataByType")
    public List<SysDictData> selectDictDataByType(String dictType){
        return dictTypeService.selectDictDataByType(dictType);
    }

    /**
     * 校验字典类型
     * @param dictType
     * @return
     */
    @PostMapping("/checkDictTypeUnique")
    public String checkDictTypeUnique(@RequestBody SysDictType dictType)
    {
        return dictTypeService.checkDictTypeUnique(dictType);
    }

    /**
     * 加载字典列表树
     * @return
     */
    @PostMapping("/treeData")
    public List<Ztree> treeData()
    {
        List<Ztree> ztrees = dictTypeService.selectDictTree(new SysDictType());
        return ztrees;
    }
}
