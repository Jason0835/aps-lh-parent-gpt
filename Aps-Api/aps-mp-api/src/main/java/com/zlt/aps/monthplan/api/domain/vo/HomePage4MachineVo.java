package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Chen
 * @date 2025/6/28
 */
@Data
public class HomePage4MachineVo implements Serializable {

    /**
     * 类型
     */
    @ApiModelProperty(value = "类型", name = "typeName")
    private String typeName;

    /**
     * 机台数量
     */
    @ApiModelProperty(value = "机台数量", name = "machineCount")
    private Integer machineCount = 0;
}
