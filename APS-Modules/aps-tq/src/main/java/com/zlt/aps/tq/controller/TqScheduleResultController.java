package com.zlt.aps.tq.controller;

import cn.hutool.core.date.DateUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tq.api.domain.dto.TqChangeMachineDTO;
import com.zlt.aps.tq.api.domain.dto.TqInsertOrderDTO;
import com.zlt.aps.tq.api.domain.entity.TqMachineChuck;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.api.domain.entity.TqMouthPlate;
import com.zlt.aps.tq.api.domain.entity.TqScheduleResult;
import com.zlt.aps.tq.api.domain.entity.TqSpecifyMachine;
import com.zlt.aps.tq.api.domain.vo.TqScheduleShiftDateVO;
import com.zlt.aps.tq.engine.service.TqEngineService;
import com.zlt.aps.tq.mapper.TqMachineChuckMapper;
import com.zlt.aps.tq.mapper.TqMachineInfoMapper;
import com.zlt.aps.tq.mapper.TqMouthPlateMapper;
import com.zlt.aps.tq.mapper.TqScheduleResultMapper;
import com.zlt.aps.tq.mapper.TqSpecifyMachineMapper;
import com.zlt.aps.tq.service.ITqScheduleResultService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 胎圈排程结果Controller
 *
 * @author APS
 */
@Slf4j
@Api(tags = "胎圈排程结果")
@RestController
@RequestMapping("/scheduleResult")
public class TqScheduleResultController extends AbstractDocBizController<TqScheduleResult> {

    @Autowired
    private ITqScheduleResultService tqScheduleResultService;

    @Resource
    private TqScheduleResultMapper tqScheduleResultMapper;

    @Autowired
    private TqEngineService tqEngineService;

    /** 机台信息Mapper（用于查询启用的机台列表） */
    @Resource
    private TqMachineInfoMapper tqMachineInfoMapper;

    /** 机台寸口Mapper（用于候选机台寸口过滤） */
    @Resource
    private TqMachineChuckMapper tqMachineChuckMapper;

    /** 口型板Mapper（用于候选机台口型板过滤） */
    @Resource
    private TqMouthPlateMapper tqMouthPlateMapper;

    /** 定点机台Mapper（用于候选机台定点/禁排过滤） */
    @Resource
    private TqSpecifyMachineMapper tqSpecifyMachineMapper;

    @ApiOperation("查询胎圈排程结果列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TqScheduleResult queryVO) {
        startPage();
        LambdaQueryWrapper<TqScheduleResult> wrapper = buildQueryWrapper(queryVO);
        List<TqScheduleResult> list = tqScheduleResultMapper.selectList(wrapper);
        return getDataTable(list);
    }

    @Log(title = "胎圈排程结果", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TqScheduleResult billVO) {
        return super.save(billVO);
    }

    @Log(title = "胎圈排程结果", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public TqScheduleResult getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "胎圈排程结果", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "胎圈排程结果", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TqScheduleResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected IDocService getDocService() {
        return tqScheduleResultService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        // 默认排序：排程日期倒序、机台号正序、一班次顺序正序（用于导出等场景）
        return "SCHEDULE_DATE desc, MACHINE_CODE asc, CLASS1_SEQUENCE asc";
    }

    /**
     * 构建查询条件
     * 注意：isDelete 已由框架通过注解自动过滤，禁止手动追加条件
     */
    private LambdaQueryWrapper<TqScheduleResult> buildQueryWrapper(TqScheduleResult queryVO) {
        LambdaQueryWrapper<TqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(queryVO.getScheduleDateQuery() != null, TqScheduleResult::getScheduleDate, queryVO.getScheduleDateQuery());
        wrapper.like(PubUtil.isNotEmpty(queryVO.getBeadCode()), TqScheduleResult::getBeadCode, queryVO.getBeadCode());
        wrapper.like(PubUtil.isNotEmpty(queryVO.getProSize()), TqScheduleResult::getProSize, queryVO.getProSize());
        wrapper.like(PubUtil.isNotEmpty(queryVO.getTriangleGlueCode()), TqScheduleResult::getTriangleGlueCode, queryVO.getTriangleGlueCode());
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getIsRelease()), TqScheduleResult::getIsRelease, queryVO.getIsRelease());
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), TqScheduleResult::getMachineCode, queryVO.getMachineCode());
        // 默认排序：排程日期倒序、机台号正序、一班次顺序正序
        wrapper.orderByDesc(TqScheduleResult::getScheduleDate);
        wrapper.orderByAsc(TqScheduleResult::getMachineCode);
        wrapper.orderByAsc(TqScheduleResult::getClass1Sequence);
        return wrapper;
    }

    /**
     * 自动排程
     */
    @Log(title = "胎圈排程结果", businessType = BusinessType.AUTOPLAN)
    @ApiOperation("自动排程")
    @PostMapping("/autoPlan")
    public AjaxResult autoPlan(@RequestBody TqScheduleResult queryVO) {
        Date scheduleDate = queryVO.getScheduleDateQuery();
        String factoryCode = queryVO.getFactoryCode();
        if (scheduleDate == null) {
            return AjaxResult.error("排程日期不能为空");
        }
        if (StringUtils.isEmpty(factoryCode)) {
            return AjaxResult.error("分厂不能为空");
        }
        tqEngineService.autoTqSchedule(DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate), factoryCode);
        return AjaxResult.success();
    }

    /**
     * 插单前校验
     */
    @ApiOperation("插单前校验")
    @PostMapping("/validateInsertOrder")
    public AjaxResult validateInsertOrder(@RequestBody TqInsertOrderDTO dto) {
        return tqScheduleResultService.validateInsertOrder(dto);
    }

    /**
     * 插单
     */
    @Log(title = "胎圈排程结果", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("插单")
    @PostMapping("/insertOrder")
    public AjaxResult insertOrder(@RequestBody TqInsertOrderDTO dto) {
        return tqScheduleResultService.insertOrder(dto);
    }

    /**
     * 转机台前校验
     */
    @ApiOperation("转机台前校验")
    @PostMapping("/validateChangeMachine")
    public AjaxResult validateChangeMachine(@RequestBody TqChangeMachineDTO dto) {
        return tqScheduleResultService.validateChangeMachine(dto);
    }

    /**
     * 根据排程记录获取转机台候选机台列表（按寸口、口型板、定点约束过滤）
     * 过滤规则与自动排程策略链口径一致：
     * 1. 仅返回启用状态的机台
     * 2. 寸口过滤：机台寸口列表必须包含胎圈的英寸尺寸(proSize)
     * 3. 口型板过滤：机台必须在胎圈三角胶口型板的可用机台列表中
     * 4. 定点机台过滤：限制作业→仅返回限制列表中的机台；不可作业→排除不可作业机台
     * 注意：维修过滤不在候选列表中做，而是在提交转机台时校验
     *
     * @param id 排程记录ID
     * @return 过滤后的候选机台列表
     */
    @ApiOperation("获取转机台候选机台列表")
    @PostMapping("/listCandidateMachines/{id}")
    public AjaxResult listCandidateMachines(@PathVariable("id") Long id) {
        // 查询排程记录
        TqScheduleResult record = tqScheduleResultMapper.selectById(id);
        if (record == null || "1".equals(record.getIsDelete())) {
            return AjaxResult.error("排程记录不存在或已删除");
        }

        String beadCode = record.getBeadCode();
        String proSize = record.getProSize();
        String triangleGlueCode = record.getTriangleGlueCode();

        // 1. 查询所有启用的机台
        LambdaQueryWrapper<TqMachineInfo> machineWrapper = new LambdaQueryWrapper<>();
        machineWrapper.eq(TqMachineInfo::getIsDelete, 0);
        machineWrapper.eq(TqMachineInfo::getStatus, "1");
        machineWrapper.orderByAsc(TqMachineInfo::getMachineCode);
        List<TqMachineInfo> allMachines = tqMachineInfoMapper.selectList(machineWrapper);

        // 2. 寸口过滤：机台寸口列表必须包含胎圈的英寸尺寸
        if (proSize != null && !proSize.isEmpty()) {
            BigDecimal dimension;
            try {
                dimension = new BigDecimal(proSize);
            } catch (NumberFormatException e) {
                log.warn("[候选机台] 胎圈{}的英寸尺寸{}无法转换为数字，跳过寸口过滤", beadCode, proSize);
                dimension = null;
            }
            if (dimension != null) {
                // 查询所有机台的寸口映射
                LambdaQueryWrapper<TqMachineChuck> chuckWrapper = new LambdaQueryWrapper<>();
                chuckWrapper.eq(TqMachineChuck::getIsDelete, 0);
                List<TqMachineChuck> allChuckList = tqMachineChuckMapper.selectList(chuckWrapper);
                // 构建机台→寸口列表映射
                java.util.Map<String, List<BigDecimal>> machineChuckMap = allChuckList.stream()
                        .filter(c -> c.getMachineCode() != null && c.getInchSize() != null)
                        .collect(Collectors.groupingBy(
                                TqMachineChuck::getMachineCode,
                                Collectors.mapping(TqMachineChuck::getInchSize, Collectors.toList())));
                BigDecimal finalDimension = dimension;
                allMachines = allMachines.stream().filter(m -> {
                    List<BigDecimal> chuckSizes = machineChuckMap.get(m.getMachineCode());
                    // 未配置寸口的机台默认保留（兼容未配置的情况）
                    if (chuckSizes == null || chuckSizes.isEmpty()) {
                        return true;
                    }
                    return chuckSizes.stream().anyMatch(c -> c.compareTo(finalDimension) == 0);
                }).collect(Collectors.toList());
            }
        }

        // 3. 口型板过滤：机台必须在口型板的可用机台列表中
        if (triangleGlueCode != null && !triangleGlueCode.isEmpty()) {
            LambdaQueryWrapper<TqMouthPlate> mpWrapper = new LambdaQueryWrapper<>();
            mpWrapper.eq(TqMouthPlate::getMouthPlateCode, triangleGlueCode);
            mpWrapper.eq(TqMouthPlate::getIsDelete, 0);
            List<TqMouthPlate> mouthPlateList = tqMouthPlateMapper.selectList(mpWrapper);
            if (!mouthPlateList.isEmpty()) {
                // 口型板绑定了机台，需要过滤
                List<String> mpMachineCodes = mouthPlateList.stream()
                        .map(TqMouthPlate::getMachineCode)
                        .collect(Collectors.toList());
                List<TqMachineInfo> filtered = allMachines.stream()
                        .filter(m -> mpMachineCodes.contains(m.getMachineCode()))
                        .collect(Collectors.toList());
                // 口型板过滤有结果时才使用过滤后的列表，否则保留原列表（兼容口型板未配置的情况）
                if (!filtered.isEmpty()) {
                    allMachines = filtered;
                }
            }
        }

        // 4. 定点机台过滤
        LambdaQueryWrapper<TqSpecifyMachine> specifyWrapper = new LambdaQueryWrapper<>();
        specifyWrapper.eq(TqSpecifyMachine::getBeadCode, beadCode);
        specifyWrapper.eq(TqSpecifyMachine::getIsDelete, 0);
        List<TqSpecifyMachine> specifyList = tqSpecifyMachineMapper.selectList(specifyWrapper);

        // 4.1 限制作业（jobType=0, lineType=0）：仅保留限制列表中的机台
        List<TqSpecifyMachine> canList = specifyList.stream()
                .filter(s -> "0".equals(s.getJobType()) && "0".equals(s.getLineType()))
                .collect(Collectors.toList());
        if (!canList.isEmpty()) {
            List<String> canMachineCodes = canList.stream()
                    .map(TqSpecifyMachine::getMachineCode)
                    .collect(Collectors.toList());
            List<TqMachineInfo> filtered = allMachines.stream()
                    .filter(m -> canMachineCodes.contains(m.getMachineCode()))
                    .collect(Collectors.toList());
            if (!filtered.isEmpty()) {
                allMachines = filtered;
            }
        }

        // 4.2 不可作业（jobType=1, lineType=1）：排除不可作业机台
        List<TqSpecifyMachine> notList = specifyList.stream()
                .filter(s -> "1".equals(s.getJobType()) && "1".equals(s.getLineType()))
                .collect(Collectors.toList());
        if (!notList.isEmpty()) {
            List<String> notMachineCodes = notList.stream()
                    .map(TqSpecifyMachine::getMachineCode)
                    .collect(Collectors.toList());
            allMachines = allMachines.stream()
                    .filter(m -> !notMachineCodes.contains(m.getMachineCode()))
                    .collect(Collectors.toList());
        }

        // 排除原机台
        allMachines = allMachines.stream()
                .filter(m -> !m.getMachineCode().equals(record.getMachineCode()))
                .collect(Collectors.toList());

        log.info("[候选机台] 胎圈{}转机台候选机台数={}, 寸口={}, 口型板={}, 定点限制={}, 定点排除={}",
                beadCode, allMachines.size(), proSize, triangleGlueCode,
                canList.size(), notList.size());

        return AjaxResult.success(allMachines);
    }

    /**
     * 转机台
     */
    @Log(title = "胎圈排程结果", businessType = BusinessType.CHANGE_MACHINE)
    @ApiOperation("转机台")
    @PostMapping("/changeMachine")
    public AjaxResult changeMachine(@RequestBody TqChangeMachineDTO dto) {
        return tqScheduleResultService.changeMachine(dto);
    }

    /**
     * 调量前校验
     */
    @ApiOperation("调量前校验")
    @PostMapping("/validateChangeQty")
    public AjaxResult validateChangeQty(@RequestBody TqScheduleResult entity) {
        return tqScheduleResultService.validateChangeQty(entity);
    }

    /**
     * 调量
     */
    @Log(title = "胎圈排程结果", businessType = BusinessType.CHANGE_QTY)
    @ApiOperation("调量")
    @PostMapping("/changeQty")
    public AjaxResult changeQty(@RequestBody TqScheduleResult entity) {
        // 先校验，校验通过再执行调量
        AjaxResult validateResult = tqScheduleResultService.validateChangeQty(entity);
        if (!validateResult.get(AjaxResult.CODE_TAG).equals(200)) {
            return validateResult;
        }
        return tqScheduleResultService.changeQty(entity);
    }

    /**
     * 逻辑删除排程记录
     * 只能删除发布成功次数等于0的计划
     */
    @Log(title = "胎圈排程结果", businessType = BusinessType.DELETE)
    @ApiOperation("逻辑删除排程记录")
    @PostMapping("/logicDelete")
    public AjaxResult logicDelete(@RequestBody List<Long> ids) {
        return tqScheduleResultService.logicDeleteByIds(ids);
    }

    /**
     * 发布排程到MES
     */
    @Log(title = "胎圈排程结果", businessType = BusinessType.PUBLISH)
    @ApiOperation("发布排程")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody TqScheduleResult queryVO) {
        return tqScheduleResultService.publish(queryVO);
    }

    /**
     * 查询排程日期是否已发布
     */
    @ApiOperation("查询排程日期是否已发布")
    @PostMapping("/isPublish")
    public Boolean isPublish(@RequestBody TqScheduleResult queryVO) {
        return tqScheduleResultService.isPublish(queryVO.getScheduleDateQuery());
    }

    /**
     * 根据排程日期构建6个班次的日期展示列表
     * 胎圈排程6个班次覆盖D日中班、D+1日夜早中、D+2日夜早（D=排程日期-2，即今天）：
     * 班次1：D日中班，班次2~4：D+1日夜早中，班次5~6：D+2日夜早
     *
     * @param queryVO 查询条件
     * @return 班次日期列表
     */
    @ApiOperation("获取胎圈排程班次日期列表")
    @PostMapping("/listScheduleShiftDates")
    public List<TqScheduleShiftDateVO> listScheduleShiftDates(@RequestBody TqScheduleResult queryVO) {
        Date scheduleDate = queryVO.getScheduleDateQuery();
        if (scheduleDate == null) {
            // 默认排程日期 = 今天 + 1（T+1），与前端默认值保持一致
            scheduleDate = DateUtil.offsetDay(new Date(), 1);
        }
        // D = 排程日期 - 1（即今天）
        // 胎圈1班=D日中班，胎圈2-4班=D+1日(夜/早/中)，胎圈5-6班=D+2日(夜/早)
        Date dDay = DateUtil.offsetDay(scheduleDate, -1);
        Date dPlus1Day = DateUtil.offsetDay(dDay, 1);
        Date dPlus2Day = DateUtil.offsetDay(dDay, 2);
        String dDateStr = DateUtil.format(dDay, "MM/dd");
        String dPlus1DateStr = DateUtil.format(dPlus1Day, "MM/dd");
        String dPlus2DateStr = DateUtil.format(dPlus2Day, "MM/dd");

        List<TqScheduleShiftDateVO> result = new ArrayList<>(6);
        result.add(buildShiftDateVO(1, "afternoon", dDateStr));       // D日中班
        result.add(buildShiftDateVO(2, "night", dPlus1DateStr));      // D+1日夜班
        result.add(buildShiftDateVO(3, "morning", dPlus1DateStr));    // D+1日早班
        result.add(buildShiftDateVO(4, "afternoon", dPlus1DateStr));  // D+1日中班
        result.add(buildShiftDateVO(5, "night", dPlus2DateStr));      // D+2日夜班
        result.add(buildShiftDateVO(6, "morning", dPlus2DateStr));    // D+2日早班
        return result;
    }

    /**
     * 构建班次日期VO
     */
    private TqScheduleShiftDateVO buildShiftDateVO(int shift, String shiftType, String shiftDate) {
        TqScheduleShiftDateVO vo = new TqScheduleShiftDateVO();
        vo.setShift(shift);
        vo.setShiftType(shiftType);
        vo.setShiftDate(shiftDate);
        return vo;
    }
}
