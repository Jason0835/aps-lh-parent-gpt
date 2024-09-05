package com.zlt.aps.gsq.api.domain.dto;

import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 工序导入日志管理错误日志设置实体
 * @author duanjuntao
 */
@Data
@ApiModel(value="GsqImportErrorLogManagementDtoo对象", description="工序导入日志管理")
public class GsqImportErrorLogManagementDto extends ApsBaseDto {

    private static final long serialVersionUID = 1110056585123675863L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC", position = 10)
    private Long id;

    @ApiModelProperty(value = "IMPORT_LOG_ID", position = 20)
    private String importLogId;

    @ApiModelProperty(value = "ERROR_ROW", position = 30)
    private String errorRow;

    @ApiModelProperty(value = "ERROR_DETAIL", position = 40)
    private String errorDetail;

    @ApiModelProperty(value = "REMARK", position = 500)
    private String remark;
}
