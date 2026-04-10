package com.zlt.aps.cx.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.mapper.CxScheduleResultMapper;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    /**
     * 查询成型排程结果列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody CxScheduleResult queryVO) {
        return super.list(queryVO);
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
    @DeleteMapping("/remove")
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
		// 排程日期区间查询（使用 searchValue 传递开始时间，remark 传递结束时间）
		if (PubUtil.isNotEmpty(queryVO.getSearchValue()) && PubUtil.isNotEmpty(queryVO.getRemark())) {
            Date beginDay = DateUtil.parse(queryVO.getSearchValue());
            Date endDay = DateUtil.parse(queryVO.getRemark());
            endDay = DateUtil.endOfDay(endDay);
			queryWrapper.between("SCHEDULE_DATE", beginDay, endDay);
		}
		// 机台代码模糊查询
		queryWrapper.like(PubUtil.isNotEmpty(queryVO.getCxMachineCode()), "CX_MACHINE_CODE", queryVO.getCxMachineCode());
		// 胎胚代码模糊查询
		queryWrapper.like(PubUtil.isNotEmpty(queryVO.getEmbryoCode()), "EMBRYO_CODE", queryVO.getEmbryoCode());
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

        // 判断是否已发布
        if ("1".equals(record.getIsRelease())) {
            return AjaxResult.error("已发布的排程记录不允许调量");
        }

        // 判断当前时间，计算可调整的班次
        LocalDateTime now = LocalDateTime.now();
        Date scheduleDate = record.getScheduleDate();
        
        // 如果是当天，需要根据当前时间判断可调整的班次
        LocalDate scheduleLocalDate = DateUtil.toLocalDateTime(scheduleDate).toLocalDate();
        if (scheduleLocalDate.equals(LocalDate.now())) {
            int currentHour = now.getHour();
            // 8点前：可调整所有班次
            // 8点后：早班已生产，不可调整；中班可调整
            // 16点后：早中班已生产，不可调整
            if (currentHour >= 16 && vo.getClass2PlanQty() != null) {
                // 中班计划量校验：不能低于已完成量
                if (record.getClass2FinishQty() != null && vo.getClass2PlanQty().compareTo(record.getClass2FinishQty()) < 0) {
                    return AjaxResult.error("中班计划量不能低于已完成量：" + record.getClass2FinishQty());
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

        // 校验唯一性
        QueryWrapper<CxScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("SCHEDULE_DATE", DateUtil.parse(vo.getScheduleDate()));
        queryWrapper.eq("CX_MACHINE_CODE", vo.getCxMachineCode());
        queryWrapper.eq("EMBRYO_CODE", vo.getEmbryoCode());
        queryWrapper.eq("ORDER_NO", vo.getMaterialCode());
        Long count = cxScheduleResultMapper.selectCount(queryWrapper);
        if (count > 0) {
            return AjaxResult.error("插单失败：该日已存在相同机台、胎胚、物料的排程记录");
        }

        // 创建新记录
        CxScheduleResult newRecord = new CxScheduleResult();
        newRecord.setScheduleDate(DateUtil.parse(vo.getScheduleDate()));
        newRecord.setCxMachineCode(vo.getCxMachineCode());
        newRecord.setCxMachineName(vo.getCxMachineName());
        newRecord.setEmbryoCode(vo.getEmbryoCode());
        newRecord.setOrderNo(vo.getMaterialCode());
        newRecord.setMaterialCode(vo.getSpecDesc());
        newRecord.setClass1PlanQty(vo.getClass1PlanQty());
        newRecord.setClass2PlanQty(vo.getClass2PlanQty());
        newRecord.setClass3PlanQty(vo.getClass3PlanQty());
        newRecord.setClass1Analysis(vo.getClass1Analysis());
        newRecord.setClass2Analysis(vo.getClass2Analysis());
        newRecord.setClass3Analysis(vo.getClass3Analysis());
        
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
     * 1. 默认带出所选记录的排程日期、原机台
     * 2. 选择新机台后自动下拉显示可选机台清单
     * 3. 检查唯一性：排程日期 + 成型机台 + 胎胚描述
     * 4. 若已发布：不允许转机台
     * 5. 更新备注【"原机台：" + 旧机台 + ",转入机台：" + 新机台】
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

        // 检查所有记录是否已发布
        List<CxScheduleResult> records = cxScheduleResultMapper.selectBatchIds(vo.getIds());
        for (CxScheduleResult record : records) {
            if ("1".equals(record.getIsRelease())) {
                return AjaxResult.error("已发布的排程记录不允许转机台");
            }
        }

        // 更新机台信息
        for (CxScheduleResult record : records) {
            String oldMachine = record.getCxMachineCode();
            record.setCxMachineCode(vo.getNewMachineCode());
            record.setCxMachineName(vo.getNewMachineName());
            
            // 更新备注
            String remark = record.getRemark() != null ? record.getRemark() : "";
            record.setRemark(remark + "【原机台：" + oldMachine + ",转入机台：" + vo.getNewMachineCode() + "】");
            
            // 设置为待发布
            record.setIsRelease("0");
            
            cxScheduleResultMapper.updateById(record);
        }

        log.info("转机台成功，记录数：{}", records.size());
        return AjaxResult.success("转机台成功");
    }

}
