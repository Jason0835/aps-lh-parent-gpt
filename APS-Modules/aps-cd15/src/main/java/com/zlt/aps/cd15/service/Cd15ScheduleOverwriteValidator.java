package com.zlt.aps.cd15.service;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.model.Cd15ScheduleOverwriteDecision;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/** 判断同日期旧斜裁排程是否允许自动排程覆盖。 */
@Component
public class Cd15ScheduleOverwriteValidator {

    private static final String LOCKED = "1";
    private static final String RELEASED_SUCCESS = "1";

    /**
     * 校验旧结果覆盖规则。
     *
     * @param existingResults 同日期旧结果
     * @param forceRegenerate 是否已由用户确认覆盖
     * @return 覆盖决策
     */
    public Cd15ScheduleOverwriteDecision validate(List<Cd15ScheduleResult> existingResults,
                                                  boolean forceRegenerate) {
        List<Cd15ScheduleResult> results = existingResults == null
                ? Collections.emptyList() : existingResults;
        if (results.isEmpty()) {
            return this.allowed();
        }
        for (Cd15ScheduleResult result : results) {
            String conflictMessage = this.conflict(result);
            if (conflictMessage != null) {
                return Cd15ScheduleOverwriteDecision.builder()
                        .rejected(true)
                        .message(conflictMessage)
                        .build();
            }
        }
        if (!forceRegenerate) {
            return Cd15ScheduleOverwriteDecision.builder()
                    .needConfirm(true)
                    .message(I18nUtil.getMessage("ui.cd15.scheduleResult.overwrite.needConfirm"))
                    .build();
        }
        return this.allowed();
    }

    /**
     * 检查单条旧结果是否存在不可覆盖状态。
     *
     * @param result 旧排程结果
     * @return 不可覆盖提示；为空表示允许覆盖
     */
    private String conflict(Cd15ScheduleResult result) {
        if (LOCKED.equals(result.getIsLocked())) {
            return I18nUtil.getMessage("ui.cd15.scheduleResult.overwrite.locked");
        }
        if (RELEASED_SUCCESS.equals(result.getReleaseStatus())) {
            return I18nUtil.getMessage("ui.cd15.scheduleResult.overwrite.released");
        }
        if (this.hasFinishedQuantity(result)) {
            return I18nUtil.getMessage("ui.cd15.scheduleResult.overwrite.finished");
        }
        return null;
    }

    /**
     * 任一班次已有完成量时，自动排程不得覆盖。
     *
     * @param result 旧排程结果
     * @return 是否存在完成量
     */
    private boolean hasFinishedQuantity(Cd15ScheduleResult result) {
        return this.positive(result.getClass1FinishQty()) || this.positive(result.getClass2FinishQty())
                || this.positive(result.getClass3FinishQty()) || this.positive(result.getClass4FinishQty())
                || this.positive(result.getClass5FinishQty()) || this.positive(result.getClass6FinishQty())
                || this.positive(result.getClass7FinishQty()) || this.positive(result.getClass8FinishQty());
    }

    /**
     * 判断完成量是否大于0。
     *
     * @param value 完成量
     * @return 是否大于0
     */
    private boolean positive(Double value) {
        return value != null && value > 0D;
    }

    /**
     * 构造允许覆盖决策。
     *
     * @return 允许覆盖
     */
    private Cd15ScheduleOverwriteDecision allowed() {
        return Cd15ScheduleOverwriteDecision.builder()
                .message(I18nUtil.getMessage("ui.cd15.scheduleResult.overwrite.allowed"))
                .build();
    }
}
