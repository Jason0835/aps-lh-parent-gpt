package com.zlt.mix.setting.api.domain.dto;

import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: Chen
 * @since: 2022/7/18 14:14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MachineOrderDto extends ZltBaseEntity {

    /**
     * 生产机台编号
     */
    @ApiModelProperty(value = "生产机台编号", position = 40)
    private String machineCode;

    @ApiModelProperty(value = "机台顺序", position = 60)
    private Integer machineOrder;
}
