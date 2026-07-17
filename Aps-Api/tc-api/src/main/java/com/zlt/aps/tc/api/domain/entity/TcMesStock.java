package com.zlt.aps.tc.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * MES胎侧库存接口对象。
 */
@Data
@ApiModel(value = "MES胎侧库存接口对象", description = "MES胎侧库存中间表数据")
public class TcMesStock implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 库存日期。 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "库存日期", name = "stockDate")
    private Date stockDate;

    /** 胎侧编码。 */
    @ApiModelProperty(value = "胎侧编码", name = "sidewallCode")
    private String sidewallCode;

    /** 库存数量。 */
    @ApiModelProperty(value = "库存数量", name = "stockQty")
    private BigDecimal stockQty;

    /** 不良数量。 */
    @ApiModelProperty(value = "不良数量", name = "badQty")
    private BigDecimal badQty;

    /** 调整数量。 */
    @ApiModelProperty(value = "调整数量", name = "adjustQty")
    private BigDecimal adjustQty;

    /** MES数据版本。 */
    @ApiModelProperty(value = "MES数据版本", name = "dataVersion")
    private String dataVersion;

    /** 公司编码。 */
    @ApiModelProperty(value = "公司编码", name = "companyCode")
    private String companyCode;

    /** 工厂编码。 */
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    private String factoryCode;
}
