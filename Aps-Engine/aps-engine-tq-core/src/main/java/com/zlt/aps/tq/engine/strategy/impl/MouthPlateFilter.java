package com.zlt.aps.tq.engine.strategy.impl;

import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.strategy.IMachineFilterStrategy;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 口型板过滤策略。
 *
 * <p>过滤规则：如果胎圈的口型板代码在"口型板→机台"映射中存在，
 * 则只保留口型板对应的机台，同时需要排除"不可作业"中的机台。</p>
 *
 * <p>该策略在定点机台过滤之后执行，作为定点机台未匹配时的兜底方案。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class MouthPlateFilter implements IMachineFilterStrategy {

    @Override
    public List<TqMachineInfo> filter(List<TqMachineInfo> candidateMachines, TqScheduleResultVo scheduleVo, TqScheduleContext context) {
        String mouthPlateCode = scheduleVo.getMouthPlateCode();
        if (mouthPlateCode == null || mouthPlateCode.isEmpty()) {
            return candidateMachines;
        }

        String mouthPlateMachineCodes = context.getMouthPlateMachineMap().get(mouthPlateCode);
        if (mouthPlateMachineCodes == null || mouthPlateMachineCodes.isEmpty()) {
            return candidateMachines;
        }

        List<String> mouthPlateCodeList = Arrays.asList(mouthPlateMachineCodes.split(","));

        // 过滤出口型板对应的机台
        List<TqMachineInfo> filtered = candidateMachines.stream()
                .filter(m -> mouthPlateCodeList.contains(m.getMachineCode()))
                .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(filtered)) {
            // 排除不可作业的机台
            String notMachineCodes = context.getSpecifyNotMachineMap().get(scheduleVo.getBeadCode());
            if (notMachineCodes != null && !notMachineCodes.isEmpty()) {
                List<String> notCodeList = Arrays.asList(notMachineCodes.split(","));
                filtered = filtered.stream()
                        .filter(m -> !notCodeList.contains(m.getMachineCode()))
                        .collect(Collectors.toList());
            }

            log.debug("[口型板过滤] 口型板{}对应机台, 候选机台从{}个过滤到{}个", mouthPlateCode, candidateMachines.size(), filtered.size());
            return filtered;
        }

        return candidateMachines;
    }

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public String getStrategyName() {
        return "口型板过滤";
    }
}
