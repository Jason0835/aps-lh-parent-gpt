package com.zlt.aps.lh.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.lh.api.domain.dto.*;
import com.zlt.aps.mdm.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhScheduleResultTemplateImportVO;
import com.zlt.aps.lh.api.domain.vo.LhScheduleShiftDateVO;
import com.zlt.aps.lh.api.domain.vo.ScheduleSummaryReportVO;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.component.ScheduleExecutionGuard;
import com.zlt.aps.lh.mapper.LhScheduleResultMapper;
import com.zlt.aps.lh.service.ILhScheduleResultService;
import com.zlt.aps.lh.mapper.LhScheduleResultMapper;
import com.zlt.aps.lh.service.ILhScheduleService;
import com.zlt.aps.lh.service.IScheduleSummaryReportService;
import com.zlt.aps.lh.mapper.MdmMaterialInfoMapper;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.maindata.mapper.MdmSkuConstructionRefEntityMapper;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.mp.api.domain.entity.LhScheduleResultIssue;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.ExcelReadUtils;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 硫化排程控制器
 *
 * @author APS
 */
@Api(tags = "硫化排程接口")
@Slf4j
@RestController
@RequestMapping("/lhScheduleResult")
public class LhScheduleResultController extends AbstractDocBizController<LhScheduleResult> {

    @Autowired
    private ILhScheduleService lhScheduleService;

    @Autowired
    private LhScheduleResultMapper lhScheduleResultMapper;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Autowired
    private ILhScheduleResultService lhScheduleResultService;

    @Autowired
    private IMesItfService mesItfService;

    @Autowired
    private ScheduleExecutionGuard scheduleExecutionGuard;

    @Autowired
    private IScheduleSummaryReportService scheduleSummaryReportService;

    @Autowired
    private MdmMaterialInfoMapper mdmMaterialInfoMapper;

    @Autowired
    private MdmSkuConstructionRefEntityMapper mdmSkuConstructionRefEntityMapper;

    @Autowired
    private IImportLogService iImportLogService;

    /**
     * 获取排程日期对象列表
     *
     * @param query 排程请求参数
     * @return 排程响应结果
     */
    @PostMapping("/listScheduleShiftDates")
    @ApiOperation("排程日期对象列表")
    public List<LhScheduleShiftDateVO> listScheduleShiftDates(@RequestBody LhScheduleShiftDateQueryDTO query) {
        if (query == null) {
            return new ArrayList<>();
        }
        return lhScheduleService.listScheduleShiftDates(query.getScheduleDate());
    }

    @PostMapping("/execute")
    @ApiOperation("执行自动排程")
    public LhScheduleResponseDTO executeSchedule(@RequestBody LhScheduleRequestDTO request) {
        log.info("收到排程请求, 工厂: {}, 日期: {}", request.getFactoryCode(), LhScheduleTimeUtil.formatDate(request.getScheduleDate()));
        return lhScheduleService.executeSchedule(request);
    }

    /**
     * 发布排程结果到MES
     *
     * @param batchNo 批次号
     * @return 发布响应结果
     */
    @PostMapping("/publishSchedule/{batchNo}")
    @ApiOperation("发布排程结果到MES")
    public LhScheduleResponseDTO publishSchedule(@PathVariable("batchNo") String batchNo) {
        log.info("收到发布请求, 批次号: {}", batchNo);
        return lhScheduleService.publishSchedule(batchNo);
    }


    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhScheduleResult entity) {
        TableDataInfo tableDataInfo = super.list(entity);
        List<LhScheduleResult> list = (List<LhScheduleResult>) tableDataInfo.getRows();
        if (CollectionUtils.isNotEmpty(list)) {
            // 解码备注和原因分析字段的特殊字符
            list.forEach(this::decodeRemarkFields);
            // 构建硫化产量今天夜班Map（key: 工厂编码|物料编码）
            Map<String, Object> todayNightFinishQtyMap = lhScheduleService.buildTodayNightFinishQtyMap(list);
            // 为每条排程结果设置今天夜班产量
            for (LhScheduleResult result : list) {
                String key = StringUtils.defaultString(result.getFactoryCode()).trim()
                        + "|" + StringUtils.defaultString(result.getMaterialCode()).trim();
                Object qty = todayNightFinishQtyMap.get(key);
                if (qty instanceof BigDecimal) {
                    result.setTodayNightFinishQty((BigDecimal) qty);
                }
            }
        }
        return tableDataInfo;
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public LhScheduleResult getInfo(@PathVariable("billId") Long billId) {
        LhScheduleResult result = super.getInfo(billId);
        if (result != null) {
            decodeRemarkFields(result);
        }
        return result;
    }


    /**
     * 编辑
     */
    @Log(title = "ui.data.column.outDn.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation(value = "编辑", hidden = true)
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody LhScheduleResult entity) {
        try {
            return super.save(entity);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.save.error.msg"));
        }
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.outDn.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }



    /**
     * 导入数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.port.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData/{updateSupport}")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }


    /**
     * 导入数据
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.port.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importDataByCust/{updateSupport}")
    public AjaxResult importDataByCust(@PathVariable("updateSupport") boolean updateSupport, @RequestBody LhScheduleImportDTO importDTO) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportContext importContext = importDTO.getImportContext();
        LhScheduleResult result = importDTO.getScheduleResult();
        byte[] fileBytes = importContext.getFileBytes();
        String sheetName = "硫化计划主表";
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        ExcelUtil<LhScheduleResultTemplateImportVO> util = new ExcelUtil<>(LhScheduleResultTemplateImportVO.class);
        // 模板第1行为key表头，第9行开始是明细数据，因此表头行数传9，并关闭二级表头。
        List<LhScheduleResultTemplateImportVO> list = util.importExcel(
                sheetName, new ByteArrayInputStream(fileBytes), 0, 7, -1);
        AjaxResult ajaxResult = lhScheduleService.importScheduleTemplate(list,result, updateSupport, importLog.getId());
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(ajaxResult, this.iImportErrorLogService);
        return ajaxResult;
    }


    @ApiOperation(value = "模板下载" , notes = "导入模板下载")
    @PostMapping("/downloadTemplate/{fileName}")
    public byte[] downloadTemplate(@RequestBody LhScheduleResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        queryVO = queryVO == null ? new LhScheduleResult() : queryVO;
        List<LhScheduleResult> list = new ArrayList<>();
//        LhScheduleResult  lhScheduleResult = new LhScheduleResult();
//        lhScheduleResult.setScheduleDate(queryVO.getScheduleDate());
//        list.add(lhScheduleResult);
        byte[] resultBytes = lhScheduleService.exportData(list, queryVO.getScheduleDate());
        return resultBytes;
    }


    /**
     * 导出列表
     */
    @Log(title = "硫化排程导出", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LhScheduleResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        Date beginTime = DateUtils.getNowDate();
        List<LhScheduleResult> list = this.listExportData(queryVO);
        byte[] resultBytes = lhScheduleService.exportData(list, queryVO.getScheduleDate());
        Date endTime = DateUtils.getNowDate();
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        exportLog.setExportParams(queryVO.toString());
        String uri = ServletUtils.getRequest().getRequestURI();
        exportLog.setFunctionCode(uri.split("/")[1]);
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ".xlsx");
        exportLog.setRowCount(list.size());
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        this.iExportLogService.add(exportLog);
        return resultBytes;
    }

    /**
     * 导出前处理班次显示数据：收尾班后续班次字段置空、计划量为0置空
     *
     * @param list 排程结果列表
     */
    private void processExportShiftDisplay(List<LhScheduleResult> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        for (LhScheduleResult result : list) {
            int referenceQty = resolveReferenceQty(result);
            if (referenceQty <= 0) {
                clearZeroPlanQty(result);
                continue;
            }
            int totalPlanQty = ShiftFieldUtil.resolveScheduledQty(result);
            if (totalPlanQty < referenceQty) {
                clearZeroPlanQty(result);
                continue;
            }
            int endingShift = resolveEndingShift(result, referenceQty);
            for (int i = 1; i <= 8; i++) {
                if (i > endingShift) {
                    ShiftFieldUtil.setShiftPlanQty(result, i, null, null, null);
                    ShiftFieldUtil.setShiftFinishQty(result, i, null);
                    ShiftFieldUtil.setShiftAnalysis(result, i, null);
                } else {
                    Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, i);
                    if (planQty == null || planQty == 0) {
                        ShiftFieldUtil.setShiftPlanQty(result, i, null,
                                ShiftFieldUtil.getShiftStartTime(result, i),
                                ShiftFieldUtil.getShiftEndTime(result, i));
                        ShiftFieldUtil.setShiftFinishQty(result, i, null);
                        ShiftFieldUtil.setShiftAnalysis(result, i, null);
                    }
                }
            }
        }
    }

    /**
     * 取硫化余量和胎胚库存中的较大值作为收尾判断基准量
     *
     * @param result 排程结果
     * @return 基准量
     */
    private int resolveReferenceQty(LhScheduleResult result) {
        int surplusQty = result.getMouldSurplusQty() != null ? result.getMouldSurplusQty() : 0;
        int embryoStock = result.getEmbryoStock() != null ? result.getEmbryoStock() : 0;
        return Math.max(surplusQty, embryoStock);
    }

    /**
     * 定位收尾班次：逐班扣减基准量，基准量耗尽的班次即为收尾班
     *
     * @param result       排程结果
     * @param surplusQty 基准量（硫化余量与胎胚库存的较大值）
     * @return 收尾班次索引（1~8），无收尾返回8
     */
    private int resolveEndingShift(LhScheduleResult result, int surplusQty) {
        int remaining = surplusQty;
        for (int i = 1; i <= 8; i++) {
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, i);
            remaining -= (planQty != null ? planQty : 0);
            if (remaining <= 0) {
                return i;
            }
        }
        return 8;
    }

    /**
     * 将无排班班次的计划量、完成量、原因分析置空
     *
     * @param result 排程结果
     */
    private void clearZeroPlanQty(LhScheduleResult result) {
        for (int i = 1; i <= 8; i++) {
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, i);
            if (planQty == null || planQty == 0) {
                ShiftFieldUtil.setShiftPlanQty(result, i, null,
                        ShiftFieldUtil.getShiftStartTime(result, i),
                        ShiftFieldUtil.getShiftEndTime(result, i));
                ShiftFieldUtil.setShiftFinishQty(result, i, null);
                ShiftFieldUtil.setShiftAnalysis(result, i, null);
            }
        }
    }
    @Override
    protected List<LhScheduleResult> listExportData(LhScheduleResult obj) {
        QueryWrapper<LhScheduleResult> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        startPage(getOrderBy());
        return lhScheduleResultMapper.selectList(wrapper);
    }
    @Log(title = "ui.data.column.lhParams.modelName")
    @ApiOperation("插单查询可用机台列表")
    @PostMapping("/getScheduleMachineInfo")
    public List<LhMachineInfo> getScheduleMachineInfo(@RequestBody LhOrderInsertParamDTO insertParamDTO) {
        if (Objects.isNull(insertParamDTO)) {
            return Collections.emptyList();
        }
        //        return lhScheduleResultService.getScheduleMachineInfo(insertParamDTO);
        return buildMockScheduleMachineInfoList(insertParamDTO);
    }

    /**
     * 构造插单场景下可用硫化机台的联调测试数据（2 条），后续可替换为真实查询。
     *
     * @param param 插单查询参数（分厂与物料等用于对齐展示字段）
     * @return 模拟机台列表
     */
    private List<LhMachineInfo> buildMockScheduleMachineInfoList(LhOrderInsertParamDTO param) {
        String factoryCode = StringUtils.isNotEmpty(param.getFactoryCode())
                ? param.getFactoryCode()
                : MOCK_SCHEDULE_MACHINE_FACTORY_DEFAULT;

        List<LhMachineInfo> list = new ArrayList<>(2);

        LhMachineInfo first = new LhMachineInfo();
        first.setFactoryCode(factoryCode);
        first.setMachineCode(MOCK_SCHEDULE_MACHINE_CODE_ALL_STEEL);
        first.setMachineName("全钢液压硫化机-A线");
        first.setDimension(new BigDecimal("22.5"));
        first.setDimensionMinimum(new BigDecimal("20.0"));
        first.setDimensionMaximum(new BigDecimal("24.0"));
        first.setCentripetalMechanism(MOCK_SCHEDULE_MACHINE_IS_HAVE_YES);
        first.setClassShift(MOCK_SCHEDULE_MACHINE_CLASS_SHIFT_THREE);
        first.setMaxMoldNum(MOCK_SCHEDULE_MACHINE_MAX_MOLD_NUM);
        first.setQuota(MOCK_SCHEDULE_MACHINE_QUOTA_ALL_STEEL);
        first.setOpenMachineClass(MOCK_SCHEDULE_MACHINE_OPEN_CLASS_MORNING);
        first.setStatus(MOCK_SCHEDULE_MACHINE_STATUS_ENABLED);
        first.setMachineType(MOCK_SCHEDULE_MACHINE_TYPE_ALL_STEEL);
        first.setMachineOrder(10);
        first.setManufacturer("示例制造A");
        first.setHotPlateDiameter("1800");
//        first.setMouldSetCode("通用");
        list.add(first);

        LhMachineInfo second = new LhMachineInfo();
        second.setFactoryCode(factoryCode);
        second.setMachineCode(MOCK_SCHEDULE_MACHINE_CODE_HALF_STEEL);
        second.setMachineName("半钢机械硫化机-B线");
        second.setDimension(new BigDecimal("17.0"));
        second.setDimensionMinimum(new BigDecimal("15.0"));
        second.setDimensionMaximum(new BigDecimal("20.0"));
        second.setCentripetalMechanism(MOCK_SCHEDULE_MACHINE_IS_HAVE_NO);
        second.setClassShift(MOCK_SCHEDULE_MACHINE_CLASS_SHIFT_THREE);
        second.setMaxMoldNum(MOCK_SCHEDULE_MACHINE_MAX_MOLD_NUM);
        second.setQuota(MOCK_SCHEDULE_MACHINE_QUOTA_HALF_STEEL);
        second.setOpenMachineClass(MOCK_SCHEDULE_MACHINE_OPEN_CLASS_MIDDLE);
        second.setStatus(MOCK_SCHEDULE_MACHINE_STATUS_ENABLED);
        second.setMachineType(MOCK_SCHEDULE_MACHINE_TYPE_HALF_STEEL);
        second.setMachineOrder(20);
        second.setManufacturer("示例制造B");
        second.setHotPlateDiameter("1650");
//        second.setMouldSetCode("通用");
        list.add(second);

        return list;
    }

    /** 插单可用机台模拟：请求未带分厂时的默认分厂编码 */
    private static final String MOCK_SCHEDULE_MACHINE_FACTORY_DEFAULT = "116";

    /**
     * 解码备注字段中的特殊字符转义
     *
     * @param remark 备注内容
     * @return 解码后的备注内容
     */
    private String decodeRemark(String remark) {
        if (remark == null) {
            return null;
        }
        return remark.replace("__PERCENT__", "%")
                     .replace("__AMP__", "&")
                     .replace("__LT__", "<")
                     .replace("__GT__", ">")
                     .replace("__QUOT__", "\"")
                     .replace("__APOS__", "'");
    }

    /**
     * 解码排程结果中需要转义的字段：备注 + 8个班次原因分析
     *
     * @param result 排程结果
     */
    private void decodeRemarkFields(LhScheduleResult result) {
        result.setRemark(decodeRemark(result.getRemark()));
        for (int i = 1; i <= 8; i++) {
            ShiftFieldUtil.setShiftAnalysis(result, i, decodeRemark(ShiftFieldUtil.getShiftAnalysis(result, i)));
        }
    }
    /** 插单可用机台模拟：字典 sys_enable_disable 启用 */
    private static final String MOCK_SCHEDULE_MACHINE_STATUS_ENABLED = "0";
    /** 插单可用机台模拟：字典 IS_HAVE 有向心机构 */
    private static final String MOCK_SCHEDULE_MACHINE_IS_HAVE_YES = "1";
    /** 插单可用机台模拟：字典 IS_HAVE 无向心机构 */
    private static final String MOCK_SCHEDULE_MACHINE_IS_HAVE_NO = "0";
    /** 插单可用机台模拟：字典 CLASS_SHIFT 三班制（示例值，以现场字典为准） */
    private static final String MOCK_SCHEDULE_MACHINE_CLASS_SHIFT_THREE = "1";
    /** 插单可用机台模拟：字典 CLASS_NUM 早班（示例值） */
    private static final String MOCK_SCHEDULE_MACHINE_OPEN_CLASS_MORNING = "1";
    /** 插单可用机台模拟：字典 CLASS_NUM 中班（示例值） */
    private static final String MOCK_SCHEDULE_MACHINE_OPEN_CLASS_MIDDLE = "2";
    /** 插单可用机台模拟：字典 LH_MACHINE_TYPE 全钢（示例值） */
    private static final String MOCK_SCHEDULE_MACHINE_TYPE_ALL_STEEL = "1";
    /** 插单可用机台模拟：字典 LH_MACHINE_TYPE 半钢（示例值） */
    private static final String MOCK_SCHEDULE_MACHINE_TYPE_HALF_STEEL = "2";
    private static final String MOCK_SCHEDULE_MACHINE_CODE_ALL_STEEL = "LH-VUL-001";
    private static final String MOCK_SCHEDULE_MACHINE_CODE_HALF_STEEL = "LH-VUL-002";
    private static final int MOCK_SCHEDULE_MACHINE_MAX_MOLD_NUM = 2;
    private static final int MOCK_SCHEDULE_MACHINE_QUOTA_ALL_STEEL = 120;
    private static final int MOCK_SCHEDULE_MACHINE_QUOTA_HALF_STEEL = 100;

    @Log(title = "ui.data.column.lhParams.modelName")
    @ApiOperation("插单校验")
    @PostMapping("/validateInsertOrder")
    public AjaxResult validateInsertOrder(@RequestBody LhOrderInsertDTO insertDTO) {
        LhInsertOrderValidateResultDTO result = lhScheduleResultService.validateInsertOrder(insertDTO);
        return AjaxResult.success(result);
    }

    @Log(title = "ui.data.column.lhParams.modelName")
    @ApiOperation("插单")
    @PostMapping("/insertOrder")
    public AjaxResult insertOrder(@RequestBody LhOrderInsertDTO insertDTO){
        LhInsertOrderValidateResultDTO validateResult = lhScheduleResultService.validateInsertOrder(insertDTO);
        if (!validateResult.isValid()) {
            return AjaxResult.error(String.join(";", validateResult.getErrorMessages()));
        }
        lhScheduleResultService.insertOrder(insertDTO);
        return AjaxResult.success();
    }


    /**
     * 转机台校验
     */
    @Log(title = "ui.data.column.lhParams.modelName")
    @PostMapping("/validateChangeMachine")
    @ApiOperation("硫化排程结果转机台校验")
    public AjaxResult validateChangeMachine(@RequestBody LhTransferDeskDTO dto) {
        return lhScheduleService.changeMachinePreCheck(dto);
    }

    /**
     * 转机台
     * @param dto 请求参数
     * @return 结果
     */
    @PostMapping("/changeMachine")
    @ApiOperation("转机台")
    public AjaxResult changeMachine(@RequestBody LhTransferDeskDTO dto) {
        AjaxResult ajaxResult = lhScheduleService.changeMachinePreCheck(dto);
        if (ajaxResult.get(AjaxResult.CODE_TAG).equals(AjaxResult.Type.ERROR.value())) {
            return ajaxResult;
        }
        //调用转机台业务
        return lhScheduleService.changeMachine(dto);
    }

    /**
     * 转机台校验
     */
    @Log(title = "ui.data.column.lhParams.modelName")
    @PostMapping("/validateAdjustQuantity")
    @ApiOperation("硫化排程结果转机台校验")
    public AjaxResult validateAdjustQuantity(@RequestBody LhScheduleResultUpdateDTO dto) {
        return lhScheduleService.adjustQuantityPreCheck(dto);
    }

    @Log(title = "ui.data.column.lhParams.modelName")
    @PostMapping("/adjustQuantity")
    @ApiOperation("调量")
    public AjaxResult adjustQuantity(@RequestBody LhScheduleResultUpdateDTO dto) {
        AjaxResult ajaxResult = lhScheduleService.adjustQuantityPreCheck(dto);
        if (ajaxResult.get(AjaxResult.CODE_TAG).equals(AjaxResult.Type.ERROR.value())) {
            return ajaxResult;
        }
        //调用调量业务
        return lhScheduleService.adjustQuantity(dto);
    }

    /**
     * 文字示方调整
     * @param dto
     * @return
     */
    @PostMapping("/adjustTextNo")
    @ApiOperation("文字示方调整")
    public AjaxResult adjustTextNo(@RequestBody LhTransferDeskDTO dto) {
        return AjaxResult.success();
    }

    /**
     * 生成文字示方换模计划。
     *
     * @param dto 生成入参
     * @return 处理结果
     */
    @PostMapping("/generateTextMouldChangePlan")
    @ApiOperation("生成文字示方换模计划")
    public AjaxResult generateTextMouldChangePlan(@RequestBody LhGenerateTextMouldPlanDTO dto) {
        return lhScheduleService.generateTextMouldChangePlan(dto);
    }

    /**
     * 换模开产增加计划。
     *
     * @param scheduleResult 当前硫化排程结果
     * @return 处理结果
     */
    @PostMapping("/increaseMouldStartPlan")
    @ApiOperation("换模开产增加计划")
    public AjaxResult increaseMouldStartPlan(@RequestBody LhScheduleResult scheduleResult) {
        return lhScheduleService.increaseMouldStartPlan(scheduleResult);
    }

    /**
     * 发布选中的排程结果到MES
     * 发布流程：1.更新发布状态为"待发布" → 2.调用issueToMes下发MES → 3.根据MES反馈更新发布状态
     * 业务规则：
     * 每条硫化排程结果自带8班数据，覆盖排程日期前2天到排程日期当天：
     * - T-2日（窗口首日）：更新夜早中3班
     * - T-1日（窗口次日）：更新夜早中3班
     * - T日（排程日期）：下发早中2班（夜班尚未排产）
     */
    @Log(title = "ui.data.column.lh.scheduleResult.modelName", businessType = BusinessType.PUBLISH)
    @ApiOperation("发布排程")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody LhScheduleResult dto, @RequestParam(value = "ids", required = false) String ids) {
        if (dto.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.errorPublish"));
        }

        List<LhScheduleResult> list = lhScheduleResultService.selectByDateAndFactory(
                dto.getScheduleDate(), dto.getFactoryCode());

        if (StringUtils.isNotEmpty(ids)) {
            List<Long> idList = Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            list = list.stream()
                    .filter(item -> idList.contains(item.getId()))
                    .collect(Collectors.toList());
        }

        List<LhScheduleResult> filteredList = list.stream()
                .filter(item -> ApsConstant.NO_RELEASE.equals(item.getIsRelease())
                        || ApsConstant.FAILURE_RELEASE.equals(item.getIsRelease())
                        || ApsConstant.WAIT_RELEASING.equals(item.getIsRelease()))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(filteredList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.errorPublish"));
        }

        List<LhScheduleResult> invalidRecords = filteredList.stream()
                .filter(item -> StringUtils.isEmpty(item.getLhMachineCode()) || item.getLhMachineCode().contains(","))
                .collect(Collectors.toList());
        if (!invalidRecords.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.hasMultipleIds"));
        }

        List<Long> selectedIds = filteredList.stream().map(BaseEntity::getId).collect(Collectors.toList());

        try {
            String token = scheduleExecutionGuard.acquireIssueLock();
            if (token == null) {
                return AjaxResult.error("排程下发操作正在进行中，请稍后再试");
            }
            try {
                AjaxResult issueResult = doIssueLhScheduleResultToMes(dto.getScheduleDate(), selectedIds);
                if (issueResult != null && Objects.equals(HttpStatus.SUCCESS, issueResult.get(AjaxResult.CODE_TAG))) {
                    for (LhScheduleResult item : filteredList) {
                        item.setIsRelease(ApsConstant.IS_RELEASE);
                        lhScheduleResultService.updateReleaseStatus(item);
                    }
                    return AjaxResult.success(I18nUtil.getMessage("ui.data.column.scheduleResult.successPublish"));
                } else {
                    for (LhScheduleResult item : filteredList) {
                        item.setIsRelease(ApsConstant.FAILURE_RELEASE);
                        lhScheduleResultService.updateReleaseStatus(item);
                    }
                    return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.failedPublish"));
                }
            } finally {
                scheduleExecutionGuard.releaseIssueLock(token);
            }
        } catch (Exception e) {
            log.error("硫化排程发布失败", e);
            for (LhScheduleResult item : filteredList) {
                item.setIsRelease(ApsConstant.FAILURE_RELEASE);
                lhScheduleResultService.updateReleaseStatus(item);
            }
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.failedPublish"));
        }
    }



    /**
     * 硫化排程结果下发到MES
     * 背景逻辑：
     * 1. 每条硫化排程结果自带8班数据，覆盖排程日期前2天到排程日期当天
     * 2. 8班对应关系（使用aps-cx-lh-api实体）：
     *    - 1-3班：T-2日的夜、早、中班（窗口首日，更新）
     *    - 4-6班：T-1日的夜、早、中班（窗口次日，更新）
     *    - 7-8班：T日的早、中班（排程日期当天，下发，夜班尚未排产不下发）
     * 3. 中间表映射：1班=夜班，2班=早班，3班=中班
     * 4. T-2日、T-1日数据更新（存在则更新，不存在则插入）
     * 5. T日数据下发（先删除后插入）
     *
     * @param scheduleDate 排程日期（窗口最后一天）
     * @return 下发结果
     */
    @ApiOperation(value = "硫化排程结果下发到MES", notes = "将硫化排程结果下发到MES中间表，8班数据对应3天班次")
    @Log(title = "硫化排程结果下发", businessType = BusinessType.PUBLISH)
    @PostMapping("/issueToMes")
    public AjaxResult issueLhScheduleResultToMes(@RequestParam(value = "scheduleDate", required = false) Date scheduleDate) {
        if (scheduleDate == null) {
            scheduleDate = java.sql.Date.valueOf(LocalDate.now());
        }
        String token = scheduleExecutionGuard.acquireIssueLock();
        if (token == null) {
            return AjaxResult.error("排程下发操作正在进行中，请稍后再试");
        }
        try {
            return doIssueLhScheduleResultToMes(scheduleDate, null);
        } finally {
            scheduleExecutionGuard.releaseIssueLock(token);
        }
    }

    /**
     * 执行硫化排程结果下发到MES
     * 支持两种模式：
     * 1. 按选中ID下发：selectedIds不为空时，按ID查询选中记录下发
     * 2. 按排程日期全量下发：selectedIds为空时，查询排程日期下所有未下发记录
     *
     * @param scheduleDate 排程日期
     * @param selectedIds  选中的记录ID列表，为空时按日期全量查询
     * @return 下发结果
     */
    private AjaxResult doIssueLhScheduleResultToMes(Date scheduleDate, List<Long> selectedIds) {
        LocalDate scheduleLocalDate = scheduleDate instanceof java.sql.Date 
                ? ((java.sql.Date) scheduleDate).toLocalDate() 
                : scheduleDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        LocalDate day1 = scheduleLocalDate.minusDays(LhScheduleConstant.SCHEDULE_DAYS - 1);
        LocalDate day2 = scheduleLocalDate.minusDays(LhScheduleConstant.SCHEDULE_DAYS - 2);
        LocalDate day3 = scheduleLocalDate;

        List<com.zlt.aps.cx.entity.schedule.LhScheduleResult> scheduleResultList;
        if (CollectionUtils.isNotEmpty(selectedIds)) {
            scheduleResultList = lhScheduleResultService.getCxLhScheduleResultListByIds(selectedIds);
        } else {
            scheduleResultList = lhScheduleResultService.getCxLhScheduleResultList(java.sql.Date.valueOf(scheduleLocalDate));
            scheduleResultList = scheduleResultList.stream()
                    .filter(item -> !ApsConstant.IS_RELEASE.equals(item.getIsRelease()))
                    .collect(Collectors.toList());
        }

        if (scheduleResultList.isEmpty()) {
            return AjaxResult.error("没有需要下发的硫化排程结果数据");
        }

        List<LhScheduleResultIssue> day1IssueList = new ArrayList<>();
        List<LhScheduleResultIssue> day2IssueList = new ArrayList<>();
        List<LhScheduleResultIssue> day3IssueList = new ArrayList<>();

        for (com.zlt.aps.cx.entity.schedule.LhScheduleResult source : scheduleResultList) {
            LhScheduleResultIssue day1Issue = convertToDay1IssueEntity(source, day1);
            if (day1Issue != null) {
                day1IssueList.add(day1Issue);
            }

            LhScheduleResultIssue day2Issue = convertToDay2IssueEntity(source, day2);
            if (day2Issue != null) {
                day2IssueList.add(day2Issue);
            }

            LhScheduleResultIssue day3Issue = convertToDay3IssueEntity(source, day3);
            if (day3Issue != null) {
                day3IssueList.add(day3Issue);
            }
        }

        if (day1IssueList.isEmpty() && day2IssueList.isEmpty() && day3IssueList.isEmpty()) {
            return AjaxResult.error("没有需要下发的硫化排程结果数据");
        }

        List<LhScheduleResultIssue> allIssueList = new ArrayList<>();
        allIssueList.addAll(day1IssueList);
        allIssueList.addAll(day2IssueList);
        allIssueList.addAll(day3IssueList);

        // 下发前补全MES物料编码和示方号（数据准备应在aps-lh模块完成，而非itf层）
        enrichMaterialAndExampleInfo(allIssueList);

        return mesItfService.issueLhScheduleResult(allIssueList);
    }


    /**
     * 补全MES物料编码和示方号
     * 1. 通过物料编码关联物料信息表(MdmMaterialInfo)获取MES物料编码
     * 2. 通过物料编码关联SKU与示方书关系表(MdmSkuConstructionRef)获取硫化示方书号作为示方号
     * 3个班的示方号都取同一个值
     *
     * @param issueList 硫化排程结果下发列表
     */
    private void enrichMaterialAndExampleInfo(List<LhScheduleResultIssue> issueList) {
        if (CollectionUtils.isEmpty(issueList)) {
            return;
        }

        // 收集所有不重复的物料编码
        List<String> materialCodeList = issueList.stream()
                .map(LhScheduleResultIssue::getMaterialCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(materialCodeList)) {
            return;
        }

        // 查询物料信息表，构建物料编码 -> MES物料编码的映射
        Map<String, String> materialCodeToMesCodeMap = getMaterialCodeToMesCodeMap(materialCodeList);

        // 查询SKU与示方书关系表，构建物料编码 -> 硫化示方书号的映射
        Map<String, String> materialCodeToLhNoMap = getMaterialCodeToLhNoMap(materialCodeList);

        // 补全每条记录的MES物料编码和示方号
        for (LhScheduleResultIssue item : issueList) {
            String materialCode = item.getMaterialCode();
            if (StringUtils.isNotBlank(materialCode)) {
                // 设置MES物料编码
                String mesMaterialCode = materialCodeToMesCodeMap.get(materialCode);
                if (StringUtils.isNotBlank(mesMaterialCode)) {
                    item.setMesMaterialCode(mesMaterialCode);
                }

                // 设置3个班的示方号（硫化示方书号），3个班的示方号都取同一个值
                String lhNo = materialCodeToLhNoMap.get(materialCode);
                if (StringUtils.isNotBlank(lhNo)) {
                    item.setClass1ExampleNo(lhNo);
                    item.setClass2ExampleNo(lhNo);
                    item.setClass3ExampleNo(lhNo);
                }
            }
        }
    }

    /**
     * 获取物料编码到MES物料编码的映射
     * 通过物料编码关联物料信息表(MdmMaterialInfo)获取MES物料编码
     *
     * @param materialCodeList 物料编码列表
     * @return 物料编码 -> MES物料编码的映射
     */
    private Map<String, String> getMaterialCodeToMesCodeMap(List<String> materialCodeList) {
        if (CollectionUtils.isEmpty(materialCodeList)) {
            return new HashMap<>();
        }

        LambdaQueryWrapper<MdmMaterialInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(MdmMaterialInfo::getMaterialCode, materialCodeList)
                .select(MdmMaterialInfo::getMaterialCode, MdmMaterialInfo::getMesMaterialCode);

        List<MdmMaterialInfo> materialInfoList = mdmMaterialInfoMapper.selectList(queryWrapper);

        if (CollectionUtils.isEmpty(materialInfoList)) {
            return new HashMap<>();
        }

        return materialInfoList.stream()
                .filter(item -> StringUtils.isNotBlank(item.getMesMaterialCode()))
                .collect(Collectors.toMap(
                        MdmMaterialInfo::getMaterialCode,
                        MdmMaterialInfo::getMesMaterialCode,
                        (v1, v2) -> v1
                ));
    }

    /**
     * 获取物料编码到硫化示方书号的映射
     * 通过物料编码关联SKU与示方书关系表(MdmSkuConstructionRef)获取硫化示方书号
     *
     * @param materialCodeList 物料编码列表
     * @return 物料编码 -> 硫化示方书号的映射
     */
    private Map<String, String> getMaterialCodeToLhNoMap(List<String> materialCodeList) {
        if (CollectionUtils.isEmpty(materialCodeList)) {
            return new HashMap<>();
        }

        LambdaQueryWrapper<MdmSkuConstructionRef> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(MdmSkuConstructionRef::getMaterialCode, materialCodeList)
                .select(MdmSkuConstructionRef::getMaterialCode, MdmSkuConstructionRef::getLhNo);

        List<MdmSkuConstructionRef> constructionRefList = mdmSkuConstructionRefEntityMapper.selectList(queryWrapper);

        if (CollectionUtils.isEmpty(constructionRefList)) {
            return new HashMap<>();
        }

        return constructionRefList.stream()
                .filter(item -> StringUtils.isNotBlank(item.getLhNo()))
                .collect(Collectors.toMap(
                        MdmSkuConstructionRef::getMaterialCode,
                        MdmSkuConstructionRef::getLhNo,
                        (v1, v2) -> v1
                ));
    }

    /**
     * 转换为T-2日（窗口首日）的下发实体
     * 8班数据：1班(夜)、2班(早)、3班(中) -> 中间表：1班(夜)=1班, 2班(早)=2班, 3班(中)=3班
     * 业务规则：更新夜早中3班数据（存在则更新，不存在则插入）
     * 使用aps-cx-lh-api实体（有8班数据）
     */
    private LhScheduleResultIssue convertToDay1IssueEntity(com.zlt.aps.cx.entity.schedule.LhScheduleResult source, LocalDate scheduleDate) {
        if (source == null) {
            return null;
        }

        LhScheduleResultIssue target = new LhScheduleResultIssue();

        // 中间表id由数据库自增生成，不能直接使用APS排程结果表的主键id
        target.setId(null);
        target.setLhBatchNo(source.getBatchNo());
        target.setOrderNo(source.getOrderNo());
        target.setScheduleDate(scheduleDate.atStartOfDay());

        target.setLhMachineCode(source.getLhMachineCode());
        target.setLhMachineName(source.getLhMachineName());
        target.setLeftRightMold(source.getLeftRightMould());

        target.setMaterialCode(source.getMaterialCode());
        target.setMesMaterialCode(null);
        target.setSpecCode(source.getSpecCode());
        target.setSpecDesc(source.getSpecDesc());
        target.setDailyPlanQty(source.getDailyPlanQty());

        // 中间表1班 = 夜班（对应APS 1班）
        target.setClass1PlanQtySeq(1);
        target.setClass1AnalysisInput(null);
        target.setClass1Analysis(source.getClass1Analysis());
        target.setClass1PlanQty(source.getClass1PlanQty());
        target.setClass1ExampleType(source.getConstructionStage());
        target.setClass1ExampleNo(null);

        // 中间表2班 = 早班（对应APS 2班）
        target.setClass2PlanQtySeq(2);
        target.setClass2AnalysisInput(null);
        target.setClass2Analysis(source.getClass2Analysis());
        target.setClass2PlanQty(source.getClass2PlanQty());
        target.setClass2ExampleType(source.getConstructionStage());
        target.setClass2ExampleNo(null);

        // 中间表3班 = 中班（对应APS 3班）
        target.setClass3PlanQtySeq(3);
        target.setClass3AnalysisInput(null);
        target.setClass3Analysis(source.getClass3Analysis());
        target.setClass3PlanQty(source.getClass3PlanQty());
        target.setClass3ExampleType(source.getConstructionStage());
        target.setClass3ExampleNo(null);

        target.setLhTime(source.getLhTime());

        target.setDataVersion(null);
        target.setCompanyCode(source.getFactoryCode());
        target.setFactoryCode(source.getFactoryCode());

        return target;
    }

    /**
     * 转换为T-1日（窗口次日）的下发实体
     * 8班数据：4班(夜)、5班(早)、6班(中) -> 中间表：1班(夜)=4班, 2班(早)=5班, 3班(中)=6班
     * 业务规则：更新夜早中3班数据（存在则更新，不存在则插入）
     * 使用aps-cx-lh-api实体（有8班数据）
     */
    private LhScheduleResultIssue convertToDay2IssueEntity(com.zlt.aps.cx.entity.schedule.LhScheduleResult source, LocalDate scheduleDate) {
        if (source == null) {
            return null;
        }

        LhScheduleResultIssue target = new LhScheduleResultIssue();

        // 中间表id由数据库自增生成，不能直接使用APS排程结果表的主键id
        target.setId(null);
        target.setLhBatchNo(source.getBatchNo());
        target.setOrderNo(source.getOrderNo());
        target.setScheduleDate(scheduleDate.atStartOfDay());

        target.setLhMachineCode(source.getLhMachineCode());
        target.setLhMachineName(source.getLhMachineName());
        target.setLeftRightMold(source.getLeftRightMould());

        target.setMaterialCode(source.getMaterialCode());
        target.setMesMaterialCode(null);
        target.setSpecCode(source.getSpecCode());
        target.setSpecDesc(source.getSpecDesc());
        target.setDailyPlanQty(source.getDailyPlanQty());

        // 中间表1班 = 夜班（对应APS 4班）
        target.setClass1PlanQtySeq(1);
        target.setClass1AnalysisInput(null);
        target.setClass1Analysis(source.getClass4Analysis());
        target.setClass1PlanQty(source.getClass4PlanQty());
        target.setClass1ExampleType(source.getConstructionStage());
        target.setClass1ExampleNo(null);

        // 中间表2班 = 早班（对应APS 5班）
        target.setClass2PlanQtySeq(2);
        target.setClass2AnalysisInput(null);
        target.setClass2Analysis(source.getClass5Analysis());
        target.setClass2PlanQty(source.getClass5PlanQty());
        target.setClass2ExampleType(source.getConstructionStage());
        target.setClass2ExampleNo(null);

        // 中间表3班 = 中班（对应APS 6班）
        target.setClass3PlanQtySeq(3);
        target.setClass3AnalysisInput(null);
        target.setClass3Analysis(source.getClass6Analysis());
        target.setClass3PlanQty(source.getClass6PlanQty());
        target.setClass3ExampleType(source.getConstructionStage());
        target.setClass3ExampleNo(null);

        target.setLhTime(source.getLhTime());

        target.setDataVersion(null);
        target.setCompanyCode(source.getFactoryCode());
        target.setFactoryCode(source.getFactoryCode());

        return target;
    }

    /**
     * 转换为T日（排程日期当天）的下发实体
     * 8班数据：7班(早)、8班(中) -> 中间表：2班(早)=7班, 3班(中)=8班
     * 业务规则：只下发早中2班数据（夜班尚未排产，不下发），先删除后插入
     * 使用aps-cx-lh-api实体（有8班数据）
     */
    private LhScheduleResultIssue convertToDay3IssueEntity(com.zlt.aps.cx.entity.schedule.LhScheduleResult source, LocalDate scheduleDate) {
        if (source == null) {
            return null;
        }

        LhScheduleResultIssue target = new LhScheduleResultIssue();

        // 中间表id由数据库自增生成，不能直接使用APS排程结果表的主键id
        target.setId(null);
        target.setLhBatchNo(source.getBatchNo());
        target.setOrderNo(source.getOrderNo());
        target.setScheduleDate(scheduleDate.atStartOfDay());

        target.setLhMachineCode(source.getLhMachineCode());
        target.setLhMachineName(source.getLhMachineName());
        target.setLeftRightMold(source.getLeftRightMould());

        target.setMaterialCode(source.getMaterialCode());
        target.setMesMaterialCode(null);
        target.setSpecCode(source.getSpecCode());
        target.setSpecDesc(source.getSpecDesc());
        target.setDailyPlanQty(source.getDailyPlanQty());

        // T日夜班尚未排产，中间表1班(夜)不赋值

        // 中间表2班 = 早班（对应APS 7班）
        target.setClass2PlanQtySeq(2);
        target.setClass2AnalysisInput(null);
        target.setClass2Analysis(source.getClass7Analysis());
        target.setClass2PlanQty(source.getClass7PlanQty());
        target.setClass2ExampleType(source.getConstructionStage());
        target.setClass2ExampleNo(null);

        // 中间表3班 = 中班（对应APS 8班）
        target.setClass3PlanQtySeq(3);
        target.setClass3AnalysisInput(null);
        target.setClass3Analysis(source.getClass8Analysis());
        target.setClass3PlanQty(source.getClass8PlanQty());
        target.setClass3ExampleType(source.getConstructionStage());
        target.setClass3ExampleNo(null);

        target.setLhTime(source.getLhTime());

        target.setDataVersion(null);
        target.setCompanyCode(source.getFactoryCode());
        target.setFactoryCode(source.getFactoryCode());

        return target;
    }



    /**
     * 查询条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<LhScheduleResult> queryWrapper, LhScheduleResult queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDate")), "SCHEDULE_DATE", queryVO.getFieldValueByFieldName("scheduleDate"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lhMachineCode")), "LH_MACHINE_CODE", queryVO.getFieldValueByFieldName("lhMachineCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mainMaterialDesc")), "MAIN_MATERIAL_DESC", queryVO.getFieldValueByFieldName("mainMaterialDesc"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specCode")), "SPEC_CODE", queryVO.getFieldValueByFieldName("specCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specDesc")), "SPEC_DESC", queryVO.getFieldValueByFieldName("specDesc"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoCode")), "EMBRYO_CODE", queryVO.getFieldValueByFieldName("embryoCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isRelease")), "IS_RELEASE", queryVO.getFieldValueByFieldName("isRelease"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("batchNo")), "BATCH_NO", queryVO.getFieldValueByFieldName("batchNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("orderNo")), "ORDER_NO", queryVO.getFieldValueByFieldName("orderNo"));
    }

    @Override
    protected IDocService getDocService() {
        return lhScheduleService;
    }


    @Override
    protected String getTypeCode() {
        return "LH_SCHEDULE_RESULT";
    }

    @Override
    protected String getOrderBy() {
        return "schedule_date desc, lh_machine_code asc";
    }

    /**
     * 排产小结报表导出
     *
     * @param queryVO 查询条件，包含排程日期和分厂编码
     * @param fileName 导出文件名
     * @return Excel文件字节数组
     */
    @Log(title = "排产小结导出", businessType = BusinessType.EXPORT)
    @ApiOperation("排产小结报表导出")
    @PostMapping("/exportScheduleSummaryReport/{fileName}")
    public byte[] exportScheduleSummaryReport(@RequestBody ScheduleSummaryReportVO queryVO,
                                               @PathVariable("fileName") String fileName) {
        return scheduleSummaryReportService.exportScheduleSummaryReport(queryVO);
    }

}
