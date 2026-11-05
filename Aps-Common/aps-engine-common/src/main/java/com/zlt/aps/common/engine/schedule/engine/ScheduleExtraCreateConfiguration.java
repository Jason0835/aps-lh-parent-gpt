package com.zlt.aps.common.engine.schedule.engine;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 工厂级备库新行开关快照；缺少配置时默认开启，显式配置必须唯一且为1才开启。
 */
public final class ScheduleExtraCreateConfiguration {

    private ScheduleExtraCreateConfiguration() {
    }

    /**
     * 从尚未转Map的有效参数列表冻结开关，防止重复配置被查询顺序覆盖；缺少配置时采用开启默认值。
     *
     * @param enabledParams 已按工厂和启用状态筛选的参数
     * @param paramCode     模块新行参数编码
     * @return 有效值为0或1的独立快照；source保留默认或关闭原因，不回写配置表
     */
    public static ScheduleParamValueModel freeze(List<ScheduleParamValueModel> enabledParams, String paramCode) {
        List<ScheduleParamValueModel> matches = (enabledParams == null
                ? java.util.Collections.<ScheduleParamValueModel>emptyList() : enabledParams).stream().filter(Objects::nonNull)
                .filter(param -> Objects.equals(paramCode, param.getParamCode())).collect(Collectors.toList());
        ScheduleParamValueModel result = new ScheduleParamValueModel();
        result.setParamCode(paramCode);
        result.setDefaultValue("1");
        result.setConfiguredValues(matches.stream().map(ScheduleParamValueModel::getParamValue)
                .collect(Collectors.toList()));
        boolean enabled = matches.isEmpty()
                || matches.size() == 1 && "1".equals(matches.get(0).getParamValue());
        result.setParamValue(enabled ? "1" : "0");
        result.setSource(matches.isEmpty() ? "EXTRA_CREATE_DEFAULT_ENABLED"
                : matches.size() > 1 ? "EXTRA_CREATE_DUPLICATE"
                : enabled ? "EXTRA_CREATE_ENABLED" : "EXTRA_CREATE_DISABLED_OR_INVALID");
        return result;
    }
}
