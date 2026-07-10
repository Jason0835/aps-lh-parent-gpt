package com.zlt.aps.cd15.engine.model;

import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * CD15 单规格单班试排请求。
 */
@Data
@Builder
public class Cd15SingleShiftScheduleRequest {

    private Cd15ConstructionMaterial material;
    private Cd15NaturalDemand demand;
    private Cd15MachineInfo machine;
    private GdyyStock gdyyStock;
    private String orderNo;
    private String groupNo;
    private Integer produceOrder;
    /** CD15 库存冲减量，单位米。 */
    private BigDecimal stockMetersAtSix;
    /** 大卷幅宽 CORD_WIDTH，单位毫米；为空时按当前净需求口径兜底。 */
    private BigDecimal cordWidthMillimeter;
}