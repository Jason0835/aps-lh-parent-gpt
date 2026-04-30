package com.zlt.aps.lh.api.domain.entity;

import lombok.Data;

import java.time.LocalDate;

/**
 * 硫化精度计划下发实体
 * 用于将硫化精度计划下发到MES系统
 *
 * @author APS Team
 */
@Data
public class LhPrecisionPlanIssue {

    private Long id;

    private String machineCode;

    private String precisionType;

    private LocalDate scheduleDate;

    private LocalDate planDate;

    private String dataVersion;

    private String companyCode;

    private String factoryCode;
}
