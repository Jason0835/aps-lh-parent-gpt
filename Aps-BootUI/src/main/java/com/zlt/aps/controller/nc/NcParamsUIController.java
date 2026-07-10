package com.zlt.aps.controller.nc;

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
import com.zlt.aps.nc.api.domain.entity.NcParams;
import com.zlt.aps.nc.api.service.INcParamsRemoteService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 内衬参数信息Controller
 *
 * @author zlt
 * @date 2026-06-11
 */
@Api(tags = "内衬参数信息维护")
@Controller
@RequestMapping("/nc/params")
public class NcParamsUIController extends BaseUIController<NcParams> {

    @Autowired
    private INcParamsRemoteService iNcParamsService;

    private final String prefix = "aps/nc/params";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("nc:params:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/ncParams";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        AjaxResult result = iNcParamsService.getInfo(id);
        mmap.put("ncParams", result.get(AjaxResult.DATA_TAG));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("nc:params:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(NcParams ncParams) {
        return iNcParamsService.list(ncParams);
    }

    /**
     * 修改
     */
    @ApiOperation("修改")
    @RequiresPermissions("nc:params:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(NcParams ncParams) {
        if (UserConstants.NOT_UNIQUE.equals(iNcParamsService.checkUnique(ncParams))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.unique"));
        }
        return iNcParamsService.edit(ncParams);
    }

    /**
     * 删除
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("nc:params:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iNcParamsService.remove(java.util.Arrays.asList(arr));
    }

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(NcParams ncParams) {
        return iNcParamsService.checkUnique(ncParams);
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
        return I18nUtil.getMessage("ui.nc.params.column.modalName");
    }
}
