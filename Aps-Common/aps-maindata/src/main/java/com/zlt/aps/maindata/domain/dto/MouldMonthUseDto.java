package com.zlt.aps.maindata.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 月度可用模具信息
 *
 * @author ZLT
 * @date 20250403
 */
@Data
public class MouldMonthUseDto implements Serializable {
    /**
     * 模具编码
     */
    private String mouldCode;
    /**
     * 模具
     */
    private String mouldNo;
}
