package com.zlt.aps.mps.domain;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 成型8-12点的完成量
 * @TableName T_MES_CX_PART_FINISH_QTY
 */
@Data
public class TMesCxPartFinishQty implements Serializable {
    /**
     * 主键ID，对应序列SEQ_MES_CX_FINISH_QTY
     */
    private Long id;

    /**
     * 日期
     */
    private Date statDate;

    /**
     * 胎胚代码
     */
    private String embryoCode;

    /**
     * sap代码
     */
    private String sapCode;

    /**
     * 完成量(8点-12点)
     */
    private Integer finishQty = 0;

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