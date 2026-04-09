package com.zlt.aps.controller.cx;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.service.ICxScheduleResultService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：CxScheduleResultUIController.java
 * 描    述：成型排程结果 UI控制层类
 * @author APS Team
 * @date 2026-04-02
 * @version 1.0
 *
 * 修改记录：
 *     修改时间：...
 *     修 改 人：...
 *     修改内容：...
 */
@Slf4j
@Api(tags = "成型排程结果管理")
@Controller
@RequestMapping("/cx/cxScheduleResult")
public class CxScheduleResultUIController extends BaseUIController<CxScheduleResult> {

    @Autowired
    private ICxScheduleResultService iCxScheduleResultService;

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("cx:cxScheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxScheduleResult cxScheduleResult) {
        return iCxScheduleResultService.list(cxScheduleResult);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("cx:cxScheduleResult:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(CxScheduleResult cxScheduleResult) {
        if (UserConstants.NOT_UNIQUE.equals(iCxScheduleResultService.checkUnique(cxScheduleResult))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.cxScheduleResult.notUnique"));
        }

        return iCxScheduleResultService.save(cxScheduleResult);
    }

    /**
     * 删除成型排程结果
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("cx:cxScheduleResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxScheduleResultService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验成型排程结果唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(CxScheduleResult cxScheduleResult) {
        return iCxScheduleResultService.checkUnique(cxScheduleResult);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * @return
     */
    @Override
    public String getExportTemplateFileName(){
        return this.getFunctionName();
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
        return I18nUtil.getMessage("ui.data.column.cxScheduleResult.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<CxScheduleResult> util = new ExcelUtil<>(CxScheduleResult.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导出
     */
    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, CxScheduleResult entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iCxScheduleResultService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    // ==================== 业务功能方法 ====================

    /**
     * 生成排程
     */
    @ApiOperation("生成排程")
    @RequiresPermissions("cx:cxScheduleResult:generate")
    @PostMapping("/generate")
    @ResponseBody
    public AjaxResult generateSchedule(@RequestBody com.zlt.aps.cx.vo.ScheduleGenerateVo dto) {
        return iCxScheduleResultService.generateSchedule(dto);
    }

    /**
     * 成型排程结果下发到MES
     */
    @ApiOperation("下发到MES")
    @RequiresPermissions("cx:cxScheduleResult:issue")
    @PostMapping("/issueToMes")
    @ResponseBody
    public AjaxResult issueToMes() {
        return iCxScheduleResultService.issueCxScheduleResultToMes();
    }

    /**
     * 【调量】调整各班计划量
     */
    @ApiOperation("调量")
    @RequiresPermissions("cx:cxScheduleResult:adjust")
    @PostMapping("/adjustQty")
    @ResponseBody
    public AjaxResult adjustQty(@RequestBody com.zlt.aps.cx.vo.ScheduleAdjustVo vo) {
        return iCxScheduleResultService.adjustQty(vo);
    }

    /**
     * 【插单】插入新的排程记录
     */
    @ApiOperation("插单")
    @RequiresPermissions("cx:cxScheduleResult:insert")
    @PostMapping("/insertOrder")
    @ResponseBody
    public AjaxResult insertOrder(@RequestBody com.zlt.aps.cx.vo.ScheduleInsertVo vo) {
        return iCxScheduleResultService.insertOrder(vo);
    }

    /**
     * 【修改】修改备注和原因分析
     */
    @ApiOperation("修改备注和原因分析")
    @RequiresPermissions("cx:cxScheduleResult:updateRemark")
    @PostMapping("/updateRemark")
    @ResponseBody
    public AjaxResult updateRemark(@RequestBody com.zlt.aps.cx.vo.ScheduleUpdateRemarkVo vo) {
        return iCxScheduleResultService.updateRemark(vo);
    }

    /**
     * 【转机台】转换机台
     */
    @ApiOperation("转机台")
    @RequiresPermissions("cx:cxScheduleResult:transferMachine")
    @PostMapping("/transferMachine")
    @ResponseBody
    public AjaxResult transferMachine(@RequestBody com.zlt.aps.cx.vo.ScheduleTransferMachineVo vo) {
        return iCxScheduleResultService.transferMachine(vo);
    }

    /**
     * 【排程发布】发布排程数据
     */
    @ApiOperation("发布排程")
    @RequiresPermissions("cx:cxScheduleResult:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(@RequestBody List<Long> ids) {
        return iCxScheduleResultService.publish(ids);
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
        AjaxResult ajaxResult = iCxScheduleResultService.importData(context, updateSupport);
        return ajaxResult;
    }
}
