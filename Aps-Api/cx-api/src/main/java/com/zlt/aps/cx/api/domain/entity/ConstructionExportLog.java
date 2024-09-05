package com.zlt.aps.cx.api.domain.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

/**
 * 施工信息导出日志对象 t_construction_export_log
 * 
 * @author zlt
 * @date 2021-12-28
 */
@ApiModel(value = "施工信息导出日志对象", description = "施工信息导出日志对象 ")
@Data
public class ConstructionExportLog extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    @ApiModelProperty(value = "${comment}")
    private Long id;

    /** 文件类型 */
    @Excel(name = "ui.data.column.constructionExportLog.fileType")
    @ApiModelProperty(value = "文件类型")
    private String fileType;

    /** 文件名称 */
    @Excel(name = "ui.data.column.constructionExportLog.fileName")
    @ApiModelProperty(value = "文件名称")
    private String fileName;

    /** 文件存储路径 */
    @Excel(name = "ui.data.column.constructionExportLog.filePath")
    @ApiModelProperty(value = "文件存储路径")
    private String filePath;

    /** 删除标识（0未删除；1已删除） */
    @ApiModelProperty(value = "文件存储路径")
    private String delFlag;





}
