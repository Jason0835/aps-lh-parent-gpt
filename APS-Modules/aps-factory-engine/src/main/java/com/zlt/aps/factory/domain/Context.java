package com.zlt.aps.factory.domain;

import com.ruoyi.common.core.utils.DateUtils;
import com.tlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * 工厂月度生产计划排产
 * 通用上下文
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
     * 操作批次号
     */
    private String operationWorkNo;

    /**
     * 月份周期排产起始天 第几天
     */
    private Integer startDay;
    /**
     * 排产周期--排产开始日
     */
    private Date productionStartDate;

    /**
     * 排产周期--排产结束日
     */
    private Date productionEndDate;

    /**
     * 是否采用自然月进行排产
     *
     * @return
     */
    public boolean isNaturalMonth() {
        if (null == startDay) {
            return true;
        }
        if (startDay > ProductionConstant.NO_NATURAL_MONTH_MAX_VALUE) {
            return true;
        }
        if (startDay <= ProductionConstant.MONTH_START_DAY) {
            return true;
        }
        return false;
    }

    /**
     * 创建新的版本号，如果排产版本号已经有值，则不进行创建
     * 否则创建新的排产版本号：规则 前缀 + yyyyMMddHHMMSS
     *
     * @return
     */
    public String createNewProductionVersion() {
        if (StringUtils.isNotBlank(productionVersion)) {
            return productionVersion;
        }
        String prefix = prefixVersion;
        if (StringUtils.isBlank(prefix)) {
            prefix = "";
        }
        productionVersion = prefix + DateUtils.dateTimeNow();
        return productionVersion;
    }

    /**
     * 当前排产月
     *
     * @return
     */
    public LocalDate getCurrentMonth() {
        return LocalDate.of(getYear(), getMonth(), ProductionConstant.MONTH_START_DAY);
    }

    /**
     * 当前排产月前一个月时间
     *
     * @return
     */
    public LocalDate getPreviousMonth() {
        LocalDate currentProductionMonth = LocalDate.of(getYear(), getMonth(), ProductionConstant.MONTH_START_DAY);
        LocalDate previousMonth = currentProductionMonth.minusMonths(BigDecimal.ONE.intValue());
        return previousMonth;
    }
}
