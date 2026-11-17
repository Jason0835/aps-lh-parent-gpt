package com.zlt.aps.lh.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.lh.api.domain.dto.LhMachineOnlineSnapshotDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.mapper.LhMachineOnlineSnapshotMapper;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 通过物料与原机台的MES最近在机日期识别已结束的前段欠产。
 * 所有原来源机台均更新才拒绝二次上机，保留真实余量和现场资源事实。
 *
 * @author APS
 * @since 2026/11/15
 */
@Slf4j
@Component
public class OverdueSurplusAdmissionPolicy {

    /**
     * 仅供本准入使用的逻辑删除感知快照查询。
     */
    @Resource
    private LhMachineOnlineSnapshotMapper machineOnlineSnapshotMapper;

    /**
     * 判断是否需要核对普通模式下窗口无计划的前段欠产来源。
     *
     * @param context 当前排程模式
     * @param sku 原始窗口量及余量来源
     * @return 前段欠产且窗口无计划时返回true
     */
    public static boolean requiresOnlineEvidence(LhScheduleContext context, SkuScheduleDTO sku) {
        return Objects.nonNull(context) && !context.isStopProductionMode()
                && Objects.nonNull(sku) && StringUtils.isNotBlank(sku.getMaterialCode())
                && sku.isOverdueSurplusFlag() && sku.getOriginalWindowPlanQty() <= 0;
    }

    /**
     * 批量查询候选物料及原机台历史，返回已结束前段的物料证据。
     * 查询上界固定为本轮T日，不把未来快照或90天截断当成下机依据。
     *
     * @param context 工厂、日期和批次上下文
     * @param candidates 尚未分类为续作或新增的候选
     * @return 已完成物料编码到d1和各机台d2证据的映射
     */
    public Map<String, String> findCompletedStageEvidence(
            LhScheduleContext context, List<SkuScheduleDTO> candidates) {
        if (Objects.isNull(context) || Objects.isNull(context.getScheduleDate())
                || CollectionUtils.isEmpty(candidates)) {
            return Collections.emptyMap();
        }
        Set<String> materialCodes = candidates.stream()
                .filter(sku -> requiresOnlineEvidence(context, sku))
                .map(SkuScheduleDTO::getMaterialCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (materialCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        Date cutoffDate = LhScheduleTimeUtil.clearTime(context.getScheduleDate());
        List<LhMachineOnlineSnapshotDTO> materialHistory = machineOnlineSnapshotMapper.selectList(
                this.baseQuery(context.getFactoryCode(), cutoffDate)
                        .in(LhMachineOnlineSnapshotDTO::getMaterialCode, materialCodes));
        Map<String, List<LhMachineOnlineSnapshotDTO>> latestSources = this.latestMaterialSources(materialHistory);
        Set<String> machineCodes = new LinkedHashSet<>();
        latestSources.values().stream().flatMap(List::stream).forEach(source -> {
            String physicalCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(source.getLhCode());
            if (StringUtils.isNotBlank(physicalCode)) {
                machineCodes.add(physicalCode);
                machineCodes.add(physicalCode + "L");
                machineCodes.add(physicalCode + "R");
            }
        });
        if (machineCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        Date earliestSourceDate = latestSources.values().stream().flatMap(List::stream)
                .map(LhMachineOnlineSnapshotDTO::getOnlineDate).min(Date::compareTo).orElse(cutoffDate);
        List<LhMachineOnlineSnapshotDTO> machineHistory = machineOnlineSnapshotMapper.selectList(
                this.baseQuery(context.getFactoryCode(), cutoffDate)
                        .in(LhMachineOnlineSnapshotDTO::getLhCode, machineCodes)
                        .ge(LhMachineOnlineSnapshotDTO::getOnlineDate, earliestSourceDate));
        Map<String, String> completed = this.resolveCompletedStages(latestSources, machineHistory);
        log.info("前段欠产MES准入证据加载, 工厂: {}, 批次: {}, 截止日: {}, 候选物料数: {}, "
                        + "物料历史行数: {}, 机台历史行数: {}, 已结束物料: {}",
                context.getFactoryCode(), context.getBatchNo(), LhScheduleTimeUtil.formatDate(cutoffDate),
                materialCodes.size(), materialHistory.size(), machineHistory.size(), completed.keySet());
        return completed;
    }

    /**
     * 构造同工厂截至T日的在机业务查询，删除条件由框架映射注入。
     *
     * @param factoryCode 工厂编号
     * @param cutoffDate 本轮T日
     * @return 仅包含业务条件的查询
     */
    private LambdaQueryWrapper<LhMachineOnlineSnapshotDTO> baseQuery(String factoryCode, Date cutoffDate) {
        return new LambdaQueryWrapper<LhMachineOnlineSnapshotDTO>()
                .eq(LhMachineOnlineSnapshotDTO::getFactoryCode, factoryCode)
                .le(LhMachineOnlineSnapshotDTO::getOnlineDate, cutoffDate)
                .orderByAsc(LhMachineOnlineSnapshotDTO::getOnlineDate)
                .orderByAsc(LhMachineOnlineSnapshotDTO::getId);
    }

    /**
     * 按物料保留最近在机日的全部来源，不以某一台或某一产品状态替代物料历史。
     *
     * @param history 有效物料历史记录
     * @return 物料与其最近日期全部在机记录
     */
    Map<String, List<LhMachineOnlineSnapshotDTO>> latestMaterialSources(List<LhMachineOnlineSnapshotDTO> history) {
        Map<String, List<LhMachineOnlineSnapshotDTO>> sources = new LinkedHashMap<>();
        for (LhMachineOnlineSnapshotDTO record : history) {
            if (Objects.isNull(record.getOnlineDate()) || StringUtils.isBlank(record.getMaterialCode())) {
                continue;
            }
            List<LhMachineOnlineSnapshotDTO> latest = sources.computeIfAbsent(
                    record.getMaterialCode(), material -> new ArrayList<>());
            if (!latest.isEmpty() && record.getOnlineDate().before(latest.get(0).getOnlineDate())) {
                continue;
            }
            if (!latest.isEmpty() && record.getOnlineDate().after(latest.get(0).getOnlineDate())) {
                latest.clear();
            }
            latest.add(record);
        }
        return sources;
    }

    /**
     * 比较d1全部来源侧与各侧d2，只有全部严格晚于d1才形成拦截证据。
     *
     * @param latestSources 各物料最近在机日的完整来源
     * @param machineHistory 原机台在本轮数据时点前的历史
     * @return 已结束前段的物料证据；缺失来源或同日记录不判完成
     */
    Map<String, String> resolveCompletedStages(Map<String, List<LhMachineOnlineSnapshotDTO>> latestSources,
                                              List<LhMachineOnlineSnapshotDTO> machineHistory) {
        Map<String, LhMachineOnlineSnapshotDTO> latestBySide = new TreeMap<>();
        for (LhMachineOnlineSnapshotDTO record : machineHistory) {
            if (Objects.isNull(record.getOnlineDate()) || StringUtils.isBlank(record.getMaterialCode())) {
                continue;
            }
            for (String side : this.sourceSides(record)) {
                latestBySide.merge(side, record, (previous, current) ->
                        current.getOnlineDate().after(previous.getOnlineDate()) ? current : previous);
            }
        }
        Map<String, String> completed = new LinkedHashMap<>();
        latestSources.forEach((material, sources) -> {
            Date sourceDate = sources.get(0).getOnlineDate();
            Set<String> sourceSides = new LinkedHashSet<>();
            for (LhMachineOnlineSnapshotDTO source : sources) {
                List<String> sides = this.sourceSides(source);
                if (sides.isEmpty()) {
                    return;
                }
                sourceSides.addAll(sides);
            }
            boolean allUpdated = sourceSides.stream().allMatch(side -> {
                LhMachineOnlineSnapshotDTO latest = latestBySide.get(side);
                return Objects.nonNull(latest) && latest.getOnlineDate().after(sourceDate);
            });
            if (allUpdated && !sourceSides.isEmpty()) {
                String machineEvidence = sourceSides.stream().map(side -> {
                    LhMachineOnlineSnapshotDTO latest = latestBySide.get(side);
                    return side + "{d2=" + LhScheduleTimeUtil.formatDate(latest.getOnlineDate())
                            + ",material=" + latest.getMaterialCode() + ",status=" + latest.getProductStatus() + "}";
                }).collect(Collectors.joining(","));
                completed.put(material, "d1=" + LhScheduleTimeUtil.formatDate(sourceDate)
                        + ",sources=[" + machineEvidence + "]");
            }
        });
        return completed;
    }

    /**
     * 展开MES来源侧，整机双模必须两侧均更新，单侧不能借配对侧更新证明下机。
     *
     * @param record MES在机记录
     * @return 来源侧编码；缺失机台时返回空集合
     */
    private List<String> sourceSides(LhMachineOnlineSnapshotDTO record) {
        String machineCode = StringUtils.upperCase(StringUtils.trimToEmpty(record.getLhCode()));
        if (machineCode.isEmpty()) {
            return Collections.emptyList();
        }
        if (LhSingleControlMachineUtil.isSingleMouldMachine(machineCode)) {
            return Collections.singletonList(machineCode);
        }
        String side = StringUtils.upperCase(StringUtils.trimToEmpty(record.getLrMolds()));
        if ("L".equals(side) || "R".equals(side)) {
            return Collections.singletonList(machineCode + side);
        }
        if ("LR".equals(side)) {
            return Arrays.asList(machineCode + "L", machineCode + "R");
        }
        // 未标明侧别只能同口径比对，不猜测整机与单侧的覆盖关系。
        return Collections.singletonList(machineCode);
    }
}
