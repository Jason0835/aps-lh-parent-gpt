package com.zlt.aps.lh.api.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * @author xh
 * @version 1.0
 * @Description 硫化插单参数校验DTO
 * @date 2025/3/19
 */
@Data
public class LhOrderInsertParamDTO implements Serializable {


    private static final long serialVersionUID = 8677666570171569531L;

    @ApiModelProperty(value = "排程时间")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "排程时间不能为空")
    private Date scheduleTime;

    @ApiModelProperty(value = "物料编号")
    @NotBlank(message = "物料编号不能为空")
    private String productCode;

    @ApiModelProperty(value = "规格代码")
    @NotBlank(message = "规格代码不能为空")
    private String specCode;

    @ApiModelProperty(value = "机台编号")
    private String machineCode;

    @ApiModelProperty(value = "分厂编号")
    private String factoryCode;
}
