package com.zlt.aps.factory.domain.vo;

import lombok.Data;

/**
 * 同模具物料信息对象
 *
 * @author ZLT
 * @date 20250220
 */
@Data
public class SameMouldProductVO {
    /**
     * 物料编号
     */
    private String productCode;

    /**
     * 同模具物料编号
     */
    private String sameMouldProductCode;
}
