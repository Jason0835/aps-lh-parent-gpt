package com.zlt.aps.tm.service.loader;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.tm.api.domain.entity.TmLossSetting;
import com.zlt.aps.tm.engine.domain.TmLossRule;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.mapper.TmLossSettingMapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 胎面自动排程损耗规则装载组件。
 *
 * <p>负责把启用的业务损耗配置转换为引擎运行态规则，不参与损耗率匹配和计划量计算。</p>
 */
public class TmLossRuleLoader {

    private static final String ENABLED = "1";

    /**
     * 加载并转换损耗规则。
     *
     * @param context 自动排程上下文
     * @param lossSettingMapper 损耗配置 Mapper；为空时返回空集合以兼容无损耗配置场景
     * @return 引擎损耗规则集合
     * @throws IllegalArgumentException 自动排程上下文为空时抛出
     */
    public List<TmLossRule> load(TmScheduleContext context, TmLossSettingMapper lossSettingMapper) {
        if (context == null) {
            throw new IllegalArgumentException("自动排程上下文不能为空");
        }
        if (lossSettingMapper == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<TmLossSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmLossSetting::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TmLossSetting::getEnableStatus, ENABLED);
        return Optional.ofNullable(lossSettingMapper.selectList(wrapper)).orElse(Collections.emptyList())
                .stream()
                .map(this::toLossRule)
                .collect(Collectors.toList());
    }

    /**
     * 将业务损耗配置转换为引擎规则。
     *
     * @param setting 损耗配置实体
     * @return 引擎损耗规则
     */
    private TmLossRule toLossRule(TmLossSetting setting) {
        TmLossRule rule = new TmLossRule();
        rule.setFactoryCode(setting.getFactoryCode());
        rule.setTreadCode(setting.getTreadCode());
        rule.setMachineCode(setting.getMachineCode());
        rule.setLossRate(setting.getLossRate());
        rule.setPriority(setting.getPriority());
        return rule;
    }
}
