package com.zlt.aps.cd15.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.cd15.api.domain.entity.Cd15DepthConfig;
import com.zlt.aps.cd15.mapper.Cd15DepthConfigMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 斜裁备库班数连续区间校验测试。 */
@ExtendWith(MockitoExtension.class)
class Cd15DepthConfigServiceImplTest {

    @Mock
    private Cd15DepthConfigMapper mapper;

    @InjectMocks
    private Cd15DepthConfigServiceImpl service;

    @Test
    void shouldAcceptContinuousRange() {
        when(mapper.selectList(any())).thenReturn(Arrays.asList(
                config(1, 1, "5"), config(2, 2, "4"), config(5, null, "2")));

        assertEquals(UserConstants.UNIQUE,
                service.checkRangeCross(config(3, 4, "3")));
    }

    @Test
    void shouldRejectGap() {
        when(mapper.selectList(any())).thenReturn(
                Collections.singletonList(config(1, 2, "5")));

        assertEquals(UserConstants.NOT_UNIQUE,
                service.checkRangeCross(config(4, null, "3")));
    }

    @Test
    void shouldRejectOverlap() {
        when(mapper.selectList(any())).thenReturn(
                Collections.singletonList(config(1, 2, "5")));

        assertEquals(UserConstants.NOT_UNIQUE,
                service.checkRangeCross(config(2, null, "3")));
    }

    @Test
    void shouldRejectInvalidBoundary() {
        when(mapper.selectList(any())).thenReturn(Collections.emptyList());

        assertEquals(UserConstants.NOT_UNIQUE,
                service.checkRangeCross(config(2, 1, "3")));
    }

    @Test
    void shouldRejectDepthWithMoreThanTwoDecimals() {
        assertEquals(UserConstants.NOT_UNIQUE,
                service.checkRangeCross(config(1, null, "2.555")));
    }

    private Cd15DepthConfig config(int minMachineQty, Integer maxMachineQty, String depth) {
        Cd15DepthConfig config = new Cd15DepthConfig();
        config.setFactoryCode("116");
        config.setMinMachineQty(minMachineQty);
        config.setMaxMachineQty(maxMachineQty);
        config.setDepthClassQty(new BigDecimal(depth));
        return config;
    }
}
