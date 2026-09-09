package com.zlt.aps.lh.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.ExcelStyleVo;
import com.zlt.aps.common.core.utils.ApsCommonUtil;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.dto.LhScheduleImportDTO;
import com.zlt.aps.lh.api.domain.entity.*;
import com.zlt.aps.lh.api.domain.vo.LhMouldChangePlanVo;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.api.enums.ReleaseStatusEnum;
import com.zlt.aps.lh.component.OrderNoGenerator;
import com.zlt.aps.lh.mapper.LhMachineOnlineInfoMapper;
import com.zlt.aps.lh.mapper.LhMouldChangePlanEntityMapper;
import com.zlt.aps.lh.mapper.LhScheduleResultMapper;
import com.zlt.aps.lh.mapper.LhSharedMouldPatEntityMapper;
import com.zlt.aps.lh.service.ILhMachineOnlineInfoService;
import com.zlt.aps.lh.service.ILhMouldChangePlanService;
import com.zlt.aps.lh.service.ILhParamsService;
import com.zlt.aps.lh.service.ILhScheduleResultService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：LhMouldChangePlanController.java
* 描    述：模具交替计划 控制层类
*@author APS Team
*@date 2026-04-01
*@version 1.0
*
 * 修改记录：
*     修改时间：...
*     修 改 人：...
*     修改内容：...
*/
@Slf4j
@Api(tags = "模具交替计划")
@RestController
@RequestMapping("/lhMouldChangePlan")
public class LhMouldChangePlanController extends AbstractDocBizController<LhMouldChangePlan> {

    @Autowired
    private ILhMouldChangePlanService lhMouldChangePlanService;

    @Resource
    private LhMouldChangePlanEntityMapper lhMouldChangePlanMapper;
    @Resource
    private OrderNoGenerator orderNoGenerator;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private ILhMachineOnlineInfoService lhMachineOnlineInfoService;

    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;

    @Autowired
    private LhSharedMouldPatEntityMapper lhSharedMouldPatEntityMapper;

    @Autowired
    private LhMachineOnlineInfoMapper lhMachineOnlineInfoMapper;

    @Autowired
    private ILhParamsService lhParamsService;

    @Autowired
    private LhScheduleResultMapper lhScheduleResultMapper;

    @Autowired
    private ILhScheduleResultService lhScheduleResultService;

    /**
     * 中心计划日期使用的Excel字体颜色。
     */
    private static final short MIDDLE_PLAN_DATE_FONT_COLOR = IndexedColors.RED.index;

    /**
     * 前日不存在对应交替计划时，后物料描述使用的Excel字体颜色。
     */
    private static final short MISSING_PREVIOUS_PLAN_FONT_COLOR = IndexedColors.RED.index;

    /**
     * 查询模具交替计划列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhMouldChangePlan queryVO) {
        TableDataInfo tableDataInfo = super.list(queryVO);
        this.fillDisplayMouldCode(tableDataInfo, queryVO);
        return tableDataInfo;
    }

    /**
     * 为列表当前页数据补充最终展示模具号。
     *
     * @param tableDataInfo 分页查询结果
     * @param queryVO       查询条件
     */
    private void fillDisplayMouldCode(TableDataInfo tableDataInfo, LhMouldChangePlan queryVO) {
        if (tableDataInfo == null || CollectionUtils.isEmpty(tableDataInfo.getRows())) {
            return;
        }
        List<LhMouldChangePlan> planList = tableDataInfo.getRows().stream()
                .filter(LhMouldChangePlan.class::isInstance)
                .map(LhMouldChangePlan.class::cast)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(planList)) {
            return;
        }

        List<LhMouldChangePlanVo> planVoList = this.buildLhMouldChangePlanVoList(planList, queryVO);
        Map<String, List<LhSharedMouldPat>> sharedMouldPatMap = this.buildSharedMouldPatMap(planVoList, queryVO);
        for (int index = 0; index < planList.size(); index++) {
            planList.get(index).setDisplayMouldCode(
                    this.resolveDisplayMouldCode(planVoList.get(index), sharedMouldPatMap));
        }
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.lhMouldChangePlan.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody LhMouldChangePlan billVO){
        if (StringUtil.isBlank(billVO.getFactoryCode())) {
            billVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        if (billVO.getId() == null) {
            billVO.setOrderNo(orderNoGenerator.generateMouldChangeOrderNo(new Date()));
            billVO.setIsRelease(ApsConstant.NO_RELEASE);
            billVO.setMouldStatus(ApsConstant.FALSE);
        } else {
            LhMouldChangePlan origin = lhMouldChangePlanMapper.selectById(billVO.getId());
            if (origin != null) {
                billVO.setOrderNo(origin.getOrderNo());
                if (ApsConstant.IS_RELEASE.equals(origin.getIsRelease())) {
                    // 已发布单据编辑后需要重新进入待发布状态
                    billVO.setIsRelease(ReleaseStatusEnum.PENDING_RELEASE.getCode());
                    billVO.setMouldStatus(ApsConstant.FALSE);
                } else {
                    billVO.setIsRelease(origin.getIsRelease());
                    billVO.setMouldStatus(origin.getMouldStatus());
                }
            }
        }
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.lhMouldChangePlan.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        if (CollectionUtils.isNotEmpty(ids)) {
            QueryWrapper<LhMouldChangePlan> wrapper = new QueryWrapper<>();
            wrapper.in("ID", ids);
            wrapper.eq("IS_RELEASE", "1");
            List<LhMouldChangePlan> releasedList = lhMouldChangePlanMapper.selectList(wrapper);
            if (CollectionUtils.isNotEmpty(releasedList)) {
                String details = releasedList.stream()
                        .map(item -> String.format("%s/%s",
                                StringUtil.isNotBlank(item.getLhResultBatchNo()) ? item.getLhResultBatchNo() : "-",
                                StringUtil.isNotBlank(item.getOrderNo()) ? item.getOrderNo() : "-"))
                        .collect(Collectors.joining("; "));
                String msg = I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.releaseCannotDelete");
                msg = StringUtils.format(msg, details);
                return AjaxResult.error(msg);
            }
        }
        return super.removeByIds(ids);
    }

    /**
     * 获取模具交替计划详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public LhMouldChangePlan getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    /**
     * 根据集合导入模具交替计划数据
     * @param lhImportContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.lhMouldChangePlan.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody LhScheduleImportDTO lhImportContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        ImportContext importContext = lhImportContext.getImportContext();
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        byte[] fileBytes = importContext.getFileBytes();
        ExcelUtil<LhMouldChangePlanVo> util = new ExcelUtil<>(LhMouldChangePlanVo.class);
        String sheetName = I18nUtil.getMessage("ui.data.column.lhMouldChangePlan.import.modelName");
        String titleFormat = I18nUtil.getMessage("mouldChangePlan.export.title");
        String templateErrorStr = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.import.templateError");
        String[] params = new String[]{};
        Date scheduleDate = null;

        LhScheduleResult scheduleResult = lhImportContext.getScheduleResult();
        if (scheduleResult == null) {
            scheduleResult = new LhScheduleResult();
        }

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null || sheet.getRow(0) == null) {
                return AjaxResult.error(templateErrorStr);
            }
            Cell titleCell = sheet.getRow(1).getCell(6);
            if (titleCell == null) {
                return AjaxResult.error(templateErrorStr);
            }
            params = ApsCommonUtil.parseFormat(titleFormat, titleCell.getStringCellValue());
            if (params == null || params.length < 3) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mpStructureAllocation.import.templateTitleError"));
            }
            scheduleDate = DateUtil.parse(params[0], "yyyy年MM月dd日");
            scheduleResult.setScheduleDate(scheduleDate);
            if (StringUtils.isBlank(scheduleResult.getFactoryCode())) {
                scheduleResult.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
            }
        } catch (Exception e) {
            log.warn("importDataStructureAllocation workbook parse failed", e);
            return AjaxResult.error(templateErrorStr);
        }
        lhImportContext.setScheduleResult(scheduleResult);
        List<LhMouldChangePlanVo> list = util.importExcel(
                sheetName, new ByteArrayInputStream(fileBytes), 0, 4, -1);
        List<LhMouldChangePlan> mouldChangePlanList = buildLhMouldChangePlanList(list, scheduleDate);
        AjaxResult ajaxResult = this.doImportData(mouldChangePlanList, updateSupport, importLog.getId());
        // 导入成功后，补全模具交替计划的批次号
        if (!ajaxResult.get(AjaxResult.CODE_TAG).equals(AjaxResult.Type.ERROR.value())) {
            if (CollectionUtils.isNotEmpty(list)) {
                String factoryCode = scheduleResult.getFactoryCode().trim();
                lhScheduleResultService.fillMouldChangePlanFieldsAfterImport(factoryCode, scheduleDate);
            }
        }
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(ajaxResult, this.iImportErrorLogService);
        return ajaxResult;
    }

    public List<LhMouldChangePlan> buildLhMouldChangePlanList(List<LhMouldChangePlanVo> list, Date scheduleDate) {
        List<LhMouldChangePlan> resultList = new ArrayList<>();
        for (LhMouldChangePlanVo lhMouldChangePlanVo : list) {
            LhMouldChangePlan lhMouldChangePlan = new LhMouldChangePlan();
            BeanUtil.copyProperties(lhMouldChangePlanVo, lhMouldChangePlan);

            lhMouldChangePlan.setScheduleDate(scheduleDate);
            lhMouldChangePlan.setChangeTime(lhMouldChangePlan.getPlanDate());

            if (StringUtil.isBlank(lhMouldChangePlan.getFactoryCode())) {
                lhMouldChangePlan.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
            }

            if (YesOrNoEnum.YES.getCode().equals(lhMouldChangePlanVo.getIsReplaceBlock())) {
                lhMouldChangePlan.setChangeMouldType(MouldChangeTypeEnum.TYPE_BLOCK.getCode());
            } else if (YesOrNoEnum.YES.getCode().equals(lhMouldChangePlanVo.getIsSandblastingClean())) {
                lhMouldChangePlan.setChangeMouldType(MouldChangeTypeEnum.SAND_BLAST.getCode());
            } else if (YesOrNoEnum.YES.getCode().equals(lhMouldChangePlanVo.getIsDryIceClean())) {
                lhMouldChangePlan.setChangeMouldType(MouldChangeTypeEnum.DRY_ICE.getCode());
            } else {
                lhMouldChangePlan.setChangeMouldType(MouldChangeTypeEnum.REGULAR.getCode());
            }
            String endType = lhMouldChangePlanVo.getEndType();
            if ("是Có".equals(endType)) {
                lhMouldChangePlan.setEndType(YesOrNoEnum.NO.getCode());
            } else {
                lhMouldChangePlan.setEndType(YesOrNoEnum.YES.getCode());
            }
            resultList.add(lhMouldChangePlan);
        }
        return resultList;
    }

    /**
     * 导出列表
     */
    @Log(title = "模具交替计划", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LhMouldChangePlan queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<LhMouldChangePlan> listExportData(LhMouldChangePlan obj) {
        // 下载模板导出空列表
        if (obj.getExportTemplate()) {
            return Collections.emptyList();
        }
        QueryWrapper<LhMouldChangePlan> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        String orderBy = this.getOrderBy(obj);
        if (StringUtils.isNotBlank(orderBy)) {
            wrapper.last("ORDER BY " + orderBy);
        }
        List<LhMouldChangePlan> list = lhMouldChangePlanMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    /**
     * 导出模具交替计划模板数据
     */
    @Log(title = "模具交替计划", businessType = BusinessType.EXPORT)
    @ApiOperation("导出模具交替计划模板数据")
    @PostMapping("/exportDataChangePlan/{fileName}")
    public byte[] exportDataChangePlan(@RequestBody LhMouldChangePlan queryVO, @PathVariable("fileName") String fileName) {
        Date beginTime = DateUtils.getNowDate();

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("excelModel/lhhmjh.xlsx");
        if (Objects.isNull(inputStream)) {
            throw new ServiceException("硫化计划导出模板不存在");
        }

        //1.获取导出数据
        List<LhMouldChangePlan> list = this.listExportData(queryVO);
        Set<String> previousDayPlanKeySet = null;
        Date originalScheduleDate = queryVO.getScheduleDate();
        if (originalScheduleDate != null) {
            try {
                queryVO.setScheduleDate(DateUtil.offsetDay(originalScheduleDate, -1));
                List<LhMouldChangePlan> previousDayPlanList = this.listExportData(queryVO);
                previousDayPlanKeySet = this.buildMouldChangePlanKeySet(previousDayPlanList);
            } finally {
                queryVO.setScheduleDate(originalScheduleDate);
            }
        }
        Map<String, Object> tableMap = new HashMap<>();
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        // 赋值表头字段名称
        setExportTitleFieldName(tableMap);
        if (CollectionUtils.isNotEmpty(list)) {
            List<LhMouldChangePlanVo> exportList = this.buildLhMouldChangePlanVoList(list, queryVO);
            excelDataList.add(buildExportDataList(exportList, queryVO, previousDayPlanKeySet));
        }
        tableMap = buildExportTableMap(queryVO.getScheduleDate());
        // 赋值表头字段名称
        setExportTitleFieldName(tableMap);

        byte[] resultBytes =  ExcelUtils.writeMultiList(inputStream, 0, tableMap, excelDataList);

        try (ByteArrayInputStream bais = new ByteArrayInputStream(resultBytes);
             XSSFWorkbook workbook = new XSSFWorkbook(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            workbook.setSheetName(0, I18nUtil.getMessage("ui.data.column.lhMouldChangePlan.import.modelName"));
            workbook.write(baos);
            resultBytes = baos.toByteArray();
        } catch (IOException e) {
            log.error("重命名导入模板Sheet失败", e);
            throw new ServiceException("生成导入模板失败");
        }
        Date endTime = DateUtils.getNowDate();

        //3.组装导出日志
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        exportLog.setExportParams(queryVO.toString());
        String uri = ServletUtils.getRequest().getRequestURI();
        exportLog.setFunctionCode(uri.split("/")[1]);
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ExcelUtil.XLSX_FILE);
        exportLog.setRowCount(list.size());
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime,beginTime));
        iExportLogService.add(exportLog);
        return resultBytes;
    }

    /**
     * 构建模板表头数据
     *
     * @param scheduleDate 排程日期
     * @return 模板表头数据
     */
    public Map<String, Object> buildExportTableMap(Date scheduleDate) {
        Map<String, Object> tableMap = new HashMap<>();
        String titleFormat = I18nUtil.getMessage("mouldChangePlan.export.title");
        String cnFormatDate = DateUtil.format(scheduleDate, "yyyy年MM月dd日");
        String vnFormatDate = DateUtil.format(scheduleDate, "dd/MM/yyyy");
        String versionDate = DateUtils.parseDateToStr("yyyyMMddHHmmss", new Date());
        String version = "版本phiên bản：" + versionDate;
        tableMap.put("title", String.format(titleFormat, cnFormatDate, vnFormatDate, versionDate));
        tableMap.put("version", version);
        return tableMap;
    }

    public void setExportTitleFieldName(Map<String, Object> tableMap) {
        ExcelUtil<LhMouldChangePlanVo> util = new ExcelUtil<>(LhMouldChangePlanVo.class);
        List<Field> allFields = util.getClassField(LhMouldChangePlanVo.class);
        for (Field field : allFields) {
            Excel attr = field.getAnnotation(Excel.class);
            if (attr != null && (attr.type() == Excel.Type.ALL || attr.type() == Excel.Type.IMPORT)) {
                // 设置类的私有字段属性可访问.
                field.setAccessible(true);
                String attrName = "".equals(attr.importName()) ? attr.name() : attr.importName();
                if (StringUtils.isNotEmpty(attrName)) {
                    attrName = attrName.replaceAll("\\{", "").replaceAll("}", "");
                    attrName = I18nUtil.getMessage(attrName);
                }
                tableMap.put(field.getName(), attrName);
            }
        }
    }

    /**
     * 添加派生模具号查询条件，查询值与页面展示及导出值保持一致。
     * <p>该条件在数据库分页前执行，避免先分页后过滤造成漏数。</p>
     *
     * @param queryWrapper 查询条件构造器
     * @param queryVO      查询条件
     */
    private void applyDisplayMouldCodeCondition(QueryWrapper<LhMouldChangePlan> queryWrapper,
                                                LhMouldChangePlan queryVO) {
        if (queryVO == null || StringUtils.isBlank(queryVO.getMouldCode())) {
            return;
        }

        String mouldCodeKeyword = queryVO.getMouldCode().trim();
        String planTable = "t_lh_mould_change_plan";
        String machinePrefixSql = "CASE WHEN RIGHT(UPPER(TRIM(" + planTable + ".LH_MACHINE_CODE)), 1) IN ('L', 'R')"
                + " THEN LEFT(UPPER(TRIM(" + planTable + ".LH_MACHINE_CODE)), LENGTH(TRIM(" + planTable
                + ".LH_MACHINE_CODE)) - 1) ELSE UPPER(TRIM(" + planTable + ".LH_MACHINE_CODE)) END";
        String sharedMouldExistsSql = "EXISTS (SELECT 1 FROM T_LH_SHARED_MOULD_PAT shared_mould "
                + "WHERE shared_mould.IS_DELETE = 0 "
                + "AND shared_mould.FACTORY_CODE = " + planTable + ".FACTORY_CODE "
                + "AND shared_mould.MATERIAL_DESC = " + planTable + ".AFTER_MATERIAL_DESC "
                + "AND CONCAT(CASE WHEN TRIM(COALESCE(shared_mould.MOULD_NO, '')) = '' THEN '' "
                + "ELSE shared_mould.MOULD_NO END, '/', "
                + "CASE WHEN TRIM(COALESCE(shared_mould.PATTERN_BLOCK, '')) = '' THEN '' "
                + "ELSE shared_mould.PATTERN_BLOCK END) "
                + "LIKE CONCAT('%', {3}, '%'))";
        String sharedMouldBranch = "(COALESCE(FIND_IN_SET({0}, " + planTable + ".CHANGE_MOULD_TYPE), 0) = 0 AND "
                + sharedMouldExistsSql + ")";
        Date mouldOnlineDateEnd = this.getMouldOnlineDateEnd(queryVO, this.getMouldChangePlanLookbackDays(queryVO));
        if (mouldOnlineDateEnd == null) {
            queryWrapper.and(wrapper -> wrapper.apply(sharedMouldBranch, MouldChangeTypeEnum.TYPE_BLOCK.getCode(),
                    mouldCodeKeyword, null, mouldCodeKeyword));
            return;
        }

        String onlineMouldExistsSql = "EXISTS (SELECT 1 FROM T_LH_MACHINE_ONLINE_INFO online_mould "
                + "WHERE online_mould.IS_DELETE = 0 "
                + "AND online_mould.FACTORY_CODE = " + planTable + ".FACTORY_CODE "
                + "AND UPPER(online_mould.LH_CODE) LIKE CONCAT(" + machinePrefixSql + ", '%') "
                + "AND online_mould.ONLINE_DATE IS NOT NULL "
                + "AND online_mould.ONLINE_DATE <= {2} "
                + "AND online_mould.IN_MACHINE_MOULD_CODE IS NOT NULL "
                + "AND TRIM(online_mould.IN_MACHINE_MOULD_CODE) <> '' "
                + "AND online_mould.IN_MACHINE_MOULD_CODE LIKE CONCAT('%', {1}, '%') "
                + "AND NOT EXISTS (SELECT 1 FROM T_LH_MACHINE_ONLINE_INFO newer_mould "
                + "WHERE newer_mould.IS_DELETE = 0 "
                + "AND newer_mould.FACTORY_CODE = online_mould.FACTORY_CODE "
                + "AND newer_mould.LH_CODE = online_mould.LH_CODE "
                + "AND newer_mould.ONLINE_DATE IS NOT NULL "
                + "AND newer_mould.ONLINE_DATE <= {2} "
                + "AND (newer_mould.ONLINE_DATE > online_mould.ONLINE_DATE "
                + "OR (newer_mould.ONLINE_DATE = online_mould.ONLINE_DATE AND ("
                + "(online_mould.UPDATE_TIME IS NULL AND newer_mould.UPDATE_TIME IS NOT NULL) "
                + "OR (online_mould.UPDATE_TIME IS NOT NULL AND newer_mould.UPDATE_TIME IS NOT NULL "
                + "AND newer_mould.UPDATE_TIME > online_mould.UPDATE_TIME) "
                + "OR ((newer_mould.UPDATE_TIME = online_mould.UPDATE_TIME "
                + "OR (newer_mould.UPDATE_TIME IS NULL AND online_mould.UPDATE_TIME IS NULL)) "
                + "AND newer_mould.ID > online_mould.ID))))))";
        String replaceBlockBranch = "(FIND_IN_SET({0}, " + planTable + ".CHANGE_MOULD_TYPE) > 0 AND "
                + onlineMouldExistsSql + ")";
        queryWrapper.and(wrapper -> wrapper.apply("(" + replaceBlockBranch + " OR " + sharedMouldBranch + ")",
                MouldChangeTypeEnum.TYPE_BLOCK.getCode(), mouldCodeKeyword, mouldOnlineDateEnd, mouldCodeKeyword));
    }

    /**
     * 获取模具交替计划模具号追溯天数。
     *
     * @param queryVO 查询条件
     * @return 追溯天数，参数不存在或无效时返回默认值2
     */
    private int getMouldChangePlanLookbackDays(LhMouldChangePlan queryVO) {
        int lookbackDays = 2;
        if (queryVO == null || StringUtils.isBlank(queryVO.getFactoryCode())) {
            return lookbackDays;
        }
        LhParams lookbackParam = lhParamsService.selectOneByParamCode(
                LhScheduleParamConstant.MOULD_CHANGE_PLAN_LOOKBACK_DAYS, queryVO.getFactoryCode());
        if (lookbackParam == null || StringUtils.isBlank(lookbackParam.getParamValue())) {
            return lookbackDays;
        }
        try {
            return Integer.parseInt(lookbackParam.getParamValue().trim());
        } catch (NumberFormatException exception) {
            log.warn("模具交替计划模具号追溯天数参数无效，使用默认值2，工厂编码：{}",
                    queryVO.getFactoryCode());
            return lookbackDays;
        }
    }

    /**
     * 计算在机模具信息的截止时间。
     *
     * @param queryVO      查询条件
     * @param lookbackDays 模具号追溯天数
     * @return 在机信息截止时间，排程日期为空时返回null
     */
    private Date getMouldOnlineDateEnd(LhMouldChangePlan queryVO, int lookbackDays) {
        if (queryVO == null || queryVO.getScheduleDate() == null) {
            return null;
        }
        return DateUtils.addDays(DateUtil.endOfDay(queryVO.getScheduleDate()), -lookbackDays);
    }

    /**
     * 批量查询共享模具配置并按后物料描述分组。
     *
     * @param list    模具交替计划导出视图列表
     * @param queryVO 查询条件
     * @return 后物料描述到共享模具配置列表的映射
     */
    private Map<String, List<LhSharedMouldPat>> buildSharedMouldPatMap(List<LhMouldChangePlanVo> list,
                                                                       LhMouldChangePlan queryVO) {
        List<String> materialDescList = list.stream()
                .map(LhMouldChangePlanVo::getAfterMaterialDesc)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(materialDescList)) {
            return new HashMap<>();
        }
        LambdaQueryWrapper<LhSharedMouldPat> sharedMouldPatQueryWrapper = new LambdaQueryWrapper<>();
        sharedMouldPatQueryWrapper.eq(LhSharedMouldPat::getIsDelete, YesOrNoEnum.NO.getCode());
        if (queryVO != null && StringUtils.isNotBlank(queryVO.getFactoryCode())) {
            sharedMouldPatQueryWrapper.eq(LhSharedMouldPat::getFactoryCode, queryVO.getFactoryCode());
        }
        sharedMouldPatQueryWrapper.in(LhSharedMouldPat::getMaterialDesc, materialDescList);
        List<LhSharedMouldPat> sharedMouldPatList = lhSharedMouldPatEntityMapper.selectList(sharedMouldPatQueryWrapper);
        if (CollectionUtils.isEmpty(sharedMouldPatList)) {
            return new HashMap<>();
        }
        return sharedMouldPatList.stream().collect(Collectors.groupingBy(LhSharedMouldPat::getMaterialDesc));
    }

    /**
     * 解析模具交替计划最终展示的模具号。
     *
     * @param item              模具交替计划导出视图
     * @param sharedMouldPatMap 共享模具配置映射
     * @return 最终展示模具号
     */
    private String resolveDisplayMouldCode(LhMouldChangePlanVo item,
                                           Map<String, List<LhSharedMouldPat>> sharedMouldPatMap) {
        if (YesOrNoEnum.YES.getCode().equals(item.getIsReplaceBlock())) {
            return StringUtils.defaultIfBlank(item.getMouldCode(), "");
        }
        List<LhSharedMouldPat> sharedMouldPatList = sharedMouldPatMap.get(item.getAfterMaterialDesc());
        if (CollectionUtils.isEmpty(sharedMouldPatList)) {
            return "";
        }
        return sharedMouldPatList.stream()
                .map(sharedMouldPat -> StringUtils.defaultIfBlank(sharedMouldPat.getMouldNo(), "") + "/"
                        + StringUtils.defaultIfBlank(sharedMouldPat.getPatternBlock(), ""))
                .collect(Collectors.joining(",\n"));
    }

    /**
     * 构建列表和导出共用的默认排序比较器。
     *
     * @return 默认排序比较器
     */
    private Comparator<LhMouldChangePlanVo> buildDefaultExportComparator() {
        return Comparator.comparingInt((LhMouldChangePlanVo item) -> {
                    if (YesOrNoEnum.YES.getCode().equals(item.getIsDryIceClean())
                            || YesOrNoEnum.YES.getCode().equals(item.getIsSandblastingClean())) {
                        return 0;
                    }
                    return 2;
                })
                .thenComparingLong(item -> item.getPlanDate() == null
                        ? Long.MAX_VALUE : DateUtil.beginOfDay(item.getPlanDate()).getTime())
                .thenComparing(item -> StringUtils.defaultIfBlank(item.getClassIndex(), ""))
                .thenComparing(item -> StringUtils.defaultIfBlank(item.getLhMachineCode(), ""))
                .thenComparing(LhMouldChangePlanVo::getPlanOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(LhMouldChangePlanVo::getId, Comparator.nullsLast(Long::compareTo));
    }

    /**
     * 判断导出请求是否携带显式排序条件。
     *
     * @param queryVO 查询条件
     * @return 携带有效显式排序条件时返回true
     */
    private boolean hasExplicitSort(LhMouldChangePlan queryVO) {
        if (queryVO == null || queryVO.getParams() == null) {
            return false;
        }
        Object orderBy = queryVO.getParams().get("orderBy");
        return orderBy != null && StringUtils.isNotBlank(String.valueOf(orderBy));
    }

    /**
     * 构建模具交替计划导出数据，保持原有调用方的导出行为。
     *
     * @param list    模具交替计划导出视图列表
     * @param queryVO 查询条件
     * @return 导出行数据
     */
    public List<Map<String, Object>> buildExportDataList(List<LhMouldChangePlanVo> list, LhMouldChangePlan queryVO) {
        return this.buildExportDataList(list, queryVO, null);
    }

    /**
     * 构建模具交替计划导出数据，并根据前日交替计划标识集合标红新增交替计划的后物料描述。
     *
     * @param list                  模具交替计划导出视图列表
     * @param queryVO               查询条件
     * @param previousDayPlanKeySet 前日交替计划唯一标识集合；为null时不执行前日计划比较
     * @return 导出行数据
     */
    public List<Map<String, Object>> buildExportDataList(List<LhMouldChangePlanVo> list,
                                                         LhMouldChangePlan queryVO,
                                                         Set<String> previousDayPlanKeySet) {
        // 未传显式排序时按列表和导出共用的默认顺序排序，计划日期仅按年月日比较，避免时分秒影响导出顺序。
        if (!this.hasExplicitSort(queryVO)) {
            list = list.stream().sorted(this.buildDefaultExportComparator()).collect(Collectors.toList());
        }
        // 查询字典用于转义
        List<SysDictData> classNumDictList = iSysDictDataCacheService.getType("class_num_two_mm");
        Map<String, String> classNumDictDictMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(classNumDictList)) {
            classNumDictDictMap = classNumDictList.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        }
        // 查询字典用于转义
        List<SysDictData> lhTrialStatusDictList = iSysDictDataCacheService.getType("lh_trial_status");
        Map<String, String> lhTrialStatusDictDictMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(lhTrialStatusDictList)) {
            lhTrialStatusDictDictMap = lhTrialStatusDictList.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        }
        // 查询硫化排程，取后规格示方类型
        List<String> materialCodeList = new ArrayList<>();
        for (LhMouldChangePlanVo mouldChangePlanVo : list) {
            if (StringUtils.isNotBlank(mouldChangePlanVo.getAfterMaterialCode())) {
                materialCodeList.add(mouldChangePlanVo.getAfterMaterialCode());
            }
        }
        // 查询硫化排程结果，获取规格产品状态
        Map<String, String> lhScheduleResultMap = new HashMap<>();
        if (CollUtil.isNotEmpty(materialCodeList)) {
            LambdaQueryWrapper<LhScheduleResult> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(LhScheduleResult::getFactoryCode, queryVO.getFactoryCode());
            queryWrapper.eq(LhScheduleResult::getScheduleDate, queryVO.getScheduleDate());
            queryWrapper.eq(LhScheduleResult::getIsDelete, YesOrNoEnum.NO.getCode());
            queryWrapper.in(LhScheduleResult::getMaterialCode, materialCodeList);
            List<LhScheduleResult> lhScheduleResultList = lhScheduleResultMapper.selectList(queryWrapper);
            if (CollectionUtils.isNotEmpty(lhScheduleResultList)) {
                lhScheduleResultMap = lhScheduleResultList.stream()
                        .sorted(Comparator.comparing(item -> StringUtils.defaultIfBlank(item.getProductStatus(), "")))
                        .collect(Collectors.toMap(item -> StringUtils.defaultIfBlank(item.getMaterialCode(), ""),
                                item -> StringUtils.defaultIfBlank(item.getProductStatus(), ""),
                                (s1, s2) -> s1));
            }
        }
        // 查询共用模具
        Map<String, List<LhSharedMouldPat>> lhSharedMouldPatMap = this.buildSharedMouldPatMap(list, queryVO);

        List<Map<String, Object>> dataList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            LhMouldChangePlanVo item = list.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("seq", i + 1);
            row.put("planDate", item.getPlanDate() == null ? "" : DateUtil.format(item.getPlanDate(), "yyyy-MM-dd"));
            String classIndex = item.getClassIndex();
            row.put("classIndex", classNumDictDictMap.getOrDefault(classIndex, classIndex));
            row.put("lhMachineCode", item.getLhMachineCode());
            row.put("planOrder", item.getPlanOrder());
            row.put("leftRightMould", item.getLeftRightMould());
            row.put("beforeMaterialCode", item.getBeforeMaterialCode());
            row.put("beforeMaterialDesc", item.getBeforeMaterialDesc());
            String afterMaterialCode = item.getAfterMaterialCode();
            row.put("afterMaterialCode", afterMaterialCode);
            String afterMaterialDesc = item.getAfterMaterialDesc();
            row.put("afterMaterialDesc", afterMaterialDesc);
            // 产品状态
            String productStatus = lhScheduleResultMap.getOrDefault(afterMaterialCode, "");
            row.put("afterMaterialType", lhTrialStatusDictDictMap.getOrDefault(productStatus, ""));
            // 按时间下机
            String endType = item.getEndType();
            if (YesOrNoEnum.YES.getCode().equals(endType)) {
                row.put("endType", "是Có");
            } else {
                row.put("endType", "");
            }
            String isDryIceClean = item.getIsDryIceClean();
            if (YesOrNoEnum.YES.getCode().equals(isDryIceClean)) {
                row.put("isDryIceClean", "是Có");
                row.put("endType", "");
            } else {
                row.put("isDryIceClean", "");
            }
            String isSandblastingClean = item.getIsSandblastingClean();
            if (YesOrNoEnum.YES.getCode().equals(isSandblastingClean)) {
                row.put("isSandblastingClean", "是Có");
                row.put("endType", "");
            } else {
                row.put("isSandblastingClean", "");
            }

            ExcelStyleVo rowStyle = null;

            String isReplaceBlock = item.getIsReplaceBlock();
            if (YesOrNoEnum.YES.getCode().equals(isReplaceBlock)) {
                row.put("isReplaceBlock", "是Có");
                row.put("endType", "");
            } else {
                row.put("isReplaceBlock", "");

                // 不是换活字块的，从共用模具花纹配置取
                if (lhSharedMouldPatMap.containsKey(afterMaterialDesc)) {
                    rowStyle = new ExcelStyleVo();
                    rowStyle.setRgbColor(new ExcelStyleVo.RgbColor(230, 184, 183));
                    row.put("style", rowStyle);
                }
            }

            row.put("mouldCode", this.resolveDisplayMouldCode(item, lhSharedMouldPatMap));

            row.put("remark", item.getRemark());

            // 当前交替计划在前日不存在且计划日期处于排程日及次日时，仅将后物料描述字体标红。
            if (previousDayPlanKeySet != null
                    && !previousDayPlanKeySet.contains(this.buildMouldChangePlanKey(item))
                    && this.isPlanDateInExportRange(item.getPlanDate(),
                    queryVO == null ? null : queryVO.getScheduleDate())) {
                ExcelStyleVo afterMaterialDescStyle = new ExcelStyleVo();
                afterMaterialDescStyle.setBorder(Boolean.FALSE);
                afterMaterialDescStyle.setFontColor(MISSING_PREVIOUS_PLAN_FONT_COLOR);
                if (rowStyle != null) {
                    afterMaterialDescStyle.setRgbColor(rowStyle.getRgbColor());
                }
                row.put("style_afterMaterialDesc", afterMaterialDescStyle);
            }

            // 中心计划日期仅标红计划日期列，其他导出字段保持原有字体样式。
            if (this.isMiddlePlanDate(item.getPlanDate(), queryVO == null ? null : queryVO.getScheduleDate())) {
                ExcelStyleVo planDateStyle = new ExcelStyleVo();
                planDateStyle.setBorder(Boolean.FALSE);
                planDateStyle.setFontColor(MIDDLE_PLAN_DATE_FONT_COLOR);
                if (rowStyle != null) {
                    // 列级样式优先于行级样式，需同步保留共享模具行的背景色。
                    planDateStyle.setRgbColor(rowStyle.getRgbColor());
                }
                row.put("style_planDate", planDateStyle);
            }

            dataList.add(row);
        }
        return dataList;
    }

    /**
     * 根据模具交替计划实体构建前日比较唯一标识。
     *
     * @param mouldChangePlan 模具交替计划实体
     * @return 计划日期、机台、前物料编码和后物料编码组成的唯一标识
     */
    private String buildMouldChangePlanKey(LhMouldChangePlan mouldChangePlan) {
        if (mouldChangePlan == null) {
            return "";
        }
        return this.buildMouldChangePlanKey(mouldChangePlan.getPlanDate(), mouldChangePlan.getLhMachineCode(),
                mouldChangePlan.getBeforeMaterialCode(), mouldChangePlan.getAfterMaterialCode());
    }

    /**
     * 构建模具交替计划唯一标识集合，供不同导出入口复用前日计划比较规则。
     *
     * @param mouldChangePlanList 模具交替计划实体列表
     * @return 模具交替计划唯一标识集合
     */
    public Set<String> buildMouldChangePlanKeySet(List<LhMouldChangePlan> mouldChangePlanList) {
        if (CollectionUtils.isEmpty(mouldChangePlanList)) {
            return Collections.emptySet();
        }
        return mouldChangePlanList.stream()
                .map(this::buildMouldChangePlanKey)
                .collect(Collectors.toSet());
    }

    /**
     * 根据模具交替计划导出视图构建前日比较唯一标识。
     *
     * @param mouldChangePlanVo 模具交替计划导出视图
     * @return 计划日期、机台、前物料编码和后物料编码组成的唯一标识
     */
    private String buildMouldChangePlanKey(LhMouldChangePlanVo mouldChangePlanVo) {
        if (mouldChangePlanVo == null) {
            return "";
        }
        return this.buildMouldChangePlanKey(mouldChangePlanVo.getPlanDate(), mouldChangePlanVo.getLhMachineCode(),
                mouldChangePlanVo.getBeforeMaterialCode(), mouldChangePlanVo.getAfterMaterialCode());
    }

    /**
     * 按统一字段顺序组装模具交替计划比较唯一标识。
     *
     * @param planDate           计划日期
     * @param lhMachineCode      硫化机台编码
     * @param beforeMaterialCode 前物料编码
     * @param afterMaterialCode  后物料编码
     * @return 使用竖线分隔的计划唯一标识
     */
    private String buildMouldChangePlanKey(Date planDate, String lhMachineCode,
                                           String beforeMaterialCode, String afterMaterialCode) {
        String planDateText = planDate == null ? "" : DateUtil.format(planDate, "yyyy-MM-dd");
        return String.join("|", planDateText,
                StringUtils.defaultIfBlank(lhMachineCode, ""),
                StringUtils.defaultIfBlank(beforeMaterialCode, ""),
                StringUtils.defaultIfBlank(afterMaterialCode, ""));
    }

    /**
     * 判断计划日期是否处于排程日期当天或排程日期次日范围内。
     *
     * @param planDate     计划日期
     * @param scheduleDate 排程日期
     * @return 计划日期为排程日期当天或次日时返回true，否则返回false
     */
    private boolean isPlanDateInExportRange(Date planDate, Date scheduleDate) {
        if (planDate == null || scheduleDate == null) {
            return false;
        }
        return DateUtil.isSameDay(planDate, scheduleDate)
                || DateUtil.isSameDay(planDate, DateUtil.offsetDay(scheduleDate, 1));
    }

    /**
     * 判断计划日期是否为导出数据的中心计划日期。
     *
     * @param planDate     记录计划日期
     * @param scheduleDate 查询条件中的排程日期
     * @return 计划日期与排程日期为同一自然日时返回true，否则返回false
     */
    private boolean isMiddlePlanDate(Date planDate, Date scheduleDate) {
        if (planDate == null || scheduleDate == null) {
            return false;
        }
        return DateUtil.beginOfDay(planDate).equals(DateUtil.beginOfDay(scheduleDate));
    }

    /**
     * 构建模具交替计划导出视图列表。
     * <p>模具号取自 T_LH_MACHINE_ONLINE_INFO 在机信息：按机台（去除末尾 L/R 后缀做模糊匹配，
     * 使 K1501L 同时命中 K1501L、K1501R）取排程日期往前追溯 N 天内（含当日）最近一条在机记录的在机模号，
     * 多条命中时按机台编码 + 上机日期排序去重后英文逗号拼接，回写模具号字段。
     * 追溯天数 N 由硫化参数 MOULD_CHANGE_PLAN_LOOKBACK_DAYS（SYS0302012）控制，默认 3 天。</p>
     *
     * @param list     模具交替计划列表
     * @param queryVO 查询条件（含排程日期、分厂）
     * @return 导出视图列表
     */
    public List<LhMouldChangePlanVo> buildLhMouldChangePlanVoList(List<LhMouldChangePlan> list, LhMouldChangePlan queryVO) {
        List<LhMouldChangePlanVo> resultList = new ArrayList<>();
        int seq = 1;

        // 读取硫化参数：模具交替计划模具号往前追溯天数，默认 2 天。
        int lookbackDays = this.getMouldChangePlanLookbackDays(queryVO);

        // 收集机台编码前缀（去除末尾 L/R 后缀），用于模糊匹配在机信息的左右模机台
        List<String> machinePrefixList = new ArrayList<>();
        for (LhMouldChangePlan mouldChangePlan : list) {
            String machineCode = mouldChangePlan.getLhMachineCode();
            if (StringUtils.isNotBlank(machineCode) && !machinePrefixList.contains(machineCode)) {
                // 机台编码可能带 L/R 左右模后缀，去除后缀得到前缀用于模糊匹配（K1501L -> K1501）
                String prefix = stripLeftRightSuffix(machineCode);
                if (!machinePrefixList.contains(prefix)) {
                    machinePrefixList.add(prefix);
                }
            }
        }

        // 机台前缀 -> 命中的在机记录（按机台 + 上机日期排序、去重后拼接模号）
        Map<String, List<LhMachineOnlineInfo>> lhMachineOnlineInfoMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(machinePrefixList) && queryVO != null && queryVO.getScheduleDate() != null) {
            Date scheduleDateEnd = this.getMouldOnlineDateEnd(queryVO, lookbackDays);
            LambdaQueryWrapper<LhMachineOnlineInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StringUtils.isNotBlank(queryVO.getFactoryCode()), LhMachineOnlineInfo::getFactoryCode, queryVO.getFactoryCode());
            wrapper.and(prefixWrapper -> {
                for (String prefix : machinePrefixList) {
                    // 模糊匹配：K1501 命中 K1501L、K1501R 等左右模机台
                    prefixWrapper.or().likeRight(LhMachineOnlineInfo::getLhCode, prefix);
                }
            });
            wrapper.isNotNull(LhMachineOnlineInfo::getOnlineDate);
            wrapper.le(LhMachineOnlineInfo::getOnlineDate, scheduleDateEnd);
            wrapper.eq(LhMachineOnlineInfo::getIsDelete, DeleteFlagEnum.NORMAL.getCode());
            List<LhMachineOnlineInfo> lhMachineOnlineInfoList = lhMachineOnlineInfoMapper.selectList(wrapper);
            if (CollectionUtils.isNotEmpty(lhMachineOnlineInfoList)) {
                // 按机台编码 + 上机日期（倒序）+ 更新时间（倒序）排序，保证取每个机台最近一条
                List<LhMachineOnlineInfo> sortedOnlineInfoList = lhMachineOnlineInfoList.stream()
                        .filter(item -> StringUtils.isNotBlank(item.getLhCode()))
                        .filter(item -> item.getOnlineDate() != null && !item.getOnlineDate().after(scheduleDateEnd))
                        .sorted(Comparator.comparing(LhMachineOnlineInfo::getLhCode, Comparator.nullsLast(String::compareTo))
                                .thenComparing(LhMachineOnlineInfo::getOnlineDate, Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(LhMachineOnlineInfo::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(LhMachineOnlineInfo::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                        .collect(Collectors.toList());
                for (LhMachineOnlineInfo onlineInfo : sortedOnlineInfoList) {
                    String prefix = stripLeftRightSuffix(onlineInfo.getLhCode());
                    lhMachineOnlineInfoMap.computeIfAbsent(prefix, k -> new ArrayList<>()).add(onlineInfo);
                }
            }
        }
        for (LhMouldChangePlan lhMouldChangePlan : list) {
            LhMouldChangePlanVo lhMouldChangePlanVo = new LhMouldChangePlanVo();
            BeanUtil.copyProperties(lhMouldChangePlan, lhMouldChangePlanVo);

            // 按机台前缀匹配在机信息，将命中记录（可能同时含 L/R 左右模）的在机模号排序去重后逗号拼接
            String machinePrefix = stripLeftRightSuffix(lhMouldChangePlan.getLhMachineCode());
            List<LhMachineOnlineInfo> matchedOnlineInfoList = lhMachineOnlineInfoMap.get(machinePrefix);
            if (CollectionUtils.isNotEmpty(matchedOnlineInfoList)) {
                // 每个机台取最近一条（已按上机日期倒序），按机台编码 + 上机日期排序去重拼接
                Map<String, LhMachineOnlineInfo> latestByMachine = new LinkedHashMap<>();
                for (LhMachineOnlineInfo onlineInfo : matchedOnlineInfoList) {
                    latestByMachine.putIfAbsent(onlineInfo.getLhCode(), onlineInfo);
                }
                List<String> mouldCodeList = latestByMachine.values().stream()
                        .map(LhMachineOnlineInfo::getInMachineMouldCode)
                        .filter(StringUtils::isNotBlank)
                        .distinct()
                        .collect(Collectors.toList());
                lhMouldChangePlanVo.setMouldCode(CollUtil.isNotEmpty(mouldCodeList) ? String.join(",", mouldCodeList) : "");
            } else {
                // 从模具变更计划赋值过来的要清空
                lhMouldChangePlanVo.setMouldCode("");
            }

            lhMouldChangePlanVo.setSeq(seq++);

            // 如果换模类型是喷砂清洗或干冰清洗，前规格不导出
            String changeMouldType = lhMouldChangePlan.getChangeMouldType();
            if (MouldChangeTypeEnum.containsCode(changeMouldType, MouldChangeTypeEnum.TYPE_BLOCK.getCode())) {
                lhMouldChangePlanVo.setIsReplaceBlock(YesOrNoEnum.YES.getCode());
            }
            if (MouldChangeTypeEnum.containsCode(changeMouldType, MouldChangeTypeEnum.SAND_BLAST.getCode())) {
                lhMouldChangePlanVo.setIsSandblastingClean(YesOrNoEnum.YES.getCode());
                lhMouldChangePlanVo.setBeforeMaterialCode("");
                lhMouldChangePlanVo.setBeforeMaterialDesc("");
            }
            if (MouldChangeTypeEnum.containsCode(changeMouldType, MouldChangeTypeEnum.DRY_ICE.getCode())) {
                lhMouldChangePlanVo.setIsDryIceClean(YesOrNoEnum.YES.getCode());
                lhMouldChangePlanVo.setBeforeMaterialCode("");
                lhMouldChangePlanVo.setBeforeMaterialDesc("");
            }
            resultList.add(lhMouldChangePlanVo);
        }
        return resultList;
    }

    /**
     * 去除机台编码末尾的左右模后缀（L/R）。
     * <p>示例：K1501L -> K1501，K1501R -> K1501，无后缀原样返回。</p>
     *
     * @param machineCode 机台编码
     * @return 去除后缀后的机台前缀
     */
    private String stripLeftRightSuffix(String machineCode) {
        if (StringUtils.isBlank(machineCode)) {
            return machineCode;
        }
        String code = machineCode.trim().toUpperCase();
        if (code.endsWith("L") || code.endsWith("R")) {
            return code.substring(0, code.length() - 1);
        }
        return code;
    }

    @Override
    protected IDocService getDocService(){
        return lhMouldChangePlanService;
    }

    @Override
    public String[] getQueryFormulas() {
        return lhMouldChangePlanService.getQueryFormulas();
    }

    /**
     * 条件拼接 - 所有数据库字段都支持查�?
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    public void builderCondition(QueryWrapper<LhMouldChangePlan> queryWrapper, LhMouldChangePlan queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getLhResultBatchNo()), "LH_RESULT_BATCH_NO", queryVO.getLhResultBatchNo());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getOrderNo()), "ORDER_NO", queryVO.getOrderNo());
        if (PubUtil.isNotEmpty(queryVO.getPlanDate())) {
            LocalDate localDate = queryVO.getPlanDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            // 计算出当天的起始和结束时刻
            LocalDateTime startOfDay = localDate.atStartOfDay();
            LocalDateTime endOfDay = localDate.atTime(LocalTime.MAX);
            queryWrapper.between("PLAN_DATE", startOfDay, endOfDay);
        }

        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getPlanOrder()), "PLAN_ORDER", queryVO.getPlanOrder());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getScheduleDate()), "SCHEDULE_DATE", queryVO.getScheduleDate());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getLeftRightMould()), "LEFT_RIGHT_MOULD", queryVO.getLeftRightMould());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getLhMachineCode()), "LH_MACHINE_CODE", queryVO.getLhMachineCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getLhMachineName()), "LH_MACHINE_NAME", queryVO.getLhMachineName());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getBeforeMaterialCode()), "BEFORE_MATERIAL_CODE", queryVO.getBeforeMaterialCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getBeforeMaterialDesc()), "BEFORE_MATERIAL_DESC", queryVO.getBeforeMaterialDesc());
        if (PubUtil.isNotEmpty(queryVO.getChangeMouldType())) {
            List<String> changeMouldTypes = Arrays.stream(queryVO.getChangeMouldType().split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toList());
            queryWrapper.and(CollectionUtils.isNotEmpty(changeMouldTypes), wrapper -> {
                for (int index = 0; index < changeMouldTypes.size(); index++) {
                    String changeMouldType = changeMouldTypes.get(index);
                    if (index == 0) {
                        wrapper.apply("FIND_IN_SET({0}, CHANGE_MOULD_TYPE)", changeMouldType);
                    } else {
                        wrapper.or().apply("FIND_IN_SET({0}, CHANGE_MOULD_TYPE)", changeMouldType);
                    }
                }
            });
        }
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getAfterMaterialCode()), "AFTER_MATERIAL_CODE", queryVO.getAfterMaterialCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getAfterMaterialDesc()), "AFTER_MATERIAL_DESC", queryVO.getAfterMaterialDesc());
        this.applyDisplayMouldCodeCondition(queryWrapper, queryVO);
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getIsRelease()), "IS_RELEASE", queryVO.getIsRelease());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMouldStatus()), "MOULD_STATUS", queryVO.getMouldStatus());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getRemark()), "REMARK", queryVO.getRemark());

        // 计划日期区间查询 - 前端daterange会自动拆分出planDateStart和planDateEnd
        queryWrapper.ge(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("planDateStart")), "PLAN_DATE", queryVO.getFieldValueByFieldName("planDateStart"));
        if (PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("planDateEnd"))) {
            queryWrapper.le("PLAN_DATE", queryVO.getFieldValueByFieldName("planDateEnd"));
        }

        // 排程日期区间查询 - 前端daterange会自动拆分出scheduleDateStart和scheduleDateEnd
        queryWrapper.ge(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDateStart")), "SCHEDULE_DATE", queryVO.getFieldValueByFieldName("scheduleDateStart"));
        if (PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDateEnd"))) {
            queryWrapper.le("SCHEDULE_DATE", queryVO.getFieldValueByFieldName("scheduleDateEnd"));
        }
    }

    @Override
    protected String getTypeCode(){
        return "0114";
    }

    @Override
    protected String getOrderBy() {
        return " CASE WHEN FIND_IN_SET('" + MouldChangeTypeEnum.SAND_BLAST.getCode()
                + "', CHANGE_MOULD_TYPE) > 0 OR FIND_IN_SET('" + MouldChangeTypeEnum.DRY_ICE.getCode()
                + "', CHANGE_MOULD_TYPE) > 0 THEN 0 ELSE 2 END"
                + ", CASE WHEN PLAN_DATE IS NULL THEN 1 ELSE 0 END, DATE(PLAN_DATE)"
                + ", COALESCE(NULLIF(TRIM(CLASS_INDEX), ''), '')"
                + ", COALESCE(NULLIF(TRIM(LH_MACHINE_CODE), ''), '')"
                + ", CASE WHEN PLAN_ORDER IS NULL THEN 1 ELSE 0 END, PLAN_ORDER, ID";
    }

    /**
     * 排程发布
     * 未勾选记录或勾选记录包含历史记录时，返回发布失败提示
     */
    @Log(title = "ui.data.column.lhMouldChangePlan.modelName", businessType = BusinessType.PUBLISH)
    @ApiOperation("排程发布")
    @PostMapping("/issueSchedule")
    public AjaxResult issueSchedule(@RequestBody List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.noSelection"));
        }
        return lhMouldChangePlanService.issueSchedule(ids);
    }

    /**
     * 按查询条件排程发布（仅支持单日排程日期）
     * 排程日期早于当前日期的历史记录不允许发布
     */
    @Log(title = "ui.data.column.lhMouldChangePlan.modelName", businessType = BusinessType.PUBLISH)
    @ApiOperation("按查询条件排程发布")
    @PostMapping("/issueScheduleByQuery")
    public AjaxResult issueScheduleByQuery(@RequestBody LhMouldChangePlan queryVO) {
        if (queryVO == null || PubUtil.isEmpty(queryVO.getScheduleDate())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.param.error"));
        }
        // 只允许单日排程日期，不支持区间下发
        if (PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDateStart"))
                || PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDateEnd"))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.param.error"));
        }

        // 校验排程日期是否为历史日期
        Date today = DateUtil.beginOfDay(new Date());
        if (queryVO.getScheduleDate() != null && queryVO.getScheduleDate().before(today)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.hasHistoryDataByQuery"));
        }

        // 忽略前端的发布状态筛选，强制下发 未发布/待发布
        queryVO.setIsRelease(null);

        QueryWrapper<LhMouldChangePlan> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, queryVO);
        wrapper.in("IS_RELEASE", Arrays.asList(ApsConstant.NO_RELEASE, ReleaseStatusEnum.PENDING_RELEASE.getCode()));
        wrapper.select("ID");
        List<LhMouldChangePlan> list = lhMouldChangePlanMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.noData"));
        }
        List<Long> ids = list.stream().map(LhMouldChangePlan::getId).collect(Collectors.toList());
        return lhMouldChangePlanService.issueSchedule(ids);
    }

}




