package com.zlt.mix.schedule.api.domain.dto;

import com.zlt.mix.schedule.api.domain.entity.MaterialSpanSend;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 跨区发送请求用于接收跨区发送对象
 * @author: Chen
 * @since: 2022/8/15 14:39
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value="跨区发送请求用于接收跨区发送对象", description="跨区发送请求用于接收跨区发送对象")
public class MaterialSpanSendDto extends MaterialSpanSend {

    /**
     * 跨区发送请求集合
     */
    @ApiModelProperty(value = "跨区发送请求集合", position = 10)
    private List<MaterialSpanSend> materialSpanSendList;
}
