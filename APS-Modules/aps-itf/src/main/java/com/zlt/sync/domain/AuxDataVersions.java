package com.zlt.sync.domain;

import com.zlt.aps.itf.vo.SyncBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * fromSys + toSys 是唯一主键
 */
@Getter
@Setter
@ApiModel(value = "数据版本信息表")
public class AuxDataVersions extends SyncBaseEntity {
	private static final long serialVersionUID = -3644918347392734352L;

	@ApiModelProperty(value = "主键id")
    private String verId; //主键id

    @ApiModelProperty(value = "来源系统")
    private String fromSys;

    @ApiModelProperty(value = "目标系统")
    private String toSys;

    @ApiModelProperty(value = "版本年月日")
    private String ymdVersion;

    @ApiModelProperty(value = "版本号")
    private Integer verVersion;

}
