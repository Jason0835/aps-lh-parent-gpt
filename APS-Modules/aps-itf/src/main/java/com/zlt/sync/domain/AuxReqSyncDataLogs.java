package com.zlt.sync.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ApiModel(value = "同步请求状态日志")
public class AuxReqSyncDataLogs extends SyncBaseEntity {
	private static final long serialVersionUID = 1L;

	@ApiModelProperty(value = "主键id")
    private String msgId; //主键id

    @ApiModelProperty(value = "同步接口key")
    private String syncKey; //同步接口key

    @ApiModelProperty(value = "对接系统")
    private String dockSys; //对接系统

    @ApiModelProperty(value = "请求参数")
    private String params; //请求参数

    @ApiModelProperty(value = "返回结果代码")
    private Integer msgCode; //返回结果代码

    @ApiModelProperty(value = "处理结果消息")
    private String msg;

    @ApiModelProperty(value = "消息标志")
    private String msgKey; //消息标志

    @ApiModelProperty(value = "请求状态")
    private Integer status;

    @ApiModelProperty(value = "是否有数据")
    private Integer hasData;

    @ApiModelProperty(value = "同步接口key")
    private String dataSys; //

    @ApiModelProperty(value = "回传下发标志")
    private Integer backIssue; //回传下发标志

    @ApiModelProperty(value = "分公司编号")
    private String companyCode; //分公司编号

    @ApiModelProperty(value = "厂别")
    private String factoryCode; //厂别
}
