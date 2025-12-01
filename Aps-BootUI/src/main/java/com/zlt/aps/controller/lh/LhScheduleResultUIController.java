package com.zlt.aps.controller.lh;

import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.lh.api.domain.dto.*;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhGanttVo;
import com.zlt.aps.lh.api.domain.vo.LhMachineInfoVo;
import com.zlt.aps.lh.api.service.ILhScheduleResultRemoteService;
import com.zlt.aps.monthplan.api.domain.entity.LhMonthPlanSurplusDetail;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductConstruction;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 硫化排程结果Controller
 *
 * @author chen
 * @date 2021-07-19
 */
@Api(tags = "硫化排程结果")
@Controller
@RequestMapping("/lh/lhScheduleResult")
public class LhScheduleResultUIController extends BaseUIController<LhScheduleResult> {

    @Autowired
    private ILhScheduleResultRemoteService iLhScheduleResultRemoteService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("lh:lhScheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhScheduleResult lhScheduleResult) {
        return iLhScheduleResultRemoteService.list(lhScheduleResult);
    }

    /**
     * 根据条件查询主表数据-大屏使用(无权限)
     */
    @ApiOperation("根据条件查询主表数据-大屏使用(无权限)")
    @PostMapping("/list4BigScreen")
    @ResponseBody
    public TableDataInfo list4BigScreen(LhScheduleResult lhScheduleResult) {
        return iLhScheduleResultRemoteService.list(lhScheduleResult);
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<LhScheduleResult> util = new ExcelUtil<>(LhScheduleResult.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @PostMapping({"/importData"})
    @ResponseBody
    @ApiOperation("数据导入(暂时不用，无法满足特殊逻辑)")
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport ) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iLhScheduleResultRemoteService.importData(context, false);
        return ajaxResult;
    }

    @PostMapping({"/importData2"})
    @ResponseBody
    @ApiOperation("数据导入2")
    public AjaxResult importData2(@RequestPart("file") MultipartFile file, boolean updateSupport,Date scheduleDate) throws Exception {
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
        lhScheduleImportFileDTO.setImportLogId(1L);
        AjaxResult ajaxResult = iLhScheduleResultRemoteService.importData2(lhScheduleImportFileDTO);
        return ajaxResult;
    }

    @ApiOperation("插单查询可用机台列表")
    @RequiresPermissions("lh:lhScheduleResult:getScheduleMachineInfo")
    @PostMapping("/getScheduleMachineInfo")
    @ResponseBody
    public AjaxResult getScheduleMachineInfo(@RequestBody LhOrderInsertParamDTO insertParamDTO){
        List<LhMachineInfoVo> resultList = iLhScheduleResultRemoteService.getScheduleMachineInfo(insertParamDTO);
        return AjaxResult.success(resultList);
    }

    @ApiOperation("根据规格号查询物料号List")
    //@RequiresPermissions("lh:lhScheduleResult:selectListMdmProductConstruction") Nick+ 前端是没有按钮的去掉权限
    @PostMapping("/selectListMdmProductConstruction")
    @ResponseBody
    public AjaxResult selectListMdmProductConstruction(@RequestBody LhSpecCodeParamDTO dto){
        List<MdmProductConstruction> resultList = iLhScheduleResultRemoteService.selectListMdmProductConstruction(dto);
        return AjaxResult.success(resultList);
    }

    @ApiOperation("插单")
    @RequiresPermissions("lh:lhScheduleResult:insertOrder")
    @PostMapping("/insertOrder")
    @ResponseBody
    public AjaxResult insertOrder(@RequestBody LhOrderInsertDTO insertDTO){
        return iLhScheduleResultRemoteService.insertOrder(insertDTO);
    }

    /**
     * 自动排程
     * @return
     */
    @ApiOperation("硫化自动排程")
    @RequiresPermissions("lh:lhScheduleResult:autoLhScheduleResult")
    @PostMapping("/autoLhScheduleResult")
    @ResponseBody
    public AjaxResult autoLhScheduleResult(@RequestBody AutoLhScheduleResultDTO autoLhScheduleResultDTO) {
        AjaxResult ajaxResult = iLhScheduleResultRemoteService.autoLhScheduleResult(autoLhScheduleResultDTO);
        return ajaxResult;
    }

    /**
     * 自动排程
     * @return
     */
    @ApiOperation("硫化自动排程测试版")
    @RequiresPermissions("lh:lhScheduleResult:autoLhScheduleResult")
    @PostMapping("/autoLhScheduleResultTest")
    @ResponseBody
    public AjaxResult autoLhScheduleResultTest(@RequestBody AutoLhScheduleResultDTO autoLhScheduleResultDTO) {
        AjaxResult ajaxResult = iLhScheduleResultRemoteService.autoLhScheduleResultTest(autoLhScheduleResultDTO);
        return ajaxResult;
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, LhScheduleResult entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iLhScheduleResultRemoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导出合并规格")
    @GetMapping({"/exportCombine"})
    @ResponseBody
    public void exportCombine(HttpServletResponse response, LhScheduleResult entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        entity.setExportCombineFlag(ApsConstant.TRUE);
        byte[] excelBytes = iLhScheduleResultRemoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 保存
     */
    @ApiOperation("保存")
    @RequiresPermissions({"lh:lhScheduleResult:save"})
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
    @RequiresPermissions("lh:lhScheduleResult:remove")
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
    public AjaxResult validateChangeMachine(@RequestBody LhTransferDeskDTO dto){
        return iLhScheduleResultRemoteService.validateChangeMachine(dto);
    }

    @ApiOperation("转机台")
    @RequiresPermissions("lh:lhScheduleResult:changeMachine")
    @PostMapping("/changeMachine")
    @ResponseBody
    public AjaxResult changeMachine(@RequestBody LhTransferDeskDTO dto){
        return iLhScheduleResultRemoteService.changeMachine(dto);
    }

    @ApiOperation("调量")
    @RequiresPermissions("lh:lhScheduleResult:adjustQuantity")
    @PostMapping("/adjustQuantity")
    @ResponseBody
    public AjaxResult adjustQuantity(@RequestBody LhScheduleResultUpdateDTO dto){
        return iLhScheduleResultRemoteService.adjustQuantity(dto);
    }

    @ApiOperation("根据排程时间获取批次号")
    @RequiresPermissions("lh:lhScheduleResult:getBatchNo")
    @PostMapping("/getBatchNo")
    @ResponseBody
    public AjaxResult getBatchNo(@RequestBody AutoLhScheduleResultDTO dto){
        return AjaxResult.success(iLhScheduleResultRemoteService.getBatchNo(dto));
    }

    public String getProcedureCode() {
        return "0";
    }

    /**
     * 生产控制台导入
     */
    public String getFunctionName() {
        return getExportTemplateFileName();
    }

    /**
     * 文件模板文件名
     *
     * @return
     */
    public String getExportTemplateFileName() {
        return I18nUtil.getMessage("ui.data.column.lh.scheduleResult.modelName");
    }

    /**
     * 完成量下载模板
     *
     * @param response 下载
     * @throws IOException 异常
     */
    @ApiOperation("完成量下载模板")
    @GetMapping("/importFinishQtyTemplate")
    @ResponseBody
    public AjaxResult importFinishQtyTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.dayFinishQty.modelName");
        ExcelUtil<LhDayFinishQty> util = new ExcelUtil<>(LhDayFinishQty.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 完成量数据导入
     *
     * @param file 要导入的文件
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("lh:finishQty:import")
    @ApiOperation("完成量数据导入")
    @PostMapping("/importFinishQty")
    @ResponseBody
    public AjaxResult importFinishQty(MultipartFile file) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data,
                ApsConstant.PROCEDURE_CODE_LH,
                I18nUtil.getMessage("ui.data.column.dayFinishQty.modelName"),
                file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<LhDayFinishQty> util = new ExcelUtil<>(LhDayFinishQty.class);
        List<LhDayFinishQty> list = util.importExcel(in);

        AjaxResult ajaxResult = iLhScheduleResultRemoteService.importFinishQty(list, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 发布排程
     */
    @ApiOperation("发布排程")
    @RequiresPermissions("lh:scheduleResult:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(LhScheduleResult dto) {
        // 默认发布当天排程结果
        if (dto.getScheduleDate() == null) {
            dto.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return iLhScheduleResultRemoteService.publish(dto);
    }

    /**
     * 查询硫化机台甘特图
     *
     * @param queryVO 查询参数
     * @return 结果
     */
    @ApiOperation("查询硫化机台甘特图")
    @PostMapping("/selectMachineGantt")
    @ResponseBody
    public AjaxResult selectMachineGantt(LhGanttVo queryVO) throws ParseException {
//        queryVO.setScheduleDate(DateUtils.addDays(DateUtils.getNowDate("yyyy-MM-dd"), 1));
        return iLhScheduleResultRemoteService.selectMachineGantt(queryVO);
    }


    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("查询月硫化信息计划列表")
    @PostMapping("/monthFinishQtyList")
    @ResponseBody
    public TableDataInfo monthFinishQtyList(LhMonthPlanSurplusDetail queryVO) {
        return iLhScheduleResultRemoteService.monthFinishQtyList(queryVO);
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("查询月硫化信息统计")
    @PostMapping("/statisticsDay")
    @ResponseBody
    public AjaxResult statisticsDay(LhMonthPlanSurplusDetail queryVO) {
        return iLhScheduleResultRemoteService.statisticsDay(queryVO);
    }
}
