package com.zlt.aps.tc.service.loader;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcParams;
import com.zlt.aps.tc.api.enums.TcParamValueSourceEnum;
import com.zlt.aps.tc.api.enums.TcScheduleStrategyEnum;
import com.zlt.aps.tc.api.enums.TcVersionMatchModeEnum;
import com.zlt.aps.tc.api.enums.TcYesNoEnum;
import com.zlt.aps.tc.engine.domain.TcParamValue;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import com.zlt.aps.tc.mapper.TcParamsMapper;
import com.zlt.aps.tc.service.cache.TcAutoScheduleRedisCacheService;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎侧自动排程参数装载组件。
 *
 * <p>负责读取工厂启用参数、补充排程默认值并生成单次排程参数快照。该组件不修改任务、
 * 机台或排程结果，参数 Redis 缓存键和回源行为继续由既有缓存服务统一管理。</p>
 */
public class TcScheduleParamLoader {

    /**
     * 加载参数并写入自动排程上下文。
     *
     * @param context 自动排程上下文，必须已设置工厂编码
     * @param paramsMapper 参数 Mapper
     * @param cacheService 自动排程基础资料 Redis 缓存服务
     * @throws IllegalArgumentException Mapper、缓存服务或上下文为空时抛出
     */
    public void load(TcScheduleContext context, TcParamsMapper paramsMapper,
                     TcAutoScheduleRedisCacheService cacheService) {
        if (context == null) {
            throw new IllegalArgumentException(I18nUtil.getMessage("ui.tc.schedule.contextEmpty"));
        }
        if (paramsMapper == null) {
            throw new IllegalArgumentException(I18nUtil.getMessage("ui.tc.schedule.paramMapperEmpty"));
        }
        if (cacheService == null) {
            throw new IllegalArgumentException(I18nUtil.getMessage("ui.tc.schedule.cacheServiceEmpty"));
        }
        LambdaQueryWrapper<TcParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcParams::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TcParams::getEnableStatus, TcYesNoEnum.YES.getCode());
        List<TcParams> paramsList = cacheService.getCachedList(
                "params:" + context.getFactoryCode(),
                () -> paramsMapper.selectList(wrapper));
        Map<String, TcParamValue> paramMap = new HashMap<>();
        if (CollUtil.isNotEmpty(paramsList)) {
            paramsList.stream()
                    .filter(params -> params != null && StrUtil.isNotBlank(params.getParamCode()))
                    .forEach(params -> paramMap.put(params.getParamCode(), this.toParamValue(params)));
        }
        this.fillDefaultParams(paramMap);
        this.validateFilterRuleOrder(paramMap.get(TcScheduleConstants.PARAM_FILTER_RULE_ORDER));
        context.setParamMap(paramMap);
        context.configureProcessLogLevel(paramMap.get(TcScheduleConstants.PARAM_PROCESS_LOG_LEVEL).getEffectiveValue());
        context.setSmallGlueCodeSet(this.parseSmallGlueCodes(
                paramMap.get(TcScheduleConstants.PARAM_SMALL_GLUE_CODES)));
    }

    /**
     * 在参数装载阶段校验过滤规则顺序，避免错误配置到候选计算时才暴露。
     *
     * @param paramValue 过滤规则顺序参数
     * @throws ServiceException 存在未知或重复规则编码时抛出
     */
    private void validateFilterRuleOrder(TcParamValue paramValue) {
        String configuredOrder = paramValue == null ? null : paramValue.getEffectiveValue();
        Set<String> supportedRuleSet = Arrays.stream(TcScheduleConstants.DEFAULT_FILTER_RULE_ORDER.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> configuredRuleSet = new HashSet<>();
        Arrays.stream(StrUtil.blankToDefault(configuredOrder,
                        TcScheduleConstants.DEFAULT_FILTER_RULE_ORDER).split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(String::toUpperCase)
                .forEach(ruleCode -> {
                    if (!supportedRuleSet.contains(ruleCode) || !configuredRuleSet.add(ruleCode)) {
                        throw new ServiceException(MessageFormat.format(
                                I18nUtil.getMessage("ui.tc.schedule.filterRuleOrderInvalid"), ruleCode));
                    }
                });
    }

    /**
     * 将数据库参数转换为运行态参数快照。
     *
     * @param params 数据库参数实体
     * @return 运行态参数快照
     */
    private TcParamValue toParamValue(TcParams params) {
        TcParamValue value = new TcParamValue();
        value.setParamCode(params.getParamCode());
        value.setParamValue(params.getParamValue());
        value.setDefaultValue(params.getDefaultValue());
        value.setSource(TcParamValueSourceEnum.TABLE.getCode());
        return value;
    }

    /**
     * 补充当前排程支持的全部默认参数。
     *
     * @param paramMap 参数快照映射
     */
    private void fillDefaultParams(Map<String, TcParamValue> paramMap) {
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_ALGORITHM_SWITCH,
                TcScheduleConstants.DEFAULT_ALGORITHM_SWITCH);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_ALG1_LOOKBACK_SHIFTS,
                String.valueOf(TcScheduleConstants.DEFAULT_ALG1_LOOKBACK_SHIFTS_VALUE));
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_MIN_STOCK_CLASS,
                TcScheduleConstants.DEFAULT_MIN_STOCK_CLASS);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_MIN_START_QTY,
                TcScheduleConstants.DEFAULT_MIN_START_QTY);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_DEFAULT_CURL_LENGTH,
                TcScheduleConstants.DEFAULT_CURL_LENGTH);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_TOOL_TOTAL_QTY,
                TcScheduleConstants.DEFAULT_TOOL_TOTAL_QTY);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_SHUTDOWN_REDISTRIBUTION_ENABLED,
                TcScheduleConstants.DEFAULT_SHUTDOWN_REDISTRIBUTION_ENABLED);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_PLAN_QTY_STRATEGY,
                TcScheduleStrategyEnum.DEFAULT.getCode());
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_TASK_SORT_STRATEGY,
                TcScheduleStrategyEnum.DEFAULT.getCode());
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_PROCESS_LOG_LEVEL,
                TcScheduleConstants.DEFAULT_PROCESS_LOG_LEVEL);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_NEW_SPEC_LOOKBACK_DAYS,
                TcScheduleConstants.DEFAULT_NEW_SPEC_LOOKBACK_DAYS);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_NEW_SPEC_ADVANCE_SHIFT_COUNT,
                TcScheduleConstants.DEFAULT_NEW_SPEC_ADVANCE_SHIFT_COUNT);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_EXPERIMENT_SPEC_LOOKBACK_DAYS,
                TcScheduleConstants.DEFAULT_EXPERIMENT_SPEC_LOOKBACK_DAYS);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_EXPERIMENT_SPEC_PLAN_QTY,
                TcScheduleConstants.DEFAULT_EXPERIMENT_SPEC_PLAN_QTY);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_FORMING_SHIFT_OFFSET,
                TcScheduleConstants.DEFAULT_FORMING_SHIFT_OFFSET);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_SMALL_GLUE_CODES,
                TcScheduleConstants.DEFAULT_SMALL_GLUE_CODES);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_VERSION_MATCH_MODE,
                TcVersionMatchModeEnum.RECIPE.getCode());
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_MACHINE_FILTER_STRATEGY,
                TcScheduleStrategyEnum.DEFAULT.getCode());
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_MACHINE_SCORE_STRATEGY,
                TcScheduleStrategyEnum.DEFAULT.getCode());
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_CHAIN_TASK_PRIORITY_STRATEGY,
                TcScheduleStrategyEnum.CONTINUITY_FIRST.getCode());
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_PROCESS_STANDING_HOURS,
                TcScheduleConstants.DEFAULT_PROCESS_STANDING_HOURS);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_DEFAULT_PRODUCTION_SPEED,
                TcScheduleConstants.DEFAULT_PRODUCTION_SPEED);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_SHUTDOWN_CHECK_WINDOW,
                TcScheduleConstants.DEFAULT_SHUTDOWN_CHECK_WINDOW);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_OPEN_SHIFT_THRESHOLD,
                TcScheduleConstants.DEFAULT_OPEN_SHIFT_THRESHOLD);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_SPEC_CHANGE_MINUTES,
                TcScheduleConstants.DEFAULT_SPEC_CHANGE_MINUTES);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_GLUE_CHANGE_CAPACITY_DEDUCT,
                TcScheduleConstants.DEFAULT_GLUE_CHANGE_CAPACITY_DEDUCT);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_VEHICLE_RATE,
                TcScheduleConstants.DEFAULT_VEHICLE_RATE);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_DEFAULT_LOSS_RATE,
                TcScheduleConstants.DEFAULT_LOSS_RATE);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_STOCK_MISSING_POLICY,
                TcScheduleConstants.DEFAULT_STOCK_MISSING_POLICY);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_FILTER_RULE_ORDER,
                TcScheduleConstants.DEFAULT_FILTER_RULE_ORDER);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_SCORE_WEIGHT_REMAIN_CAP,
                TcScheduleConstants.DEFAULT_SCORE_WEIGHT_REMAIN_CAP);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_SCORE_WEIGHT_GLUE_CONT,
                TcScheduleConstants.DEFAULT_SCORE_WEIGHT_GLUE_CONT);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_SCORE_WEIGHT_BASE_GLUE,
                TcScheduleConstants.DEFAULT_SCORE_WEIGHT_BASE_GLUE);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_SCORE_WEIGHT_MOUTH_CONT,
                TcScheduleConstants.DEFAULT_SCORE_WEIGHT_MOUTH_CONT);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_SCORE_WEIGHT_SWITCH_COST,
                TcScheduleConstants.DEFAULT_SCORE_WEIGHT_SWITCH_COST);
        this.putDefaultParam(paramMap, TcScheduleConstants.PARAM_SCORE_WEIGHT_FIXED_MACHINE,
                TcScheduleConstants.DEFAULT_SCORE_WEIGHT_FIXED_MACHINE);
    }

    /**
     * 在参数未配置时写入默认快照。
     *
     * @param paramMap 参数快照映射
     * @param paramCode 参数编码
     * @param defaultValue 默认值
     */
    private void putDefaultParam(Map<String, TcParamValue> paramMap, String paramCode, String defaultValue) {
        if (paramMap.containsKey(paramCode)) {
            return;
        }
        TcParamValue value = new TcParamValue();
        value.setParamCode(paramCode);
        value.setDefaultValue(defaultValue);
        value.setSource(TcParamValueSourceEnum.DEFAULT.getCode());
        paramMap.put(paramCode, value);
    }

    /**
     * 解析小胶种参数编码集合。
     *
     * @param paramValue 参数快照值
     * @return 去重并保持配置顺序的小胶种编码集合
     */
    private Set<String> parseSmallGlueCodes(TcParamValue paramValue) {
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
