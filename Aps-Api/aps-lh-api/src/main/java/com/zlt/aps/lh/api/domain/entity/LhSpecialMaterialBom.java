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
 * 特殊物料清单配置实体
 * <p>
 * 定义特殊物料（19.5寸宽基、22.5寸宽基、芯片胎）与结构的对应关系
 * 结构与物料可以只填1个
 *
 * @author zlt
 * @date 2026-05-06
 */
@Data
@TableName("T_LH_SPECIAL_MATERIAL_BOM")
@ApiModel(value = "特殊物料清单配置")
public class LhSpecialMaterialBom extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号
     */
    @Excel(name = "ui.data.column.lhSpecialMaterialBom.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号")
    @TableField(value = "FACTORY_CODE")
    @ImportExcelValidated(required = true)
    private String factoryCode;

    /**
     * 结构名称
     */
    @Excel(name = "ui.data.column.lhSpecialMaterialBom.structureName", width = 40, align = Excel.Align.LEFT)
    @ApiModelProperty(value = "结构名称")
    @TableField(value = "STRUCTURE_NAME")
    @ImportExcelValidated(maxLength = 300)
    private String structureName;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.lhSpecialMaterialBom.materialCode")
    @ApiModelProperty(value = "物料编码")
    @TableField(value = "MATERIAL_CODE")
    @ImportExcelValidated(maxLength = 50)
    private String materialCode;

    /**
     * 物料描述
     */
    @Excel(name = "ui.data.column.lhSpecialMaterialBom.materialDesc", width = 60, align = Excel.Align.LEFT)
    @ApiModelProperty(value = "物料描述")
    @TableField(value = "MATERIAL_DESC")
    @ImportExcelValidated(maxLength = 300)
    private String materialDesc;

    /**
     * 分类（19.5寸宽基、22.5寸宽基、芯片胎）
     */
    @Excel(name = "ui.data.column.lhSpecialMaterialBom.category", dictType = "lh_special_material_category")
    @ApiModelProperty(value = "分类（19.5寸宽基、22.5寸宽基、芯片胎）")
    @TableField(value = "CATEGORY")
    @ImportExcelValidated(required = true, dictType = "lh_special_material_category")
    private String category;

    /**
     * 分公司编号
     */
    @ApiModelProperty(value = "分公司编号")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    /**
     * 修改人
     */
    @ApiModelProperty(value = "修改人")
    @TableField(value = "UPDATE_BY")
    private String updateBy;

    /**
     * 修改时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "修改时间")
    @TableField(value = "UPDATE_TIME")
    private Date updateTime;

    /**
     * 行号（导入时使用，非数据库字段）
     */
    @ApiModelProperty(value = "行号")
    @TableField(exist = false)
    private Integer rowNo;
}
