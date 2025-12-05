package com.zlt.aps.factory.domain;

import com.tlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import lombok.Data;

import java.util.List;

/**
 * 工厂生产计划排产上下文
 *
 * @author ZLT
 * 20251205
 */
@Data
public class Context {
    /**
     * 工厂编号
     */
    private String factoryCode;

    /**
     * 年度
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 产品品类
     */
    private ProductTypeEnum productType;

    /**
     * 月度销售生产需求计划版本
     */
    private String monthPlanVersion;

    /**
     * 排产版本
     */
    private String productionVersion;

    /**
     * 是否生成
     */
    private Boolean general = false;

    /**
     * 版本前缀
     */
    private String prefixVersion;
    /**
     * 日志存储器
     */
    private StringBuilder logBuilder;
    /**
     * 寸口产能需求计划集合--只用于寸口产能需求计划时使用，跟排产流程没有关系
     */
    private List<MonthPlanManufacturingRequirementVo> sizeCapacityRequireList;
}
