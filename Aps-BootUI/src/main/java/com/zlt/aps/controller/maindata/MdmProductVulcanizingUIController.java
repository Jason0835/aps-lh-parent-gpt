package com.zlt.aps.controller.maindata;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductVulcanizing;
import com.zlt.aps.monthplan.api.service.IMdmProductVulcanizingRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * 基础数据-硫化机正在生产品种Controller
 *
 * @author hsc
 * @date 2021-09-01
 */
@Api(tags = "基础数据-硫化机正在生产品种")
@Controller
@RequestMapping("/lean/vulcanization")
public class MdmProductVulcanizingUIController extends BaseUIController<MdmProductVulcanizing> {
    @Autowired
    private IMdmProductVulcanizingRemoteService iMdmProductVulcanizingRemoteService;

    /**
     * 根据条件查询基础数据-硫化机正在生产品种列表
     */
    @ApiOperation("根据条件查询基础数据-硫化机正在生产品种列表")
//    @RequiresPermissions("fac:vulcanization:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmProductVulcanizing entity) {
        return iMdmProductVulcanizingRemoteService.list(entity);
    }

    /**
     * 修改或新增基础数据-硫化机正在生产品种
     */
    @ApiOperation("修改或新增基础数据-硫化机正在生产品种")
//    @RequiresPermissions("lean:vulcanization:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MdmProductVulcanizing docProductVulcanization) {
        AjaxResult ajaxResult = null;
        String unique = checkDocProductVulcanizationUnique(docProductVulcanization);
        if (StringUtils.equals(unique, UserConstants.NOT_UNIQUE)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.doc.product.vulcanization.unique"));
        }
        if (docProductVulcanization.getId() != null) {
            ajaxResult = iMdmProductVulcanizingRemoteService.edit(docProductVulcanization);
        } else {
            ajaxResult = iMdmProductVulcanizingRemoteService.add(docProductVulcanization);
        }
        return ajaxResult;
    }

    /**
     * 删除基础数据-硫化机正在生产品种
     */
    @ApiOperation("删除基础数据-硫化机正在生产品种（id不为空）")
    @RequiresPermissions("lean:vulcanization:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMdmProductVulcanizingRemoteService.remove(arr);
    }

    /**
     * 校验基础数据-硫化机正在生产品种唯一性
     */
    @ApiOperation("校验基础数据-硫化机正在生产品种唯一性")
    @PostMapping("/checkDocProductVulcanizationUnique")
    @ResponseBody
    public String checkDocProductVulcanizationUnique(MdmProductVulcanizing docProductVulcanization) {
        return iMdmProductVulcanizingRemoteService.checkDocProductVulcanizationUnique(docProductVulcanization);
    }

    @ApiOperation("获取物料信息接口")
    @PostMapping("/getProductInfo")
    @ResponseBody
    public AjaxResult getProductInfo(@RequestParam("productCode") String productCode) {
        return iMdmProductVulcanizingRemoteService.getProductInfo(productCode);
    }


    /**
     * 重写导入模板的生成逻辑
     */
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MdmProductVulcanizing> util = new ExcelUtil<>(MdmProductVulcanizing.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }
    
    @Override
    public String getProcedureCode() {
        return "0";
    }

    @Override
    public String getFunctionName() {
        return getExportTemplateFileName();
    }

    @Override
    public String getExportTemplateFileName() {
        return I18nUtil.getMessage("ui.data.column.vulcanization.modelName");
    }

    @Override
    public String getExportTemplateFileNameForce() {
        return I18nUtil.getMessage("ui.data.column.vulcanization.modelName", Locale.SIMPLIFIED_CHINESE);
    }

    @Override
    public List<MdmProductVulcanizing> importDataInit(List<MdmProductVulcanizing> list) {
        return list;
    }

    @Override
    public AjaxResult importDataByFeign(List list, boolean updateSupport, Long importLogId) {
        // 页面默认不支持修改
        return iMdmProductVulcanizingRemoteService.importData(list, true, importLogId);
    }

    @Override
    public List<MdmProductVulcanizing> exportDataByFeign(MdmProductVulcanizing entity) {
        return iMdmProductVulcanizingRemoteService.getList(entity);
    }
}
