package com.zlt.aps.cd15.engine.model;

import com.zlt.aps.cd15.api.domain.entity.Cd15AngleWidthMapping;
import com.zlt.aps.cd15.api.domain.entity.Cd15CurlLength;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.gdyy.api.domain.entity.GdyyScheduleResult;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 斜裁自动排程统一输入模型。
 */
@Data
@Builder
public class Cd15AutoScheduleInput {

    /** 成型排程来源数据。 */
    private List<CxScheduleResult> formingSchedules;
    /** 施工拆解后的钢带和加强层材料。 */
    private List<Cd15ConstructionMaterial> constructionMaterials;
    /** 6点库存来源数据。 */
    private List<Cd15Stock> stocksAtSix;
    /** 启用的斜裁机台档案。 */
    private List<Cd15MachineInfo> machines;
    /** 标准卷曲长度配置。 */
    private List<Cd15CurlLength> curlLengths;
    /** 角度宽度配置。 */
    private List<Cd15AngleWidthMapping> angleWidthMappings;
    /** 按裁断角度归集的最大可分裁宽度。 */
    private Map<String, BigDecimal> angleWidthMaxByAngle;
    /** GDYY 实际库存。 */
    private List<GdyyStock> gdyyStocks;
    /** GDYY 排程结果，用于后续成熟库存推算。 */
    private List<GdyyScheduleResult> gdyyPlans;
}