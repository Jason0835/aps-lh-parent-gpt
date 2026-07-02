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
import com.zlt.aps.enums.ConstructionStageEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.lh.api.domain.dto.LhScheduleImportDTO;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhSharedMouldPat;
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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
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
    private LhScheduleResultMapper lhScheduleResultMapper;

    /**
     * 查询模具交替计划列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhMouldChangePlan queryVO) {
        return super.list(queryVO);
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
        Map<String, Object> tableMap = new HashMap<>();
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        // 赋值表头字段名称
        setExportTitleFieldName(tableMap);
        if (CollectionUtils.isNotEmpty(list)) {
            List<LhMouldChangePlanVo> exportList = this.buildLhMouldChangePlanVoList(list, queryVO);
            excelDataList.add(buildExportDataList(exportList, queryVO));
        }
        tableMap = buildExportTableMap(queryVO.getScheduleDate());
        // 赋值表头字段名称
        setExportTitleFieldName(tableMap);

        byte[] resultBytes =  ExcelUtils.writeMultiList(inputStream, 0, tableMap, excelDataList);
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

    public List<Map<String, Object>> buildExportDataList(List<LhMouldChangePlanVo> list, LhMouldChangePlan queryVO) {
        // 按计划日期、班次、机台排序，计划日期仅按年月日比较，避免时分秒影响导出顺序
        list = list.stream().sorted(Comparator.comparing((LhMouldChangePlanVo item) ->
                                DateUtil.beginOfDay(item.getPlanDate()))
                .thenComparing(item -> StringUtils.defaultIfBlank(item.getClassIndex(), ""))
                .thenComparing(item -> StringUtils.defaultIfBlank(item.getLhMachineCode(), "")))
                .collect(Collectors.toList());
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
        List<String> materialDescList = new ArrayList<>();
        for (LhMouldChangePlanVo mouldChangePlanVo : list) {
            if (StringUtils.isNotBlank(mouldChangePlanVo.getAfterMaterialCode())) {
                materialCodeList.add(mouldChangePlanVo.getAfterMaterialCode());
            }
            if (StringUtils.isNotBlank(mouldChangePlanVo.getAfterMaterialDesc())) {
                materialDescList.add(mouldChangePlanVo.getAfterMaterialDesc());
            }
        }
        // 查询硫化排程结果，获取规格示方类型
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
                        .sorted(Comparator.comparing(item -> StringUtils.defaultIfBlank(item.getConstructionStage(), "")))
                        .collect(Collectors.toMap(item -> StringUtils.defaultIfBlank(item.getMaterialCode(), ""),
                                item -> StringUtils.defaultIfBlank(item.getConstructionStage(), ""),
                                (s1, s2) -> s1));
            }
        }
        // 查询共用模具
        Map<String, List<LhSharedMouldPat>> lhSharedMouldPatMap = new HashMap<>();
        if (CollUtil.isNotEmpty(materialDescList)) {
            LambdaQueryWrapper<LhSharedMouldPat> sharedMouldPatQueryWrapper = new LambdaQueryWrapper<>();
            sharedMouldPatQueryWrapper.eq(LhSharedMouldPat::getIsDelete, YesOrNoEnum.NO.getCode());
            sharedMouldPatQueryWrapper.eq(LhSharedMouldPat::getFactoryCode, queryVO.getFactoryCode());
            sharedMouldPatQueryWrapper.in(LhSharedMouldPat::getMaterialDesc, materialDescList);
            List<LhSharedMouldPat> lhSharedMouldPatList = lhSharedMouldPatEntityMapper.selectList(sharedMouldPatQueryWrapper);
            if (CollectionUtils.isNotEmpty(lhSharedMouldPatList)) {
                lhSharedMouldPatMap = lhSharedMouldPatList.stream().collect(Collectors.groupingBy(LhSharedMouldPat::getMaterialDesc));
            }
        }

        List<Map<String, Object>> dataList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            LhMouldChangePlanVo item = list.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("seq", i + 1);
            row.put("planDate", DateUtil.format(item.getPlanDate(), "yyyy-MM-dd"));
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
            // 示方类型
            String constructionStage = lhScheduleResultMap.getOrDefault(afterMaterialCode, "");
            String markFlag = ConstructionStageEnum.getInstance(constructionStage).getMarkFlag();
            row.put("afterMaterialType", lhTrialStatusDictDictMap.getOrDefault(markFlag, ""));
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

            List<String> mouldCodeList = new ArrayList<>();

            String isReplaceBlock = item.getIsReplaceBlock();
            if (YesOrNoEnum.YES.getCode().equals(isReplaceBlock)) {
                row.put("isReplaceBlock", "是Có");
                row.put("endType", "");
                mouldCodeList.add(item.getMouldCode());
            } else {
                row.put("isReplaceBlock", "");
            }

            if (lhSharedMouldPatMap.containsKey(afterMaterialDesc)) {
                List<LhSharedMouldPat> sharedMouldPatList = lhSharedMouldPatMap.get(afterMaterialDesc);
                for (LhSharedMouldPat lhSharedMouldPat : sharedMouldPatList) {
                    String mouldNo = StringUtils.defaultIfBlank(lhSharedMouldPat.getMouldNo(), "");
                    String patternBlock = StringUtils.defaultIfBlank(lhSharedMouldPat.getPatternBlock(), "");
                    String mouldCode = mouldNo + "/" + patternBlock;
                    mouldCodeList.add(mouldCode);
                }

                ExcelStyleVo excelStyleVo = new ExcelStyleVo();
                excelStyleVo.setRgbColor(new ExcelStyleVo.RgbColor(230, 184, 183));
                row.put("style", excelStyleVo);
            }

            row.put("mouldCode", CollUtil.isNotEmpty(mouldCodeList) ? String.join(",\n", mouldCodeList) : "");

            row.put("remark", item.getRemark());

            dataList.add(row);
        }
        return dataList;
    }

    public List<LhMouldChangePlanVo> buildLhMouldChangePlanVoList(List<LhMouldChangePlan> list, LhMouldChangePlan queryVO) {
        List<LhMouldChangePlanVo> resultList = new ArrayList<>();
        int seq = 1;
        // 查询硫化在机数据，通过机台查询，取排程日期往前最近一条数据的在机模号，拼接后回写模具号字段
        List<String> machineCodeList = new ArrayList<>();
        for (LhMouldChangePlan mouldChangePlan : list) {
            String machineCode = mouldChangePlan.getLhMachineCode();
            if (StringUtils.isNotBlank(machineCode) && !machineCodeList.contains(machineCode)) {
                machineCodeList.add(machineCode);
            }
        }
        Map<String, LhMachineOnlineInfo> lhMachineOnlineInfoMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(machineCodeList) && queryVO != null && queryVO.getScheduleDate() != null) {
            Date scheduleDateEnd = DateUtils.addDays(DateUtil.endOfDay(queryVO.getScheduleDate()), -3);
            LambdaQueryWrapper<LhMachineOnlineInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StringUtils.isNotBlank(queryVO.getFactoryCode()), LhMachineOnlineInfo::getFactoryCode, queryVO.getFactoryCode());
            wrapper.in(LhMachineOnlineInfo::getLhCode, machineCodeList);
            wrapper.isNotNull(LhMachineOnlineInfo::getOnlineDate);
            wrapper.le(LhMachineOnlineInfo::getOnlineDate, scheduleDateEnd);
            wrapper.eq(LhMachineOnlineInfo::getIsDelete, DeleteFlagEnum.NORMAL.getCode());
            wrapper.orderByDesc(LhMachineOnlineInfo::getOnlineDate);
            wrapper.orderByDesc(LhMachineOnlineInfo::getUpdateTime);
            wrapper.orderByAsc(LhMachineOnlineInfo::getLhCode);
            List<LhMachineOnlineInfo> lhMachineOnlineInfoList = lhMachineOnlineInfoMapper.selectList(wrapper);
            if (CollectionUtils.isNotEmpty(lhMachineOnlineInfoList)) {
                List<LhMachineOnlineInfo> sortedOnlineInfoList = lhMachineOnlineInfoList.stream()
                        .filter(item -> StringUtils.isNotBlank(item.getLhCode()))
                        .filter(item -> item.getOnlineDate() != null && !item.getOnlineDate().after(scheduleDateEnd))
                        .sorted(Comparator.comparing(LhMachineOnlineInfo::getOnlineDate, Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(LhMachineOnlineInfo::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(LhMachineOnlineInfo::getLhCode, Comparator.nullsLast(String::compareTo)))
                        .collect(Collectors.toList());
                for (LhMachineOnlineInfo onlineInfo : sortedOnlineInfoList) {
                    // 查询结果按上机日期和更新时间倒序，首条即为该机台排程日期前最新在机记录。
                    lhMachineOnlineInfoMap.putIfAbsent(onlineInfo.getLhCode(), onlineInfo);
                }
            }
        }
        for (LhMouldChangePlan lhMouldChangePlan : list) {
            LhMouldChangePlanVo lhMouldChangePlanVo = new LhMouldChangePlanVo();
            BeanUtil.copyProperties(lhMouldChangePlan, lhMouldChangePlanVo);

            LhMachineOnlineInfo onlineInfo = lhMachineOnlineInfoMap.get(lhMouldChangePlan.getLhMachineCode());
            if (onlineInfo != null && StringUtils.isNotBlank(onlineInfo.getInMachineMouldCode())) {
                lhMouldChangePlanVo.setMouldCode(onlineInfo.getInMachineMouldCode());
            } else {
                // 从模具变更计划赋值过来的要清空
                lhMouldChangePlanVo.setMouldCode("");
            }

            lhMouldChangePlanVo.setSeq(seq++);

            // 如果换模类型是喷砂清洗或干冰清洗，前规格不导出
            String changeMouldType = lhMouldChangePlan.getChangeMouldType();
            if (MouldChangeTypeEnum.TYPE_BLOCK.getCode().equals(changeMouldType)) {
                lhMouldChangePlanVo.setIsReplaceBlock(YesOrNoEnum.YES.getCode());
            }
            if (MouldChangeTypeEnum.SAND_BLAST.getCode().equals(changeMouldType)) {
                lhMouldChangePlanVo.setIsSandblastingClean(YesOrNoEnum.YES.getCode());
                lhMouldChangePlanVo.setBeforeMaterialCode("---");
                lhMouldChangePlanVo.setBeforeMaterialDesc("---");
            }
            if (MouldChangeTypeEnum.DRY_ICE.getCode().equals(changeMouldType)) {
                lhMouldChangePlanVo.setIsDryIceClean(YesOrNoEnum.YES.getCode());
                lhMouldChangePlanVo.setBeforeMaterialCode("---");
                lhMouldChangePlanVo.setBeforeMaterialDesc("---");
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
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getChangeMouldType()), "CHANGE_MOULD_TYPE", queryVO.getChangeMouldType());
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
        return " DATE(PLAN_DATE), CLASS_INDEX, LH_MACHINE_CODE";
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




