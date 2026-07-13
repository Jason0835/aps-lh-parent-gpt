package com.zlt.aps.tm.service.loader;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.tm.api.domain.entity.TmParams;
import com.zlt.aps.tm.engine.domain.TmParamValue;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.mapper.TmParamsMapper;
import com.zlt.aps.tm.service.TmAutoScheduleRedisCacheService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎面自动排程参数装载组件。
 *
 * <p>负责读取工厂启用参数、补充排程默认值并生成单次排程参数快照。该组件不修改任务、
 * 机台或排程结果，参数 Redis 缓存键和回源行为继续由既有缓存服务统一管理。</p>
 */
public class TmScheduleParamLoader {

    private static final String PARAM_ALGORITHM_SWITCH = "TM_ALGORITHM_SWITCH";

    private static final String PARAM_MIN_STOCK_CLASS = "TM_MIN_STOCK_CLASS";

    private static final String PARAM_MIN_START_QTY = "TM_MIN_START_QTY";

    private static final String PARAM_DEFAULT_CURL_LENGTH = "TM_DEFAULT_CURL_LENGTH";

    private static final String PARAM_TOOL_TOTAL_QTY = "TM_TOOL_TOTAL_QTY";

    private static final String PARAM_SHUTDOWN_REDISTRIBUTION_ENABLED = "TM_SHUTDOWN_REDISTRIBUTION_ENABLED";

    private static final String PARAM_PLAN_QTY_STRATEGY = "TM_PLAN_QTY_STRATEGY";

    private static final String PARAM_TASK_SORT_STRATEGY = "TM_TASK_SORT_STRATEGY";

    private static final String PARAM_NEW_SPEC_LOOKBACK_DAYS = "TM_NEW_SPEC_LOOKBACK_DAYS";

    private static final String PARAM_NEW_SPEC_ADVANCE_SHIFT_COUNT = "TM_NEW_SPEC_ADVANCE_SHIFT_COUNT";

    private static final String PARAM_EXPERIMENT_SPEC_LOOKBACK_DAYS = "TM_EXPERIMENT_SPEC_LOOKBACK_DAYS";

    private static final String PARAM_EXPERIMENT_SPEC_PLAN_QTY = "TM_EXPERIMENT_SPEC_PLAN_QTY";

    private static final String PARAM_FORMING_SHIFT_OFFSET = "TM_FORMING_SHIFT_OFFSET";

    private static final String PARAM_SMALL_GLUE_CODES = "TM_SMALL_GLUE_CODES";

    private static final String PARAM_VERSION_MATCH_MODE = "TM_VERSION_MATCH_MODE";

    private static final String PARAM_MACHINE_FILTER_STRATEGY = "TM_MACHINE_FILTER_STRATEGY";

    private static final String PARAM_MACHINE_SCORE_STRATEGY = "TM_MACHINE_SCORE_STRATEGY";

    private static final String PARAM_CHAIN_TASK_PRIORITY_STRATEGY = "TM_CHAIN_TASK_PRIORITY_STRATEGY";

    private static final String VERSION_MATCH_MODE_RECIPE = "RECIPE";

    private static final String ENABLED = "1";

    /**
     * 加载参数并写入自动排程上下文。
     *
     * @param context 自动排程上下文，必须已设置工厂编码
     * @param paramsMapper 参数 Mapper
     * @param cacheService 自动排程基础资料 Redis 缓存服务
     * @throws IllegalArgumentException Mapper、缓存服务或上下文为空时抛出
     */
    public void load(TmScheduleContext context, TmParamsMapper paramsMapper,
                     TmAutoScheduleRedisCacheService cacheService) {
        if (context == null) {
            throw new IllegalArgumentException("自动排程上下文不能为空");
        }
        if (paramsMapper == null) {
            throw new IllegalArgumentException("胎面排程参数Mapper不能为空");
        }
        if (cacheService == null) {
            throw new IllegalArgumentException("胎面自动排程缓存服务不能为空");
        }
        LambdaQueryWrapper<TmParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmParams::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TmParams::getEnableStatus, ENABLED);
        List<TmParams> paramsList = cacheService.getCachedList("params:" + context.getFactoryCode(),
                () -> paramsMapper.selectList(wrapper));
        Map<String, TmParamValue> paramMap = new HashMap<>();
        if (CollUtil.isNotEmpty(paramsList)) {
            paramsList.stream()
                    .filter(params -> params != null && StrUtil.isNotBlank(params.getParamCode()))
                    .forEach(params -> paramMap.put(params.getParamCode(), this.toParamValue(params)));
        }
        this.fillDefaultParams(paramMap);
        context.setParamMap(paramMap);
        context.setSmallGlueCodeSet(this.parseSmallGlueCodes(paramMap.get(PARAM_SMALL_GLUE_CODES)));
    }

    /**
     * 将数据库参数转换为运行态参数快照。
     *
     * @param params 数据库参数实体
     * @return 运行态参数快照
     */
    private TmParamValue toParamValue(TmParams params) {
        TmParamValue value = new TmParamValue();
        value.setParamCode(params.getParamCode());
        value.setParamValue(params.getParamValue());
        value.setDefaultValue(params.getDefaultValue());
        value.setSource("T_TM_PARAMS");
        return value;
    }

    /**
     * 补充当前排程支持的全部默认参数。
     *
     * @param paramMap 参数快照映射
     */
    private void fillDefaultParams(Map<String, TmParamValue> paramMap) {
        this.putDefaultParam(paramMap, PARAM_ALGORITHM_SWITCH, "1");
        this.putDefaultParam(paramMap, PARAM_MIN_STOCK_CLASS, "1");
        this.putDefaultParam(paramMap, PARAM_MIN_START_QTY, "0");
        this.putDefaultParam(paramMap, PARAM_DEFAULT_CURL_LENGTH, "0");
        this.putDefaultParam(paramMap, PARAM_TOOL_TOTAL_QTY, "0");
        this.putDefaultParam(paramMap, PARAM_SHUTDOWN_REDISTRIBUTION_ENABLED, "1");
        this.putDefaultParam(paramMap, PARAM_PLAN_QTY_STRATEGY, "DEFAULT");
        this.putDefaultParam(paramMap, PARAM_TASK_SORT_STRATEGY, "DEFAULT");
        this.putDefaultParam(paramMap, PARAM_NEW_SPEC_LOOKBACK_DAYS, "7");
        this.putDefaultParam(paramMap, PARAM_NEW_SPEC_ADVANCE_SHIFT_COUNT, "2");
        this.putDefaultParam(paramMap, PARAM_EXPERIMENT_SPEC_LOOKBACK_DAYS, "5");
        this.putDefaultParam(paramMap, PARAM_EXPERIMENT_SPEC_PLAN_QTY, "30");
        this.putDefaultParam(paramMap, PARAM_FORMING_SHIFT_OFFSET, "2");
        this.putDefaultParam(paramMap, PARAM_SMALL_GLUE_CODES, "");
        this.putDefaultParam(paramMap, PARAM_VERSION_MATCH_MODE, VERSION_MATCH_MODE_RECIPE);
        this.putDefaultParam(paramMap, PARAM_MACHINE_FILTER_STRATEGY, "DEFAULT");
        this.putDefaultParam(paramMap, PARAM_MACHINE_SCORE_STRATEGY, "DEFAULT");
        this.putDefaultParam(paramMap, PARAM_CHAIN_TASK_PRIORITY_STRATEGY, "CONTINUITY_FIRST");
    }

    /**
     * 在参数未配置时写入默认快照。
     *
     * @param paramMap 参数快照映射
     * @param paramCode 参数编码
     * @param defaultValue 默认值
     */
    private void putDefaultParam(Map<String, TmParamValue> paramMap, String paramCode, String defaultValue) {
        if (paramMap.containsKey(paramCode)) {
            return;
        }
        TmParamValue value = new TmParamValue();
        value.setParamCode(paramCode);
        value.setDefaultValue(defaultValue);
        value.setSource("DEFAULT");
        paramMap.put(paramCode, value);
    }

    /**
     * 解析小胶种参数编码集合。
     *
     * @param paramValue 参数快照值
     * @return 去重并保持配置顺序的小胶种编码集合
     */
    private Set<String> parseSmallGlueCodes(TmParamValue paramValue) {
        if (paramValue == null) {
            return new LinkedHashSet<>();
        }
        String effectiveValue = StrUtil.blankToDefault(paramValue.getParamValue(), paramValue.getDefaultValue());
        if (StrUtil.isBlank(effectiveValue)) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(effectiveValue.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
