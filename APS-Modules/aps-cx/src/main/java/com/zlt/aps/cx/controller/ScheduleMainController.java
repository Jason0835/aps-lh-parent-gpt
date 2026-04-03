package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.service.CxScheduleResultService;
import com.zlt.aps.cx.service.ScheduleService;
import com.zlt.aps.cx.vo.ScheduleGenerateVo;
import com.zlt.aps.cx.vo.ScheduleRequestVo;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.mp.api.domain.entity.CxScheduleResultIssue;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 排程管理Controller
 *
 * @author APS Team
 */
@Api(tags = "排程管理")
@RestController
@RequestMapping("/schedule")
public class ScheduleMainController {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private CxScheduleResultService cxScheduleResultService;

    @Autowired
    private IMesItfService mesItfService;

    @ApiOperation(value = "生成排程", notes = "根据日期和天数生成排程")
    @PostMapping("/generate")
    public AjaxResult generateSchedule(@RequestBody ScheduleGenerateVo dto) {
        if (dto.getScheduleDate() == null) {
            return AjaxResult.error("排程日期不能为空");
        }
        if (dto.getDays() == null || dto.getDays() < 1) {
            dto.setDays(1);
        }

        List<Object> allResults = new ArrayList<>();
        LocalDate currentDate = dto.getScheduleDate();

        for (int i = 0; i < dto.getDays(); i++) {
            ScheduleRequestVo request = new ScheduleRequestVo();
            request.setScheduleDate(currentDate);
            request.setOverwrite(dto.getOverwrite() != null ? dto.getOverwrite() : false);
            request.setFactoryCode(dto.getFactoryCode());
            request.setScheduleType(dto.getScheduleType());
            request.setScheduleMode(dto.getScheduleType());

            ScheduleService.ScheduleResult result = scheduleService.executeSchedule(request);

            if (result.isSuccess()) {
                allResults.add(result);
            } else {
                return AjaxResult.error("排程失败[" + currentDate + "]: " + result.getMessage());
            }

            currentDate = currentDate.plusDays(1);
        }

        return AjaxResult.success(allResults);
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
        target.setMaterialCode(source.getSapCode());
        target.setMesMaterialCode(null); // MES物料编码需要另外查询
        target.setSpecDesc(source.getSpecDesc());
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
        target.setMaterialCode(source.getSapCode());
        target.setMesMaterialCode(null);
        target.setSpecDesc(source.getSpecDesc());
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
        target.setMaterialCode(source.getSapCode());
        target.setMesMaterialCode(null);
        target.setSpecDesc(source.getSpecDesc());
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
}
