package com.zlt.aps.itf.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 自动滚动班次库存MES同步请求。
 */
@Data
@ApiModel(value = "自动滚动班次库存MES同步请求", description = "指定工厂、物理库存日和班序同步实时库存")
public class MesShiftStockSyncRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码。 */
    @ApiModelProperty(value = "工厂编码", required = true)
    private String factoryCode;

    /** 公司编码，为空时使用工厂编码。 */
    @ApiModelProperty(value = "公司编码")
    private String companyCode;

    /** MES库存物理日期。 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "MES库存物理日期", required = true)
    private Date stockDate;

    /** 班次顺序，TM/TC取值一至六。 */
    @ApiModelProperty(value = "班次顺序，TM/TC取值一至六")
    private Integer shiftOrder;

    /** 物理班次编码，CD15/CD90使用。 */
    @ApiModelProperty(value = "物理班次编码")
    private String shiftCode;

    /** 班次开始时间，CD15/CD90用于精确定位交班快照。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "班次开始时间")
    private Date shiftStartTime;

    /** 可选MES数据版本。 */
    @ApiModelProperty(value = "MES数据版本")
    private String dataVersion;
}
