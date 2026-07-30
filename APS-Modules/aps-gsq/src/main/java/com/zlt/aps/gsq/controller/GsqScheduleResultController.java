package com.zlt.aps.gsq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.gsq.api.domain.dto.GsqChangeMachineDTO;
import com.zlt.aps.gsq.api.domain.dto.GsqInsertOrderDTO;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResult;
import com.zlt.aps.gsq.api.domain.entity.GsqSpecifyMachine;
import com.zlt.aps.gsq.api.domain.vo.GsqScheduleShiftDateVO;
import com.zlt.aps.gsq.engine.service.GsqEngineService;
import com.zlt.aps.gsq.mapper.GsqScheduleResultMapper;
import com.zlt.aps.gsq.service.GsqMachineInfoService;
import com.zlt.aps.gsq.api.domain.vo.GsqRollingCheckRequestVo;
import com.zlt.aps.gsq.api.domain.vo.GsqRollingTaskVo;
import com.zlt.aps.gsq.service.GsqAutoRollingApplicationService;
import com.zlt.aps.gsq.service.IGsqScheduleResultService;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * 钢丝圈排程结果Controller
 *
 * <p>6班次制：1班=D日中班，2班=D+1日夜班，3班=D+1日早班，4班=D+1日中班，5班=D+2日夜班，6班=D+2日早班
 * 其中 D+1 = 排程日期（SCHEDULE_DATE）
 *
 * @author APS
 */
@Slf4j
@Api(tags = "钢丝圈排程结果")
@RestController
@RequestMapping("/scheduleResult")
public class GsqScheduleResultController extends AbstractDocBizController<GsqScheduleResult> {

    @Autowired
    private IGsqScheduleResultService gsqScheduleResultService;

    @Autowired
    private GsqAutoRollingApplicationService gsqAutoRollingApplicationService;

    @Resource
    private GsqScheduleResultMapper gsqScheduleResultMapper;

    @Autowired
    private GsqEngineService gsqEngineService;

    /**
     * 钢丝圈机台信息服务（用于查询启用状态的机台）
     */
    @Autowired
    private GsqMachineInfoService gsqMachineInfoService;

    /**
     * 钢丝圈定点机台Mapper（用于查询限定/不可作业机台）
     */
    @Resource
    private com.zlt.aps.gsq.mapper.GsqSpecifyMachineMapper gsqSpecifyMachineMapper;

    /**
     * 查询钢丝圈排程结果列表
     */
    @ApiOperation("查询钢丝圈排程结果列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody GsqScheduleResult queryVO) {
        startPage();
        LambdaQueryWrapper<GsqScheduleResult> wrapper = buildQueryWrapper(queryVO);
        List<GsqScheduleResult> list = gsqScheduleResultMapper.selectList(wrapper);
        // 回填胎圈排程结果数据到 TQ_CLASS1~6_PLAN 字段
        gsqScheduleResultService.fillTqPlanQty(list);
        return getDataTable(list);
    }

    /**
     * 保存（新增/修改）
     */
    @Log(title = "钢丝圈排程结果", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody GsqScheduleResult billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "钢丝圈排程结果", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 获取详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public GsqScheduleResult getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /**
     * 导入数据
     */
    @Log(title = "钢丝圈排程结果", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext,
                                 @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出数据
     */
    @Log(title = "钢丝圈排程结果", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody GsqScheduleResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected IDocService getDocService() {
        return gsqScheduleResultService;
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
    private LambdaQueryWrapper<GsqScheduleResult> buildQueryWrapper(GsqScheduleResult queryVO) {
        LambdaQueryWrapper<GsqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(queryVO.getScheduleDateQuery() != null,
                GsqScheduleResult::getScheduleDate, queryVO.getScheduleDateQuery());
        wrapper.like(PubUtil.isNotEmpty(queryVO.getSteelRingCode()),
                GsqScheduleResult::getSteelRingCode, queryVO.getSteelRingCode());
        wrapper.like(PubUtil.isNotEmpty(queryVO.getProSize()),
                GsqScheduleResult::getProSize, queryVO.getProSize());
        wrapper.like(PubUtil.isNotEmpty(queryVO.getTwiningDiscCode()),
                GsqScheduleResult::getTwiningDiscCode, queryVO.getTwiningDiscCode());
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getIsRelease()),
                GsqScheduleResult::getIsRelease, queryVO.getIsRelease());
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()),
                GsqScheduleResult::getMachineCode, queryVO.getMachineCode());
        // 默认排序：排程日期倒序、机台号正序、一班次顺序正序
        wrapper.orderByDesc(GsqScheduleResult::getScheduleDate);
        wrapper.orderByAsc(GsqScheduleResult::getMachineCode);
        wrapper.orderByAsc(GsqScheduleResult::getClass1Sequence);
        return wrapper;
    }

    /**
     * 自动排程
     */
    @Log(title = "钢丝圈排程结果", businessType = BusinessType.AUTOPLAN)
    @ApiOperation("自动排程")
    @PostMapping("/autoPlan")
    public AjaxResult autoPlan(@RequestBody GsqScheduleResult queryVO) {
        return gsqScheduleResultService.autoPlan(queryVO);
    }

    /**
     * 插单前校验
     */
    @ApiOperation("插单前校验")
    @PostMapping("/validateInsertOrder")
    public AjaxResult validateInsertOrder(@RequestBody GsqInsertOrderDTO dto) {
        return gsqScheduleResultService.validateInsertOrder(dto);
    }

    /**
     * 插单
     */
    @Log(title = "钢丝圈排程结果", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("插单")
    @PostMapping("/insertOrder")
    public AjaxResult insertOrder(@RequestBody GsqInsertOrderDTO dto) {
        return gsqScheduleResultService.insertOrder(dto);
    }

    /**
     * 获取转机台候选机台列表
     *
     * <p>过滤策略（参考自动排程的机台过滤优先级规则）：</p>
     * <ol>
     *   <li>寸口过滤：机台支持的英寸范围必须包含当前钢丝圈的英寸尺寸</li>
     *   <li>定点机台过滤：限制作业仅保留限定列表中的机台，不可作业排除对应机台</li>
     *   <li>排除原机台</li>
     * </ol>
     * <p>注意：维修过滤不在候选列表中做，而是在提交转机台时校验</p>
     *
     * @param id 排程记录ID
     * @return 过滤后的候选机台列表
     */
    @ApiOperation("获取转机台候选机台列表")
    @PostMapping("/listCandidateMachines/{id}")
    public AjaxResult listCandidateMachines(@PathVariable("id") Long id) {
        // 查询排程记录
        GsqScheduleResult record = gsqScheduleResultMapper.selectById(id);
        if (record == null || "1".equals(record.getIsDelete())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.recordNotFound"));
        }

        String steelRingCode = record.getSteelRingCode();

        // 1. 查询所有启用的机台
        GsqMachineInfo queryMachine = new GsqMachineInfo();
        queryMachine.setStatus("1");
        List<GsqMachineInfo> allMachines = gsqMachineInfoService.listMachineInfo(queryMachine);
        // 补充排序：按机台编码升序
        allMachines.sort(java.util.Comparator.comparing(GsqMachineInfo::getMachineCode));

        // 2. 定点机台过滤
        LambdaQueryWrapper<GsqSpecifyMachine> specifyWrapper = new LambdaQueryWrapper<>();
        specifyWrapper.eq(GsqSpecifyMachine::getSteelRingCode, steelRingCode);
        specifyWrapper.eq(GsqSpecifyMachine::getIsDelete, 0);
        List<GsqSpecifyMachine> specifyList = gsqSpecifyMachineMapper.selectList(specifyWrapper);

        // 2.1 限制作业（jobType=0, lineType=0）：仅保留限制列表中的机台
        List<GsqSpecifyMachine> canList = specifyList.stream()
                .filter(s -> "0".equals(s.getJobType()) && "0".equals(s.getLineType()))
                .collect(Collectors.toList());
        if (!canList.isEmpty()) {
            List<String> canMachineCodes = canList.stream()
                    .map(GsqSpecifyMachine::getMachineCode)
                    .collect(Collectors.toList());
            List<GsqMachineInfo> filtered = allMachines.stream()
                    .filter(m -> canMachineCodes.contains(m.getMachineCode()))
                    .collect(Collectors.toList());
            if (!filtered.isEmpty()) {
                allMachines = filtered;
            }
        }

        // 2.2 不可作业（jobType=1, lineType=1）：排除不可作业机台
        List<GsqSpecifyMachine> notList = specifyList.stream()
                .filter(s -> "1".equals(s.getJobType()) && "1".equals(s.getLineType()))
                .collect(Collectors.toList());
        if (!notList.isEmpty()) {
            List<String> notMachineCodes = notList.stream()
                    .map(GsqSpecifyMachine::getMachineCode)
                    .collect(Collectors.toList());
            allMachines = allMachines.stream()
                    .filter(m -> !notMachineCodes.contains(m.getMachineCode()))
                    .collect(Collectors.toList());
        }

        // 3. 排除原机台
        allMachines = allMachines.stream()
                .filter(m -> !m.getMachineCode().equals(record.getMachineCode()))
                .collect(Collectors.toList());

        log.info("[候选机台] 钢丝圈{}转机台候选机台数={}, 定点限制={}, 定点排除={}",
                steelRingCode, allMachines.size(), canList.size(), notList.size());

        return AjaxResult.success(allMachines);
    }

    /**
     * 转机台前校验
     */
    @ApiOperation("转机台前校验")
    @PostMapping("/validateChangeMachine")
    public AjaxResult validateChangeMachine(@RequestBody GsqChangeMachineDTO dto) {
        return gsqScheduleResultService.validateChangeMachine(dto);
    }

    /**
     * 转机台
     */
    @Log(title = "钢丝圈排程结果", businessType = BusinessType.CHANGE_MACHINE)
    @ApiOperation("转机台")
    @PostMapping("/changeMachine")
    public AjaxResult changeMachine(@RequestBody GsqChangeMachineDTO dto) {
        return gsqScheduleResultService.changeMachine(dto);
    }

    /**
     * 调量前校验
     */
    @ApiOperation("调量前校验")
    @PostMapping("/validateChangeQty")
    public AjaxResult validateChangeQty(@RequestBody GsqScheduleResult entity) {
        return gsqScheduleResultService.validateChangeQty(entity);
    }

    /**
     * 调量
     */
    @Log(title = "钢丝圈排程结果", businessType = BusinessType.CHANGE_QTY)
    @ApiOperation("调量")
    @PostMapping("/changeQty")
    public AjaxResult changeQty(@RequestBody GsqScheduleResult entity) {
        // 先校验，校验通过再执行调量
        AjaxResult validateResult = gsqScheduleResultService.validateChangeQty(entity);
        if (!validateResult.get(AjaxResult.CODE_TAG).equals(200)) {
            return validateResult;
        }
        return gsqScheduleResultService.changeQty(entity);
    }

    /**
     * 逻辑删除前校验
     * 校验规则：发布成功次数等于0 且 未发送给MES
     * 用于前端删除按钮点击后、确认弹窗前的二次状态校验
     */
    @ApiOperation("逻辑删除前校验")
    @PostMapping("/validateLogicDelete")
    public AjaxResult validateLogicDelete(@RequestBody List<Long> ids) {
        return gsqScheduleResultService.validateLogicDelete(ids);
    }

    /**
     * 逻辑删除排程记录
     * 只能删除发布成功次数等于0且未发送给MES的计划
     */
    @Log(title = "钢丝圈排程结果", businessType = BusinessType.DELETE)
    @ApiOperation("逻辑删除排程记录")
    @PostMapping("/logicDelete")
    public AjaxResult logicDelete(@RequestBody List<Long> ids) {
        return gsqScheduleResultService.logicDeleteByIds(ids);
    }

    /**
     * 发布排程到MES
     * 前端传入选中记录ID列表（ids），后端按发布状态过滤可发布记录：
     * 仅处理"未发布(0)"、"待发布(5)"、"发布失败(2)"三种状态，其余状态忽略。
     *
     * @param queryVO 查询条件（含 scheduleDate、factoryCode、ids）
     * @return 发布结果
     */
    @Log(title = "钢丝圈排程结果", businessType = BusinessType.PUBLISH)
    @ApiOperation("发布排程")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody GsqScheduleResult queryVO) {
        return gsqScheduleResultService.publish(queryVO);
    }

    /**
     * 查询排程日期是否已发布
     */
    @ApiOperation("查询排程日期是否已发布")
    @PostMapping("/isPublish")
    public Boolean isPublish(@RequestBody GsqScheduleResult queryVO) {
        return gsqScheduleResultService.isPublish(queryVO.getScheduleDateQuery());
    }

    /**
     * 唯一性校验
     */
    @ApiOperation("唯一性校验")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody GsqScheduleResult queryVO) {
        return gsqScheduleResultService.checkUnique(queryVO);
    }

    /**
     * 根据排程日期查询发布中或超时失败的记录数
     */
    @ApiOperation("根据排程日期查询发布中或超时失败的记录数")
    @PostMapping("/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody GsqScheduleResult queryVO) {
        return gsqScheduleResultService.isReleasingOrTimeoutByDate(queryVO.getScheduleDateQuery());
    }

    /**
     * 根据排程日期构建6个班次的日期展示列表
     * 钢丝圈排程6个班次覆盖D日中班、D+1日夜早中、D+2日夜早（D=排程日期-2，即今天）：
     * 班次1：D日中班，班次2~4：D+1日(夜/早/中)，班次5~6：D+2日(夜/早)
     *
     * @param queryVO 查询条件
     * @return 班次日期列表
     */
    @ApiOperation("获取钢丝圈排程班次日期列表")
    @PostMapping("/listScheduleShiftDates")
    public List<GsqScheduleShiftDateVO> listScheduleShiftDates(@RequestBody GsqScheduleResult queryVO) {
        return gsqScheduleResultService.listScheduleShiftDates(queryVO);
    }

    /**
     * 检查并提交钢丝圈自动滚动任务（内部接口，供 aps-job 通过 Feign 调用）
     *
     * <p>平台定时任务触发本接口，由 {@link GsqAutoRollingApplicationService} 完成窗口识别、
     * 库存同步、幂等防重和异步派发，与胎侧模块的 /internal/checkTimedRolling 对齐。</p>
     *
     * @param request 检查请求（含 triggerTime 和可选 factoryCode）
     * @return 已创建或复用的自动滚动任务列表
     */
    @ApiOperation("检查钢丝圈自动滚动窗口")
    @PostMapping("/internal/checkTimedRolling")
    public AjaxResult checkTimedRolling(@RequestBody GsqRollingCheckRequestVo request) {
        List<GsqRollingTaskVo> taskList = gsqAutoRollingApplicationService.checkAndSubmit(request);
        return AjaxResult.success(taskList);
    }
}
