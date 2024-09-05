package com.zlt.aps.common.engine.domain;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 库存地点映射(没有功能界面，库配置)对象 t_storage_location_cast
 * @author zlt
 * @date 2021-09-28
 */
@ApiModel(value = "库存地点映射(没有功能界面，库配置)对象", description = "库存地点映射(没有功能界面，库配置)对象 ")
@Data
public class StorageLocationCast extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 生产排程库存地点编号（生产排程库存地点字典键值） */
    @Excel(name = "ui.data.column.cast.apsStorageLocation", readConverterExp = "生=产排程库存地点字典键值")
    @ApiModelProperty(value = "生产排程库存地点编号")
    private String apsStorageLocation;

    /** 主计划库存地点编号(主计划库存地点字典键值) */
    @Excel(name = "ui.data.column.cast.mpsStorageLocation")
    @ApiModelProperty(value = "主计划库存地点编号(主计划库存地点字典键值)")
    private String mpsStorageLocation;

    /** 轮胎所属类型：0：配套胎;1：非配套胎 */
    @Excel(name = "ui.data.column.cast.tireStoreType")
    @ApiModelProperty(value = "轮胎所属类型：0：配套胎;1：非配套胎")
    private String tireStoreType;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("apsStorageLocation", getApsStorageLocation())
            .append("mpsStorageLocation", getMpsStorageLocation())
            .append("tireStoreType", getTireStoreType())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("delFlag", getDelFlag())
            .append("remark", getRemark())
            .toString();
    }

}
