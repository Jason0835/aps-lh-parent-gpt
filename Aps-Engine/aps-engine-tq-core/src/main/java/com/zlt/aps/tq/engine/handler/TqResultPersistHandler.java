package com.zlt.aps.tq.engine.handler;

import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.domain.TqPersistResult;
import com.zlt.aps.tq.engine.domain.TqSnapshotBuildResult;
import com.zlt.aps.tq.engine.mapper.TqEngineMapper;
import com.zlt.aps.tq.engine.service.impl.TqScheduleQualitySummaryService;
import com.zlt.aps.tq.engine.service.impl.TqSnapshotBuildService;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;

/**
 * S4: 结果校验与持久化Handler。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>同步排程数据到日志表，删除历史数据</li>
 *   <li>创建自动排程记录</li>
 *   <li>合并已有排程记录（重排场景下保留已发布数据）</li>
 *   <li>批量保存排程结果（统一写入主表T_TQ_SCHEDULE_RESULT）</li>
 * </ol>
 *
 * <p>变更说明（2026-06-27）：</p>
 * <ul>
 *   <li>外协规格相关逻辑已废弃（旧4班次算法遗留），6班次排程不再区分外协/非外协</li>
 *   <li>所有排程结果统一写入 T_TQ_SCHEDULE_RESULT，不再写入 T_TQ_ASSIST_SCHEDULE</li>
 *   <li>原外协分离逻辑已注释保留，便于追溯</li>
 * </ul>
 *
 * @author APS
 */
@Slf4j
@Component
public class TqResultPersistHandler extends AbsTqScheduleStepHandler {

    @Resource
    private TqEngineMapper tqEngineMapper;

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    /**
     * 解释快照构建服务。
     * <p>Phase 4 重构新增：在 S6 阶段构建每个胎圈规格的解释快照（含规则命中、候选机台、未排证据等），
     * 替代 Phase 2 仅序列化 {@code TqRuleTrace} 的简单逻辑。</p>
     */
    @Resource
    private TqSnapshotBuildService tqSnapshotBuildService;

    /**
     * 质量指标汇总服务。
     * <p>Phase 4 重构新增：在 S6 阶段统一计算 10 项核心指标，避免不同入口使用不同口径。</p>
     */
    @Resource
    private TqScheduleQualitySummaryService tqScheduleQualitySummaryService;

    @Override
    protected String getStepName() {
        return "S4-结果校验与持久化";
    }

    @Override
    protected void doHandle(TqScheduleContext context) {
        String scheduleDate = context.getScheduleDate();
        String batchNo = context.getBatchNo();
        String cxBatchNo = context.getCxBatchNo();
        // 外协规格逻辑已废弃，assistSpecMap 不再使用
        // Map<String, String> assistSpecMap = context.getAssistSpecMap();
        List<TqScheduleResultVo> scheduleList = context.getScheduleList();
        String factoryCode = context.getFactoryCode();

        // 给所有排程结果设置分厂编码
        scheduleList.forEach(r -> r.setFactoryCode(factoryCode));

        // Phase 4 重构：初始化落库汇总对象，贯穿 S6 阶段统计
        TqPersistResult persistResult = new TqPersistResult();
        context.setPersistResult(persistResult);

        // 1. 构建解释快照并填充 explainJson 字段（必须在过滤前构建，保留未排任务的快照）
        // Phase 4 重构：从仅序列化 TqRuleTrace 升级为构建完整解释快照（含候选机台、未排证据、异常等）
        buildSnapshotAndFillExplainJson(context, scheduleList);

        // 2. 过滤掉无效数据（避免无效数据落库）
        // 过滤条件：6个班次都没排计划量，或机台编码为空（未成功分配机台的记录不应落库）
        // 这些数据作为未排任务计入 persistResult.unplannedCount
        int beforeFilterSize = scheduleList.size();
        scheduleList.removeIf(this::isInvalidSchedule);
        int filteredCount = beforeFilterSize - scheduleList.size();
        persistResult.setUnplannedCount(filteredCount);
        if (filteredCount > 0) {
            log.info("[S6] 过滤掉无效数据(无计划量或机台编码为空):{}条, 剩余有效数据:{}条",
                    filteredCount, scheduleList.size());
            autoScheduleLogService.insertTqScheduleLog(batchNo, "",
                    "S6-过滤无效数据", "无计划量或机台编码为空的记录数:" + filteredCount
                            + "，剩余有效数据:" + scheduleList.size() + "条");
        }

        // 3. 分离外协排程数据（外协逻辑已废弃，所有数据统一作为非外协处理）
        // List<TqScheduleResultVo> assistScheduleList = scheduleList.stream()
        //         .filter(r -> assistSpecMap.containsKey(r.getBeadCode()))
        //         .collect(Collectors.toList());
        // List<TqScheduleResultVo> normalScheduleList = scheduleList.stream()
        //         .filter(r -> !assistSpecMap.containsKey(r.getBeadCode()))
        //         .collect(Collectors.toList());
        // context.setAssistScheduleList(assistScheduleList);
        // context.setNormalScheduleList(normalScheduleList);
        // log.info("[S4] 外协排程数据:{}条, 非外协排程数据:{}条", assistScheduleList.size(), normalScheduleList.size());

        List<TqScheduleResultVo> normalScheduleList = scheduleList;
        log.info("[S4] 排程数据:{}条（外协分离逻辑已废弃，统一写入主表）", normalScheduleList.size());

        // 4. 同步排程数据到日志表，删除历史数据
        syncTqScheduleToLog(scheduleDate);

        // 5. 创建自动排程记录
        createScheduleRecord(scheduleDate, cxBatchNo, batchNo);

        // 6. 批量新增外协排程结果数据（外协逻辑已废弃，不再写入 T_TQ_ASSIST_SCHEDULE）
        // if (CollectionUtils.isNotEmpty(assistScheduleList)) {
        //     tqEngineMapper.batchCreateAssistScheduleResult(assistScheduleList);
        //     log.info("[S4] 外协排程结果保存完成, 记录数:{}", assistScheduleList.size());
        // }

        // 7. 查询已有排程记录并合并
        List<TqScheduleResultVo> existScheduleList = tqEngineMapper.listTqEnginSchedule(scheduleDate);
        context.setExistScheduleList(existScheduleList);
        normalScheduleList = mergeExistSchedule(batchNo, normalScheduleList, existScheduleList);

        // 8. 批量新增排程结果数据（统一写入主表）
        if (CollectionUtils.isNotEmpty(normalScheduleList)) {
            tqEngineMapper.batchCreateScheduleResult(normalScheduleList);
            context.setInsertedCount(normalScheduleList.size());
            persistResult.setResultCount(normalScheduleList.size());
            // 统计填充了 explainJson 的记录数
            long explainCount = normalScheduleList.stream()
                    .filter(r -> StringUtils.isNotEmpty(r.getExplainJson()))
                    .count();
            persistResult.setExplainCount((int) explainCount);
            log.info("[S4] 排程结果保存完成, 记录数:{}, 解释记录数:{}",
                    normalScheduleList.size(), explainCount);
        }

        // 9. Phase 4 重构新增：构建质量指标摘要，写入 context 供后续日志和返回
        try {
            Map<String, Object> qualitySummary = tqScheduleQualitySummaryService.build(context, persistResult);
            context.setQualitySummary(qualitySummary);
            log.info("[S6] 质量指标摘要: {}", qualitySummary);
            autoScheduleLogService.insertTqScheduleLog(batchNo, "",
                    "S6-质量汇总", formatQualitySummary(qualitySummary));
        } catch (Exception e) {
            // 质量汇总失败不应阻断主流程，仅记录错误
            String errorMsg = "质量指标汇总失败: " + e.getMessage();
            persistResult.addErrorMsg(errorMsg);
            log.warn("[S6] 质量指标汇总失败: {}", e.getMessage(), e);
        }

        autoScheduleLogService.insertTqScheduleLog(batchNo, "",
                "自动排程完成", "排程记录数:" + scheduleList.size()
                        + ", 保存记录数:" + normalScheduleList.size()
                        + ", 未排数:" + persistResult.getUnplannedCount()
                        + ", 解释数:" + persistResult.getExplainCount());
    }

    /**
     * 构建解释快照并填充 explainJson 字段。
     *
     * <p>Phase 4 重构新增：替代原 {@code fillExplainJson} 方法，从仅序列化 {@code TqRuleTrace}
     * 升级为构建完整解释快照（含规则命中、候选机台、未排证据、异常等多元字段）。</p>
     *
     * <p>处理逻辑：</p>
     * <ul>
     *   <li>遍历所有排程结果（包括将被过滤的未排任务），按 beadCode 构建解释快照</li>
     *   <li>调用 {@link TqSnapshotBuildService#buildTaskExplain} 构建快照</li>
     *   <li>调用 {@link TqSnapshotBuildService#toExplainJson} 序列化为 JSON 文本</li>
     *   <li>快照写入 context.snapshotMap 供后续查询</li>
     *   <li>JSON 文本写入 scheduleVo.explainJson 字段，落库到 T_TQ_SCHEDULE_RESULT.EXPLAIN_JSON</li>
     * </ul>
     *
     * @param context      排程上下文
     * @param scheduleList 排程结果列表（按引用写入 explainJson 字段）
     */
    private void buildSnapshotAndFillExplainJson(TqScheduleContext context, List<TqScheduleResultVo> scheduleList) {
        if (scheduleList == null || scheduleList.isEmpty()) {
            return;
        }
        Map<String, TqSnapshotBuildResult> snapshotMap = context.getSnapshotMap();
        for (TqScheduleResultVo scheduleVo : scheduleList) {
            String beadCode = scheduleVo.getBeadCode();
            try {
                // 构建解释快照
                TqSnapshotBuildResult snapshot = tqSnapshotBuildService.buildTaskExplain(scheduleVo, context);
                snapshotMap.put(beadCode, snapshot);
                // 序列化为 JSON 文本，写入排程结果的 explainJson 字段
                String explainJson = tqSnapshotBuildService.toExplainJson(snapshot);
                scheduleVo.setExplainJson(explainJson);
            } catch (Exception e) {
                // 构建失败不应阻断主流程，仅记录日志并设置为 null
                log.warn("[S6] 构建解释快照失败, beadCode:{}, 错误:{}", beadCode, e.getMessage());
                scheduleVo.setExplainJson(null);
                if (context.getPersistResult() != null) {
                    context.getPersistResult().addErrorMsg(
                            "构建解释快照失败, beadCode=" + beadCode + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * 格式化质量指标摘要为日志文本。
     *
     * @param qualitySummary 质量指标摘要
     * @return 格式化后的文本
     */
    private String formatQualitySummary(Map<String, Object> qualitySummary) {
        if (qualitySummary == null || qualitySummary.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        qualitySummary.forEach((key, value) -> {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(key).append("=").append(value);
        });
        return sb.toString();
    }

    /**
     * 把排程数据同步到日志表，删除历史排程数据。
     */
    private void syncTqScheduleToLog(String scheduleDate) {
        tqEngineMapper.syncTqScheduleToLog(scheduleDate);
        tqEngineMapper.deleteTqSchedule(scheduleDate);
    }

    /**
     * 创建自动排程记录。
     */
    private void createScheduleRecord(String scheduleDate, String cxBatchNo, String batchNo) {
        Map<String, Object> params = new HashMap<>();
        params.put("scheduleDate", scheduleDate);
        params.put("cxBatchNo", cxBatchNo);
        params.put("batchNo", batchNo);
        params.put("userName", SecurityUtils.getUsername());
        tqEngineMapper.createScheduleRecord(params);
    }

    /**
     * 如果当天排程已经存在，则把当天的排程合并到自动排程的列表中。
     *
     * <p>合并规则：</p>
     * <ul>
     *   <li>重排前已发布且只有一条记录：沿用原订单号和机台，计划量按新排程</li>
     *   <li>重排前已发布且有多条记录：保留原排产数据，新排程计划量写入备注</li>
     *   <li>重排前未找到记录：直接使用新排程数据</li>
     *   <li>重排前已发布但新排程中没有的规格：保留原排产数据</li>
     * </ul>
     */
    private List<TqScheduleResultVo> mergeExistSchedule(String batchNo, List<TqScheduleResultVo> autoScheduleList,
                                                        List<TqScheduleResultVo> existScheduleList) {
        if (CollectionUtils.isEmpty(existScheduleList)) {
            return autoScheduleList;
        }

        List<TqScheduleResultVo> mergeList = new ArrayList<>();

        // 拿到重排前已经发布给MES的排产数据，key为胎圈代码
        Map<String, List<TqScheduleResultVo>> existScheduleMap = existScheduleList.stream()
                .filter(s -> s.getPublishSuccessCount() != null && s.getPublishSuccessCount() > 0)
                .collect(Collectors.groupingBy(TqScheduleResultVo::getBeadCode));

        for (TqScheduleResultVo autoSchedule : autoScheduleList) {
            List<TqScheduleResultVo> existScheduleGroupList = existScheduleMap.get(autoSchedule.getBeadCode());

            if (existScheduleGroupList != null && existScheduleGroupList.size() == 1) {
                // 对应规格重排前已发布，且只有一条排程记录（只对应了一个机台）
                TqScheduleResultVo existSchedule = existScheduleGroupList.get(0);
                autoSchedule.setOrderNo(existSchedule.getOrderNo());
                autoSchedule.setPublishSuccessCount(existSchedule.getPublishSuccessCount());
                autoSchedule.setNewestPublishTime(existSchedule.getNewestPublishTime());
                autoSchedule.setIsRelease(ApsConstant.WAIT_RELEASING);
                autoSchedule.setMachineCode(existSchedule.getMachineCode());
                mergeList.add(autoSchedule);
            } else if (existScheduleGroupList != null && existScheduleGroupList.size() > 1) {
                // 对应规格重排前已发布，且有多条排程记录（对应了多个机台）
                String remarkTip = I18nUtil.getMessage("reschedule.double.spec.remark2");
                remarkTip = StringUtils.format(remarkTip,
                        stripZeros(autoSchedule.getClass1PlanQty()),
                        stripZeros(autoSchedule.getClass2PlanQty()),
                        stripZeros(autoSchedule.getClass3PlanQty()),
                        stripZeros(autoSchedule.getClass4PlanQty()),
                        stripZeros(autoSchedule.getClass5PlanQty()),
                        stripZeros(autoSchedule.getClass6PlanQty()));
                for (TqScheduleResultVo existSchedule : existScheduleGroupList) {
                    existSchedule.setBatchNo(batchNo);
                    existSchedule.setRemark(remarkTip);
                    mergeList.add(existSchedule);
                }
            } else {
                // 对应的规格，重排前没有找到相应记录
                mergeList.add(autoSchedule);
            }
            existScheduleMap.remove(autoSchedule.getBeadCode());
        }

        // 重排前的已发布规格如果没有在重排后的列表中，也需要保留
        for (List<TqScheduleResultVo> list : existScheduleMap.values()) {
            list.forEach(r -> r.setBatchNo(batchNo));
            mergeList.addAll(list);
        }

        return mergeList;
    }

    /**
     * 去除数字末尾的0（如 1200.0 → 1200）
     */
    private String stripZeros(Double value) {
        if (value == null) {
            return "0";
        }
        if (value == value.longValue()) {
            return String.valueOf(value.longValue());
        }
        return String.valueOf(value);
    }

    /**
     * 判断排程记录是否为无效数据（应过滤掉，不落库）。
     *
     * <p>过滤条件：</p>
     * <ul>
     *   <li>6个班次计划量均为空或0 → 无排产任务</li>
     *   <li>机台编码为空 → 未成功分配机台，无法执行生产</li>
     * </ul>
     *
     * @param scheduleVo 排程记录
     * @return true表示应过滤掉；false表示有效数据，应保留
     */
    private boolean isInvalidSchedule(TqScheduleResultVo scheduleVo) {
        return isAllClassPlanEmpty(scheduleVo) || StringUtils.isEmpty(scheduleVo.getMachineCode());
    }

    /**
     * 判断6个班次的计划量是否全部为空或0。
     *
     * <p>用于过滤无效排程数据：6个班次都没排计划量的记录不应落库。</p>
     *
     * @param scheduleVo 排程记录
     * @return true表示6个班次计划量均为空或0，应过滤掉；false表示至少有一个班次有计划量
     */
    private boolean isAllClassPlanEmpty(TqScheduleResultVo scheduleVo) {
        return isPlanEmpty(scheduleVo.getClass1PlanQty())
                && isPlanEmpty(scheduleVo.getClass2PlanQty())
                && isPlanEmpty(scheduleVo.getClass3PlanQty())
                && isPlanEmpty(scheduleVo.getClass4PlanQty())
                && isPlanEmpty(scheduleVo.getClass5PlanQty())
                && isPlanEmpty(scheduleVo.getClass6PlanQty());
    }

    /**
     * 判断单个班次计划量是否为空或0
     */
    private boolean isPlanEmpty(Double planQty) {
        return planQty == null || planQty <= 0;
    }

    /**
     * 填充规则解释JSON字段（explainJson）。
     *
     * <p>Phase 2 实现（已被 Phase 4 {@link #buildSnapshotAndFillExplainJson} 替代，仅保留方法签名避免外部引用断裂）。</p>
     *
     * <p>历史实现：把 Context 中按 beadCode 聚合的 {@link com.zlt.aps.tq.engine.domain.TqRuleTrace}
     * 序列化为 JSON 文本，写入每条排程结果的 explainJson 字段。
     * Phase 4 升级为构建完整解释快照（含规则命中、候选机台、未排证据、异常等多元字段）。</p>
     *
     * @param context      排程上下文（含 ruleTraceMap）
     * @param scheduleList 排程结果列表（按引用写入 explainJson 字段）
     * @deprecated Phase 4 重构后由 {@link #buildSnapshotAndFillExplainJson} 替代
     */
    @Deprecated
    private void fillExplainJson(TqScheduleContext context, List<TqScheduleResultVo> scheduleList) {
        if (scheduleList == null || scheduleList.isEmpty()) {
            return;
        }
        for (TqScheduleResultVo scheduleVo : scheduleList) {
            String beadCode = scheduleVo.getBeadCode();
            com.zlt.aps.tq.engine.domain.TqRuleTrace trace = context.getRuleTrace(beadCode);
            if (trace == null || trace.getRuleHits().isEmpty()) {
                // 无证据时设置为 null，数据库存储 NULL
                scheduleVo.setExplainJson(null);
                continue;
            }
            try {
                scheduleVo.setExplainJson(trace.toExplainJson());
            } catch (Exception e) {
                // 序列化失败不应阻断主流程，仅记录日志并设置为 null
                log.warn("[S6] 序列化规则解释JSON失败, beadCode:{}, 错误:{}", beadCode, e.getMessage());
                scheduleVo.setExplainJson(null);
            }
        }
    }
}
