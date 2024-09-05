package com.ruoyi.api.gateway.system.domain;

import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * <p>
 * 导入错误日志记录表
 * </p>
 *
 * @author chen
 * @since 2021-07-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="ImportErrorLog对象", description="导入错误日志记录表")
public class ImportErrorLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public ImportErrorLog() {
    }

    public ImportErrorLog(Long importLogId, Integer errorRow, String errorDetail) {
        this.importLogId = importLogId;
        this.errorRow = errorRow;
        this.errorDetail = errorDetail;
        try {
            this.setDelFlag("0");
            this.setCreateBy(SecurityUtils.getUsername());
            this.setCreateTime(new Date());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_IMPORT_LOG")
    private Long id;

    @ApiModelProperty(value = "导入记录id，和T_IMPORT_LOG的id对应")
    private Long importLogId;

    @ApiModelProperty(value = "错误行数")
    private Integer errorRow;

    @ApiModelProperty(value = "错误详细信息")
    private String errorDetail;

    /**
     * 删除标识
     */
    @ApiModelProperty(value = "导出文件路径")
    private String delFlag;
}
