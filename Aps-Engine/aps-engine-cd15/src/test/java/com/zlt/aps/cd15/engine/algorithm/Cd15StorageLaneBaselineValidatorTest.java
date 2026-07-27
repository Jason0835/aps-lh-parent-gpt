package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.api.domain.entity.Cd15StorageLaneLimit;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 库排资源基线唯一性校验测试。
 */
public class Cd15StorageLaneBaselineValidatorTest {

    private final Cd15StorageLaneBaselineValidator validator =
            new Cd15StorageLaneBaselineValidator();

    @Test
    public void shouldRejectDuplicateLaneCodesWithBaselineContext() {
        Cd15StorageLaneLimit first = this.lane("G30-1");
        Cd15StorageLaneLimit duplicate = this.lane("G30-1");
        Cd15StorageLaneLimit other = this.lane("G30-2");

        assertEquals(Arrays.asList("G30-1"),
                validator.findDuplicateLaneCodes(Arrays.asList(first, duplicate, other)));
        try {
            validator.validateUnique(LocalDate.of(2026, 7, 23), "03",
                    Arrays.asList(first, duplicate, other));
            fail("重复库排号必须拦截");
        } catch (IllegalStateException exception) {
            assertTrue(exception.getMessage().contains("2026-07-23/03"));
            assertTrue(exception.getMessage().contains("G30-1"));
        }
    }

    private Cd15StorageLaneLimit lane(String laneCode) {
        Cd15StorageLaneLimit lane = new Cd15StorageLaneLimit();
        lane.setStorageLaneCode(laneCode);
        return lane;
    }
}