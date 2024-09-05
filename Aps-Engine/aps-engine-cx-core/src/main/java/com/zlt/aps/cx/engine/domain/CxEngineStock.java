package com.zlt.aps.cx.engine.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * 成型库存信息对象 t_cx_stock
 * 
 * @author Joran.zhang
 * @date 2021-07-14
 */
@Data
@ApiModel(value = "成型库存信息对象", description = "成型库存信息对象 ")
public class CxEngineStock extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_CX_STOCK */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 库存日期，格式：yyyy-MM-dd */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "库存日期，格式：yyyy-MM-dd")
    private Date stockDate;

    /** 胎胚代码 */
    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    /** 施工版本 */
    @ApiModelProperty(value = "施工版本")
    private String bomDataVersion;

    /** 库存量 */
    @ApiModelProperty(value = "库存量")
    private Integer stockNum;

    /** 修正数量 */
    @ApiModelProperty(value = "修正数量")
    private Integer modifyNum;

    /** 不良数量 */
    @ApiModelProperty(value = "不良数量")
    private Integer badNum;

    /**
     * 真实库存：库存+修正-不良
     */
    private Integer stockRealNum;

    /**
     * 库存日期
     */
    private String stockDateStr;


    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("stockDate", getStockDate())
            .append("embryoCode", getEmbryoCode())
            .append("stockNum", getStockNum())
            .append("modifyNum", getModifyNum())
            .append("badNum", getBadNum())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("delFlag", getDelFlag())
            .append("remark", getRemark())
            .toString();
    }

}
