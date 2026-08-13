package com.zlt.aps.tm.service.loader;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.entity.TmParams;
import com.zlt.aps.tm.api.enums.TmParamValueSourceEnum;
import com.zlt.aps.tm.api.enums.TmScheduleStrategyEnum;
import com.zlt.aps.tm.api.enums.TmVersionMatchModeEnum;
import com.zlt.aps.tm.api.enums.TmYesNoEnum;
import com.zlt.aps.tm.engine.domain.TmParamValue;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.mapper.TmParamsMapper;
import com.zlt.aps.tm.service.cache.TmAutoScheduleRedisCacheService;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎面自动排程参数装载组件。
 *
 * <p>负责读取工厂启用参数、补充排程默认值并生成单次排程参数快照。该组件不修改任务、
 * 机台或排程结果，参数 Redis 缓存键和回源行为继续由既有缓存服务统一管理。</p>
 */
public class TmScheduleParamLoader {

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
        wrapper.eq(TmParams::getEnableStatus, TmYesNoEnum.YES.getCode());
        List<TmParams> paramsList = cacheService.getCachedList("params:" + context.getFactoryCode(),
                () -> paramsMapper.selectList(wrapper));
        Map<String, TmParamValue> paramMap = new HashMap<>();
        if (CollUtil.isNotEmpty(paramsList)) {
            paramsList.stream()
                    .filter(params -> params != null && StrUtil.isNotBlank(params.getParamCode()))
                    .forEach(params -> paramMap.put(params.getParamCode(), this.toParamValue(params)));
        }
        this.fillDefaultParams(paramMap);
        this.validateFilterRuleOrder(paramMap.get(TmScheduleConstants.PARAM_FILTER_RULE_ORDER));
        context.setParamMap(paramMap);
        context.configureProcessLogLevel(paramMap.get(TmScheduleConstants.PARAM_PROCESS_LOG_LEVEL).getEffectiveValue());
        context.setSmallGlueCodeSet(this.parseSmallGlueCodes(
                paramMap.get(TmScheduleConstants.PARAM_SMALL_GLUE_CODES)));
    }

    /**
     * 在参数装载阶段校验过滤规则顺序，避免错误配置到候选计算时才暴露。
     *
     * @param paramValue 过滤规则顺序参数
     * @throws ServiceException 存在未知或重复规则编码时抛出
     */
    private void validateFilterRuleOrder(TmParamValue paramValue) {
        String configuredOrder = paramValue == null ? null : paramValue.getEffectiveValue();
        Set<String> supportedRuleSet = Arrays.stream(TmScheduleConstants.DEFAULT_FILTER_RULE_ORDER.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> configuredRuleSet = new HashSet<>();
        Arrays.stream(StrUtil.blankToDefault(configuredOrder,
                        TmScheduleConstants.DEFAULT_FILTER_RULE_ORDER).split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(String::toUpperCase)
                .forEach(ruleCode -> {
                    if (!supportedRuleSet.contains(ruleCode) || !configuredRuleSet.add(ruleCode)) {
                        throw new ServiceException(MessageFormat.format(
                                I18nUtil.getMessage("ui.tm.schedule.filterRuleOrderInvalid"), ruleCode));
                    }
                });
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
        value.setSource(TmParamValueSourceEnum.TABLE.getCode());
        return value;
    }

    /**
     * 补充当前排程支持的全部默认参数。
     *
     * @param paramMap 参数快照映射
     */
    private void fillDefaultParams(Map<String, TmParamValue> paramMap) {
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_ALGORITHM_SWITCH,
                TmScheduleConstants.DEFAULT_ALGORITHM_SWITCH);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_MIN_STOCK_CLASS,
                TmScheduleConstants.DEFAULT_MIN_STOCK_CLASS);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_MIN_START_QTY,
                TmScheduleConstants.DEFAULT_MIN_START_QTY);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_DEFAULT_CURL_LENGTH,
                TmScheduleConstants.DEFAULT_CURL_LENGTH);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_TOOL_TOTAL_QTY,
                TmScheduleConstants.DEFAULT_TOOL_TOTAL_QTY);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_SHUTDOWN_REDISTRIBUTION_ENABLED,
                TmScheduleConstants.DEFAULT_SHUTDOWN_REDISTRIBUTION_ENABLED);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_PLAN_QTY_STRATEGY,
                TmScheduleStrategyEnum.DEFAULT.getCode());
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_TASK_SORT_STRATEGY,
                TmScheduleStrategyEnum.DEFAULT.getCode());
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_PROCESS_LOG_LEVEL,
                TmScheduleConstants.DEFAULT_PROCESS_LOG_LEVEL);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_NEW_SPEC_LOOKBACK_DAYS,
                TmScheduleConstants.DEFAULT_NEW_SPEC_LOOKBACK_DAYS);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_NEW_SPEC_ADVANCE_SHIFT_COUNT,
                TmScheduleConstants.DEFAULT_NEW_SPEC_ADVANCE_SHIFT_COUNT);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_EXPERIMENT_SPEC_LOOKBACK_DAYS,
                TmScheduleConstants.DEFAULT_EXPERIMENT_SPEC_LOOKBACK_DAYS);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_EXPERIMENT_SPEC_PLAN_QTY,
                TmScheduleConstants.DEFAULT_EXPERIMENT_SPEC_PLAN_QTY);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_FORMING_SHIFT_OFFSET,
                TmScheduleConstants.DEFAULT_FORMING_SHIFT_OFFSET);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_SMALL_GLUE_CODES,
                TmScheduleConstants.DEFAULT_SMALL_GLUE_CODES);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_VERSION_MATCH_MODE,
                TmVersionMatchModeEnum.RECIPE.getCode());
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_MACHINE_FILTER_STRATEGY,
                TmScheduleStrategyEnum.DEFAULT.getCode());
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_MACHINE_SCORE_STRATEGY,
                TmScheduleStrategyEnum.DEFAULT.getCode());
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_CHAIN_TASK_PRIORITY_STRATEGY,
                TmScheduleStrategyEnum.CONTINUITY_FIRST.getCode());
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_PROCESS_STANDING_HOURS,
                TmScheduleConstants.DEFAULT_PROCESS_STANDING_HOURS);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_DEFAULT_PRODUCTION_SPEED,
                TmScheduleConstants.DEFAULT_PRODUCTION_SPEED);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_SHUTDOWN_CHECK_WINDOW,
                TmScheduleConstants.DEFAULT_SHUTDOWN_CHECK_WINDOW);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_OPEN_SHIFT_THRESHOLD,
                TmScheduleConstants.DEFAULT_OPEN_SHIFT_THRESHOLD);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_SPEC_CHANGE_MINUTES,
                TmScheduleConstants.DEFAULT_SPEC_CHANGE_MINUTES);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_GLUE_CHANGE_CAPACITY_DEDUCT,
                TmScheduleConstants.DEFAULT_GLUE_CHANGE_CAPACITY_DEDUCT);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_VEHICLE_RATE,
                TmScheduleConstants.DEFAULT_VEHICLE_RATE);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_DEFAULT_LOSS_RATE,
                TmScheduleConstants.DEFAULT_LOSS_RATE);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_STOCK_MISSING_POLICY,
                TmScheduleConstants.DEFAULT_STOCK_MISSING_POLICY);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_FILTER_RULE_ORDER,
                TmScheduleConstants.DEFAULT_FILTER_RULE_ORDER);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_SCORE_WEIGHT_REMAIN_CAP,
                TmScheduleConstants.DEFAULT_SCORE_WEIGHT_REMAIN_CAP);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_SCORE_WEIGHT_GLUE_CONT,
                TmScheduleConstants.DEFAULT_SCORE_WEIGHT_GLUE_CONT);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_SCORE_WEIGHT_BASE_GLUE,
                TmScheduleConstants.DEFAULT_SCORE_WEIGHT_BASE_GLUE);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_SCORE_WEIGHT_MOUTH_CONT,
                TmScheduleConstants.DEFAULT_SCORE_WEIGHT_MOUTH_CONT);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_SCORE_WEIGHT_SWITCH_COST,
                TmScheduleConstants.DEFAULT_SCORE_WEIGHT_SWITCH_COST);
        this.putDefaultParam(paramMap, TmScheduleConstants.PARAM_SCORE_WEIGHT_FIXED_MACHINE,
                TmScheduleConstants.DEFAULT_SCORE_WEIGHT_FIXED_MACHINE);
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
        value.setSource(TmParamValueSourceEnum.DEFAULT.getCode());
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
