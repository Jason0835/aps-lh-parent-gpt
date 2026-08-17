package com.zlt.aps.lh.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 共用模具花纹配置实体
 * <p>
 * 定义共用模具花纹与物料的对应关系
 *
 * @author zlt
 * @date 2026-05-14
 */
@Data
@TableName("T_LH_SHARED_MOULD_PAT")
@ApiModel(value = "共用模具花纹配置")
public class LhSharedMouldPat extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号
     */
    @Excel(name = "ui.data.column.lhSharedMouldPat.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号")
    @TableField(value = "FACTORY_CODE")
    @ImportExcelValidated(required = true)
    private String factoryCode;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.lhSharedMouldPat.materialCode")
    @ApiModelProperty(value = "物料编码")
    @TableField(value = "MATERIAL_CODE")
    @ImportExcelValidated(required = true, maxLength = 50)
    private String materialCode;

    /**
     * 物料描述（根据物料编码从物料主数据反显，导入模板中不需要填写）
     */
    @Excel(name = "ui.data.column.lhSharedMouldPat.materialDesc", width = 60, align = Excel.Align.LEFT, type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "物料描述")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.lhSharedMouldPat.specifications", width = 40, align = Excel.Align.LEFT)
    @ApiModelProperty(value = "规格")
    @TableField(value = "SPECIFICATIONS")
    @ImportExcelValidated(maxLength = 300)
    private String specifications;

    /**
     * 主花纹
     */
    @Excel(name = "ui.data.column.lhSharedMouldPat.mainPattern")
    @ApiModelProperty(value = "主花纹")
    @TableField(value = "MAIN_PATTERN")
    @ImportExcelValidated(maxLength = 50)
    private String mainPattern;

    /**
     * 模具类型
     */
    @Excel(name = "ui.data.column.lhSharedMouldPat.mouldType", dictType = "biz_mould_Type")
    @ApiModelProperty(value = "模具类型")
    @TableField(value = "MOULD_TYPE")
    @ImportExcelValidated(dictType = "biz_mould_Type", maxLength = 64)
    private String mouldType;

    /**
     * 模具号
     */
    @Excel(name = "ui.data.column.lhSharedMouldPat.mouldNo")
    @ApiModelProperty(value = "模具号")
    @TableField(value = "MOULD_NO")
    @ImportExcelValidated(required = true, maxLength = 64)
    private String mouldNo;

    /**
     * 花纹块
     */
    @Excel(name = "ui.data.column.lhSharedMouldPat.patternBlock")
    @ApiModelProperty(value = "花纹块")
    @TableField(value = "PATTERN_BLOCK")
    @ImportExcelValidated(required = true, maxLength = 64)
    private String patternBlock;

    /**
     * 公司编号
     */
    @ApiModelProperty(value = "公司编号")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    /**
     * 修改人
     */
    @ApiModelProperty(value = "修改人")
    @TableField(value = "UPDATE_BY")
    private String updateBy;

    /**
     * 修改时间（仅导出显示，导入时不读取，避免清空）
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "ui.data.column.lhSharedMouldPat.updateTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "修改时间")
    @TableField(value = "UPDATE_TIME")
    private Date updateTime;

    /**
     * 备注
     */
    @Excel(name = "ui.data.column.lhSharedMouldPat.remark", width = 40, align = Excel.Align.LEFT)
    @ApiModelProperty(value = "备注")
    @TableField(value = "REMARK")
    private String remark;

    /**
     * 行号（导入时使用，非数据库字段）
     */
    @ApiModelProperty(value = "行号")
    @TableField(exist = false)
    private Integer rowNo;
}
