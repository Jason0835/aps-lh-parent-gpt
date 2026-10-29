package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.enums.LhScheduleSceneTypeEnum;
import com.zlt.aps.lh.api.enums.SkuScheduleSourceTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 按结果生成与正式来源绑定统一维护场景元数据，不改变排程决策及资源账本。
 *
 * @author APS
 * @date 2026/10/26
 */
public final class ScheduleResultSceneTypeSupport {

    /**
     * 限制为无状态公共方法使用。
     */
    private ScheduleResultSceneTypeSupport() {
    }

    /**
     * 写入当前真实入口确认的来源，保留已绑定的历史与结构切换身份。
     *
     * <p>历史指定承接优先于结构切换；普通构建与原机恢复不得覆盖上述精确绑定。
     * 本方法仅修改场景字段，不读取或改变排量、时间、动作类型及候选资格。</p>
     *
     * @param result 当前入口创建或绑定的结果，为空时不处理
     * @param sceneType 当前入口确认的来源，为空时不处理
     */
    public static void assign(LhScheduleResult result, LhScheduleSceneTypeEnum sceneType) {
        if (Objects.isNull(result) || Objects.isNull(sceneType)) {
            return;
        }
        LhScheduleSceneTypeEnum existing = LhScheduleSceneTypeEnum.fromCode(result.getSceneType());
        if (existing == LhScheduleSceneTypeEnum.PREVIOUS_ALTERNATE
                || (existing == LhScheduleSceneTypeEnum.STRUCTURE_SWITCH
                && sceneType != LhScheduleSceneTypeEnum.PREVIOUS_ALTERNATE)) {
            return;
        }
        result.setSceneType(sceneType.getCode());
    }

    /**
     * 在新增结果创建时冻结候选来源，续作派生身份优先于换块残量回流身份。
     *
     * @param context 当前运行态，为空时只读取候选显式来源
     * @param sku 本次实际消费的候选，为空时使用其他场景
     * @param result 新创建的新增结果，为空时不处理
     */
    public static void assignNewSpec(LhScheduleContext context, SkuScheduleDTO sku,
                                     LhScheduleResult result) {
        LhScheduleSceneTypeEnum sceneType = LhScheduleSceneTypeEnum.OTHER;
        if (Objects.nonNull(sku)) {
            String sourceType = sku.getSourceType();
            if (sku.isContinuousCompensationSku()
                    || SkuScheduleSourceTypeEnum.isContinuationAddMachine(sourceType)) {
                sceneType = LhScheduleSceneTypeEnum.CONTINUATION_NEW_SPEC;
            } else if (StringUtils.equals(SkuScheduleSourceTypeEnum.TYPE_BLOCK_TO_NEW_SPEC.getCode(), sourceType)
                    || (Objects.nonNull(context) && context.getTypeBlockResidualSceneSkuSet().contains(sku))) {
                sceneType = LhScheduleSceneTypeEnum.TYPE_BLOCK_NEW_SPEC;
            } else if (StringUtils.isEmpty(sourceType)
                    || StringUtils.equals(SkuScheduleSourceTypeEnum.NORMAL_NEW_SPEC.getCode(), sourceType)) {
                sceneType = LhScheduleSceneTypeEnum.NORMAL_NEW_SPEC;
            }
        }
        assign(result, sceneType);
    }

    /**
     * 记录已经成功换块且残量实际保留到新增流程的候选对象身份。
     *
     * <p>只在原有成功保留分支调用，不回写参与提前生产等决策的候选来源字段。
     * 失败预演由原尝试快照同步恢复此集合。</p>
     *
     * @param context 当前运行态，为空时不处理
     * @param sku 已成功换块并保留残量的候选，为空时不处理
     */
    public static void recordTypeBlockResidual(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.nonNull(context) && Objects.nonNull(sku)) {
            context.getTypeBlockResidualSceneSkuSet().add(sku);
        }
    }

    /**
     * 在新增保存入口补齐空值及未知编码，已有合法来源保持不变。
     *
     * @param result 即将新增保存的结果，为空时不处理
     */
    public static void ensureSceneType(LhScheduleResult result) {
        if (Objects.nonNull(result)) {
            result.setSceneType(normalizeCode(result.getSceneType()));
        }
    }

    /**
     * 将历史空值或未知编码归一为其他，保留合法两位编码。
     *
     * @param code 待兼容的来源编码
     * @return 合法场景编码，未匹配时为其他编码
     */
    public static String normalizeCode(String code) {
        return LhScheduleSceneTypeEnum.fromCode(code).getCode();
    }
}
