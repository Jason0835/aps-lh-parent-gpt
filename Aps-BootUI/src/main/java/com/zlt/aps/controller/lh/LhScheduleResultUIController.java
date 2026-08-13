package com.zlt.aps.controller.lh;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.lh.api.domain.dto.*;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhScheduleShiftDateVO;
import com.zlt.aps.lh.api.domain.vo.ScheduleSummaryReportVO;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.service.ICxScheduleResultService;
import com.zlt.aps.lh.api.service.ILhScheduleResultRemoteService;
import com.zlt.aps.mdm.api.domain.entity.LhMachineInfo;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Api(tags = "硫化排程结果")
@Controller
@RequestMapping("/lh/lhScheduleResult")
public class LhScheduleResultUIController extends BaseUIController<LhScheduleResult> {

    @Autowired
    private ILhScheduleResultRemoteService iLhScheduleResultRemoteService;

    @Autowired
    private ICxScheduleResultService iCxScheduleResultService;

    @ApiOperation("获取详细信息")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(iLhScheduleResultRemoteService.getInfo(id));
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
//    @RequiresPermissions("lh:lhScheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhScheduleResult lhScheduleResult) {
        return iLhScheduleResultRemoteService.list(lhScheduleResult);
    }


    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplateDown")
    @ResponseBody
    public void importTemplateDown(LhScheduleResult result,HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        LhScheduleResult entity = result == null ? new LhScheduleResult() : result;
        byte[] excelBytes = iLhScheduleResultRemoteService.downloadTemplate(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();

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
        AjaxResult ajaxResult = iLhScheduleResultRemoteService.importData(context, updateSupport);
        return ajaxResult;
    }


    @PostMapping({"/importDataByCust"})
    @ResponseBody
    @ApiOperation("数据导入")
    public AjaxResult importDataByCust(@RequestPart("file") MultipartFile file,
                                       @RequestParam("updateSupport") boolean updateSupport,
                                       @RequestParam(value = "factoryCode", required = false) String factoryCode,
                                       @RequestParam(value = "scheduleDate", required = false) String scheduleDate,
                                       LhScheduleResult result) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);

        LhScheduleImportDTO importDTO = new LhScheduleImportDTO();
        importDTO.setImportContext(context);
        result = result == null ? new LhScheduleResult() : result;
        if (StringUtils.isBlank(result.getFactoryCode()) && StringUtils.isNotBlank(factoryCode)) {
            result.setFactoryCode(factoryCode);
        }
        if (result.getScheduleDate() == null && StringUtils.isNotBlank(scheduleDate)) {
            result.setScheduleDate(DateUtils.parseDate(scheduleDate));
        }
        importDTO.setScheduleResult(result);

        AjaxResult ajaxResult = iLhScheduleResultRemoteService.importDataByCust(updateSupport, importDTO);
        return ajaxResult;
    }

    @ApiOperation("插单查询可用机台列表")
//    @RequiresPermissions("lh:lhScheduleResult:getScheduleMachineInfo")
    @PostMapping("/getScheduleMachineInfo")
    @ResponseBody
    public AjaxResult getScheduleMachineInfo(@RequestBody LhOrderInsertParamDTO insertParamDTO) {
        List<LhMachineInfo> resultList = iLhScheduleResultRemoteService.getScheduleMachineInfo(insertParamDTO);
        return AjaxResult.success(resultList);
    }


    /**
     * 插单校验
     *
     * @param insertDTO 插单请求数据
     * @return 校验结果
     */
    @ApiOperation("插单校验")
    @PostMapping("/validateInsertOrder")
    @ResponseBody
    public AjaxResult validateInsertOrder(@RequestBody LhOrderInsertDTO insertDTO) {
        return iLhScheduleResultRemoteService.validateInsertOrder(insertDTO);
    }

    @ApiOperation("获取SKU关联数据（硫化余量/胎胚库存/硫化班产/示方类型）")
    @PostMapping("/getSkuRelatedData")
    @ResponseBody
    public AjaxResult getSkuRelatedData(@RequestBody LhOrderInsertDTO insertDTO) {
        return iLhScheduleResultRemoteService.getSkuRelatedData(insertDTO);
    }

    @ApiOperation("插单")
//    @RequiresPermissions("lh:lhScheduleResult:insertOrder")
    @PostMapping("/insertOrder")
    @ResponseBody
    public AjaxResult insertOrder(@RequestBody LhOrderInsertDTO insertDTO) {
        return iLhScheduleResultRemoteService.insertOrder(insertDTO);
    }

    /**
     * 获取排程日期对象列表
     *
     * @return
     */
    @ApiOperation("排程日期对象列表")
    @PostMapping("/listScheduleShiftDates")
    @ResponseBody
    public AjaxResult listScheduleShiftDates(LhScheduleShiftDateQueryDTO query) {
        if (query == null) {
            return AjaxResult.success(Collections.emptyList());
        }
        List<LhScheduleShiftDateVO> list = iLhScheduleResultRemoteService.listScheduleShiftDates(query);
        return AjaxResult.success(list);
    }

    @ApiOperation("硫化自动排程")
//    @RequiresPermissions("lh:lhScheduleResult:autoLhScheduleResult")
    @PostMapping("/execute")
    @ResponseBody
    public LhScheduleResponseDTO execute(@RequestBody LhScheduleRequestDTO lhScheduleRequestDTO) {
        LhScheduleResponseDTO lhScheduleResponseDTO = iLhScheduleResultRemoteService.execute(lhScheduleRequestDTO);
        return lhScheduleResponseDTO;
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, LhScheduleResult entity) throws IOException {
        Date scheduleDate = entity != null && entity.getScheduleDate() != null ? entity.getScheduleDate() : new Date();
        String fileName = "硫化日计划" + cn.hutool.core.date.DateUtil.format(scheduleDate, "yyyyMMdd");
        byte[] excelBytes = exportCombined(entity, scheduleDate, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 合并导出：硫化日计划 + 成型日计划 合并为一份Excel。
     * 直接复用硫化、成型各自的导出方法（返回byte[]），再用POI将两个工作簿合并，
     * 不挪动成型导出原有代码，原方法仍可独立使用。
     */
    @ApiOperation("合并导出（硫化+成型）")
    @GetMapping({"/exportCombine"})
    @ResponseBody
    public void exportCombine(HttpServletResponse response, LhScheduleResult entity) throws IOException {
        Date scheduleDate = entity != null && entity.getScheduleDate() != null ? entity.getScheduleDate() : new Date();
        String fileName = "硫化日计划" + cn.hutool.core.date.DateUtil.format(scheduleDate, "yyyyMMdd");
        byte[] excelBytes = exportCombined(entity, scheduleDate, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 合并导出核心逻辑：分别调用硫化导出、成型余量导出，再将两个工作簿合并为一个。
     *
     * @param entity 硫化排程查询条件
     * @param scheduleDate 排程日期（已兜底非空）
     * @param fileName 导出文件名
     * @return 合并后的Excel字节数组
     */
    private byte[] exportCombined(LhScheduleResult entity, Date scheduleDate, String fileName) throws IOException {
        // 1. 硫化导出
        byte[] lhBytes = iLhScheduleResultRemoteService.exportData(entity, fileName);

        // 2. 成型导出（使用相同排程日期与工厂条件，导出 CxExport.xlsx 多Sheet）
        CxScheduleResult cxEntity = new CxScheduleResult();
        cxEntity.setScheduleDate(scheduleDate);
        if (entity != null) {
            cxEntity.setFactoryCode(entity.getFactoryCode());
        }
        byte[] cxBytes = iCxScheduleResultService.exportCxRemainQty(cxEntity, "成型日计划");

        // 3. 合并两个工作簿：成型只复制有数据的 0(成型余量)/1(成型日计划)/7(成型结构切换)/8(排产小结) 四个页签
        return mergeExcel(lhBytes, cxBytes, 0, 1, 7, 8);
    }

    /**
     * 将源工作簿指定索引的Sheet复制到目标工作簿中，返回合并后的字节数组。
     *
     * @param targetBytes 目标工作簿字节数组（硫化导出，保留其原有Sheet）
     * @param sourceBytes 源工作簿字节数组（成型导出，按sheetIndices指定的Sheet被追加到目标）
     * @param sheetIndices 需要从源工作簿复制的Sheet索引（从0开始）
     * @return 合并后的工作簿字节数组
     */
    private byte[] mergeExcel(byte[] targetBytes, byte[] sourceBytes, int... sheetIndices) throws IOException {
        try (Workbook targetWorkbook = WorkbookFactory.create(new ByteArrayInputStream(targetBytes));
             Workbook sourceWorkbook = WorkbookFactory.create(new ByteArrayInputStream(sourceBytes))) {
            for (int i : sheetIndices) {
                ExcelUtils.copySheet(sourceWorkbook, i, targetWorkbook);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            targetWorkbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * 保存
     */
    @ApiOperation("保存")
//    @RequiresPermissions({"lh:lhScheduleResult:save"})
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult editSave(LhScheduleResult lhScheduleResult) {
        AjaxResult ajaxResult = iLhScheduleResultRemoteService.save(lhScheduleResult);
        return ajaxResult;
    }

    /**
     * 删除
     */
    @ApiOperation("删除")
//    @RequiresPermissions("lh:lhScheduleResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iLhScheduleResultRemoteService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("硫化排程结果转机台校验")
    //@RequiresPermissions("lh:lhScheduleResult:validateChangeMachine")
    @PostMapping("/validateChangeMachine")
    @ResponseBody
    public AjaxResult validateChangeMachine(@RequestBody LhTransferDeskDTO dto) {
        return iLhScheduleResultRemoteService.validateChangeMachine(dto);
    }

    @ApiOperation("转机台")
//    @RequiresPermissions("lh:lhScheduleResult:changeMachine")
    @PostMapping("/changeMachine")
    @ResponseBody
    public AjaxResult changeMachine(@RequestBody LhTransferDeskDTO dto) {
        return iLhScheduleResultRemoteService.changeMachine(dto);
    }

    @ApiOperation("硫化排程结果调量校验")
    //@RequiresPermissions("lh:lhScheduleResult:validateAdjustQuantity")
    @PostMapping("/validateAdjustQuantity")
    @ResponseBody
    public AjaxResult validateAdjustQuantity(@RequestBody LhScheduleResultUpdateDTO dto) {
        return iLhScheduleResultRemoteService.validateAdjustQuantity(dto);
    }

    @ApiOperation("调量")
//    @RequiresPermissions("lh:lhScheduleResult:adjustQuantity")
    @PostMapping("/adjustQuantity")
    @ResponseBody
    public AjaxResult adjustQuantity(@RequestBody LhScheduleResultUpdateDTO dto) {
        return iLhScheduleResultRemoteService.adjustQuantity(dto);
    }

    @ApiOperation("文字示方调整")
    @PostMapping("/adjustTextNo")
    @ResponseBody
    public AjaxResult adjustTextNo(LhTransferDeskDTO dto) {
        return iLhScheduleResultRemoteService.adjustTextNo(dto);
    }

    /**
     * 文字示方更新
     *
     * @param dto 生成入参
     * @return 处理结果
     */
    @RequiresPermissions("lh:lhScheduleResult:generateTextPlan")
    @ApiOperation("文字示方更新")
    @PostMapping("/generateTextMouldChangePlan")
    @ResponseBody
    public AjaxResult generateTextMouldChangePlan(@RequestBody LhGenerateTextMouldPlanDTO dto) {
        return iLhScheduleResultRemoteService.generateTextMouldChangePlan(dto);
    }

    /**
     * 计划更新
     *
     * @param scheduleResult 当前硫化排程结果
     * @return 处理结果
     */
    @RequiresPermissions("lh:lhScheduleResult:increaseMouldStartPlan")
    @ApiOperation("计划更新")
    @PostMapping("/increaseMouldStartPlan")
    @ResponseBody
    public AjaxResult increaseMouldStartPlan(@RequestBody LhScheduleResult scheduleResult) {
        return iLhScheduleResultRemoteService.increaseMouldStartPlan(scheduleResult);
    }

    @Override
    public String getProcedureCode() {
        return "0";
    }

    /**
     * 生产控制台导入
     */
    @Override
    public String getFunctionName() {
        return getExportTemplateFileName();
    }

    /**
     * 文件模板文件名
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return I18nUtil.getMessage("ui.data.column.lh.scheduleResult.modelName");
    }


    /**
     * 发布排程
     */
    @ApiOperation("发布排程")
//    @RequiresPermissions("lh:scheduleResult:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(@RequestBody Map<String, String> params) {
        LhScheduleResult dto = new LhScheduleResult();
        String ids = params.get("ids");
        String scheduleDateStr = params.get("scheduleDate");
        String factoryCode = params.get("factoryCode");
        if (StringUtils.isNotEmpty(scheduleDateStr)) {
            try {
                dto.setScheduleDate(DateUtils.parseDate(scheduleDateStr));
            } catch (Exception e) {
                dto.setScheduleDate(DateUtils.addDays(new Date(), 1));
            }
        }
        if (dto.getScheduleDate() == null) {
            dto.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        dto.setFactoryCode(factoryCode);
        return iLhScheduleResultRemoteService.publish(dto, ids);
    }

    @ApiOperation("硫化排程结果下发到MES")
//    @RequiresPermissions("lh:scheduleResult:issueToMes")
    @PostMapping("/issueToMes")
    @ResponseBody
    public AjaxResult issueToMes() {
        return iLhScheduleResultRemoteService.issueToMes();
    }

    @ApiOperation("排产小结报表导出")
    @GetMapping("/exportScheduleSummaryReport")
    @ResponseBody
    public void exportScheduleSummaryReport(HttpServletResponse response,
                                             ScheduleSummaryReportVO queryVO) throws IOException {
        String fileName = "排产小结报表";
        byte[] excelBytes = iLhScheduleResultRemoteService.exportScheduleSummaryReport(queryVO, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

}
