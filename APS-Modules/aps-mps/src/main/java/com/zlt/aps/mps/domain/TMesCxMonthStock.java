package com.zlt.aps.mps.domain;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 胎胚月结库存表
 * @TableName T_MES_CX_MONTH_STOCK
 */
@Data
public class TMesCxMonthStock implements Serializable {
    /**
     * 主键ID，对应自增序列为：SEQ_NC_STOCK
     */
    private Long id;

    /**
     * 库存所属月份：yyyy-mm
     */
    @JsonFormat(pattern = "yyyy-MM")
    private Date stockMonth;

    /**
     * 胎胚代码
     */
    private String embryoCode;

    /**
     * 可用库存
     */
    private Integer availableStock = 0;

    /**
     * 库存量
     */
    private Integer stockNum = 0;

    /**
     * 立体库
     */
    private Integer litiStock = 0;

    /**
     * 胎胚车
     */
    private Integer embryoCar = 0;

    /**
     * 硫化库存
     */
    private Integer lhStock = 0;

    /**
     * 超期库存
     */
    private Integer overTimeStock = 0;

    /**
     * SAP品号
     */
    private String sapCode;

    /**
     * 备注
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

    /**
     * 施工信息版本
     */
    private String bomDataVersion;

    private static final long serialVersionUID = 1L;
}