package com.zlt.aps.cx.api.domain.entity;

import com.zlt.aps.common.core.annotation.ImportValidated;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

/**
 * 库存地点映射对象 t_storage_location_cast
 * 
 * @author zlt
 * @date 2021-11-15
 */
@ApiModel(value = "库存地点映射对象", description = "库存地点映射对象 ")
@Data
public class CxStockLocationMapping extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "id")
    private Long id;

    /** 生产排程库存地点编码 */
    @Excel(name = "ui.data.column.stockLocationMapping.apsStorageLocation",dictType = "STORAGE_LOCATION")
    @ApiModelProperty(value = "生产排程库存地点编码")
    private String apsStorageLocation;

    /** 主计划库存地点编码 */
    @ImportValidated(isCode = true,maxLength = 20)
    @Excel(name = "ui.data.column.stockLocationMapping.mpsStorageLocation")
    @ApiModelProperty(value = "主计划库存地点编码")
    private String mpsStorageLocation;

    /** 轮胎所属类型：0：配套胎;1：非配套胎 */
    @Excel(name = "ui.data.column.stockLocationMapping.tireStoreType",dictType = "STOCK_LOCATION_TIRE_TYPE")
    @ApiModelProperty(value = "轮胎所属类型：0：配套胎;1：非配套胎")
    private String tireStoreType;

    /** 删除标识 */
    private String delFlag;

    @ImportValidated(maxLength = 300)
    @Excel(name="ui.data.column.remark")
    private String remark;





}
