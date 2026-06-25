package com.zlt.aps.cd90.service;

import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.model.Cd90ScheduleOverwriteDecision;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 自动排程旧结果覆盖决策测试。 */
public class Cd90ScheduleOverwriteValidatorTest {

    private final Cd90ScheduleOverwriteValidator validator = new Cd90ScheduleOverwriteValidator();

    @Test
    public void shouldRequestConfirmationForReplaceableAutoSchedule() {
        Cd90ScheduleResult old = automaticResult();
        Cd90ScheduleOverwriteDecision decision = validator.validate(
                Collections.singletonList(old), false);
        assertTrue(decision.isNeedConfirm());
        assertFalse(decision.isRejected());
    }

    @Test
    public void shouldAllowConfirmedReplaceableAutoSchedule() {
        Cd90ScheduleOverwriteDecision decision = validator.validate(
                Collections.singletonList(automaticResult()), true);
        assertFalse(decision.isNeedConfirm());
        assertFalse(decision.isRejected());
    }

    @Test
    public void shouldRejectPublishedOrLockedResult() {
        Cd90ScheduleResult old = automaticResult();
        old.setPublishSuccessCount(1);
        assertTrue(validator.validate(Collections.singletonList(old), true).isRejected());
        old.setPublishSuccessCount(0);
        old.setIsLocked(1);
        assertTrue(validator.validate(Collections.singletonList(old), true).isRejected());
    }

    private Cd90ScheduleResult automaticResult() {
        Cd90ScheduleResult result = new Cd90ScheduleResult();
        result.setDataSource("0");
        result.setPublishSuccessCount(0);
        result.setIsLocked(0);
        result.setClass1FinishQty(0D);
        return result;
    }
}
