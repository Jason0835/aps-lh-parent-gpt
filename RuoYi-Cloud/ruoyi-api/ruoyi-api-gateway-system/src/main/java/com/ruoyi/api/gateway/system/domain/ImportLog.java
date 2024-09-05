package com.ruoyi.api.gateway.system.domain;

import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 导入记录表
 * </p>
 *
 * @author chen
 * @since 2021-07-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="ImportLog对象", description="导入记录表")
public class ImportLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_IMPORT_LOG")
    private Long id;

    @ApiModelProperty(value = "工序code：0-硫化、1-成型、2-胎面、3-胎侧、4-内衬、5-胎圈、6-钢丝圈、7-15度裁断、8-90裁断、9-钢带压延、10-纤维压延")
    private String procedureCode;

    @ApiModelProperty(value = "功能code，比如：glueOrderExport")
    private String functionCode;

    @ApiModelProperty(value = "功能名称，比如：胎面胶料顺序管理")
    private String functionName;

    @ApiModelProperty(value = "导入文件名称")
    private String fileName;

    @ApiModelProperty(value = "导入文件路径")
    private String fileUrl;

    /** 成功记录数 */
    @ApiModelProperty(value = "成功记录数")
    private Long successNum;

    /** 失败记录数 */
    @ApiModelProperty(value = "失败记录数")
    private Long failNum;

    /**
     * 删除标识
     */
    @ApiModelProperty(value = "导出文件路径")
    private String delFlag;
}
