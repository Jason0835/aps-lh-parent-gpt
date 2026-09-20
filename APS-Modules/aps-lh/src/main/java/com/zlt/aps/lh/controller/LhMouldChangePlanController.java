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
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.PageDomain;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.core.web.page.TableSupport;
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
import com.zlt.aps.lh.api.domain.dto.LhScheduleImportDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhSharedMouldPat;
import com.zlt.aps.lh.api.domain.vo.LhMouldChangePlanVo;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.api.enums.ReleaseStatusEnum;
import com.zlt.aps.lh.component.OrderNoGenerator;
import com.zlt.aps.lh.mapper.LhMouldChangePlanEntityMapper;
import com.zlt.aps.lh.mapper.LhScheduleResultMapper;
import com.zlt.aps.lh.mapper.LhSharedMouldPatEntityMapper;
import com.zlt.aps.lh.service.ILhMouldChangePlanService;
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
    private ISysDictDataCacheService iSysDictDataCacheService;

    @Autowired
    private LhSharedMouldPatEntityMapper lhSharedMouldPatEntityMapper;

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
        TableDataInfo tableDataInfo;
        if (this.hasExplicitSort(queryVO)) {
            tableDataInfo = super.list(queryVO);
        } else {
            QueryWrapper<LhMouldChangePlan> queryWrapper = new QueryWrapper<>();
            this.builderCondition(queryWrapper, queryVO);
            // 复用基类的数据权限条件，保证全量排序前的候选集合与原分页查询一致。
            this.buildDataAuthSQL(queryWrapper);
            List<LhMouldChangePlan> allRows = lhMouldChangePlanMapper.selectList(queryWrapper);
            List<LhMouldChangePlan> sortedRows = lhMouldChangePlanService == null
                    ? allRows : lhMouldChangePlanService.sortByDefaultOrder(allRows);
            if (sortedRows == null) {
                sortedRows = allRows == null ? Collections.emptyList() : allRows;
            }
            PageDomain pageDomain = this.resolvePageDomain();
            if (pageDomain == null) {
                pageDomain = new PageDomain();
            }
            int pageNum = pageDomain.getPageNum() == null || pageDomain.getPageNum() < 1
                    ? 1 : pageDomain.getPageNum();
            int pageSize = pageDomain.getPageSize() == null || pageDomain.getPageSize() < 1
                    ? 20 : pageDomain.getPageSize();
            int fromIndex = Math.min((pageNum - 1) * pageSize, sortedRows.size());
            int toIndex = Math.min(fromIndex + pageSize, sortedRows.size());
            tableDataInfo = new TableDataInfo();
            tableDataInfo.setCode(HttpStatus.SUCCESS);
            tableDataInfo.setRows(new ArrayList<>(sortedRows.subList(fromIndex, toIndex)));
            tableDataInfo.setTotal(sortedRows.size());
            tableDataInfo.setMsg(I18nUtil.getMessage("common.msg.base.query.success"));
            AppUtils.formatData(tableDataInfo.getRows(), getQueryFormulas());
        }
        this.fillDisplayMouldCode(tableDataInfo);
        return tableDataInfo;
    }

    /**
     * 为列表当前页数据补充最终展示模具号。
     *
     * @param tableDataInfo 分页查询结果
     */
    private void fillDisplayMouldCode(TableDataInfo tableDataInfo) {
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

        planList.forEach(plan -> plan.setDisplayMouldCode(
                StringUtils.defaultIfBlank(plan.getMouldCode(), "")));
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
        List<LhMouldChangePlan> list;
        if (this.hasExplicitSort(obj)) {
            String orderBy = this.getOrderBy(obj);
            if (StringUtils.isNotBlank(orderBy)) {
                wrapper.last("ORDER BY " + orderBy);
            }
            list = lhMouldChangePlanMapper.selectList(wrapper);
        } else {
            list = lhMouldChangePlanMapper.selectList(wrapper);
            List<LhMouldChangePlan> sortedList = lhMouldChangePlanService == null
                    ? list : lhMouldChangePlanService.sortByDefaultOrder(list);
            list = sortedList == null ? list : sortedList;
        }
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
            // listExportData 已完成默认全量排序，模板组装阶段直接复用该顺序，避免重复查询计划量。
            excelDataList.add(buildExportDataList(exportList, queryVO, previousDayPlanKeySet, true));
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
     * @param item 模具交替计划导出视图
     * @return 最终展示模具号
     */
    private String resolveDisplayMouldCode(LhMouldChangePlanVo item) {
        return StringUtils.defaultIfBlank(item.getMouldCode(), "");
    }

    /**
     * 构建服务未注入时的导出排序比较器，兼容独立导出组装测试调用。
     *
     * @return 默认导出排序比较器
     */
    private Comparator<LhMouldChangePlanVo> buildLegacyExportComparator() {
        return Comparator.comparingLong((LhMouldChangePlanVo item) -> item.getPlanDate() == null
                        ? Long.MAX_VALUE : DateUtil.beginOfDay(item.getPlanDate()).getTime())
                .thenComparingInt(this::resolveLegacyMouldChangeTypeSortOrder)
                .thenComparing((LhMouldChangePlanVo item) -> StringUtils.defaultIfBlank(item.getClassIndex(), ""))
                .thenComparing(LhMouldChangePlanVo::getChangeTime,
                        Comparator.nullsLast(Date::compareTo))
                .thenComparing((LhMouldChangePlanVo item) -> StringUtils.defaultIfBlank(item.getLhMachineCode(), ""))
                .thenComparing(LhMouldChangePlanVo::getPlanOrder,
                        Comparator.nullsLast(Integer::compareTo))
                .thenComparing(LhMouldChangePlanVo::getId,
                        Comparator.nullsLast(Long::compareTo));
    }

    /**
     * 解析兼容导出排序中的模具交替类型优先级。
     *
     * @param item 模具交替计划导出视图
     * @return 清洗计划返回0，交替计划返回1，其他计划返回2
     */
    private int resolveLegacyMouldChangeTypeSortOrder(LhMouldChangePlanVo item) {
        if (item == null) {
            return 2;
        }
        String changeMouldType = item.getChangeMouldType();
        if (MouldChangeTypeEnum.containsAnyCode(changeMouldType,
                MouldChangeTypeEnum.SAND_BLAST.getCode(), MouldChangeTypeEnum.DRY_ICE.getCode())) {
            return 0;
        }
        return MouldChangeTypeEnum.containsAnyCode(changeMouldType,
                MouldChangeTypeEnum.REGULAR.getCode(), MouldChangeTypeEnum.TYPE_BLOCK.getCode()) ? 1 : 2;
    }

    /**
     * 判断导出请求是否携带显式排序条件。
     *
     * @param queryVO 查询条件
     * @return 携带有效显式排序条件时返回true
     */
    private boolean hasExplicitSort(LhMouldChangePlan queryVO) {
        if (queryVO == null || queryVO.getParams() == null) {
            PageDomain pageDomain = this.resolvePageDomain();
            return pageDomain != null && this.isEffectiveSortValue(pageDomain.getOrderByColumn());
        }
        Object orderBy = queryVO.getParams().get("orderBy");
        Object orderByColumn = queryVO.getParams().get("orderByColumn");
        PageDomain pageDomain = this.resolvePageDomain();
        return this.isEffectiveSortValue(orderBy)
                || this.isEffectiveSortValue(orderByColumn)
                || (pageDomain != null && this.isEffectiveSortValue(pageDomain.getOrderByColumn()));
    }

    /**
     * 在存在HTTP请求时读取分页和排序参数；独立服务调用没有请求上下文时返回null。
     *
     * @return 当前请求的分页参数，无请求上下文时返回null
     */
    private PageDomain resolvePageDomain() {
        if (ServletUtils.getRequest() == null) {
            return null;
        }
        return TableSupport.getPageDomain();
    }

    /**
     * 判断排序值是否为前端实际传入的字段，过滤框架无参数时产生的字符串 null。
     *
     * @param sortValue 待判断的排序值
     * @return 有效排序字段返回true
     */
    private boolean isEffectiveSortValue(Object sortValue) {
        if (sortValue == null) {
            return false;
        }
        String value = String.valueOf(sortValue).trim();
        return StringUtils.isNotBlank(value) && !"null".equalsIgnoreCase(value);
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
        return this.buildExportDataList(list, queryVO, previousDayPlanKeySet, false);
    }

    /**
     * 构建导出数据，并允许调用方声明列表已经完成统一默认排序。
     *
     * @param list                  模具交替计划导出视图列表
     * @param queryVO               查询条件
     * @param previousDayPlanKeySet 前日交替计划唯一标识集合
     * @param alreadySorted         列表是否已由统一排序服务完成排序
     * @return 导出行数据
     */
    private List<Map<String, Object>> buildExportDataList(List<LhMouldChangePlanVo> list,
                                                          LhMouldChangePlan queryVO,
                                                          Set<String> previousDayPlanKeySet,
                                                          boolean alreadySorted) {
        if (!alreadySorted && !this.hasExplicitSort(queryVO)) {
            if (lhMouldChangePlanService != null) {
                List<LhMouldChangePlanVo> sortedList = lhMouldChangePlanService.sortVoByDefaultOrder(list);
                // 单元测试或独立组装场景可能仅提供服务桩，返回空值时沿用本地兼容排序。
                list = sortedList == null
                        ? list.stream().sorted(this.buildLegacyExportComparator()).collect(Collectors.toList())
                        : sortedList;
            } else {
                // 独立组装调用没有Spring服务时，保留原有日期、类型和稳定字段排序。
                list = list.stream().sorted(this.buildLegacyExportComparator()).collect(Collectors.toList());
            }
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

            row.put("mouldCode", this.resolveDisplayMouldCode(item));

            row.put("remark", item.getRemark());

            // 当前交替计划在前日不存在且计划日期处于排程日-1日时，仅将后物料描述字体标红。
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
     * @return 机台、前物料编码和后物料编码组成的唯一标识
     */
    private String buildMouldChangePlanKey(LhMouldChangePlan mouldChangePlan) {
        if (mouldChangePlan == null) {
            return "";
        }
        return this.buildMouldChangePlanKey(mouldChangePlan.getLhMachineCode(),
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
     * @return 机台、前物料编码和后物料编码组成的唯一标识
     */
    private String buildMouldChangePlanKey(LhMouldChangePlanVo mouldChangePlanVo) {
        if (mouldChangePlanVo == null) {
            return "";
        }
        return this.buildMouldChangePlanKey(mouldChangePlanVo.getLhMachineCode(),
                mouldChangePlanVo.getBeforeMaterialCode(), mouldChangePlanVo.getAfterMaterialCode());
    }

    /**
     * 按统一字段顺序组装模具交替计划比较唯一标识。
     *
     * @param lhMachineCode      硫化机台编码
     * @param beforeMaterialCode 前物料编码
     * @param afterMaterialCode  后物料编码
     * @return 使用竖线分隔的计划唯一标识
     */
    private String buildMouldChangePlanKey(String lhMachineCode, String beforeMaterialCode,
                                           String afterMaterialCode) {
        return String.join("|",
                StringUtils.defaultIfBlank(lhMachineCode, ""),
                StringUtils.defaultIfBlank(beforeMaterialCode, ""),
                StringUtils.defaultIfBlank(afterMaterialCode, ""));
    }

    /**
     * 判断计划日期是否处于排程日期当天或排程日期-1日范围内。
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
                || DateUtil.isSameDay(planDate, DateUtil.offsetDay(scheduleDate, -1));
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
     * <p>模具号直接取模具交替计划库表字段 MOULD_CODE，不在导出过程中重新匹配在线模具或共享模具配置。</p>
     *
     * @param list    模具交替计划列表
     * @param queryVO 查询条件，保留现有导出调用参数
     * @return 导出视图列表
     */
    public List<LhMouldChangePlanVo> buildLhMouldChangePlanVoList(List<LhMouldChangePlan> list, LhMouldChangePlan queryVO) {
        List<LhMouldChangePlanVo> resultList = new ArrayList<>();
        int seq = 1;

        for (LhMouldChangePlan lhMouldChangePlan : list) {
            LhMouldChangePlanVo lhMouldChangePlanVo = new LhMouldChangePlanVo();
            BeanUtil.copyProperties(lhMouldChangePlan, lhMouldChangePlanVo);
            lhMouldChangePlanVo.setMouldCode(StringUtils.defaultIfBlank(lhMouldChangePlan.getMouldCode(), ""));

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
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMouldCode()), "MOULD_CODE", queryVO.getMouldCode());
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
        return " CASE WHEN PLAN_DATE IS NULL THEN 1 ELSE 0 END, DATE(PLAN_DATE)"
                + ", CASE WHEN FIND_IN_SET('" + MouldChangeTypeEnum.SAND_BLAST.getCode()
                + "', CHANGE_MOULD_TYPE) > 0 OR FIND_IN_SET('" + MouldChangeTypeEnum.DRY_ICE.getCode()
                + "', CHANGE_MOULD_TYPE) > 0 THEN 0"
                + " WHEN FIND_IN_SET('" + MouldChangeTypeEnum.REGULAR.getCode()
                + "', CHANGE_MOULD_TYPE) > 0 OR FIND_IN_SET('" + MouldChangeTypeEnum.TYPE_BLOCK.getCode()
                + "', CHANGE_MOULD_TYPE) > 0 THEN 1 ELSE 2 END"
                + ", COALESCE(NULLIF(TRIM(CLASS_INDEX), ''), '')"
                + ", CASE WHEN CHANGE_TIME IS NULL THEN 1 ELSE 0 END, CHANGE_TIME"
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

    /**
     * 手工刷新指定分厂、排程日期和硫化结果批次的模具号。
     *
     * @param requestVO 请求参数，使用 factoryCode、scheduleDate 和 lhResultBatchNo
     * @return 刷新结果
     */
    @Log(title = "ui.data.column.lhMouldChangePlan.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("手工刷新模具号")
    @PostMapping("/refreshMouldCode")
    public AjaxResult refreshMouldCode(@RequestBody LhMouldChangePlan requestVO) {
        if (requestVO == null
                || StringUtils.isBlank(requestVO.getFactoryCode())
                || requestVO.getScheduleDate() == null
                || StringUtils.isBlank(requestVO.getLhResultBatchNo())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.param.error"));
        }
        lhMouldChangePlanService.refreshMouldCode(requestVO.getFactoryCode(),
                requestVO.getScheduleDate(), requestVO.getLhResultBatchNo());
        return AjaxResult.success(I18nUtil.getMessage("ui.message.operation.success"));
    }

}




