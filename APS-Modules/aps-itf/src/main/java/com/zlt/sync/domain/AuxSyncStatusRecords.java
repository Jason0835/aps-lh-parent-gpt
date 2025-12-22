package com.zlt.sync.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ApiModel(value = "同步状态记录表")
public class AuxSyncStatusRecords extends SyncBaseEntity {
	private static final long serialVersionUID = -6419250260385295714L;

	@ApiModelProperty(value = "主键id")
    private String stId; //主键id

    @ApiModelProperty(value = "状态类型")
    private String stType; //状态类型

    @ApiModelProperty(value = "状态名称")
    private String stName; //状态名称

    @ApiModelProperty(value = "状态值")
    private String stValue; //状态值
}
