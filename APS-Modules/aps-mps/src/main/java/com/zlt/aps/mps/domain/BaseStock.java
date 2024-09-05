package com.zlt.aps.mps.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 基础库存信息对象 t_*_stock
 *
 * @author zlt
 * @date 2021-05-25
 */

@ApiModel(value = "基础库存信息对象", description = "基础库存信息对象")
@Data
public class BaseStock extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_*_STOCK
     */
    private Long id;

    /**
     * 库存日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date stockDate;

    /**
     * 库存物料编号
     */
    private String materialCode;

    /**
     * 库存量
     */
    private BigDecimal stockNum;

    /**
     * 修正数量
     */
    private BigDecimal modifyNum;

    /**
     * 不良数量
     */
    private BigDecimal badNum;

    private String remark;

    /**
     * 删除标识：0--正常，1-删除
     */
    private String delFlag;

}
