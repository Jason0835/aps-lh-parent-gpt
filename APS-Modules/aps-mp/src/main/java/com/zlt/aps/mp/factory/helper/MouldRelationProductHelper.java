package com.zlt.aps.mp.factory.helper;

import lombok.Data;

import java.io.Serializable;

/**
 * 模具关联物料(SAP)辅助类
 *
 * @author ZLT
 * @date 20250426
 */
@Data
public class MouldRelationProductHelper implements Serializable {
    /**
     * 模具编号
     */
    private String mouldCode;
    /**
     * 关联的SAP个数
     */
    private Integer relationCount;
}
