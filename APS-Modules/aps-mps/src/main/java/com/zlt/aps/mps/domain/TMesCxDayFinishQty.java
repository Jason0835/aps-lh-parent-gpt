package com.zlt.aps.mps.domain;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 成型排程日完成量回报接口
 * @TableName T_MES_CX_DAY_FINISH_QTY
 */
@Data
public class TMesCxDayFinishQty implements Serializable {
    /**
     * 主键ID，对应序列SEQ_CX_FINISH_QTY
     */
    private Long id;

    /**
     * 排程日期
     */
    private Date finishDate;

    /**
     * 胎胚代码
     */
    private String embryoCode;

    /**
     * 胎胚SAP品号
     */
    private String sapCode;

    /**
     * 胎胚施工版本
     */
    private String bomDataVersion;

    /**
     * 胎胚日完成量
     */
    private Integer dayFinishQty;

    /**
     * 分公司代码
     */
    private String companyCode;

    /**
     * 分厂厂别代码
     */
    private String factoryCode;

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

    private static final long serialVersionUID = 1L;
}