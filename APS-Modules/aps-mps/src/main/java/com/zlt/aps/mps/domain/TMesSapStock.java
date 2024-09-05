package com.zlt.aps.mps.domain;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 成品库存同步接口
 * @TableName T_MES_SAP_STOCK
 */
@Data
public class TMesSapStock implements Serializable {
    /**
     * 主键ID，对应自增序列为：SEQ_NC_STOCK
     */
    private Long id;

    /**
     * 库存日期，格式：yyyy-MM-dd
     */
    private Date stockDate;

    /**
     * SAP品号，外胎物料编号
     */
    private String sapCode;

    /**
     * 库存量
     */
    private Integer stockNum;

    /**
     * 被领用量
     */
    private Integer pickedQty;

    /**
     * 备注说明字段
     */
    private String remark;

    /**
     * 版本号
     */
    private String dataVersion;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 更新时间
     */
    private Date updateDate;

    /**
     * 删除标识：0--正常，1-删除
     */
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}