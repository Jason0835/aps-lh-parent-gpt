package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class Cd15RollingShiftSlicerTest {

    @Test
    public void shouldExcludeEveryShiftBeforeRollingTarget() {
        List<Cd15ShiftDescriptor> shifts = Arrays.asList(
                shift("CLASS1"), shift("CLASS2"), shift("CLASS3"));

        List<String> affected = new Cd15RollingShiftSlicer().slice(shifts, "CLASS2")
                .stream().map(Cd15ShiftDescriptor::getClassField)
                .collect(Collectors.toList());

        assertEquals(Arrays.asList("CLASS2", "CLASS3"), affected);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectUnknownTargetClass() {
        new Cd15RollingShiftSlicer().slice(
                Arrays.asList(shift("CLASS1"), shift("CLASS2")), "CLASS9");
    }

    private Cd15ShiftDescriptor shift(String classField) {
        return Cd15ShiftDescriptor.builder().classField(classField).build();
    }
}
