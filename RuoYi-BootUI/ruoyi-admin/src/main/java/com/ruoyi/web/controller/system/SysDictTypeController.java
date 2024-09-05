package com.ruoyi.web.controller.system;

import com.ruoyi.api.gateway.system.domain.SysDictType;
import com.ruoyi.api.gateway.system.domain.Ztree;
import com.ruoyi.api.gateway.system.service.ISysDictTypeService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseController;
import com.ruoyi.common.text.Convert;
import com.ruoyi.framework.web.service.DictServiceImpl;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 数据字典信息
 * 
 * @author ruoyi
 */
@Controller
@RequestMapping("/system/dict")
public class SysDictTypeController extends BaseController
{
    private String prefix = "system/dict/type";

    @Autowired
    private ISysDictTypeService dictTypeService;

    @Autowired
    private DictServiceImpl dictServiceImpl;

    @RequiresPermissions("system:dict:view")
    @GetMapping()
    public String dictType()
    {
        return prefix + "/type";
    }

    @PostMapping("/list")
    @RequiresPermissions("system:dict:list")
    @ResponseBody
    public TableDataInfo list(SysDictType dictType)
    {
        return dictTypeService.list(dictType);
    }

    @ApiOperation("导出字典类型")
    @RequiresPermissions("system:dict:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, SysDictType entity) throws IOException {
        List<SysDictType> list = dictTypeService.totalList(entity);
        ExcelUtil<SysDictType> util = new ExcelUtil(SysDictType.class);
        util.exportExcel(response, list, I18nUtil.getMessage("ui.dict.info.export.sheetName"), I18nUtil.getMessage("ui.dict.info.export.sheetName"));
    }

//   @Log(title = "字典类型", businessType = BusinessType.EXPORT)
//    @RequiresPermissions("system:dict:export")
//    @PostMapping("/export")
//    @ResponseBody
//    public AjaxResult export(SysDictType dictType)
//    {
//
//        List<SysDictType> list = dictTypeService.selectDictTypeList(dictType);
//        ExcelUtil<SysDictType> util = new ExcelUtil<SysDictType>(SysDictType.class);
//        return util.exportExcel(list, "字典类型");
//    }
    
    /**
     * 新增字典类型
     */
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    /**
     * 新增保存字典类型
     */
    @RequiresPermissions("system:dict:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(SysDictType dict)
    {
        return dictTypeService.add(dict);
    }

    /**
     * 修改字典类型
     */
    @GetMapping("/edit/{dictId}")
    public String edit(@PathVariable("dictId") Long dictId, ModelMap mmap)
    {
        mmap.put("dict", dictTypeService.selectDictTypeById(dictId));
        return prefix + "/edit";
    }

    /**
     * 修改保存字典类型
     */
    @RequiresPermissions("system:dict:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(SysDictType dict)
    {
        return dictTypeService.edit(dict);
    }

    @RequiresPermissions("system:dict:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        Long[] arr= Convert.toLongArray(ids);
        return dictTypeService.remove(arr);
    }

    /**
     * 清空缓存
     */
    @RequiresPermissions("system:dict:remove")
    @GetMapping("/clearCache")
    @ResponseBody
    public AjaxResult clearCache()
    {
        return dictTypeService.clearCache();
    }

    /**
     * 查询字典详细
     */
    @RequiresPermissions("system:dict:list")
    @GetMapping("/detail/{dictId}")
    public String detail(@PathVariable("dictId") Long dictId, ModelMap mmap)
    {
        mmap.put("dict", dictTypeService.selectDictTypeById(dictId));
        mmap.put("dictList", dictTypeService.selectDictTypeAll());
        return "system/dict/data/data";
    }

    /**
     * 校验字典类型
     */
    @PostMapping("/checkDictTypeUnique")
    @ResponseBody
    public String checkDictTypeUnique(SysDictType dictType)
    {
        return dictTypeService.checkDictTypeUnique(dictType);
    }

    /**
     * 选择字典树
     */
    @GetMapping("/selectDictTree/{columnId}/{dictType}")
    public String selectDeptTree(@PathVariable("columnId") Long columnId, @PathVariable("dictType") String dictType,
            ModelMap mmap)
    {
        mmap.put("columnId", columnId);
        mmap.put("dict", dictTypeService.selectDictTypeByType(dictType));
        return prefix + "/tree";
    }

    /**
     * 加载字典列表树
     */
    @GetMapping("/treeData")
    @ResponseBody
    public List<Ztree> treeData()
    {
        List<Ztree> ztrees = dictTypeService.treeData();
        return ztrees;
    }

    @GetMapping("/reloadCache")
    @ResponseBody
    public AjaxResult reloadCache(){
        dictServiceImpl.reloadCache();
        return AjaxResult.success();
    }
}
