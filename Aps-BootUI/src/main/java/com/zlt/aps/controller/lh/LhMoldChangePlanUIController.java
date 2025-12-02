package com.zlt.aps.controller.lh;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.lh.api.domain.dto.LhScheduleImportFileDTO;
import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlan;
import com.zlt.aps.lh.api.service.ILhMoldChangePlanRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhMoldChangePlanUIController.java
 * 描    述：模具变动单 UI控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-17
 */
@Slf4j
@Api(tags = "模具变动单")
@Controller
@RequestMapping("/lh/lhMoldChangePlan")
public class LhMoldChangePlanUIController extends BaseUIController<LhMoldChangePlan> {

    @Autowired
    private ILhMoldChangePlanRemoteService iLhMoldChangePlanService;



    @ApiOperation("生成换模计划")
    @RequiresPermissions("lh:lhMoldChangePlan:generateMoldReplacementPlan")
    @PostMapping("/generateMoldReplacementPlan")
    @ResponseBody
    public AjaxResult generateMoldReplacementPlan(@RequestBody LhMoldChangePlan queryVO) {
        AjaxResult ajaxResult = iLhMoldChangePlanService.generateMoldReplacementPlan(queryVO);
        return ajaxResult;
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("lh:lhMoldChangePlan:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhMoldChangePlan lhMoldChangePlan) {
        return iLhMoldChangePlanService.list(lhMoldChangePlan);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("lh:lhMoldChangePlan:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(LhMoldChangePlan lhMoldChangePlan) {
        if (UserConstants.NOT_UNIQUE.equals(iLhMoldChangePlanService.checkUnique(lhMoldChangePlan))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.lhMoldChangePlan.checkUnique"));
        }

        return iLhMoldChangePlanService.save(lhMoldChangePlan);
    }

    /**
     * 删除模具变动单
     */
    @ApiOperation("删除,id不为空）")
    @RequiresPermissions("lh:lhMoldChangePlan:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iLhMoldChangePlanService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验模具变动单唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(LhMoldChangePlan lhMoldChangePlan) {
        return iLhMoldChangePlanService.checkUnique(lhMoldChangePlan);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return I18nUtil.getMessage("ui.data.column.lh.lhMoldChangePlan.modelName");
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
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.no.export.sheetName");
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, LhMoldChangePlan entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iLhMoldChangePlanService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }


    @PostMapping({"/importMoldChangePlan"})
    @ResponseBody
    @ApiOperation("导入换模计划")
    public AjaxResult importMoldChangePlan(@RequestPart("file") MultipartFile file, boolean updateSupport, Date scheduleDate) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        LhScheduleImportFileDTO lhScheduleImportFileDTO = new LhScheduleImportFileDTO();
        lhScheduleImportFileDTO.setImportContext(context);
        lhScheduleImportFileDTO.setScheduleDate(scheduleDate);
        lhScheduleImportFileDTO.setImportLogId(100L);
        AjaxResult ajaxResult = iLhMoldChangePlanService.importMoldChangePlan(lhScheduleImportFileDTO);
        return ajaxResult;
    }

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
        AjaxResult ajaxResult = iLhMoldChangePlanService.importData(context, false);
        return ajaxResult;
    }
}
