package com.zlt.kettle.api.domain;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import com.ruoyi.common.core.web.domain.BaseEntity;

@Setter
@Getter
@ApiModel(value = "任务信息记录")
public class TaskInfo  extends BaseEntity {

    private static final long serialVersionUID = 1L;
    private Integer id;

    /**
     *分类ID，筛选本系统的key
     */
    @ApiModelProperty(value = "分类ID，筛选本系统的key")
    private Integer categoryId;

    /**
     * 任务名称
     */
    @ApiModelProperty(value = "任务名称")
    private String taskName;
    /**
     * 0JOB,1TRANS
     */
    @ApiModelProperty(value = "任务类型：0JOB,1TRANS")
    private String taskType;
    /**
     * 远程服务调用地址
     */
    @ApiModelProperty(value = "远程服务调用地址")
    private String remoteUrl;
    /**
     * 删除标志
     */
    @ApiModelProperty(value = "删除标志")
    private String delFlag;
    /**
     * 执行记录数-待开发
     */
    @ApiModelProperty(value = "执行记录数")
    private Integer recordCount;

    /**
     * 最后执行时间-待开发
     */
    @ApiModelProperty(value = "最后执行时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastRuntime;
}
