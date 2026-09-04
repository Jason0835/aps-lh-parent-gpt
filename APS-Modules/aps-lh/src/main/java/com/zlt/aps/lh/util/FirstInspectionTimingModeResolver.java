package com.zlt.aps.lh.util;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.enums.ConstructionStageEnum;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.Objects;

/**
 * 首检时间轴模式统一解析器。
 *
 * <p>普通切换总时长已包含首检，默认按切换完成时间倒推；试制、量试或硬性生产
 * 门禁晚于切换完成时才从生产就绪时间正向执行。调用方不得复制该判断。</p>
 */
public final class FirstInspectionTimingModeResolver {

    /** 普通换模动作。 */
    public static final String CHANGE_OVER_ACTION_MOULD_CHANGE = "MOULD_CHANGE";

    /** 普通换活字块动作。 */
    public static final String CHANGE_OVER_ACTION_TYPE_BLOCK_CHANGE = "TYPE_BLOCK_CHANGE";

    /** 完全新增场景。 */
    public static final String BUSINESS_SCENE_NEW_SPEC = "NEW_SPEC";

    /** 续作加机台场景。 */
    public static final String BUSINESS_SCENE_CONTINUATION_ADD_MACHINE =
            "CONTINUATION_ADD_MACHINE";

    /** 机台驱动候选预演场景。 */
    public static final String BUSINESS_SCENE_PREVIEW = "PREVIEW";

    /** 正式提交场景。 */
    public static final String BUSINESS_SCENE_COMMIT = "COMMIT";

    private static final String REASON_NORMAL_CHANGE_OVER =
            "普通切换总时长包含首检，按切换完成时间倒推";
    private static final String REASON_TRIAL_MASS_TRIAL =
            "试制或量试首检属于开产，从生产就绪时间正向执行";
    private static final String REASON_PRODUCTION_GATE =
            "硬性生产门禁晚于切换完成时间，首检随生产就绪时间正向执行";

    private FirstInspectionTimingModeResolver() {
    }

    /**
     * 解析首检时间模式。
     *
     * @param sku 当前SKU
     * @param scheduleType 排程类型
     * @param changeoverAction 切换动作
     * @param businessScene 业务场景
     * @param productionReadyTime 生产就绪或硬性生产门禁时间
     * @param changeoverEndTime 切换完成时间
     * @return 模式及原因
     */
    public static FirstInspectionTimingModeDecision resolve(SkuScheduleDTO sku,
                                                            String scheduleType,
                                                            String changeoverAction,
                                                            String businessScene,
                                                            Date productionReadyTime,
                                                            Date changeoverEndTime) {
        if (Objects.nonNull(productionReadyTime) && Objects.nonNull(changeoverEndTime)
                && productionReadyTime.after(changeoverEndTime)) {
            return FirstInspectionTimingModeDecision.of(
                    FirstInspectionTimingMode.START_AT_PRODUCTION_READY, REASON_PRODUCTION_GATE);
        }
        if (Objects.nonNull(sku)
                && (FirstInspectionQtyUtil.isMassTrialQuantityFirstInspection(sku, scheduleType)
                || isTrialSku(sku, scheduleType))) {
            return FirstInspectionTimingModeDecision.of(
                    FirstInspectionTimingMode.START_AT_PRODUCTION_READY, REASON_TRIAL_MASS_TRIAL);
        }
        return FirstInspectionTimingModeDecision.of(
                FirstInspectionTimingMode.INCLUDED_IN_CHANGEOVER,
                buildNormalReason(changeoverAction, businessScene));
    }

    /**
     * 判断试制新增或换活字块场景。
     *
     * @param sku 当前SKU
     * @param scheduleType 排程类型
     * @return true-试制时间语义
     */
    private static boolean isTrialSku(SkuScheduleDTO sku, String scheduleType) {
        return StringUtils.isNotBlank(scheduleType)
                && StringUtils.isNotBlank(sku.getConstructionStage())
                && ConstructionStageEnum.TRIAL.getCode().equals(sku.getConstructionStage())
                && ("02".equals(scheduleType) || "03".equals(scheduleType));
    }

    /**
     * 构建普通模式原因，保留业务动作与场景用于日志对账。
     *
     * @param changeoverAction 切换动作
     * @param businessScene 业务场景
     * @return 模式原因
     */
    private static String buildNormalReason(String changeoverAction, String businessScene) {
        return REASON_NORMAL_CHANGE_OVER
                + ", action=" + StringUtils.defaultIfBlank(changeoverAction, "-")
                + ", scene=" + StringUtils.defaultIfBlank(businessScene, "-");
    }
}
