package com.zlt.aps.controller.monthplan;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlanSum;
import com.zlt.aps.monthplan.api.service.IDpDemandPlanRemoteService;
import com.zlt.aps.monthplan.api.service.IDpDemandPlanSumRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import lombok.extern.slf4j.Slf4j;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import org.apache.commons.io.IOUtils;

import java.util.Arrays;
import java.io.IOException;
import java.io.ByteArrayInputStream;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpDemandPlanUIController.java
 * 描    述：需求计划 UI控制层类：....
 *@author yelq
 *@date 2025-12-25
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Api(tags = "需求计划")
@Controller
@RequestMapping("/monthplan/demandPlan")
public class DpDemandPlanUIController extends BaseUIController<DpDemandPlan> {
    @Autowired
    private IDpDemandPlanRemoteService iDpDemandPlanService;
    @Autowired
    private IDpDemandPlanSumRemoteService iDpDemandPlanSumRemoteService;

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:demandPlan:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(DpDemandPlanSum dpDemandPlan) {
        return iDpDemandPlanSumRemoteService.list(dpDemandPlan);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("monthplan:demandPlan:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(DpDemandPlan dpDemandPlan) {
        if (UserConstants.NOT_UNIQUE.equals(iDpDemandPlanService.checkUnique(dpDemandPlan))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.dpDemandPlan.checkUnique"));
        }

        return iDpDemandPlanService.save(dpDemandPlan);
    }

    /**
     * 删除需求计划
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("monthplan:demandPlan:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iDpDemandPlanService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验需求计划唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(DpDemandPlan dpDemandPlan) {
        return iDpDemandPlanService.checkUnique(dpDemandPlan);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     * @return
     */
    @Override
    public String getExportTemplateFileName(){
        return I18nUtil.getMessage("ui.data.column.demandPlan.modelName");
    }


    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getProcedureCode() {
         return I18nUtil.getMessage("ui.data.column.demandPlan.modelName");
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.demandPlan.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<DpDemandPlan> util = new ExcelUtil<>(DpDemandPlan.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @RequiresPermissions("monthplan:demandPlan:export")
    @ResponseBody
    public void export(HttpServletResponse response, DpDemandPlanSum entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iDpDemandPlanSumRemoteService.exportData(entity,fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping({"/importData"})
    @ResponseBody
    @RequiresPermissions("monthplan:demandPlan:import")
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
      return iDpDemandPlanService.importData(context,false);
    }

    /**
     * 生成需求计划
     */
    @ApiOperation("生成需求计划")
    @PostMapping("/createMonthRequire")
    @RequiresPermissions("monthplan:demandPlan:createMonthRequire")
    @ResponseBody
    public AjaxResult createMonthRequire(DpDemandPlan createCondition) {
        return iDpDemandPlanService.createMonthRequire(createCondition);
    }

    /**
     * 生成需求计划
     */
    @ApiOperation("生成需求计划版本")
    @PostMapping("/createMonthRequireVersion")
    @RequiresPermissions("monthplan:demandPlan:createMonthRequire")
    @ResponseBody
    public AjaxResult createMonthRequireVersion() {
        return iDpDemandPlanService.createMonthRequireVersion();
    }

    /**
     * 查询需求计划版本号
     */
    @ApiOperation("查询需求计划版本号")
    @PostMapping("/findMonthPlanVersion")
    @ResponseBody
    public AjaxResult findMonthPlanVersion(DpDemandPlan queryCondition) {
        return iDpDemandPlanService.findMonthPlanVersion(queryCondition);
    }
}
