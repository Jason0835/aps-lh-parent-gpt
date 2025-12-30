package com.zlt.aps.controller.maindata;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.MdmMustFinishPlan;
import com.zlt.aps.monthplan.api.domain.vo.MdmMustFinishPlanTemplateVo;
import com.zlt.aps.monthplan.api.service.IMdmMustFinishPlanRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMustFinishPlanUIController.java
 * 描    述：必须保证的客户月计划 UI控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-25
 */
@Slf4j
@Api(tags = "必须保证的客户月计划")
@Controller
@RequestMapping("/maindata/mustFinishPlan")
public class MdmMustFinishPlanUIController extends BaseUIController<MdmMustFinishPlan> {

    @Autowired
    private IMdmMustFinishPlanRemoteService iMdmMustFinishPlanService;

    /**
     * 根据条件查询必须保证的客户月计划列表
     */
    @ApiOperation("根据条件查询必须保证的客户月计划列表")
    @RequiresPermissions("maindata:mustFinishPlan:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmMustFinishPlan entity) {
        return iMdmMustFinishPlanService.list(entity);
    }

    /**
     * 修改或新增必须保证的客户月计划
     */
    @ApiOperation("修改或新增必须保证的客户月计划")
    @RequiresPermissions(value = {"maindata:mustFinishPlan:edit", "maindata:mustFinishPlan:addd"}, logical = Logical.OR)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MdmMustFinishPlan mdmMustFinishPlan) {
        if (UserConstants.NOT_UNIQUE.equals(iMdmMustFinishPlanService.checkMdmMustFinishPlanUnique(mdmMustFinishPlan))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mdmMustFinishPlan.checkUnique"));
        }
        if (mdmMustFinishPlan.getId() != null) {
            return iMdmMustFinishPlanService.edit(mdmMustFinishPlan);
        } else {
            return iMdmMustFinishPlanService.add(mdmMustFinishPlan);
        }
    }

    /**
     * 删除必须保证的客户月计划
     */
    @ApiOperation("删除必须保证的客户月计划（id不为空）")
    @RequiresPermissions("maindata:mustFinishPlan:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMdmMustFinishPlanService.remove(arr);
    }

    /**
     * 校验必须保证的客户月计划唯一性
     */
    @ApiOperation("校验必须保证的客户月计划唯一性")
    @PostMapping("/checkMdmMustFinishPlanUnique")
    @ResponseBody
    public String checkMdmMustFinishPlanUnique(MdmMustFinishPlan mdmMustFinishPlan) {
        return iMdmMustFinishPlanService.checkMdmMustFinishPlanUnique(mdmMustFinishPlan);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return I18nUtil.getMessage("ui.data.column.mustFinishPlan.modelName");
    }


    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getProcedureCode() {
        return "0";
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MdmMustFinishPlanTemplateVo> util = new ExcelUtil<>(MdmMustFinishPlanTemplateVo.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }


    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.mustFinishPlan.modelName");
    }

    @RequiresPermissions("maindata:mustFinishPlan:export")
    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MdmMustFinishPlan entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMdmMustFinishPlanService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @RequiresPermissions("maindata:mustFinishPlan:import")
    @PostMapping({"/importData"})
    @ResponseBody
    @ApiOperation("数据导入")
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        return iMdmMustFinishPlanService.importData(context, updateSupport);
    }
}
