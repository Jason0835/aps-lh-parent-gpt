package com.zlt.aps.mp.api.domain.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * MES 直裁库存中间表实体，映射 T_MES_CD90_STOCK。
 * 用于 aps-itf 从 MES 数据源查询直裁库存同步数据。
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
public class Cd90MesStock implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 库存日期 */
    private Date stockDate;

    /** 库存物料编号 */
    private String materialCode;

    /** SAP 物料编码 */
    private String sapMaterialCode;

    /** 库存量 */
    private BigDecimal stockNum;

    /** 版本号 */
    private String dataVersion;

    /** 创建时间 */
    private Date createDate;

    /** 更新时间 */
    private Date updateDate;

    /** 可用库存 */
    private BigDecimal availableStock;

    /** 删除标识：0-正常，1-删除 */
    private Integer isDelete;
}
