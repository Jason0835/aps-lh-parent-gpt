package com.zlt.aps.cx.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.vo.*;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.enums.DayVulcanizationModeEnum;
import com.zlt.aps.cx.mapper.*;
import com.zlt.aps.cx.service.CxScheduleResultService;
import com.zlt.aps.cx.service.ScheduleService;
import com.zlt.aps.cx.vo.CxScheduleImportDTO;
import com.zlt.aps.cx.vo.CxScheduleResultTemplateImportVO;
import com.zlt.aps.cx.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.cx.vo.ScheduleRequestVo;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.mp.api.domain.entity.CxScheduleResultIssue;
import com.zlt.aps.mp.api.domain.entity.MdmCxMachineFixed;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 排程管理Controller
 *
 * @author APS Team
 */
@Slf4j
@Api(tags = "排程管理")
@RestController
@RequestMapping("/cxScheduleResult")
public class ScheduleMainController extends AbstractDocBizController<CxScheduleResult> {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private CxScheduleResultService cxScheduleResultService;

    @Resource
    private CxScheduleResultMapper cxScheduleResultMapper;

    @Autowired
    private IMesItfService mesItfService;

    @Autowired
    private MdmMoldingMachineMapper moldingMachineMapper;

    @Autowired
    private CxShiftConfigMapper cxShiftConfigMapper;

    @Autowired
    private CxParamConfigMapper cxParamConfigMapper;

    @Resource
    private MdmCxMachineFixedMapper mdmCxMachineFixedMapper;

    @Resource
    private MdmStructureLhRatioMapper structureLhRatioMapper;

    @Resource
    private MdmMonthPlanProductLhCapacityMapper monthPlanProductLhCapacityMapper;

    /**
     * 查询成型排程结果列表，同时填充各班次的开始/结束时间
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody CxScheduleResult queryVO) {
        TableDataInfo tableDataInfo = super.list(queryVO);

        List<?> rows = tableDataInfo.getRows();
        if (rows == null || rows.isEmpty()) {
            return tableDataInfo;
        }

        List<CxScheduleResult> results = (List<CxScheduleResult>) rows;

        Map<String, CxShiftConfig> classFieldMap = getShiftConfigClassMap();

        for (CxScheduleResult record : results) {
            if (record.getScheduleDate() == null) {
                continue;
            }


            LocalDate scheduleDate = DateUtil.toLocalDateTime(record.getScheduleDate()).toLocalDate();

            for (int i = 1; i <= 8; i++) {
                CxShiftConfig config = classFieldMap.get("CLASS" + i);
                if (config == null) {
                    continue;
                }

                int dayOffset;
                if (config.getScheduleDay() == 1) {
                    dayOffset = -2;
                } else if (config.getScheduleDay() == 2) {
                    dayOffset = -1;
                } else {
                    dayOffset = 0;
                }

                LocalTime startLocalTime = config.getShiftStartTime();
                LocalTime endLocalTime = config.getShiftEndTime();

                LocalDate startDate;
                LocalDate endDate;
                if (config.getIsCrossDay() != null && config.getIsCrossDay() == 1) {
                    startDate = scheduleDate.plusDays(dayOffset - 1);
                    endDate = scheduleDate.plusDays(dayOffset);
                } else {
                    startDate = scheduleDate.plusDays(dayOffset);
                    endDate = scheduleDate.plusDays(dayOffset);
                }

                Date start = Date.from(startDate.atTime(startLocalTime).atZone(java.time.ZoneId.systemDefault()).toInstant());
                Date end = Date.from(endDate.atTime(endLocalTime).atZone(java.time.ZoneId.systemDefault()).toInstant());

                switch (i) {
                    case 1: record.setClass1StartTime(start); record.setClass1EndTime(end); break;
                    case 2: record.setClass2StartTime(start); record.setClass2EndTime(end); break;
                    case 3: record.setClass3StartTime(start); record.setClass3EndTime(end); break;
                    case 4: record.setClass4StartTime(start); record.setClass4EndTime(end); break;
                    case 5: record.setClass5StartTime(start); record.setClass5EndTime(end); break;
                    case 6: record.setClass6StartTime(start); record.setClass6EndTime(end); break;
                    case 7: record.setClass7StartTime(start); record.setClass7EndTime(end); break;
                    case 8: record.setClass8StartTime(start); record.setClass8EndTime(end); break;
                }
            }

            // classXFinishQty = 0 时设为 null
            if (record.getClass1FinishQty() != null && record.getClass1FinishQty().compareTo(BigDecimal.ZERO) == 0) {
                record.setClass1FinishQty(null);
            }
            if (record.getClass2FinishQty() != null && record.getClass2FinishQty().compareTo(BigDecimal.ZERO) == 0) {
                record.setClass2FinishQty(null);
            }
            if (record.getClass3FinishQty() != null && record.getClass3FinishQty().compareTo(BigDecimal.ZERO) == 0) {
                record.setClass3FinishQty(null);
            }
            if (record.getClass4FinishQty() != null && record.getClass4FinishQty().compareTo(BigDecimal.ZERO) == 0) {
                record.setClass4FinishQty(null);
            }
            if (record.getClass5FinishQty() != null && record.getClass5FinishQty().compareTo(BigDecimal.ZERO) == 0) {
                record.setClass5FinishQty(null);
            }
            if (record.getClass6FinishQty() != null && record.getClass6FinishQty().compareTo(BigDecimal.ZERO) == 0) {
                record.setClass6FinishQty(null);
            }
            if (record.getClass7FinishQty() != null && record.getClass7FinishQty().compareTo(BigDecimal.ZERO) == 0) {
                record.setClass7FinishQty(null);
            }
            if (record.getClass8FinishQty() != null && record.getClass8FinishQty().compareTo(BigDecimal.ZERO) == 0) {
                record.setClass8FinishQty(null);
            }

            // classXPlanQty = 0 时设为 null
            if (record.getClass1PlanQty() != null && record.getClass1PlanQty().compareTo(BigDecimal.ZERO) == 0) {
                record.setClass1PlanQty(null);
            }
            if (record.getClass2PlanQty() != null && record.getClass2PlanQty().compareTo(BigDecimal.ZERO) == 0) {
                record.setClass2PlanQty(null);
            }
            if (record.getClass3PlanQty() != null && record.getClass3PlanQty().compareTo(BigDecimal.ZERO) == 0) {
                record.setClass3PlanQty(null);
            }
            if (record.getClass4PlanQty() != null && record.getClass4PlanQty().compareTo(BigDecimal.ZERO) == 0) {
                record.setClass4PlanQty(null);
            }
            if (record.getClass5PlanQty() != null && record.getClass5PlanQty().compareTo(BigDecimal.ZERO) == 0) {
                record.setClass5PlanQty(null);
            }
            if (record.getClass6PlanQty() != null && record.getClass6PlanQty().compareTo(BigDecimal.ZERO) == 0) {
                record.setClass6PlanQty(null);
            }
            if (record.getClass7PlanQty() != null && record.getClass7PlanQty().compareTo(BigDecimal.ZERO) == 0) {
                record.setClass7PlanQty(null);
            }
            if (record.getClass8PlanQty() != null && record.getClass8PlanQty().compareTo(BigDecimal.ZERO) == 0) {
                record.setClass8PlanQty(null);
            }
        }

        return tableDataInfo;
    }

    /**
     * 根据硫化排程结果ID列表查询关联的成型排程结果。
     *
     * @param lhScheduleIds 硫化排程结果ID列表
     * @return 成型排程结果列表
     */
    @ApiOperation("根据硫化排程结果ID列表查询关联成型排程结果")
    @PostMapping("/listByLhScheduleIds")
    public List<CxScheduleResult> listByLhScheduleIds(@RequestBody List<Long> lhScheduleIds) {
        return cxScheduleResultService.listByLhScheduleIds(lhScheduleIds);
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.cxScheduleResult.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody CxScheduleResult entity) {
        return super.save(entity);
    }

    /**
     * 删除成型排程结果
     */
    @Log(title = "ui.data.column.cxScheduleResult.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 获取成型排程结果详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public CxScheduleResult getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    /**
     * 根据集合导入成型排程结果数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.cxScheduleResult.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody com.ruoyi.api.gateway.system.domain.vo.ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出成型排程结果列表
     */
    @Log(title = "ui.data.column.cxScheduleResult.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody CxScheduleResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    /**
     * 导出成型余量数据。
     *
     * @param queryVO 查询条件，按成型排程结果列表查询口径筛选数据
     * @param fileName 导出文件名
     * @return 成型余量Excel文件字节数组
     */
    @Log(title = "成型余量数据", businessType = BusinessType.EXPORT)
    @ApiOperation("导出成型余量数据")
    @PostMapping("/exportCxRemainQty/{fileName}")
    public byte[] exportCxRemainQty(@RequestBody CxScheduleResult queryVO, @PathVariable("fileName") String fileName) {
        return cxScheduleResultService.exportCxRemainQty(queryVO, fileName);
    }

    /**
     * 导出成型结构切换数据。
     *
     * @param queryVO 查询条件，按成型排程结果列表查询口径筛选数据
     * @param fileName 导出文件名
     * @return 成型结构切换Excel文件字节数组
     */
    @Log(title = "成型结构切换数据", businessType = BusinessType.EXPORT)
    @ApiOperation("导出成型结构切换数据")
    @PostMapping("/exportStructureChange/{fileName}")
    public byte[] exportStructureChange(@RequestBody CxScheduleResult queryVO, @PathVariable("fileName") String fileName) {
        return cxScheduleResultService.exportStructureChange(queryVO, fileName);
    }

    @ApiOperation(value = "生成排程", notes = "根据日期和天数生成排程")
    @PostMapping("/generate")
    public AjaxResult generateSchedule(@RequestBody ScheduleGenerateVo dto) {
        if (dto.getScheduleDate() == null) {
            return AjaxResult.error("排程日期不能为空");
        }
        if (dto.getDays() == null || dto.getDays() < 1) {
            // 默认排产3天
            dto.setDays(3);
        }
        // 只需要调用一次executeSchedule，days参数表示排产天数
        ScheduleRequestVo request = new ScheduleRequestVo();
        // 最后一天日期
        request.setScheduleDate(dto.getScheduleDate());
        request.setOverwrite(dto.getOverwrite() != null ? dto.getOverwrite() : false);
        request.setFactoryCode(dto.getFactoryCode());
        request.setScheduleType(dto.getScheduleType());
        request.setScheduleMode(dto.getScheduleType());
        // 传递排产天数
        request.setDays(dto.getDays());
        ScheduleService.ScheduleResult result = scheduleService.executeSchedule(request);
        if (result.isSuccess()) {
            return AjaxResult.success();
        } else {
            // 校验不通过时，构建校验摘要返回前端
            ScheduleService.ValidationSummary summary = new ScheduleService.ValidationSummary();
            summary.setErrorCount(result.getValidationErrors() != null ? result.getValidationErrors().size() : 0);
            summary.setWarningCount(result.getValidationWarnings() != null ? result.getValidationWarnings().size() : 0);
            summary.setErrors(result.getValidationErrors());
            summary.setWarnings(result.getValidationWarnings());
            return AjaxResult.error(
                    "排程失败[" + dto.getScheduleDate() + "]: 数据完整性校验不通过，共 "
                            + summary.getErrorCount() + " 项错误，"
                            + summary.getWarningCount() + " 项警告",
                    summary);
        }
    }

    /**
     * 成型排程结果下发到MES中间表
     * 业务规则：
     * 1. 查询当天的排程日期，获取8班数据
     * 2. 8班对应关系：
     *    - 1-2班：当天的早、中班（夜班已生产）
     *    - 3-5班：第二天的夜、早、中班
     *    - 6-8班：第三天的夜、早、中班
     * 3. 中间表映射：1班=夜班，2班=早班，3班=中班
     * 4. 当天、隔天数据更新（存在则更新，不存在则插入）
     * 5. 第三天数据下发（插入）
     *
     * @return 下发结果
     */
    @ApiOperation(value = "成型排程结果下发到MES", notes = "将成型排程结果下发到MES中间表，8班数据对应3天班次")
    @Log(title = "成型排程结果下发", businessType = BusinessType.PUBLISH)
    @PostMapping("/issueToMes")
    public AjaxResult issueCxScheduleResultToMes() {

        // 获取今天、明天、后天的日期
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfterTomorrow = today.plusDays(2);

        // 只查询当天的排程结果数据（包含8班数据）
        List<CxScheduleResult> scheduleResultList = cxScheduleResultService.listByScheduleDate(today);

        if (scheduleResultList.isEmpty()) {
            return AjaxResult.error("没有需要下发的成型排程结果数据");
        }

        // 转换为3天的下发数据
        List<CxScheduleResultIssue> day1IssueList = new ArrayList<>();    // 当天（更新）
        List<CxScheduleResultIssue> day2IssueList = new ArrayList<>();    // 隔天（更新）
        List<CxScheduleResultIssue> day3IssueList = new ArrayList<>();    // 后天（插入）

        for (CxScheduleResult source : scheduleResultList) {
            // 第1天（当天）- 更新2班数据（早中班）
            CxScheduleResultIssue day1Issue = convertToDay1IssueEntity(source, today);
            if (day1Issue != null) {
                day1IssueList.add(day1Issue);
            }

            // 第2天（隔天）- 更新3班数据（夜早中班）
            CxScheduleResultIssue day2Issue = convertToDay2IssueEntity(source, tomorrow);
            if (day2Issue != null) {
                day2IssueList.add(day2Issue);
            }

            // 第3天（后天）- 下发3班数据（夜早中班）
            CxScheduleResultIssue day3Issue = convertToDay3IssueEntity(source, dayAfterTomorrow);
            if (day3Issue != null) {
                day3IssueList.add(day3Issue);
            }
        }

        if (day1IssueList.isEmpty() && day2IssueList.isEmpty() && day3IssueList.isEmpty()) {
            return AjaxResult.error("没有需要下发的成型排程结果数据");
        }

        // 合并所有数据并调用下发接口
        List<CxScheduleResultIssue> allIssueList = new ArrayList<>();
        allIssueList.addAll(day1IssueList);
        allIssueList.addAll(day2IssueList);
        allIssueList.addAll(day3IssueList);

        // 通过Feign客户端调用itf模块的下发接口
        return mesItfService.issueCxScheduleResult(allIssueList);
    }

    /**
     * 转换为第1天（当天）的下发实体
     * 8班数据：1班(早)、2班(中) -> 中间表：1班(夜)=空, 2班(早)=1班, 3班(中)=2班
     * 业务规则：只更新2班数据（早中班），即中间表的2班(早)和3班(中)
     */
    private CxScheduleResultIssue convertToDay1IssueEntity(CxScheduleResult source, LocalDate scheduleDate) {
        if (source == null) {
            return null;
        }

        CxScheduleResultIssue target = new CxScheduleResultIssue();

        // 基础字段映射
        target.setId(source.getId());
        target.setCxBatchNo(source.getCxBatchNo());
        target.setOrderNo(source.getOrderNo());
        target.setScheduleDate(scheduleDate.atStartOfDay());

        // 机台信息
        target.setMachineCode(source.getCxMachineCode());
        target.setMachineName(source.getCxMachineName());
        target.setLhMachineCode(source.getLhMachineCode());
        target.setLhMachineName(source.getLhMachineName());
        target.setAvailableMoldQty(source.getLhMachineQty());

        // 物料信息
        target.setMaterialCode(source.getMaterialCode());
        target.setMesMaterialCode(null); // MES物料编码需要另外查询
        target.setSpecDesc(source.getMaterialDesc());
        target.setEmbryoCode(source.getEmbryoCode());
        target.setEmbryoSpecDesc(null); // 胎胚物料描述需要另外查询

        // 中间表1班 = 夜班（当天夜班已生产，清空）
        target.setClass1PlanQtySeq(BigDecimal.valueOf(1));
        target.setClass1AnalysisInput(null);
        target.setClass1Analysis(null);
        target.setClass1PlanQty(BigDecimal.ZERO);
        target.setClass1ExampleType(null);
        target.setClass1ExampleNo(null);

        // 中间表2班 = 早班（1班数据）
        target.setClass2PlanQtySeq(BigDecimal.valueOf(2));
        target.setClass2AnalysisInput(source.getClass1AnalysisInput());
        target.setClass2Analysis(source.getClass1Analysis());
        target.setClass2PlanQty(source.getClass1PlanQty());
        target.setClass2ExampleType(null); // 示方类型需要另外查询
        target.setClass2ExampleNo(null); // 示方号需要另外查询

        // 中间表3班 = 中班（2班数据）
        target.setClass3PlanQtySeq(BigDecimal.valueOf(3));
        target.setClass3AnalysisInput(source.getClass2AnalysisInput());
        target.setClass3Analysis(source.getClass2Analysis());
        target.setClass3PlanQty(source.getClass2PlanQty());
        target.setClass3ExampleType(null);
        target.setClass3ExampleNo(null);

        return target;
    }

    /**
     * 转换为第2天（隔天）的下发实体
     * 8班数据：3班(夜)、4班(早)、5班(中) -> 中间表：1班(夜)=3班, 2班(早)=4班, 3班(中)=5班
     * 业务规则：更新3班数据（夜早中班）
     */
    private CxScheduleResultIssue convertToDay2IssueEntity(CxScheduleResult source, LocalDate scheduleDate) {
        if (source == null) {
            return null;
        }

        CxScheduleResultIssue target = new CxScheduleResultIssue();

        // 基础字段映射
        target.setId(source.getId());
        target.setCxBatchNo(source.getCxBatchNo());
        target.setOrderNo(source.getOrderNo());
        target.setScheduleDate(scheduleDate.atStartOfDay());

        // 机台信息
        target.setMachineCode(source.getCxMachineCode());
        target.setMachineName(source.getCxMachineName());
        target.setLhMachineCode(source.getLhMachineCode());
        target.setLhMachineName(source.getLhMachineName());
        target.setAvailableMoldQty(source.getLhMachineQty());

        // 物料信息
        target.setMaterialCode(source.getMaterialCode());
        target.setMesMaterialCode(null);
        target.setSpecDesc(source.getMaterialDesc());
        target.setEmbryoCode(source.getEmbryoCode());
        target.setEmbryoSpecDesc(null);

        // 中间表1班 = 夜班（3班数据）
        target.setClass1PlanQtySeq(BigDecimal.valueOf(1));
        target.setClass1AnalysisInput(source.getClass3AnalysisInput());
        target.setClass1Analysis(source.getClass3Analysis());
        target.setClass1PlanQty(source.getClass3PlanQty());
        target.setClass1ExampleType(null);
        target.setClass1ExampleNo(null);

        // 中间表2班 = 早班（4班数据）
        target.setClass2PlanQtySeq(BigDecimal.valueOf(2));
        target.setClass2AnalysisInput(source.getClass4AnalysisInput());
        target.setClass2Analysis(source.getClass4Analysis());
        target.setClass2PlanQty(source.getClass4PlanQty());
        target.setClass2ExampleType(null);
        target.setClass2ExampleNo(null);

        // 中间表3班 = 中班（5班数据）
        target.setClass3PlanQtySeq(BigDecimal.valueOf(3));
        target.setClass3AnalysisInput(source.getClass5AnalysisInput());
        target.setClass3Analysis(source.getClass5Analysis());
        target.setClass3PlanQty(source.getClass5PlanQty());
        target.setClass3ExampleType(null);
        target.setClass3ExampleNo(null);

        return target;
    }

    /**
     * 转换为第3天（后天）的下发实体
     * 8班数据：6班(夜)、7班(早)、8班(中) -> 中间表：1班(夜)=6班, 2班(早)=7班, 3班(中)=8班
     * 业务规则：下发3班数据（夜早中班）
     */
    private CxScheduleResultIssue convertToDay3IssueEntity(CxScheduleResult source, LocalDate scheduleDate) {
        if (source == null) {
            return null;
        }

        CxScheduleResultIssue target = new CxScheduleResultIssue();

        // 基础字段映射
        target.setId(source.getId());
        target.setCxBatchNo(source.getCxBatchNo());
        target.setOrderNo(source.getOrderNo());
        target.setScheduleDate(scheduleDate.atStartOfDay());

        // 机台信息
        target.setMachineCode(source.getCxMachineCode());
        target.setMachineName(source.getCxMachineName());
        target.setLhMachineCode(source.getLhMachineCode());
        target.setLhMachineName(source.getLhMachineName());
        target.setAvailableMoldQty(source.getLhMachineQty());

        // 物料信息
        target.setMaterialCode(source.getMaterialCode());
        target.setMesMaterialCode(null);
        target.setSpecDesc(source.getMaterialDesc());
        target.setEmbryoCode(source.getEmbryoCode());
        target.setEmbryoSpecDesc(null);

        // 中间表1班 = 夜班（6班数据）
        target.setClass1PlanQtySeq(BigDecimal.valueOf(1));
        target.setClass1AnalysisInput(source.getClass6AnalysisInput());
        target.setClass1Analysis(source.getClass6Analysis());
        target.setClass1PlanQty(source.getClass6PlanQty());
        target.setClass1ExampleType(null);
        target.setClass1ExampleNo(null);

        // 中间表2班 = 早班（7班数据）
        target.setClass2PlanQtySeq(BigDecimal.valueOf(2));
        target.setClass2AnalysisInput(source.getClass7AnalysisInput());
        target.setClass2Analysis(source.getClass7Analysis());
        target.setClass2PlanQty(source.getClass7PlanQty());
        target.setClass2ExampleType(null);
        target.setClass2ExampleNo(null);

        // 中间表3班 = 中班（8班数据）
        target.setClass3PlanQtySeq(BigDecimal.valueOf(3));
        target.setClass3AnalysisInput(source.getClass8AnalysisInput());
        target.setClass3Analysis(source.getClass8Analysis());
        target.setClass3PlanQty(source.getClass8PlanQty());
        target.setClass3ExampleType(null);
        target.setClass3ExampleNo(null);

        return target;
    }

    @Override
    protected List<CxScheduleResult> listExportData(CxScheduleResult obj) {
        QueryWrapper<CxScheduleResult> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return cxScheduleResultMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return cxScheduleResultService;
    }

	@Override
	protected void builderCondition(QueryWrapper<CxScheduleResult> queryWrapper, CxScheduleResult queryVO) {
		// 排程日期查询
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getScheduleDate()), "SCHEDULE_DATE", queryVO.getScheduleDate());
		// 机台代码模糊查询
		queryWrapper.like(PubUtil.isNotEmpty(queryVO.getCxMachineCode()), "CX_MACHINE_CODE", queryVO.getCxMachineCode());
		// 物料代码模糊查询
		queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMaterialCode()), "MATERIAL_CODE", queryVO.getMaterialCode());
        // 物料代码模糊查询
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMaterialDesc()), "MATERIAL_DESC", queryVO.getMaterialDesc());
        // 物料代码模糊查询
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMainMaterialDesc()), "MAIN_MATERIAL_DESC", queryVO.getMainMaterialDesc());
		// 订单号精确查询
		queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getOrderNo()), "ORDER_NO", queryVO.getOrderNo());
		// 生产状态精确查询
		queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getProductionStatus()), "PRODUCTION_STATUS", queryVO.getProductionStatus());
		// 发布状态精确查询
		queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getIsRelease()), "IS_RELEASE", queryVO.getIsRelease());
    }

    /**
     * 按排程日期生成班次名称（含日期），便于用户在提示信息中理解
     * scheduleDate 为 T+2 日（D3日），反推 D1~D3 各天
     * 例如 scheduleDate=2026-05-20 → 早班(05-18), 中班(05-18), 夜班(05-19), ...
     */
    private String[] buildShiftNames(LocalDate scheduleDate) {
        LocalDate d1 = scheduleDate.minusDays(2);
        LocalDate d2 = scheduleDate.minusDays(1);
        LocalDate d3 = scheduleDate;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        return new String[]{"",
                "早班(" + d1.format(fmt) + ")",
                "中班(" + d1.format(fmt) + ")",
                "夜班(" + d2.format(fmt) + ")",
                "早班(" + d2.format(fmt) + ")",
                "中班(" + d2.format(fmt) + ")",
                "夜班(" + d3.format(fmt) + ")",
                "早班(" + d3.format(fmt) + ")",
                "中班(" + d3.format(fmt) + ")"};
    }

    /**
     * 将秒数格式化为可读时间（h/min/s）
     */
    private String formatSeconds(BigDecimal seconds) {
        if (seconds == null) return "0s";
        long totalSecs = seconds.longValue();
        long hours = totalSecs / 3600;
        long mins = (totalSecs % 3600) / 60;
        long secs = totalSecs % 60;
        if (hours > 0) {
            return hours + "h" + (mins > 0 ? mins + "min" : "") + (secs > 0 ? secs + "s" : "");
        } else if (mins > 0) {
            return mins + "min" + (secs > 0 ? secs + "s" : "");
        }
        return secs + "s";
    }

    @Override
    protected String getTypeCode() {
        return "CX_SCHEDULE_RESULT";
    }

    @Override
    protected String getOrderBy() {
        return "schedule_date desc, cx_machine_code asc";
    }

    /**
     * 【调量】调整各班计划量
     * 业务规则：
     * 1. 只能修改当前班次及后续班次，不能修改历史班次
     * 2. 修改后的计划量不能低于已完成量
     * 3. 将排程记录的发布状态调整为待发布
     * 4. 按单据ID数据库物理修改单据数据
     */
    @Log(title = "调量", businessType = BusinessType.UPDATE)
    @ApiOperation("调量")
    @PostMapping("/adjustQty")
    public AjaxResult adjustQty(@RequestBody ScheduleAdjustVo vo) {
        if (vo.getId() == null) {
            return AjaxResult.error("排程记录ID不能为空");
        }

        CxScheduleResult record = cxScheduleResultMapper.selectById(vo.getId());
        if (record == null) {
            return AjaxResult.error("排程记录不存在");
        }

        LocalDate scheduleLocalDate = DateUtil.toLocalDateTime(record.getScheduleDate()).toLocalDate();
        LocalDateTime now = LocalDateTime.now();

        // 加载班次配置，用于校验班次是否已过
        Map<String, CxShiftConfig> shiftConfigMap = getShiftConfigClassMap();

        // 校验每个班次的计划量
        AjaxResult validationResult = validateAdjustQtyShifts(vo, record, scheduleLocalDate, now, shiftConfigMap);
        if (validationResult != null) {
            return validationResult;
        }

        // 校验调量后是否超出机台产能
        MdmMoldingMachine adjMachine = moldingMachineMapper.selectOne(
                new QueryWrapper<MdmMoldingMachine>()
                        .eq("CX_MACHINE_CODE", record.getCxMachineCode())
                        .eq("IS_ACTIVE", 1));
        if (adjMachine != null && adjMachine.getMaxDayCapacity() != null && adjMachine.getMaxDayCapacity() > 0) {
            Object[] capData = loadCapacityData(record.getFactoryCode());
            @SuppressWarnings("unchecked")
            List<MonthPlanProductLhCapacityVo> capacityList = (List<MonthPlanProductLhCapacityVo>) capData[0];
            DayVulcanizationModeEnum mode = (DayVulcanizationModeEnum) capData[1];

            int dailyLh = getDailyLhCapacity(capacityList, mode, record.getMaterialCode(), adjMachine.getMaxDayCapacity());
            BigDecimal singleTireTime = calcSingleTireTime(adjMachine, record.getStructureName(), dailyLh);

            // 查询该机台当天所有记录（含当前记录自身）
            List<CxScheduleResult> allRecords = cxScheduleResultMapper.selectList(
                    new LambdaQueryWrapper<CxScheduleResult>()
                            .eq(CxScheduleResult::getScheduleDate, new java.sql.Date(record.getScheduleDate().getTime()))
                            .eq(CxScheduleResult::getCxMachineCode, record.getCxMachineCode()));
            BigDecimal[] existingTime = calcShiftTimeConsumed(adjMachine, record.getScheduleDate(),
                    allRecords, capacityList, mode, adjMachine.getMaxDayCapacity());

            // 对每个变更的班次，扣除旧计划的时间+加上新计划的时间，检查是否超产能
            BigDecimal[] newPlanQtys = {null, vo.getClass1PlanQty(), vo.getClass2PlanQty(), vo.getClass3PlanQty(),
                    vo.getClass4PlanQty(), vo.getClass5PlanQty(), vo.getClass6PlanQty(),
                    vo.getClass7PlanQty(), vo.getClass8PlanQty()};
            BigDecimal[] oldPlanQtys = {null, record.getClass1PlanQty(), record.getClass2PlanQty(), record.getClass3PlanQty(),
                    record.getClass4PlanQty(), record.getClass5PlanQty(), record.getClass6PlanQty(),
                    record.getClass7PlanQty(), record.getClass8PlanQty()};
            BigDecimal shiftTotalSeconds = BigDecimal.valueOf(28800L);
            String[] shiftNames = buildShiftNames(scheduleLocalDate);
            for (int i = 1; i <= 8; i++) {
                if (newPlanQtys[i] == null || (oldPlanQtys[i] != null && newPlanQtys[i].compareTo(oldPlanQtys[i]) == 0)) continue;
                // 扣除旧时间，加上新时间
                BigDecimal oldTime = oldPlanQtys[i] != null ? oldPlanQtys[i].multiply(singleTireTime) : BigDecimal.ZERO;
                BigDecimal newTime = newPlanQtys[i].multiply(singleTireTime);
                BigDecimal adjustedTotal = existingTime[i].subtract(oldTime).add(newTime);
                if (adjustedTotal.compareTo(shiftTotalSeconds) > 0) {
                    return AjaxResult.error("调量失败：" + shiftNames[i] + "调整后总耗时(" + formatSeconds(adjustedTotal)
                            + ")超过每班8h");
                }
            }
        }

        // 更新计划量
        if (vo.getClass1PlanQty() != null) record.setClass1PlanQty(vo.getClass1PlanQty());
        if (vo.getClass2PlanQty() != null) record.setClass2PlanQty(vo.getClass2PlanQty());
        if (vo.getClass3PlanQty() != null) record.setClass3PlanQty(vo.getClass3PlanQty());
        if (vo.getClass4PlanQty() != null) record.setClass4PlanQty(vo.getClass4PlanQty());
        if (vo.getClass5PlanQty() != null) record.setClass5PlanQty(vo.getClass5PlanQty());
        if (vo.getClass6PlanQty() != null) record.setClass6PlanQty(vo.getClass6PlanQty());
        if (vo.getClass7PlanQty() != null) record.setClass7PlanQty(vo.getClass7PlanQty());
        if (vo.getClass8PlanQty() != null) record.setClass8PlanQty(vo.getClass8PlanQty());

        // 更新示方书类型
        if (vo.getClass1RecipeType() != null) record.setClass1RecipeType(vo.getClass1RecipeType());
        if (vo.getClass2RecipeType() != null) record.setClass2RecipeType(vo.getClass2RecipeType());
        if (vo.getClass3RecipeType() != null) record.setClass3RecipeType(vo.getClass3RecipeType());
        if (vo.getClass4RecipeType() != null) record.setClass4RecipeType(vo.getClass4RecipeType());
        if (vo.getClass5RecipeType() != null) record.setClass5RecipeType(vo.getClass5RecipeType());
        if (vo.getClass6RecipeType() != null) record.setClass6RecipeType(vo.getClass6RecipeType());
        if (vo.getClass7RecipeType() != null) record.setClass7RecipeType(vo.getClass7RecipeType());
        if (vo.getClass8RecipeType() != null) record.setClass8RecipeType(vo.getClass8RecipeType());

        // 更新示方书编号
        if (vo.getClass1RecipeNo() != null) record.setClass1RecipeNo(vo.getClass1RecipeNo());
        if (vo.getClass2RecipeNo() != null) record.setClass2RecipeNo(vo.getClass2RecipeNo());
        if (vo.getClass3RecipeNo() != null) record.setClass3RecipeNo(vo.getClass3RecipeNo());
        if (vo.getClass4RecipeNo() != null) record.setClass4RecipeNo(vo.getClass4RecipeNo());
        if (vo.getClass5RecipeNo() != null) record.setClass5RecipeNo(vo.getClass5RecipeNo());
        if (vo.getClass6RecipeNo() != null) record.setClass6RecipeNo(vo.getClass6RecipeNo());
        if (vo.getClass7RecipeNo() != null) record.setClass7RecipeNo(vo.getClass7RecipeNo());
        if (vo.getClass8RecipeNo() != null) record.setClass8RecipeNo(vo.getClass8RecipeNo());

        // 更新原因分析手工输入
        if (vo.getClass1AnalysisInput() != null) record.setClass1AnalysisInput(vo.getClass1AnalysisInput());
        if (vo.getClass2AnalysisInput() != null) record.setClass2AnalysisInput(vo.getClass2AnalysisInput());
        if (vo.getClass3AnalysisInput() != null) record.setClass3AnalysisInput(vo.getClass3AnalysisInput());
        if (vo.getClass4AnalysisInput() != null) record.setClass4AnalysisInput(vo.getClass4AnalysisInput());
        if (vo.getClass5AnalysisInput() != null) record.setClass5AnalysisInput(vo.getClass5AnalysisInput());
        if (vo.getClass6AnalysisInput() != null) record.setClass6AnalysisInput(vo.getClass6AnalysisInput());
        if (vo.getClass7AnalysisInput() != null) record.setClass7AnalysisInput(vo.getClass7AnalysisInput());
        if (vo.getClass8AnalysisInput() != null) record.setClass8AnalysisInput(vo.getClass8AnalysisInput());

        // 调整为待发布状态
        record.setIsRelease("0");

        int rows = cxScheduleResultMapper.updateById(record);
        if (rows > 0) {
            log.info("调量成功，记录ID：{}", vo.getId());
            return AjaxResult.success("调量成功");
        } else {
            return AjaxResult.error("调量失败");
        }
    }

    /**
     * 校验调量时各班的计划量
     * 业务规则：
     * - 根据各班次的结束时间判断是否为历史班次
     * - 已结束的班次不可调整
     * - 修改后的计划量不能低于已完成量
     */
    private AjaxResult validateAdjustQtyShifts(ScheduleAdjustVo vo, CxScheduleResult record,
                                                LocalDate scheduleLocalDate, LocalDateTime now,
                                                 Map<String, CxShiftConfig> configMap) {
        String[] shiftNames = buildShiftNames(scheduleLocalDate);

        BigDecimal[] planQtys = {null, vo.getClass1PlanQty(), vo.getClass2PlanQty(), vo.getClass3PlanQty(),
                vo.getClass4PlanQty(), vo.getClass5PlanQty(), vo.getClass6PlanQty(),
                vo.getClass7PlanQty(), vo.getClass8PlanQty()};
        // 原始数据库值，用于判断用户是否实际修改
        BigDecimal[] origPlans = {null, record.getClass1PlanQty(), record.getClass2PlanQty(), record.getClass3PlanQty(),
                record.getClass4PlanQty(), record.getClass5PlanQty(), record.getClass6PlanQty(),
                record.getClass7PlanQty(), record.getClass8PlanQty()};
        BigDecimal[] finishQtys = {null, record.getClass1FinishQty(), record.getClass2FinishQty(),
                record.getClass3FinishQty(), record.getClass4FinishQty(), record.getClass5FinishQty(),
                record.getClass6FinishQty(), record.getClass7FinishQty(), record.getClass8FinishQty()};

        for (int i = 1; i <= 8; i++) {
            // 前端未传值 或 值未变化（前端置灰未修改）→ 跳过校验
            // 用 compareTo 代替 equals，避免 BigDecimal 精度/scale 差异导致误判
            if (planQtys[i] == null
                    || (origPlans[i] != null && planQtys[i].compareTo(origPlans[i]) == 0)) {
                continue;
            }

            if (isShiftPast(i, scheduleLocalDate, now, configMap)) {
                return AjaxResult.error(shiftNames[i] + "计划量不可调整：该班次已过");
            }

            if (finishQtys[i] != null && planQtys[i].compareTo(finishQtys[i]) < 0) {
                return AjaxResult.error(shiftNames[i] + "计划量不能低于已完成量：" + finishQtys[i]);
            }
        }

        return null;
    }

    /**
     * 查询启用状态的班次配置，按 classField(CLASS1~CLASS8) 构建映射
     * 供各方法复用，避免硬编码班次时间
     *
     * @return Map<"CLASS1"~"CLASS8", CxShiftConfig>
     */
    private Map<String, CxShiftConfig> getShiftConfigClassMap() {
        List<CxShiftConfig> shiftConfigs = cxShiftConfigMapper.selectList(
                new LambdaQueryWrapper<CxShiftConfig>()
                        .eq(CxShiftConfig::getIsActive, 1)
                        .orderByAsc(CxShiftConfig::getScheduleDay)
                        .orderByAsc(CxShiftConfig::getDayShiftOrder));
        Map<String, CxShiftConfig> classFieldMap = new HashMap<>();
        for (CxShiftConfig config : shiftConfigs) {
            if (config.getClassField() != null) {
                classFieldMap.put(config.getClassField(), config);
            }
        }
        return classFieldMap;
    }

    /**
     * 根据班次配置和排程日期，计算该班次的结束时间
     * 与 list() 中填充班次开始/结束时间的逻辑完全一致
     *
     * @param classIndex 班次序号 1~8
     * @param scheduleDate 排程日期（T+2日，即8班的最后一天）
     * @param config 对应班次的配置
     * @return 班次结束时间，若 config 为 null 则返回 null
     */
    private LocalDateTime getShiftEndDateTime(int classIndex, LocalDate scheduleDate, CxShiftConfig config) {
        if (config == null) {
            return null;
        }
        int dayOffset;
        if (config.getScheduleDay() == 1) {
            dayOffset = -2;
        } else if (config.getScheduleDay() == 2) {
            dayOffset = -1;
        } else {
            dayOffset = 0;
        }
        LocalTime endLocalTime = config.getShiftEndTime();
        LocalDate endDate;
        if (config.getIsCrossDay() != null && config.getIsCrossDay() == 1) {
            endDate = scheduleDate.plusDays(dayOffset);
        } else {
            endDate = scheduleDate.plusDays(dayOffset);
        }
        return endDate.atTime(endLocalTime);
    }

    /**
     * 判断指定班次是否为历史班次（已结束）
     * 基于 CxShiftConfig 配置表的实际班次时间判断，不再硬编码
     *
     * @param classIndex 班次序号 1~8
     * @param scheduleDate 排程日期
     * @param now 当前时间
     * @param configMap 班次配置映射（key=CLASS1~CLASS8）
     */
    private boolean isShiftPast(int classIndex, LocalDate scheduleDate, LocalDateTime now,
                                Map<String, CxShiftConfig> configMap) {
        CxShiftConfig config = configMap.get("CLASS" + classIndex);
        if (config == null) {
            return false;
        }
        LocalDateTime shiftEnd = getShiftEndDateTime(classIndex, scheduleDate, config);
        if (shiftEnd == null) {
            return false;
        }
        return !now.isBefore(shiftEnd);
    }

    /**
     * 转机台专用：判断班次是否可转移到新机台
     * 1. 已结束的班次不可转移（基于 isShiftPast 时间判断）
     * 2. T日夜班(CLASS3)即使未到结束时间也不转移（业务规则）
     */
    private boolean isShiftTransferable(int classIndex, LocalDate scheduleDate, LocalDateTime now,
                                         Map<String, CxShiftConfig> configMap) {
        if (isShiftPast(classIndex, scheduleDate, now, configMap)) {
            return false;
        }
        if (classIndex == 3) {
            return false;
        }
        return true;
    }

    /**
     * 【插单】插入新的排程记录
     * 业务规则：
     * 1. 校验唯一性：排程日期 + 机台编号 + 胎胚编号 + 物料编码 + 示方书版本
     * 2. 校验计划量不能超过成型机台设备最大日产
     * 3. 将排程记录的发布状态调整为待发布
     * 4. 批次号 = 成型排程记录批次号
     */
    @Log(title = "插单", businessType = BusinessType.INSERT)
    @ApiOperation("插单")
    @PostMapping("/insertOrder")
    public AjaxResult insertOrder(@RequestBody ScheduleInsertVo vo) {
        if (vo.getScheduleDate() == null || vo.getCxMachineCode() == null || vo.getEmbryoCode() == null) {
            return AjaxResult.error("排程日期、机台编码、胎胚编码不能为空");
        }

        Date scheduleDate = DateUtil.parse(vo.getScheduleDate());

        // 校验唯一性：排程日期 + 机台编号 + 胎胚编号 + 物料编码
        // 转为 java.sql.Date 避免 java.util.Date 精度/时区导致 = 比较失败
        LambdaQueryWrapper<CxScheduleResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CxScheduleResult::getScheduleDate, new java.sql.Date(scheduleDate.getTime()));
        queryWrapper.eq(CxScheduleResult::getCxMachineCode, vo.getCxMachineCode());
        queryWrapper.eq(CxScheduleResult::getEmbryoCode, vo.getEmbryoCode());
        if (vo.getMaterialCode() != null && !vo.getMaterialCode().isEmpty()) {
            queryWrapper.eq(CxScheduleResult::getMaterialCode, vo.getMaterialCode());
        } else {
            queryWrapper.isNull(CxScheduleResult::getMaterialCode);
        }

        Long count = cxScheduleResultMapper.selectCount(queryWrapper);
        if (count > 0) {
            return AjaxResult.error("插单失败：该日已存在相同机台、胎胚、物料的排程记录");
        }

        // 校验该机台是否配置了不可作业规格/物料
        MdmCxMachineFixed machineFixed = mdmCxMachineFixedMapper.selectOne(
                new LambdaQueryWrapper<MdmCxMachineFixed>()
                        .eq(MdmCxMachineFixed::getCxMachineCode, vo.getCxMachineCode()));
        if (machineFixed != null) {
            // 检查不可作业结构
            if (vo.getStructureName() != null
                    && machineFixed.getSplitDisableStructure().contains(vo.getStructureName())) {
                return AjaxResult.error("当前插单规格不可在" + vo.getCxMachineCode() + "机台生产");
            }
            // 检查不可作业SKU（物料编码）
            if (vo.getMaterialCode() != null
                    && machineFixed.getSplitDisableMaterialCode().contains(vo.getMaterialCode())) {
                return AjaxResult.error("当前插单规格不可在" + vo.getCxMachineCode() + "机台生产");
            }
        }

        // 校验各班计划量是否超过机台产能（按单条胎时间计算剩余产能）
        MdmMoldingMachine machine = moldingMachineMapper.selectOne(
                new QueryWrapper<MdmMoldingMachine>()
                        .eq("CX_MACHINE_CODE", vo.getCxMachineCode())
                        .eq("IS_ACTIVE", 1));
        if (machine != null && machine.getMaxDayCapacity() != null && machine.getMaxDayCapacity() > 0) {
            // 加载该工厂所有物料日硫化量
            Object[] capData = loadCapacityData(vo.getFactoryCode());
            @SuppressWarnings("unchecked")
            List<MonthPlanProductLhCapacityVo> capacityList = (List<MonthPlanProductLhCapacityVo>) capData[0];
            DayVulcanizationModeEnum mode = (DayVulcanizationModeEnum) capData[1];

            int dailyLhCapacity = getDailyLhCapacity(capacityList, mode, vo.getMaterialCode(), machine.getMaxDayCapacity());

            // 查询该机台当天已有排程记录
            List<CxScheduleResult> existingRecords = cxScheduleResultMapper.selectList(
                    new LambdaQueryWrapper<CxScheduleResult>()
                            .eq(CxScheduleResult::getScheduleDate, new java.sql.Date(scheduleDate.getTime()))
                            .eq(CxScheduleResult::getCxMachineCode, vo.getCxMachineCode()));

            // 计算各班次时间占用，同时收集每条记录耗时明细
            StringBuilder detailSb = new StringBuilder();
            BigDecimal[] existingTimeSeconds = new BigDecimal[9];
            for (int i = 1; i <= 8; i++) existingTimeSeconds[i] = BigDecimal.ZERO;
            for (CxScheduleResult er : existingRecords) {
                int erDailyLh = getDailyLhCapacity(capacityList, mode, er.getMaterialCode(), machine.getMaxDayCapacity());
                BigDecimal erSingleTireTime = calcSingleTireTime(machine, er.getStructureName(), erDailyLh);
                BigDecimal recordTotal = BigDecimal.ZERO;
                for (int i = 1; i <= 8; i++) {
                    BigDecimal pq = getClassPlanQty(er, i);
                    if (pq != null) {
                        BigDecimal time = pq.multiply(erSingleTireTime);
                        existingTimeSeconds[i] = existingTimeSeconds[i].add(time);
                        recordTotal = recordTotal.add(time);
                    }
                }
                detailSb.append("  ").append(er.getMaterialCode()).append("(").append(er.getMaterialDesc()).append(")")
                        .append(" 结构:").append(er.getStructureName())
                        .append(" 单条:").append(formatSeconds(erSingleTireTime))
                        .append(" 总耗时:").append(formatSeconds(recordTotal)).append("\n");
            }
            log.info("机台{} 已有记录明细:\n{}", vo.getCxMachineCode(), detailSb.toString());

            // 插单物料单条胎耗时
            BigDecimal insertSingleTireTime = calcSingleTireTime(machine, vo.getStructureName(), dailyLhCapacity);

            // 逐班校验
            BigDecimal[] planQtys = {null, vo.getClass1PlanQty(), vo.getClass2PlanQty(), vo.getClass3PlanQty(),
                    vo.getClass4PlanQty(), vo.getClass5PlanQty(), vo.getClass6PlanQty(),
                    vo.getClass7PlanQty(), vo.getClass8PlanQty()};
            String[] shiftNames = buildShiftNames(DateUtil.toLocalDateTime(scheduleDate).toLocalDate());
            String capErr = checkPerShiftCapacity(planQtys, existingTimeSeconds, insertSingleTireTime, shiftNames);
            if (capErr != null) {
                StringBuilder msg = new StringBuilder();
                msg.append("<b>⚠ 插单产能校验不通过</b><br/>");
                msg.append("排程日期：").append(DateUtil.format(scheduleDate, "yyyy-MM-dd")).append("<br/>");
                msg.append("机台：").append(vo.getCxMachineCode()).append("<br/>");
                msg.append("插单物料：").append(vo.getMaterialCode()).append("(").append(vo.getSpecDesc()).append(")<br/>");
                msg.append("插单结构：").append(vo.getStructureName()).append("<br/>");
                msg.append("单条耗时：").append(formatSeconds(insertSingleTireTime)).append("<br/>");
                msg.append("单班产能：").append(formatSeconds(BigDecimal.valueOf(28800L))).append("<br/><br/>");
                msg.append("<b>错误：</b><br/>");
                String capErrText = capErr.replace("已有耗时", "<br/>已有耗时")
                        .replace("剩余时间", "<br/>剩余时间")
                        .replace("最多可生产", "<br/>最多可生产")
                        .replace("计划量", "<br/>计划量");
                msg.append("  ").append(capErrText).append("<br/><br/>");
                msg.append("<b>该机台当天已有记录耗时明细：</b><br/>");
                for (String line : detailSb.toString().split("\n")) {
                    if (!line.trim().isEmpty()) {
                        msg.append("  ").append(line).append("<br/>");
                    }
                }
                msg.append("<br/>💡 提示：请减少插单计划量或清理该机台已有记录");
                return AjaxResult.error(msg.toString());
            }
        }

        // 创建新记录
        CxScheduleResult newRecord = new CxScheduleResult();
        newRecord.setScheduleDate(scheduleDate);
        newRecord.setFactoryCode(vo.getFactoryCode());
        newRecord.setCxMachineCode(vo.getCxMachineCode());
        newRecord.setCxMachineName(vo.getCxMachineName());
        newRecord.setEmbryoCode(vo.getEmbryoCode());
        newRecord.setMaterialCode(vo.getMaterialCode());
        newRecord.setMaterialDesc(vo.getSpecDesc());
        newRecord.setMainMaterialDesc(vo.getMainMaterialDesc());

        // 生成工单号：CXGD + 日期(yyyyMMdd) + 自增序号(3位)
        // 用工单号前缀 LIKE 匹配当天已有记录，避免 Date 字段比较的精度/时区问题
        String dateStr = DateUtil.format(scheduleDate, "yyyyMMdd");
        String orderNoPrefix = "CXGD" + dateStr;
        int seq = 1;
        List<CxScheduleResult> orderList = cxScheduleResultMapper.selectList(
                new LambdaQueryWrapper<CxScheduleResult>()
                        .likeRight(CxScheduleResult::getOrderNo, orderNoPrefix)
                        .orderByDesc(CxScheduleResult::getOrderNo));
        if (!orderList.isEmpty()) {
            CxScheduleResult lastRecord = orderList.get(0);
            if (lastRecord.getOrderNo() != null && lastRecord.getOrderNo().startsWith(orderNoPrefix)) {
                try {
                    seq = Integer.parseInt(lastRecord.getOrderNo().substring(orderNoPrefix.length())) + 1;
                } catch (NumberFormatException e) {
                    log.warn("解析工单号序号失败，使用默认序号1：{}", lastRecord.getOrderNo());
                }
            }
        }
        newRecord.setOrderNo(orderNoPrefix + String.format("%03d", seq));

        // 成型批次号：若用户未提供则自动生成 CXPC + 日期 + 序号
        if (vo.getCxBatchNo() != null && !vo.getCxBatchNo().isEmpty()) {
            newRecord.setCxBatchNo(vo.getCxBatchNo());
        } else {
            String batchNoPrefix = "CXPC" + dateStr;
            int batchSeq = 1;
            List<CxScheduleResult> batchList = cxScheduleResultMapper.selectList(
                    new LambdaQueryWrapper<CxScheduleResult>()
                            .likeRight(CxScheduleResult::getCxBatchNo, batchNoPrefix)
                            .orderByDesc(CxScheduleResult::getCxBatchNo));
            if (!batchList.isEmpty()) {
                CxScheduleResult lastBatchRecord = batchList.get(0);
                if (lastBatchRecord.getCxBatchNo() != null && lastBatchRecord.getCxBatchNo().startsWith(batchNoPrefix)) {
                    try {
                        batchSeq = Integer.parseInt(lastBatchRecord.getCxBatchNo().substring(batchNoPrefix.length())) + 1;
                    } catch (NumberFormatException e) {
                        log.warn("解析批次号序号失败，使用默认序号1：{}", lastBatchRecord.getCxBatchNo());
                    }
                }
            }
            newRecord.setCxBatchNo(batchNoPrefix + String.format("%03d", batchSeq));
        }

        newRecord.setBomDataVersion(vo.getExampleNo());
        newRecord.setStructureName(vo.getStructureName());
        newRecord.setTotalStock(vo.getTotalStock());
        newRecord.setLhMachineCode(vo.getLhMachineCode());
        newRecord.setLhMachineQty(vo.getLhMachineQty());
        newRecord.setSpecDimension(vo.getSpecDimension());
        newRecord.setCxRemainQty(vo.getCxRemainQty());
        newRecord.setLhRemainQty(vo.getLhRemainQty());
        newRecord.setLhClassQty(vo.getLhClassQty());

        // 计划量
        newRecord.setClass1PlanQty(vo.getClass1PlanQty());
        newRecord.setClass2PlanQty(vo.getClass2PlanQty());
        newRecord.setClass3PlanQty(vo.getClass3PlanQty());
        newRecord.setClass4PlanQty(vo.getClass4PlanQty());
        newRecord.setClass5PlanQty(vo.getClass5PlanQty());
        newRecord.setClass6PlanQty(vo.getClass6PlanQty());
        newRecord.setClass7PlanQty(vo.getClass7PlanQty());
        newRecord.setClass8PlanQty(vo.getClass8PlanQty());

        // 完成量
        newRecord.setClass1FinishQty(vo.getClass1FinishQty());
        newRecord.setClass2FinishQty(vo.getClass2FinishQty());
        newRecord.setClass3FinishQty(vo.getClass3FinishQty());
        newRecord.setClass4FinishQty(vo.getClass4FinishQty());
        newRecord.setClass5FinishQty(vo.getClass5FinishQty());
        newRecord.setClass6FinishQty(vo.getClass6FinishQty());
        newRecord.setClass7FinishQty(vo.getClass7FinishQty());
        newRecord.setClass8FinishQty(vo.getClass8FinishQty());

        // 原因分析
        newRecord.setClass1Analysis(vo.getClass1Analysis());
        newRecord.setClass2Analysis(vo.getClass2Analysis());
        newRecord.setClass3Analysis(vo.getClass3Analysis());
        newRecord.setClass4Analysis(vo.getClass4Analysis());
        newRecord.setClass5Analysis(vo.getClass5Analysis());
        newRecord.setClass6Analysis(vo.getClass6Analysis());
        newRecord.setClass7Analysis(vo.getClass7Analysis());
        newRecord.setClass8Analysis(vo.getClass8Analysis());

        // 原因分析手工输入
        newRecord.setClass1AnalysisInput(vo.getClass1AnalysisInput());
        newRecord.setClass2AnalysisInput(vo.getClass2AnalysisInput());
        newRecord.setClass3AnalysisInput(vo.getClass3AnalysisInput());
        newRecord.setClass4AnalysisInput(vo.getClass4AnalysisInput());
        newRecord.setClass5AnalysisInput(vo.getClass5AnalysisInput());
        newRecord.setClass6AnalysisInput(vo.getClass6AnalysisInput());
        newRecord.setClass7AnalysisInput(vo.getClass7AnalysisInput());
        newRecord.setClass8AnalysisInput(vo.getClass8AnalysisInput());

        // 示方书类型
        newRecord.setClass1RecipeType(vo.getClass1RecipeType());
        newRecord.setClass2RecipeType(vo.getClass2RecipeType());
        newRecord.setClass3RecipeType(vo.getClass3RecipeType());
        newRecord.setClass4RecipeType(vo.getClass4RecipeType());
        newRecord.setClass5RecipeType(vo.getClass5RecipeType());
        newRecord.setClass6RecipeType(vo.getClass6RecipeType());
        newRecord.setClass7RecipeType(vo.getClass7RecipeType());
        newRecord.setClass8RecipeType(vo.getClass8RecipeType());

        // 示方书编号
        newRecord.setClass1RecipeNo(vo.getClass1RecipeNo());
        newRecord.setClass2RecipeNo(vo.getClass2RecipeNo());
        newRecord.setClass3RecipeNo(vo.getClass3RecipeNo());
        newRecord.setClass4RecipeNo(vo.getClass4RecipeNo());
        newRecord.setClass5RecipeNo(vo.getClass5RecipeNo());
        newRecord.setClass6RecipeNo(vo.getClass6RecipeNo());
        newRecord.setClass7RecipeNo(vo.getClass7RecipeNo());
        newRecord.setClass8RecipeNo(vo.getClass8RecipeNo());

        // 数据来源：1-插单
        newRecord.setDataSource("1");
        // 设置为待发布状态
        newRecord.setIsRelease("0");
        newRecord.setProductionStatus("0");

        int rows = cxScheduleResultMapper.insert(newRecord);
        if (rows > 0) {
            log.info("插单成功，记录ID：{}", newRecord.getId());
            return AjaxResult.success("插单成功");
        } else {
            return AjaxResult.error("插单失败");
        }
    }

    /**
     * 【修改】修改备注和原因分析
     * 业务规则：
     * 1. 只能更新备注、各个班次原因分析数据
     * 2. 若已发布：不允许此操作
     */
    @Log(title = "修改", businessType = BusinessType.UPDATE)
    @ApiOperation("修改备注和原因分析")
    @PostMapping("/updateRemark")
    public AjaxResult updateRemark(@RequestBody ScheduleUpdateRemarkVo vo) {
        if (vo.getId() == null) {
            return AjaxResult.error("排程记录ID不能为空");
        }

        CxScheduleResult record = cxScheduleResultMapper.selectById(vo.getId());
        if (record == null) {
            return AjaxResult.error("排程记录不存在");
        }

        // 若已发布，不允许修改
        if ("1".equals(record.getIsRelease())) {
            return AjaxResult.error("已发布的排程记录不允许修改");
        }

        // 更新备注和原因分析
        if (vo.getRemark() != null) record.setRemark(vo.getRemark());
        if (vo.getClass1Analysis() != null) record.setClass1Analysis(vo.getClass1Analysis());
        if (vo.getClass2Analysis() != null) record.setClass2Analysis(vo.getClass2Analysis());
        if (vo.getClass3Analysis() != null) record.setClass3Analysis(vo.getClass3Analysis());
        if (vo.getClass4Analysis() != null) record.setClass4Analysis(vo.getClass4Analysis());
        if (vo.getClass5Analysis() != null) record.setClass5Analysis(vo.getClass5Analysis());
        if (vo.getClass6Analysis() != null) record.setClass6Analysis(vo.getClass6Analysis());
        if (vo.getClass7Analysis() != null) record.setClass7Analysis(vo.getClass7Analysis());
        if (vo.getClass8Analysis() != null) record.setClass8Analysis(vo.getClass8Analysis());

        int rows = cxScheduleResultMapper.updateById(record);
        if (rows > 0) {
            log.info("修改成功，记录ID：{}", vo.getId());
            return AjaxResult.success("数据修改成功");
        } else {
            return AjaxResult.error("修改失败");
        }
    }

    /**
     * 【转机台】转换机台
     * 业务规则：
     * 1. 只能转移当前班次未执行计划以及后续班次计划，不能转移历史班次计划
     * 2. 选择新机台后需校验新机台对应班次是否有足够的产能
     * 3. 若已发布过给MES，则发布状态更新为待发布，需再次发布至MES
     * 4. 更新备注【"原机台：" + 旧机台 + ",转入机台：" + 新机台】
     * 5. 历史班次的计划量保留在原记录上（清空为0不转移到新机台）
     */
    @Log(title = "转机台", businessType = BusinessType.UPDATE)
    @ApiOperation("转机台")
    @PostMapping("/transferMachine")
    public AjaxResult transferMachine(@RequestBody ScheduleTransferMachineVo vo) {
        if (vo.getIds() == null || vo.getIds().isEmpty()) {
            return AjaxResult.error("请选择需要转机台的记录");
        }
        if (vo.getNewMachineCode() == null) {
            return AjaxResult.error("新机台编码不能为空");
        }

        List<CxScheduleResult> records = cxScheduleResultMapper.selectBatchIds(vo.getIds());
        if (records.isEmpty()) {
            return AjaxResult.error("未找到排程记录");
        }

        LocalDateTime now = LocalDateTime.now();

        // 加载班次配置，用于判断班次是否可转移
        Map<String, CxShiftConfig> shiftConfigMap = getShiftConfigClassMap();

        // 校验并收集每个记录的转移数据
        List<CxScheduleResult> transferRecords = new ArrayList<>();
        for (CxScheduleResult record : records) {
            LocalDate scheduleLocalDate = DateUtil.toLocalDateTime(record.getScheduleDate()).toLocalDate();

            if (scheduleLocalDate.isBefore(now.toLocalDate())) {
                return AjaxResult.error("<b>转机台失败</b><br/>胎胚：" + record.getEmbryoCode()
                        + "<br/>物料：" + (record.getMaterialCode() != null ? record.getMaterialCode() : "")
                        + "<br/>排程日期(" + DateUtil.formatDate(record.getScheduleDate()) + ")为历史日期，不可转机台");
            }

            boolean hasTransferableShift = false;
            for (int i = 1; i <= 8; i++) {
                if (isShiftTransferable(i, scheduleLocalDate, now, shiftConfigMap)) {
                    hasTransferableShift = true;
                    break;
                }
            }
            if (!hasTransferableShift) {
                return AjaxResult.error("<b>转机台失败</b><br/>胎胚：" + record.getEmbryoCode()
                        + "<br/>物料：" + (record.getMaterialCode() != null ? record.getMaterialCode() : "")
                        + "<br/>排程日期(" + DateUtil.formatDate(record.getScheduleDate()) + ")没有可转移的班次计划");
            }

            // 校验新机台唯一性（排程日期 + 新机台 + 胎胚 + 物料）
            LambdaQueryWrapper<CxScheduleResult> uniqueCheck = new LambdaQueryWrapper<>();
            uniqueCheck.eq(CxScheduleResult::getScheduleDate, record.getScheduleDate());
            uniqueCheck.eq(CxScheduleResult::getCxMachineCode, vo.getNewMachineCode());
            uniqueCheck.eq(CxScheduleResult::getEmbryoCode, record.getEmbryoCode());
            if (record.getMaterialCode() != null && !record.getMaterialCode().isEmpty()) {
                uniqueCheck.eq(CxScheduleResult::getMaterialCode, record.getMaterialCode());
            } else {
                uniqueCheck.isNull(CxScheduleResult::getMaterialCode);
            }
            Long duplicateCount = cxScheduleResultMapper.selectCount(uniqueCheck);
            if (duplicateCount > 0) {
                return AjaxResult.error("<b>转机台失败</b><br/>"
                        + "新机台(" + vo.getNewMachineCode() + ")<br/>"
                        + "排程日期：" + DateUtil.formatDate(record.getScheduleDate()) + "<br/>"
                        + "胎胚：" + record.getEmbryoCode() + "<br/>"
                        + "物料：" + record.getMaterialCode() + "<br/>"
                        + "<br/>该机台当天已存在相同胎胚、物料的排程记录");
            }

            transferRecords.add(record);
        }

        // 校验新机台是否存在
        MdmMoldingMachine newMachine = moldingMachineMapper.selectOne(
                new QueryWrapper<MdmMoldingMachine>()
                        .eq("CX_MACHINE_CODE", vo.getNewMachineCode())
                        .eq("IS_ACTIVE", 1));
        if (newMachine == null) {
            return AjaxResult.error("新机台(" + vo.getNewMachineCode() + ")不存在或未启用");
        }

        // 产能校验（未确认时返回提示不强制拦截，已确认时跳过直接执行）
        if (!Boolean.TRUE.equals(vo.getConfirmed())) {
            log.info("转机台首次调用 confirmed=false，执行产能校验");
            String capacityWarning = checkNewMachineCapacityTimeBased(transferRecords, vo.getNewMachineCode(), newMachine, shiftConfigMap);
            if (capacityWarning != null) {
                AjaxResult result = AjaxResult.success(capacityWarning);
                result.put("needConfirm", true);
                return result;
            }
        } else {
            log.info("转机台二次调用 confirmed=true，跳过产能校验直接执行");
        }

        // 执行转机台
        for (CxScheduleResult record : transferRecords) {
            LocalDate scheduleLocalDate = DateUtil.toLocalDateTime(record.getScheduleDate()).toLocalDate();
            String oldMachine = record.getCxMachineCode();

            // 清空不可转移班次的计划量（历史班次+T日夜班保留在原机台）
            for (int i = 1; i <= 8; i++) {
                if (!isShiftTransferable(i, scheduleLocalDate, now, shiftConfigMap)) {
                    setClassPlanQty(record, i, BigDecimal.ZERO);
                }
            }

            record.setCxMachineCode(vo.getNewMachineCode());
            record.setCxMachineName(vo.getNewMachineName());

            String remark = record.getRemark() != null ? record.getRemark() : "";
            record.setRemark(remark + "【原机台：" + oldMachine + ",转入机台：" + vo.getNewMachineCode() + "】");

            record.setIsRelease("0");

            cxScheduleResultMapper.updateById(record);
        }

        log.info("转机台成功，记录数：{}", transferRecords.size());
        return AjaxResult.success("转机台成功");
    }

    /**
     * 校验新机台是否有足够产能承接转移的计划量（按单条胎时间计算）
     */
    private String checkNewMachineCapacityTimeBased(List<CxScheduleResult> transferRecords, String newMachineCode,
                                                     MdmMoldingMachine newMachine, Map<String, CxShiftConfig> configMap) {
        // 按排程日期分组转移记录
        Map<Date, List<CxScheduleResult>> recordsByDate = new HashMap<>();
        for (CxScheduleResult record : transferRecords) {
            recordsByDate.computeIfAbsent(record.getScheduleDate(), k -> new ArrayList<>()).add(record);
        }

        // 加载产能数据（用第一条记录的工厂编码）
        String factoryCode = transferRecords.isEmpty() ? null : transferRecords.get(0).getFactoryCode();
        Object[] capData = loadCapacityData(factoryCode);
        @SuppressWarnings("unchecked")
        List<MonthPlanProductLhCapacityVo> capacityList = (List<MonthPlanProductLhCapacityVo>) capData[0];
        DayVulcanizationModeEnum mode = (DayVulcanizationModeEnum) capData[1];
        int fallbackDailyLh = newMachine.getMaxDayCapacity() != null ? newMachine.getMaxDayCapacity() : 1;

        LocalDateTime now = LocalDateTime.now();
        BigDecimal shiftTotalSeconds = BigDecimal.valueOf(28800L);

        for (Map.Entry<Date, List<CxScheduleResult>> entry : recordsByDate.entrySet()) {
            Date scheduleDate = entry.getKey();
            List<CxScheduleResult> sameDateRecords = entry.getValue();

            // 查询新机台当天已有记录
            List<CxScheduleResult> existingRecords = cxScheduleResultMapper.selectList(
                    new LambdaQueryWrapper<CxScheduleResult>()
                            .eq(CxScheduleResult::getScheduleDate, new java.sql.Date(scheduleDate.getTime()))
                            .eq(CxScheduleResult::getCxMachineCode, newMachineCode));
            // 排除正在转移的记录（它们还在旧机台上，不在新机台已有记录中，但为了安全也过滤）
            BigDecimal[] existingTime = calcShiftTimeConsumed(newMachine, scheduleDate, existingRecords,
                    capacityList, mode, fallbackDailyLh);

            // 按班次汇总转入计划量的耗时
            BigDecimal[] transferTime = new BigDecimal[9];
            for (int i = 1; i <= 8; i++) transferTime[i] = BigDecimal.ZERO;
            for (CxScheduleResult record : sameDateRecords) {
                int dailyLh = getDailyLhCapacity(capacityList, mode, record.getMaterialCode(), fallbackDailyLh);
                BigDecimal singleTireTime = calcSingleTireTime(newMachine, record.getStructureName(), dailyLh);
                LocalDate scheduleLocalDate = DateUtil.toLocalDateTime(record.getScheduleDate()).toLocalDate();
                for (int i = 1; i <= 8; i++) {
                    if (isShiftTransferable(i, scheduleLocalDate, now, configMap)) {
                        BigDecimal planQty = getClassPlanQty(record, i);
                        if (planQty != null) {
                            transferTime[i] = transferTime[i].add(planQty.multiply(singleTireTime));
                        }
                    }
                }
            }

            // 逐班检查
            String[] shiftNames = buildShiftNames(DateUtil.toLocalDateTime(scheduleDate).toLocalDate());
            for (int i = 1; i <= 8; i++) {
                BigDecimal totalTime = existingTime[i].add(transferTime[i]);
                if (totalTime.compareTo(shiftTotalSeconds) > 0) {
                    return "新机台(" + newMachineCode + ")排程日期"
                            + DateUtil.formatDate(scheduleDate) + shiftNames[i] + "产能不足，"
                            + "已有耗时(" + formatSeconds(existingTime[i])
                            + ")+转入耗时(" + formatSeconds(transferTime[i])
                            + ")=" + formatSeconds(totalTime)
                            + "，超过每班8h，是否确认转机台？";
                }
            }
        }

        return null;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal getClassPlanQty(CxScheduleResult record, int classIndex) {
        switch (classIndex) {
            case 1: return record.getClass1PlanQty();
            case 2: return record.getClass2PlanQty();
            case 3: return record.getClass3PlanQty();
            case 4: return record.getClass4PlanQty();
            case 5: return record.getClass5PlanQty();
            case 6: return record.getClass6PlanQty();
            case 7: return record.getClass7PlanQty();
            case 8: return record.getClass8PlanQty();
            default: return BigDecimal.ZERO;
        }
    }

    private void setClassPlanQty(CxScheduleResult record, int classIndex, BigDecimal value) {
        switch (classIndex) {
            case 1: record.setClass1PlanQty(value); break;
            case 2: record.setClass2PlanQty(value); break;
            case 3: record.setClass3PlanQty(value); break;
            case 4: record.setClass4PlanQty(value); break;
            case 5: record.setClass5PlanQty(value); break;
            case 6: record.setClass6PlanQty(value); break;
            case 7: record.setClass7PlanQty(value); break;
            case 8: record.setClass8PlanQty(value); break;
        }
    }

    /**
     * 下载导入模板
     */
    @ApiOperation(value = "导入模板下载")
    @PostMapping("/downloadTemplate/{fileName}")
    public byte[] downloadTemplate(@RequestBody CxScheduleResult queryVO, @PathVariable("fileName") String fileName,
                                   HttpServletResponse response) throws IOException {
        queryVO = queryVO == null ? new CxScheduleResult() : queryVO;
        return cxScheduleResultService.exportData(Collections.emptyList(), queryVO.getScheduleDate());
    }

    /**
     * 自定义导入数据（基于模板cxjhtemplate.xls）
     */
    @ApiOperation(value = "自定义导入数据")
    @PostMapping("/importDataByCust/{updateSupport}")
    @Log(title = "成型排程导入", businessType = BusinessType.IMPORT)
    public AjaxResult importDataByCust(@PathVariable("updateSupport") boolean updateSupport,
                                       @RequestBody CxScheduleImportDTO importDTO) throws Exception {
        ImportContext importContext = importDTO.getImportContext();
        if (importContext == null) {
            return AjaxResult.error("导入上下文不能为空");
        }
        CxScheduleResult scheduleResult = importDTO.getScheduleResult();
        if (scheduleResult == null) {
            scheduleResult = new CxScheduleResult();
        }
        byte[] fileBytes = importContext.getFileBytes();
        String sheetName = "成型计划";
        ExcelUtil<CxScheduleResultTemplateImportVO> util = new ExcelUtil<>(CxScheduleResultTemplateImportVO.class);
        // 使用3参数版本: sheetName, InputStream, headRowNum
        List<CxScheduleResultTemplateImportVO> list = util.importExcel(
                sheetName, new java.io.ByteArrayInputStream(fileBytes), 0);
        return cxScheduleResultService.importScheduleTemplate(list, scheduleResult, updateSupport, null);
    }

    /**
     * 计算机台对指定物料的单条胎耗时（秒）
     * 1. 从结构-硫化配比表获取该机型+结构的配比
     * 2. 单条胎耗时(s) = 86400 / (配比 × 日硫化量)
     */
    private BigDecimal calcSingleTireTime(MdmMoldingMachine machine, String structureName,
                                           int dailyLhCapacity) {
        if (machine == null || structureName == null) {
            return BigDecimal.valueOf(86400L); // 无法获取配比时默认24h一条
        }
        int ratio = 1;
        MdmStructureLhRatio lhRatio = structureLhRatioMapper.selectOne(
                new LambdaQueryWrapper<MdmStructureLhRatio>()
                        .eq(MdmStructureLhRatio::getCxMachineTypeCode, machine.getCxMachineTypeCode())
                        .eq(MdmStructureLhRatio::getStructureName, structureName));
        if (lhRatio != null && lhRatio.getLhMachineMaxQty() != null && lhRatio.getLhMachineMaxQty() > 0) {
            ratio = lhRatio.getLhMachineMaxQty();
        }
        if (dailyLhCapacity <= 0) dailyLhCapacity = 1;

        // 单条胎耗时(s) = 86400 / (配比 × 日硫化量)
        BigDecimal timePerTire = BigDecimal.valueOf(86400L)
                .divide(BigDecimal.valueOf((long) ratio * dailyLhCapacity), 2, RoundingMode.HALF_UP);
        log.info("机台{}结构{}单条胎耗时: 配比={}, 日硫化量={}, 耗时={}s",
                machine.getCxMachineCode(), structureName, ratio, dailyLhCapacity, timePerTire);
        return timePerTire;
    }

    /**
     * 加载工厂的日硫化量数据
     * @return [capacityList, mode]
     */
    private Object[] loadCapacityData(String factoryCode) {
        List<MonthPlanProductLhCapacityVo> capacityList = null;
        DayVulcanizationModeEnum mode = DayVulcanizationModeEnum.STANDARD_CAPACITY;
        if (factoryCode != null) {
            CxParamConfig modeConfig = cxParamConfigMapper.selectOne(
                    new LambdaQueryWrapper<CxParamConfig>()
                            .eq(CxParamConfig::getParamCode, "DAY_VULCANIZATION_MODE")
                            .eq(CxParamConfig::getIsActive, 1));
            if (modeConfig != null && modeConfig.getParamValue() != null) {
                mode = DayVulcanizationModeEnum.getByCode(modeConfig.getParamValue());
                if (mode == null) mode = DayVulcanizationModeEnum.STANDARD_CAPACITY;
            }
            capacityList = monthPlanProductLhCapacityMapper.selectByFactoryCode(factoryCode);
        }
        return new Object[]{capacityList, mode};
    }

    /**
     * 从产能列表中获取指定物料的日硫化量（按模式计算）
     */
    private int getDailyLhCapacity(List<MonthPlanProductLhCapacityVo> capacityList,
                                    DayVulcanizationModeEnum mode, String materialCode, int fallbackValue) {
        if (capacityList != null && materialCode != null) {
            for (MonthPlanProductLhCapacityVo capVo : capacityList) {
                if (materialCode.equals(capVo.getMaterialCode())) {
                    capVo.calculateDayVulcanizationQty(mode);
                    int val = capVo.getDayVulcanizationQty();
                    if (val > 0) return val;
                }
            }
        }
        return fallbackValue > 0 ? fallbackValue : 1;
    }

    /**
     * 计算机台+日期下的已有记录在各班次的时间占用（秒）
     * 每个物料按自身的单条胎耗时独立计算
     */
    private BigDecimal[] calcShiftTimeConsumed(MdmMoldingMachine machine, Date scheduleDate,
                                                List<CxScheduleResult> records,
                                                List<MonthPlanProductLhCapacityVo> capacityList,
                                                DayVulcanizationModeEnum mode, int fallbackDailyLh) {
        BigDecimal[] shiftTime = new BigDecimal[9];
        for (int i = 1; i <= 8; i++) shiftTime[i] = BigDecimal.ZERO;
        for (CxScheduleResult record : records) {
            int dailyLh = getDailyLhCapacity(capacityList, mode, record.getMaterialCode(), fallbackDailyLh);
            BigDecimal singleTireTime = calcSingleTireTime(machine, record.getStructureName(), dailyLh);
            // 记录每条已有记录的耗时明细
            log.info("  机台{} 物料{} 结构{} 单条耗时={} 各班计划量: class1={} class2={} class3={} class4={} class5={} class6={} class7={} class8={}",
                    machine.getCxMachineCode(), record.getMaterialCode(), record.getStructureName(),
                    formatSeconds(singleTireTime),
                    record.getClass1PlanQty(), record.getClass2PlanQty(), record.getClass3PlanQty(), record.getClass4PlanQty(),
                    record.getClass5PlanQty(), record.getClass6PlanQty(), record.getClass7PlanQty(), record.getClass8PlanQty());
            if (record.getClass1PlanQty() != null) shiftTime[1] = shiftTime[1].add(record.getClass1PlanQty().multiply(singleTireTime));
            if (record.getClass2PlanQty() != null) shiftTime[2] = shiftTime[2].add(record.getClass2PlanQty().multiply(singleTireTime));
            if (record.getClass3PlanQty() != null) shiftTime[3] = shiftTime[3].add(record.getClass3PlanQty().multiply(singleTireTime));
            if (record.getClass4PlanQty() != null) shiftTime[4] = shiftTime[4].add(record.getClass4PlanQty().multiply(singleTireTime));
            if (record.getClass5PlanQty() != null) shiftTime[5] = shiftTime[5].add(record.getClass5PlanQty().multiply(singleTireTime));
            if (record.getClass6PlanQty() != null) shiftTime[6] = shiftTime[6].add(record.getClass6PlanQty().multiply(singleTireTime));
            if (record.getClass7PlanQty() != null) shiftTime[7] = shiftTime[7].add(record.getClass7PlanQty().multiply(singleTireTime));
            if (record.getClass8PlanQty() != null) shiftTime[8] = shiftTime[8].add(record.getClass8PlanQty().multiply(singleTireTime));
        }
        // 汇总各班次总耗时
        log.info("  机台{} 各班次总耗时: class1={} class2={} class3={} class4={} class5={} class6={} class7={} class8={}",
                machine.getCxMachineCode(),
                formatSeconds(shiftTime[1]), formatSeconds(shiftTime[2]), formatSeconds(shiftTime[3]), formatSeconds(shiftTime[4]),
                formatSeconds(shiftTime[5]), formatSeconds(shiftTime[6]), formatSeconds(shiftTime[7]), formatSeconds(shiftTime[8]));
        return shiftTime;
    }


    /**
     * 校验指定班次的计划量是否超出机台剩余产能
     * @return 产能不足时返回错误信息，充足时返回 null
     */
    private String checkPerShiftCapacity(BigDecimal[] planQtys, BigDecimal[] existingTimeSeconds,
                                          BigDecimal insertSingleTireTime, String[] shiftNames) {
        BigDecimal shiftTotalSeconds = BigDecimal.valueOf(28800L);
        for (int i = 1; i <= 8; i++) {
            if (planQtys[i] == null) continue;
            BigDecimal remainingSeconds = shiftTotalSeconds.subtract(existingTimeSeconds[i]);
            if (remainingSeconds.compareTo(BigDecimal.ZERO) < 0) remainingSeconds = BigDecimal.ZERO;
            BigDecimal maxInsertQty = BigDecimal.ZERO;
            if (insertSingleTireTime.compareTo(BigDecimal.ZERO) > 0) {
                maxInsertQty = remainingSeconds.divide(insertSingleTireTime, 0, RoundingMode.FLOOR);
            }
            if (planQtys[i].compareTo(maxInsertQty) > 0) {
                return shiftNames[i] + "已有耗时(" + formatSeconds(existingTimeSeconds[i])
                        + ")，剩余时间(" + formatSeconds(remainingSeconds)
                        + ")，最多可生产(" + maxInsertQty + ")条，计划量(" + planQtys[i] + ")超出产能";
            }
        }
        return null;
    }

}
