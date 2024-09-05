package com.zlt.aps.xwyy.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p>
 * 工序导入日志管理
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-07
 */
@Data
@TableName("T_IMPORT_LOG")
@ApiModel(value="XwyyImportLogManagement对象", description="工序导入日志管理")
@KeySequence(value = "SEQ_PUBLIC", clazz = Long.class)
public class XwyyImportLogManagement extends ApsBaseEntity {

    private static final long serialVersionUID = 1110056585174675868L;

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

    @ApiModelProperty(value = "导入文件名称")
    @TableField("FILE_NAME")
    private String fileName;

    @ApiModelProperty(value = "导入文件路径")
    @TableField("FILE_URL")
    private String fileUrl;

    @ApiModelProperty(value = "成功记录数")
    @TableField("SUCCESS_NUM")
    private Integer successNum;

    @ApiModelProperty(value = "失败记录数")
    @TableField("FAIL_NUM")
    private Integer failNum;

}
