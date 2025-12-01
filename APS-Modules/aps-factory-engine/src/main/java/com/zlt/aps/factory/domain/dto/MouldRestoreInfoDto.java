package com.zlt.aps.factory.domain.dto;

import com.zlt.aps.factory.enums.ProductionOrientEnum;
import lombok.Data;

/**
 * 模具数据复原信息对象
 *
 * @author ZLT
 * @date 20250219
 */
@Data
public class MouldRestoreInfoDto {
    /**
     * 模具号
     */
    private String mouldCode;
    /**
     * 开始排产日期--随着排产继续一直变化
     * 正向排产初始为1，方向排产为月末
     */
    private Integer beginDay;
    /**
     * 排产截止日--随着排产继续会存在变化
     * 主要是因为交期影响，如果无交期，
     * 则正向排产=月末，反向排产=月初
     */
    private Integer endDay;
    /**
     * 分组值--两副、两副一组
     */
    private Integer groupValue;
    /**
     * 排产方向
     */
    private ProductionOrientEnum productionOrientEnum;

}
