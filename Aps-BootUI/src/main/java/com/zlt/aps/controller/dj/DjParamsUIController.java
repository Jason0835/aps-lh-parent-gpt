package com.zlt.aps.controller.dj;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.dj.api.domain.entity.DjParams;
import com.zlt.aps.dj.api.service.IDjParamsRemoteService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 垫胶参数信息Controller
 *
 * @author zlt
 * @date 2026-06-11
 */
@Api(tags = "垫胶参数信息维护")
@Controller
@RequestMapping("/dj/params")
public class DjParamsUIController extends BaseUIController<DjParams> {

    @Autowired
    private IDjParamsRemoteService iDjParamsService;

    private final String prefix = "aps/dj/params";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("dj:params:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/djParams";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        AjaxResult result = iDjParamsService.getInfo(id);
        mmap.put("djParams", result.get(AjaxResult.DATA_TAG));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("dj:params:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(DjParams djParams) {
        return iDjParamsService.list(djParams);
    }

    /**
     * 修改
     */
    @ApiOperation("修改")
    @RequiresPermissions("dj:params:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(DjParams djParams) {
        if (UserConstants.NOT_UNIQUE.equals(iDjParamsService.checkUnique(djParams))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.unique"));
        }
        return iDjParamsService.edit(djParams);
    }

    /**
     * 删除
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("dj:params:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iDjParamsService.remove(java.util.Arrays.asList(arr));
    }

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(DjParams djParams) {
        return iDjParamsService.checkUnique(djParams);
    }

    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "0";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.dj.params.column.modalName");
    }
}