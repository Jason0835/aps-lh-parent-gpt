package com.zlt.aps.cx.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.mapper.CxScheduleResultMapper;
import com.zlt.aps.cx.mapper.CxShiftConfigMapper;
import com.zlt.aps.cx.mapper.MdmMoldingMachineMapper;
import com.zlt.aps.cx.service.CxScheduleResultService;
import com.zlt.aps.cx.service.ScheduleService;
import com.zlt.aps.cx.api.domain.vo.ScheduleAdjustVo;
import com.zlt.aps.cx.api.domain.vo.ScheduleGenerateVo;
import com.zlt.aps.cx.api.domain.vo.ScheduleInsertVo;
import com.zlt.aps.cx.api.domain.vo.ScheduleTransferMachineVo;
import com.zlt.aps.cx.api.domain.vo.ScheduleUpdateRemarkVo;
import com.zlt.aps.cx.vo.ScheduleRequestVo;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.mp.api.domain.entity.CxScheduleResultIssue;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

                Date start = DateUtil.toDate(startDate.atTime(startLocalTime));
                Date end = DateUtil.toDate(endDate.atTime(endLocalTime));

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
        }

        return tableDataInfo;
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

        // 校验每个班次的计划量
        AjaxResult validationResult = validateAdjustQtyShifts(vo, record, scheduleLocalDate, now);
        if (validationResult != null) {
            return validationResult;
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
                                                LocalDate scheduleLocalDate, LocalDateTime now) {
        String[] shiftNames = {"", "早班(D1)", "中班(D1)", "夜班(D2)", "早班(D2)", "中班(D2)", "夜班(D3)", "早班(D3)", "中班(D3)"};

        BigDecimal[] planQtys = {null, vo.getClass1PlanQty(), vo.getClass2PlanQty(), vo.getClass3PlanQty(),
                vo.getClass4PlanQty(), vo.getClass5PlanQty(), vo.getClass6PlanQty(),
                vo.getClass7PlanQty(), vo.getClass8PlanQty()};
        BigDecimal[] finishQtys = {null, record.getClass1FinishQty(), record.getClass2FinishQty(),
                record.getClass3FinishQty(), record.getClass4FinishQty(), record.getClass5FinishQty(),
                record.getClass6FinishQty(), record.getClass7FinishQty(), record.getClass8FinishQty()};

        for (int i = 1; i <= 8; i++) {
            if (planQtys[i] == null) {
                continue;
            }

            if (isShiftPast(i, scheduleLocalDate, now)) {
                return AjaxResult.error(shiftNames[i] + "计划量不可调整：该班次已过");
            }

            if (finishQtys[i] != null && planQtys[i].compareTo(finishQtys[i]) < 0) {
                return AjaxResult.error(shiftNames[i] + "计划量不能低于已完成量：" + finishQtys[i]);
            }
        }

        return null;
    }

    /**
     * 判断指定班次是否为历史班次（已结束）
     * 注：record.getScheduleDate() 存储的是 T+2日（8班的最后一天），需要反推T日和T+1日
     *   CLASS1: D1早班 T日=scheduleDate-2, 06:00-13:59 -> 结束于 scheduleDate-2 14:00
     *   CLASS2: D1中班 T日=scheduleDate-2, 14:00-21:59 -> 结束于 scheduleDate-2 22:00
     *   CLASS3: D2夜班 T+1日=scheduleDate-1(跨天), 22:00-05:59 -> 结束于 scheduleDate-1 06:00
     *   CLASS4: D2早班 T+1日=scheduleDate-1, 06:00-13:59 -> 结束于 scheduleDate-1 14:00
     *   CLASS5: D2中班 T+1日=scheduleDate-1, 14:00-21:59 -> 结束于 scheduleDate-1 22:00
     *   CLASS6: D3夜班 T+2日=scheduleDate(跨天), 22:00-05:59 -> 结束于 scheduleDate+1 06:00
     *   CLASS7: D3早班 T+2日=scheduleDate, 06:00-13:59 -> 结束于 scheduleDate 14:00
     *   CLASS8: D3中班 T+2日=scheduleDate, 14:00-21:59 -> 结束于 scheduleDate 22:00
     */
    private boolean isShiftPast(int classIndex, LocalDate scheduleDate, LocalDateTime now) {
        LocalDate endDate;
        int endHour;

        switch (classIndex) {
            case 1: endDate = scheduleDate.minusDays(2); endHour = 14; break;
            case 2: endDate = scheduleDate.minusDays(2); endHour = 22; break;
            case 3: endDate = scheduleDate.minusDays(1); endHour = 6; break;
            case 4: endDate = scheduleDate.minusDays(1); endHour = 14; break;
            case 5: endDate = scheduleDate.minusDays(1); endHour = 22; break;
            case 6: endDate = scheduleDate; endHour = 6; break;
            case 7: endDate = scheduleDate; endHour = 14; break;
            case 8: endDate = scheduleDate; endHour = 22; break;
            default: return false;
        }

        LocalDateTime shiftEnd = endDate.atTime(endHour, 0);
        return !now.isBefore(shiftEnd);
    }

    /**
     * 转机台专用：判断班次是否可转移到新机台
     * 1. 已结束的班次不可转移（基于 isShiftPast 时间判断）
     * 2. T日夜班(CLASS3)即使未到结束时间也不转移（业务规则）
     */
    private boolean isShiftTransferable(int classIndex, LocalDate scheduleDate, LocalDateTime now) {
        if (isShiftPast(classIndex, scheduleDate, now)) {
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

        // 校验唯一性：排程日期 + 机台编号 + 胎胚编号 + 物料编码 + 示方书版本
        QueryWrapper<CxScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("SCHEDULE_DATE", scheduleDate);
        queryWrapper.eq("CX_MACHINE_CODE", vo.getCxMachineCode());
        queryWrapper.eq("EMBRYO_CODE", vo.getEmbryoCode());
        queryWrapper.eq("MATERIAL_CODE", vo.getMaterialCode());
        if (vo.getExampleNo() != null) {
            queryWrapper.eq("BOM_DATA_VERSION", vo.getExampleNo());
        }
        Long count = cxScheduleResultMapper.selectCount(queryWrapper);
        if (count > 0) {
            return AjaxResult.error("插单失败：该日已存在相同机台、胎胚、物料、示方书版本的排程记录");
        }

        // 校验各班计划量不能超过机台最大日产能（按天分组，排程记录scheduleDate=T+2日）
        //   CLASS1(D1早班=T日) + CLASS2(D1中班=T日)  -> 一天
        //   CLASS3(D2夜班=T+1日)                      -> 一天
        int maxDayCapacity = 0;
        MdmMoldingMachine machine = moldingMachineMapper.selectOne(
                new QueryWrapper<MdmMoldingMachine>()
                        .eq("CX_MACHINE_CODE", vo.getCxMachineCode())
                        .eq("IS_ACTIVE", 1));
        if (machine != null && machine.getMaxDayCapacity() != null) {
            maxDayCapacity = machine.getMaxDayCapacity();
        }
        if (maxDayCapacity > 0) {
            BigDecimal day1Total = BigDecimal.ZERO;
            BigDecimal day2Total = BigDecimal.ZERO;
            if (vo.getClass1PlanQty() != null) day1Total = day1Total.add(vo.getClass1PlanQty());
            if (vo.getClass2PlanQty() != null) day1Total = day1Total.add(vo.getClass2PlanQty());
            if (vo.getClass3PlanQty() != null) day2Total = day2Total.add(vo.getClass3PlanQty());

            if (day1Total.compareTo(BigDecimal.valueOf(maxDayCapacity)) > 0) {
                return AjaxResult.error("插单失败：T日(早班+中班)计划量(" + day1Total + ")超过机台最大日产(" + maxDayCapacity + ")");
            }
            if (day2Total.compareTo(BigDecimal.valueOf(maxDayCapacity)) > 0) {
                return AjaxResult.error("插单失败：T+1日(夜班)计划量(" + day2Total + ")超过机台最大日产(" + maxDayCapacity + ")");
            }
        }

        // 创建新记录
        CxScheduleResult newRecord = new CxScheduleResult();
        newRecord.setScheduleDate(scheduleDate);
        newRecord.setCxMachineCode(vo.getCxMachineCode());
        newRecord.setCxMachineName(vo.getCxMachineName());
        newRecord.setEmbryoCode(vo.getEmbryoCode());
        newRecord.setMaterialCode(vo.getMaterialCode());
        newRecord.setMaterialDesc(vo.getSpecDesc());
        newRecord.setOrderNo(vo.getMaterialCode());
        newRecord.setBomDataVersion(vo.getExampleNo());
        newRecord.setClass1PlanQty(vo.getClass1PlanQty());
        newRecord.setClass2PlanQty(vo.getClass2PlanQty());
        newRecord.setClass3PlanQty(vo.getClass3PlanQty());
        newRecord.setClass1Analysis(vo.getClass1Analysis());
        newRecord.setClass2Analysis(vo.getClass2Analysis());
        newRecord.setClass3Analysis(vo.getClass3Analysis());

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

        // 校验并收集每个记录的转移数据
        List<CxScheduleResult> transferRecords = new ArrayList<>();
        for (CxScheduleResult record : records) {
            LocalDate scheduleLocalDate = DateUtil.toLocalDateTime(record.getScheduleDate()).toLocalDate();

            if (scheduleLocalDate.isBefore(now.toLocalDate())) {
                return AjaxResult.error("记录ID=" + record.getId() + "的排程日期为历史日期，不可转机台");
            }

            boolean hasTransferableShift = false;
            for (int i = 1; i <= 8; i++) {
                if (isShiftTransferable(i, scheduleLocalDate, now)) {
                    BigDecimal planQty = getClassPlanQty(record, i);
                    if (planQty != null && planQty.compareTo(BigDecimal.ZERO) > 0) {
                        hasTransferableShift = true;
                        break;
                    }
                }
            }
            if (!hasTransferableShift) {
                return AjaxResult.error("记录ID=" + record.getId() + "没有可转移的班次计划（所有班次均已过或计划量为0）");
            }

            // 校验新机台唯一性（排程日期 + 新机台 + 胎胚 + 物料）
            QueryWrapper<CxScheduleResult> uniqueCheck = new QueryWrapper<>();
            uniqueCheck.eq("SCHEDULE_DATE", record.getScheduleDate());
            uniqueCheck.eq("CX_MACHINE_CODE", vo.getNewMachineCode());
            uniqueCheck.eq("EMBRYO_CODE", record.getEmbryoCode());
            if (record.getMaterialCode() != null) {
                uniqueCheck.eq("MATERIAL_CODE", record.getMaterialCode());
            }
            Long duplicateCount = cxScheduleResultMapper.selectCount(uniqueCheck);
            if (duplicateCount > 0) {
                return AjaxResult.error("新机台(" + vo.getNewMachineCode() + ")在排程日期"
                        + DateUtil.formatDate(record.getScheduleDate()) + "已存在相同胎胚、物料的排程记录");
            }

            transferRecords.add(record);
        }

        // 产能校验（未确认时检查，已确认时跳过检查直接执行）
        if (!Boolean.TRUE.equals(vo.getConfirmed())) {
            AjaxResult capacityCheck = checkNewMachineCapacity(transferRecords, vo.getNewMachineCode());
            if (capacityCheck != null) {
                return capacityCheck;
            }
        } else {
            log.info("用户已确认转机台，跳过产能校验直接执行");
        }

        // 执行转机台
        for (CxScheduleResult record : transferRecords) {
            LocalDate scheduleLocalDate = DateUtil.toLocalDateTime(record.getScheduleDate()).toLocalDate();
            String oldMachine = record.getCxMachineCode();

            // 清空不可转移班次的计划量（历史班次+T日夜班保留在原机台）
            for (int i = 1; i <= 8; i++) {
                if (!isShiftTransferable(i, scheduleLocalDate, now)) {
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
     * 校验新机台是否有足够产能承接转移的计划量
     * 按每台设备的日产能进行比较
     */
    private AjaxResult checkNewMachineCapacity(List<CxScheduleResult> records, String newMachineCode) {
        MdmMoldingMachine newMachine = moldingMachineMapper.selectOne(
                new QueryWrapper<MdmMoldingMachine>()
                        .eq("CX_MACHINE_CODE", newMachineCode)
                        .eq("IS_ACTIVE", 1));
        if (newMachine == null) {
            return AjaxResult.error("新机台(" + newMachineCode + ")不存在或未启用");
        }

        Integer maxDayCapacity = newMachine.getMaxDayCapacity();
        if (maxDayCapacity == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();

        BigDecimal existingTotal = BigDecimal.ZERO;
        for (CxScheduleResult record : records) {
            QueryWrapper<CxScheduleResult> existingQuery = new QueryWrapper<>();
            existingQuery.eq("SCHEDULE_DATE", record.getScheduleDate());
            existingQuery.eq("CX_MACHINE_CODE", newMachineCode);
            List<CxScheduleResult> existingRecords = cxScheduleResultMapper.selectList(existingQuery);
            for (CxScheduleResult existing : existingRecords) {
                existingTotal = existingTotal
                        .add(defaultZero(existing.getClass1PlanQty()))
                        .add(defaultZero(existing.getClass2PlanQty()))
                        .add(defaultZero(existing.getClass3PlanQty()))
                        .add(defaultZero(existing.getClass4PlanQty()))
                        .add(defaultZero(existing.getClass5PlanQty()))
                        .add(defaultZero(existing.getClass6PlanQty()))
                        .add(defaultZero(existing.getClass7PlanQty()))
                        .add(defaultZero(existing.getClass8PlanQty()));
            }
        }

        BigDecimal transferTotal = BigDecimal.ZERO;
        for (CxScheduleResult record : records) {
            LocalDate scheduleLocalDate = DateUtil.toLocalDateTime(record.getScheduleDate()).toLocalDate();
            for (int i = 1; i <= 8; i++) {
                if (isShiftTransferable(i, scheduleLocalDate, now)) {
                    transferTotal = transferTotal.add(defaultZero(getClassPlanQty(record, i)));
                }
            }
        }

        BigDecimal totalAfterTransfer = existingTotal.add(transferTotal);
        if (totalAfterTransfer.compareTo(BigDecimal.valueOf(maxDayCapacity)) > 0) {
            return AjaxResult.error("新机台(" + newMachineCode + ")产能不足，"
                    + "现有计划量(" + existingTotal + ")+转入计划量(" + transferTotal + ")="
                    + totalAfterTransfer + "，超过机台最大日产(" + maxDayCapacity + ")，是否确认转机台？");
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

}
