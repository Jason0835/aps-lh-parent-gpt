package com.zlt.aps.tq.api.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 胎圈排程跨班次推迟确认DTO
 *
 * <p>用户预览推迟效果后，携带预览批次号进行确认执行。</p>
 *
 * @author APS
 */
@Data
@ApiModel(value = "胎圈排程跨班次推迟确认", description = "跨班次推迟确认参数")
public class TqPostponeConfirmDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 预览批次号（来自预览接口返回） */
    @NotBlank(message = "预览批次号不能为空")
    @ApiModelProperty(value = "预览批次号", name = "previewBatchNo", required = true)
    private String previewBatchNo;

    /** 是否确认执行（true-确认执行；false-取消） */
    @NotNull(message = "确认标识不能为空")
    @ApiModelProperty(value = "是否确认执行", name = "confirm", required = true)
    private Boolean confirm;

    /** 调整原因（用户填写的推迟原因） */
    @ApiModelProperty(value = "调整原因", name = "adjustReason")
    private String adjustReason;
}
