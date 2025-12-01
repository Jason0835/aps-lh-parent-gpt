package com.zlt.mix.schedule.api.domain.dto;

import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 跨区接收请求用于接收跨区接收对象
 * @author: Chen
 * @since: 2022/8/16 10:39
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value="跨区接收请求用于接收跨区接收对象", description="跨区接收请求用于接收跨区接收对象")
public class GlueSpanReceiveDto extends GlueSpanReceive {

    /**
     * 跨区发送请求集合
     */
    @ApiModelProperty(value = "跨区接收请求集合", position = 10)
    private List<GlueSpanReceive> glueSpanReceiveList;
    
    /**
     * 排程ID列表
     */
    private List<Long> scheduleIdList;
}
