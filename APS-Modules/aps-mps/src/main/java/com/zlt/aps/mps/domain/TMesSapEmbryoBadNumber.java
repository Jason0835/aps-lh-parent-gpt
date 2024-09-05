package com.zlt.aps.mps.domain;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 不良数接口
 * @TableName T_MES_SAP_EMBRYO_BAD_NUMBER
 */
@Data
public class TMesSapEmbryoBadNumber implements Serializable{
    /**
     * 主键ID，对应自增序列为：SEQ_NC_STOCK
     */
    private Long id;

    /**
     * 不良日期，格式：yyyy-MM-dd
     */
    private Date badDate;

    /**
     * SAP品号，外胎物料编号
     */
    private String sapCode;

    /**
     * 胎胚代码
     */
    private String embryoCode;

    /**
     * 不良数
     */
    private Integer badNum;


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

    /**
     * 施工信息版本
     */
    private String bomDataVersion;

    private static final long serialVersionUID = 1L;
}