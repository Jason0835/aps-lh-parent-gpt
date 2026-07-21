package com.zlt.aps.tc.service.loader;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.domain.entity.TcLossSetting;
import com.zlt.aps.tc.api.enums.TcYesNoEnum;
import com.zlt.aps.tc.engine.domain.TcLossRule;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import com.zlt.aps.tc.mapper.TcLossSettingMapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 胎侧自动排程损耗规则装载组件。
 *
 * <p>负责把启用的业务损耗配置转换为引擎运行态规则，不参与损耗率匹配和计划量计算。</p>
 */
public class TcLossRuleLoader {

    /**
     * 加载并转换损耗规则。
     *
     * @param context 自动排程上下文
     * @param lossSettingMapper 损耗配置 Mapper；为空时返回空集合以兼容无损耗配置场景
     * @return 引擎损耗规则集合
     * @throws IllegalArgumentException 自动排程上下文为空时抛出
     */
    public List<TcLossRule> load(TcScheduleContext context, TcLossSettingMapper lossSettingMapper) {
        if (context == null) {
            throw new IllegalArgumentException(I18nUtil.getMessage("ui.tc.schedule.contextEmpty"));
        }
        if (lossSettingMapper == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<TcLossSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcLossSetting::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TcLossSetting::getEnableStatus, TcYesNoEnum.YES.getCode());
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
    private TcLossRule toLossRule(TcLossSetting setting) {
        TcLossRule rule = new TcLossRule();
        rule.setFactoryCode(setting.getFactoryCode());
        rule.setSidewallCode(setting.getSidewallCode());
        rule.setMachineCode(setting.getMachineCode());
        rule.setLossRate(setting.getLossRate());
        rule.setPriority(setting.getPriority());
        return rule;
    }
}
