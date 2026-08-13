package com.zlt.aps.cd90.service;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.model.Cd90ScheduleOverwriteDecision;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/** 判断同日期旧排程是否允许自动排程覆盖。 */
@Component
public class Cd90ScheduleOverwriteValidator {

    /**
     * 校验旧结果覆盖规则。
     *
     * @param existingResults 同日期旧结果
     * @param forceRegenerate 是否已由用户确认覆盖
     * @return 覆盖决策
     */
    public Cd90ScheduleOverwriteDecision validate(List<Cd90ScheduleResult> existingResults,
                                                   boolean forceRegenerate) {
        List<Cd90ScheduleResult> results = existingResults == null
                ? Collections.emptyList() : existingResults;
        if (results.isEmpty()) {
            return allowed();
        }
        for (Cd90ScheduleResult result : results) {
            String conflict = conflict(result);
            if (conflict != null) {
                return Cd90ScheduleOverwriteDecision.builder().rejected(true)
                        .message(conflict).build();
            }
        }
        if (!forceRegenerate) {
            return Cd90ScheduleOverwriteDecision.builder().needConfirm(true)
                    .message(I18nUtil.getMessage(
                            "ui.cd90.scheduleResult.overwrite.needConfirm"))
                    .build();
        }
        return allowed();
    }

    private String conflict(Cd90ScheduleResult result) {
        // 人工插单/调整（DATA_SOURCE=1）按业务约定允许被重新自动排程覆盖；
        // 是否可覆盖只由锁定、发布和已生产状态决定。
        if (Integer.valueOf(1).equals(result.getIsLocked())) {
            return "当前日期存在人工锁定结果，自动排程不能覆盖";
        }
        if (result.getPublishSuccessCount() != null && result.getPublishSuccessCount() > 0) {
            return "当前日期存在已发布成功结果，自动排程不能覆盖";
        }
        if (hasFinishedQuantity(result)) {
            return "当前日期存在已生产数量，自动排程不能覆盖";
        }
        return null;
    }

    private boolean hasFinishedQuantity(Cd90ScheduleResult result) {
        return positive(result.getClass1FinishQty()) || positive(result.getClass2FinishQty())
                || positive(result.getClass3FinishQty()) || positive(result.getClass4FinishQty())
                || positive(result.getClass5FinishQty()) || positive(result.getClass6FinishQty())
                || positive(result.getClass7FinishQty()) || positive(result.getClass8FinishQty());
    }

    private boolean positive(Double value) {
        return value != null && value > 0D;
    }

    private Cd90ScheduleOverwriteDecision allowed() {
        return Cd90ScheduleOverwriteDecision.builder().message("允许自动排程").build();
    }
}
