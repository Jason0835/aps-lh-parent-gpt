package com.zlt.aps.xwyy.api.domain.dto;

import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 工序导出日志管理设置实体
 *
 * @author chenxueyuan
 */
@Data
@ApiModel(value = "XwyyExportLogManagementDto对象", description = "工序导出日志管理")
public class XwyyExportLogManagementDto extends ApsBaseDto {

    private static final long serialVersionUID = 1110056585123675863L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC", position = 10)
    private Long id;

    @ApiModelProperty(value = "PROCEDURE_CODE", position = 20)
    private String procedureCode;

    @ApiModelProperty(value = "FUNCTION_CODE", position = 30)
    private String functionCode;

    @ApiModelProperty(value = "FUNCTION_NAME", position = 40)
    private String functionName;

    @ApiModelProperty(value = "EXPORT_PARAMS", position = 3000)
    private String exportParams;

    @ApiModelProperty(value = "FILE_NAME", position = 150)
    private String fileName;

    @ApiModelProperty(value = "FILE_URL", position = 100)
    private String fileUrl;

    @ApiModelProperty(value = "REMARK", position = 500)
    private String remark;
}
