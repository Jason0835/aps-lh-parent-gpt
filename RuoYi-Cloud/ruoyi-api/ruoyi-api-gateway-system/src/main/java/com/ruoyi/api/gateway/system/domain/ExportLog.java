package com.ruoyi.api.gateway.system.domain;

import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 导出记录对象 t_export_log
 *
 * @author zlt
 * @date 2021-07-24
 */
@ApiModel(value = "导出记录对象", description = "导出记录对象 ")
public class ExportLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_EXPORT_LOG
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 工序code：0-硫化、1-成型、2-胎面、3-胎侧、4-内衬、5-胎圈、6-钢丝圈、7-15度裁断、8-90裁断、9-90度裁断、10-纤维压延
     */
    @ApiModelProperty(value = "工序code：0-硫化、1-成型、2-胎面、3-胎侧、4-内衬、5-胎圈、6-钢丝圈、7-15度裁断、8-90裁断、9-90度裁断、10-纤维压延")
    private String procedureCode;

    /**
     * 功能code
     */
    @ApiModelProperty(value = "功能code")
    private String functionCode;

    /**
     * 功能名称
     */
    @ApiModelProperty(value = "功能名称")
    private String functionName;

    /**
     * 导出参数
     */
    @ApiModelProperty(value = "导出参数")
    private String exportParams;

    /**
     * 导出文件名称
     */
    @ApiModelProperty(value = "导出文件名称")
    private String fileName;

    /**
     * 导出文件路径
     */
    @ApiModelProperty(value = "导出文件路径")
    private String fileUrl;

    /**
     * 删除标识
     */
    @ApiModelProperty(value = "导出文件路径")
    private String delFlag;


    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setProcedureCode(String procedureCode) {
        this.procedureCode = procedureCode;
    }

    public String getProcedureCode() {
        return procedureCode;
    }

    public void setFunctionCode(String functionCode) {
        this.functionCode = functionCode;
    }

    public String getFunctionCode() {
        return functionCode;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setExportParams(String exportParams) {
        this.exportParams = exportParams;
    }

    public String getExportParams() {
        return exportParams;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public String getDelFlag() {
        return delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("procedureCode", getProcedureCode())
                .append("functionCode", getFunctionCode())
                .append("functionName", getFunctionName())
                .append("exportParams", getExportParams())
                .append("fileName", getFileName())
                .append("fileUrl", getFileUrl())
                .append("remark", getRemark())
                .append("delFlag", getDelFlag())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .toString();
    }

}
