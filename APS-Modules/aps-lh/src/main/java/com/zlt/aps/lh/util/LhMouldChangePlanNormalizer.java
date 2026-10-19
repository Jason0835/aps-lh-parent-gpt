package com.zlt.aps.lh.util;

import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 归并同一整机动作中一侧缺少前料的互补左右模计划，历史源对象保持只读。
 *
 * @author APS
 * @date 2026/10/16
 */
@Slf4j
public final class LhMouldChangePlanNormalizer {
    /**
     * 阻止创建无状态规范化工具实例。
     */
    private LhMouldChangePlanNormalizer() {
    }

    /**
     * 将同动作唯一L/R配对复制为LR并继承非空前料；同侧重复、前料冲突及独立单控侧保持原记录。
     *
     * @param plans 历史来源或本次待保存计划，不修改其中原对象
     * @return 保持原顺序的规范化计划，非合并记录继续保留原对象身份
     */
    public static List<LhMouldChangePlan> normalize(List<LhMouldChangePlan> plans) {
        Map<List<Object>, List<LhMouldChangePlan>> groups = plans.stream()
                .filter(Objects::nonNull)
                .filter(plan -> StringUtils.isNotBlank(plan.getLhMachineCode())
                        && !LhSingleControlMachineUtil.isSingleMouldMachine(plan.getLhMachineCode()))
                .filter(plan -> "L".equals(plan.getLeftRightMould()) || "R".equals(plan.getLeftRightMould()))
                .filter(plan -> MouldChangeTypeEnum.REGULAR.getCode().equals(plan.getChangeMouldType())
                        || MouldChangeTypeEnum.TYPE_BLOCK.getCode().equals(plan.getChangeMouldType()))
                .filter(plan -> StringUtils.isNotBlank(plan.getAfterMaterialCode())
                        && Objects.nonNull(plan.getPlanDate()) && StringUtils.isNotBlank(plan.getClassIndex()))
                .collect(Collectors.groupingBy(plan -> Arrays.asList(plan.getFactoryCode(),
                        plan.getLhResultBatchNo(), plan.getScheduleDate(), plan.getLhMachineCode(),
                        plan.getPlanDate(), plan.getClassIndex(), plan.getChangeTime(),
                        plan.getChangeMouldType(), plan.getCleanType(), plan.getAfterMaterialCode(),
                        plan.getProductStatus(), plan.getEndType()), LinkedHashMap::new, Collectors.toList()));
        Map<LhMouldChangePlan, LhMouldChangePlan> replacements = new IdentityHashMap<>();
        for (List<LhMouldChangePlan> group : groups.values()) {
            if (group.size() != 2) {
                continue;
            }
            LhMouldChangePlan first = group.get(0);
            LhMouldChangePlan second = group.get(1);
            boolean firstBeforeMissing = StringUtils.isBlank(first.getBeforeMaterialCode());
            boolean secondBeforeMissing = StringUtils.isBlank(second.getBeforeMaterialCode());
            if (StringUtils.equals(first.getLeftRightMould(), second.getLeftRightMould())
                    || firstBeforeMissing == secondBeforeMissing) {
                continue;
            }
            LhMouldChangePlan source = StringUtils.isNotBlank(first.getBeforeMaterialCode()) ? first : second;
            LhMouldChangePlan merged = BeanUtil.copyProperties(source, LhMouldChangePlan.class);
            merged.setLeftRightMould("LR");
            merged.setMouldCode(Stream.of(first, second).flatMap(plan ->
                            LhMouldCodeUtil.splitMouldCode(plan.getMouldCode()).stream())
                    .distinct().collect(Collectors.joining(",")));
            String sources = "左右模合并来源ID=" + first.getId() + "," + second.getId();
            // 来源ID写入可对账日志，保持非空前料记录的业务备注，避免合并后超过数据库字段长度。
            replacements.put(first, merged);
            replacements.put(second, merged);
            log.info("交替计划左右模来源合并, 工厂={}, 批次={}, 机台={}, 来源ID={}, 前料={}, 后料={}, 班次={}",
                    merged.getFactoryCode(), merged.getLhResultBatchNo(), merged.getLhMachineCode(),
                    sources, merged.getBeforeMaterialCode(), merged.getAfterMaterialCode(), merged.getClassIndex());
        }
        List<LhMouldChangePlan> normalized = new ArrayList<>(plans.size());
        Map<LhMouldChangePlan, Boolean> emitted = new IdentityHashMap<>();
        for (LhMouldChangePlan plan : plans) {
            LhMouldChangePlan replacement = replacements.get(plan);
            if (Objects.isNull(replacement)) {
                normalized.add(plan);
            } else if (Objects.isNull(emitted.put(replacement, Boolean.TRUE))) {
                normalized.add(replacement);
            }
        }
        return normalized;
    }
}
