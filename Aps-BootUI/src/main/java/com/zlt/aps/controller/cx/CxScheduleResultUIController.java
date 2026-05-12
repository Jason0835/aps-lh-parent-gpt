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
import com.zlt.aps.cx.vo.CxScheduleImportDTO;
import com.zlt.aps.cx.vo.ScheduleInsertVo;
import com.zlt.aps.cx.vo.ScheduleTransferMachineVo;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxScheduleResult cxScheduleResult) {
        return iCxScheduleResultService.list(cxScheduleResult);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
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
    @GetMapping("/importTemplateDown")
    @ResponseBody
    public void importTemplateDown(CxScheduleResult result, HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        result = result == null ? new CxScheduleResult() : result;
        byte[] excelBytes = iCxScheduleResultService.downloadTemplate(result, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
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

    /**
     * 导出成型余量数据。
     *
     * @param response HTTP响应对象，用于写出Excel文件流
     * @param entity 查询条件，按成型排程结果列表查询口径筛选数据
     * @throws IOException 写出Excel文件流失败时抛出
     */
    @ApiOperation("导出成型余量数据")
    @GetMapping({"/exportCxRemainQty"})
    @ResponseBody
    public void exportCxRemainQty(HttpServletResponse response, CxScheduleResult entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iCxScheduleResultService.exportCxRemainQty(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 导出成型结构切换数据。
     *
     * @param response HTTP响应对象，用于写出Excel文件流
     * @param entity 查询条件，按成型排程结果列表查询口径筛选数据
     * @throws IOException 写出Excel文件流失败时抛出
     */
    @ApiOperation("导出成型结构切换数据")
    @GetMapping({"/exportStructureChange"})
    @ResponseBody
    public void exportStructureChange(HttpServletResponse response, CxScheduleResult entity) throws IOException {
        String fileName = "成型结构切换";
        byte[] excelBytes = iCxScheduleResultService.exportStructureChange(entity, fileName);
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
    @PostMapping("/generate")
    @ResponseBody
    public AjaxResult generateSchedule(@RequestBody com.zlt.aps.cx.vo.ScheduleGenerateVo dto) {
        try {
            return iCxScheduleResultService.generateSchedule(dto);
        } catch (feign.FeignException e) {
            // Feign调用异常（如超时）：排程服务可能仍在执行中，提示用户稍后查看
            log.warn("排程Feign调用异常, status={}, message={}", e.status(), e.getMessage());
            if (e.status() == 423) {
                return AjaxResult.error(423, e.getMessage());
            }
            return AjaxResult.error("排程请求超时，如排程正在执行中，请稍后刷新页面查看结果");
        } catch (Exception e) {
            log.error("排程调用异常", e);
            return AjaxResult.error("排程请求异常，请稍后重试");
        }
    }

    /**
     * 成型排程结果下发到MES
     */
    @ApiOperation("下发到MES")
    @PostMapping("/issueToMes")
    @ResponseBody
    public AjaxResult issueToMes() {
        return iCxScheduleResultService.issueCxScheduleResultToMes();
    }

    /**
     * 【调量】调整各班计划量
     */
    @ApiOperation("调量")
    @PostMapping("/adjustQty")
    @ResponseBody
    public AjaxResult adjustQty(@RequestBody com.zlt.aps.cx.vo.ScheduleAdjustVo vo) {
        return iCxScheduleResultService.adjustQty(vo);
    }

    /**
     * 【插单】插入新的排程记录
     */
    @ApiOperation("插单")
    @PostMapping("/insertOrder")
    @ResponseBody
    public AjaxResult insertOrder(@RequestBody ScheduleInsertVo vo) {
        return iCxScheduleResultService.insertOrder(vo);
    }

    /**
     * 【修改】修改备注和原因分析
     */
    @ApiOperation("修改备注和原因分析")
    @PostMapping("/updateRemark")
    @ResponseBody
    public AjaxResult updateRemark(@RequestBody com.zlt.aps.cx.vo.ScheduleUpdateRemarkVo vo) {
        return iCxScheduleResultService.updateRemark(vo);
    }

    /**
     * 【转机台】转换机台
     */
    @ApiOperation("转机台")
    @PostMapping("/transferMachine")
    @ResponseBody
    public AjaxResult transferMachine(@RequestBody ScheduleTransferMachineVo vo) {
        return iCxScheduleResultService.transferMachine(vo);
    }

    /**
     * 【排程发布】发布排程数据
     */
    @ApiOperation("发布排程")
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

    @PostMapping({"/importDataByCust"})
    @ResponseBody
    @ApiOperation("数据导入")
    public AjaxResult importDataByCust(@RequestPart("file") MultipartFile file,
                                       @RequestParam("updateSupport") boolean updateSupport,
                                       @RequestParam(value = "factoryCode", required = false) String factoryCode,
                                       @RequestParam(value = "scheduleDate", required = false) String scheduleDate,
                                       CxScheduleResult result) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);

        CxScheduleImportDTO importDTO = new CxScheduleImportDTO();
        importDTO.setImportContext(context);
        result = result == null ? new CxScheduleResult() : result;
        if (result.getScheduleDate() == null && StringUtils.isNotBlank(scheduleDate)) {
            result.setScheduleDate(com.ruoyi.common.core.utils.DateUtils.parseDate(scheduleDate));
        }
        importDTO.setScheduleResult(result);

        AjaxResult ajaxResult = iCxScheduleResultService.importDataByCust(updateSupport, importDTO);
        return ajaxResult;
    }
}
