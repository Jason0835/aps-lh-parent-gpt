package com.zlt.aps.common.engine.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 物料与产品状态在允许提前范围内的首个正计划量日期。
 *
 * @author APS
 * @date 2026/11/16
 */
@Data
public class MonthPlanFirstDateDTO implements Serializable {

    /**
     * 序列化版本。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 物料编码。
     */
    private String materialCode;

    /**
     * 产品状态，空白状态统一为空字符串，不通配其他状态。
     */
    private String productStatus;

    /**
     * 月度计划中首个日计划量大于零的自然日。
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate firstPlanDate;

    /**
     * 获取物料+计划类型Key
     *
     * @return
     */
    public String getMaterialStatusKey() {
        String keyFormat = "%s|*|%s";
        String trimmedProductStatus = StringUtils.trimToEmpty(productStatus);
        return String.format(keyFormat, materialCode, trimmedProductStatus);
    }
}
