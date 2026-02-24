package com.zlt.aps.mp.engine.domain.vo;

import lombok.Data;

import java.util.Date;

/**
 * @author nicl
 */
@Data
public class ProductionCycleInfo {
    private final Date startDate;
    private final Date endDate;
    private final Integer cycleStartDay;
}
