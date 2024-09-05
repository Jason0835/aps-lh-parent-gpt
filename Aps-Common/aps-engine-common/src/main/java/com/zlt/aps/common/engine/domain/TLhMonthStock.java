package com.zlt.aps.common.engine.domain;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 每月1号早8库存数据，需要跟MES沟通是否可以提供
 * @TableName T_LH_MONTH_STOCK
 */
@Data
public class TLhMonthStock implements Serializable {
    /**
     * 主键ID，对应自增序列为：SEQ_NC_STOCK
     */
    private Long id;

    /**
     * 库存所属月份：yyyy-mm
     */
    private Date stockMonth;

    /**
     * SAP品号
     */
    private String sapCode;

    /**
     * 库存量
     */
    private Integer stockNum;

    /**
     * 超期库存
     */
//    private Integer overTimeStock;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除标识：0--正常，1-删除
     */
    private String delFlag;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}