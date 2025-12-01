package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.mix.common.core.domain.ZltBaseDto;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 导出日志管理设置实体
 */
@Data
@TableName("T_EXPORT_LOG")
@ApiModel(value="SettingExportLogManagement对象", description="导出日志管理")
@KeySequence(value = "SEQ_PUBLIC", dbType = DbType.ORACLE)
public class SettingExportLogManagement extends ZltBaseEntity {

    private static final long serialVersionUID = 1110056585123675863L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "工序code")
    @TableField("PROCEDURE_CODE")
    private String procedureCode;

    @ApiModelProperty(value = "功能code")
    @TableField("FUNCTION_CODE")
    private String functionCode;

    @ApiModelProperty(value = "功能名称")
    @TableField("FUNCTION_NAME")
    private String functionName;

    @ApiModelProperty(value = "导出参数（JSON字符串）")
    @TableField("EXPORT_PARAMS")
    private String exportParams;

    @ApiModelProperty(value = "导出文件名称")
    @TableField("FILE_NAME")
    private String fileName;

    @ApiModelProperty(value = "导出文件路径")
    @TableField("FILE_URL")
    private String fileUrl;
}
