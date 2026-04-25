package com.zlt.aps.lh.api.domain.dto;

import lombok.Data;

/**
 * 模具校验错误明细
 * <p>用于结构化承载模具台账状态校验中的禁用/缺失模具信息，
 * 替代原有将全部模具号拼接为一条长字符串的方式，便于前端分页、搜索和导出。</p>
 *
 * @author APS
 */
@Data
public class MouldValidationErrorDetail {

    /** 模具编号（型腔模号） */
    private String mouldCode;

    /** 模具号（业务模具号） */
    private String mouldNo;

    /** 规格 */
    private String specifications;

    /** 模具类型 */
    private String mouldType;

    /** 错误原因（如：模具状态为禁用、模具台账缺失） */
    private String reason;

    /** 状态标签（如：禁用、缺失） */
    private String status;

    public MouldValidationErrorDetail() {
    }

    public MouldValidationErrorDetail(String mouldCode, String mouldNo, String specifications,
                                      String mouldType, String reason, String status) {
        this.mouldCode = mouldCode;
        this.mouldNo = mouldNo;
        this.specifications = specifications;
        this.mouldType = mouldType;
        this.reason = reason;
        this.status = status;
    }
}
