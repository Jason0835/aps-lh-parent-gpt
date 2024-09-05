package com.zlt.aps.mps.domain;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 胎胚库存同步接口
 * @TableName T_MES_CX_STOCK
 */
@Data
public class TMesCxStock implements Serializable {
    /**
     * 主键ID，对应自增序列为：SEQ_NC_STOCK
     */
    private Long id;

    /**
     * 库存日期，格式：yyyy-MM-dd
     */
    private Date stockDate;

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
     * 不可用库存量
     */
    private Integer unavailableStock = 0;

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
     * BOM版本
     */
    private String bomDataVersion;

    /**
     * 删除标识：0--正常，1-删除
     */
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}