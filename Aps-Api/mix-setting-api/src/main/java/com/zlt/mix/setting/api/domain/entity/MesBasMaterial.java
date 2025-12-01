package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.common.annotation.ImportExcelValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 物料对象 t_mes_bas_material
 * 
 * @author Joran.zhang
 * @date 2022-05-30
 */
@ApiModel(value = "物料对象", description = "物料对象 ")
@TableName("t_mes_bas_material")
@KeySequence(value = "seq_t_mes_bas_material", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class MesBasMaterial extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_T_MES_BAS_MATERIAL */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_MES_BAS_MATERIAL", position = 10)
    private Long id;
    /** MES主键 */
    // @ImportExcelValidated(name = "setting.material.mesId", number=true, min=0, max=9999999)
    @ApiModelProperty(value = "MES主键", position = 20)
    private Long mesId;
    /** 物料编码 */
    @Excel(name = "setting.material.materialCode")
    @ImportExcelValidated(name = "setting.material.materialCode", maxLength = 13, required = true)
    @ApiModelProperty(value = "物料编码", position = 30)
    private String materialCode;
    /** 物料名称 */
    @Excel(name = "setting.material.materialName")
    @ImportExcelValidated(name = "setting.material.materialName", maxLength = 50, required = true)
    @ApiModelProperty(value = "物料名称", position = 40)
    private String materialName;
    /** ERP编码 */
    @Excel(name = "setting.material.erpCode")
    @ImportExcelValidated(name = "setting.material.erpCode", maxLength=13)
    @ApiModelProperty(value = "ERP编码", position = 50)
    private String erpCode;
    /** SAP编码 */
    @Excel(name = "setting.material.sapMaterialCode")
    @ImportExcelValidated(name = "setting.material.sapMaterialCode", maxLength=13)
    @ApiModelProperty(value = "SAP编码", position = 60)
    private String sapMaterialCode;
    /** 有效期 单位小时 */
    // @Excel(name = "setting.material.validDate")
    // @ImportExcelValidated(name = "setting.material.validDate", max=9999999)
    @ApiModelProperty(value = "有效期 单位小时", position = 70)
    private Integer validDate;
    /** 物料分组 */
    // @Excel(name = "setting.material.materialGroup")
    // @ImportExcelValidated(name = "setting.material.materialGroup", number=true, min=0, max=9999999)
    @ApiModelProperty(value = "物料分组", position = 80)
    private String  materialGroup;
    /** 最少停放时间 */
    @Excel(name = "setting.material.minparkTime")
    @ImportExcelValidated(name = "setting.material.minparkTime", max=999999, required = true)
    @ApiModelProperty(value = "最少停放时间", position = 90)
    private BigDecimal minparkTime;

    @Excel(name = "setting.material.isHighConsumption",dictType = "ISORNOT")
    @ImportExcelValidated(name = "setting.material.isHighConsumption", maxLength=1)
    @ApiModelProperty(value = "是否高能耗(对应数据字典，ISORNOT，0-是，1-否)", position = 110)
    private String isHighConsumption;
    
    /** 物料大类(对应数据字典：MAJOR_TYPE) */
    @Excel(name = "setting.material.majorType",dictType = "MAJOR_TYPE")
    @ImportExcelValidated(name = "setting.material.majorType", number=true, min=0,max=9999999)
    @ApiModelProperty(value = "物料大类(对应数据字典：MAJOR_TYPE)", position = 100)
    private String majorType;
    /** 胶料类型(对应数据字典，GLUE_TYPE) */
    @Excel(name = "setting.material.glueType",dictType = "GLUE_TYPE")
    @ImportExcelValidated(name = "setting.material.glueType", maxLength=3)
    @ApiModelProperty(value = "胶料类型(对应数据字典，GLUE_TYPE)", position = 110)
    private String glueType;
    
    /** 备注 */
    @Excel(name = "setting.material.remark")
    @ImportExcelValidated(name = "setting.material.remark", maxLength=300)
    @ApiModelProperty(value = "备注", position = 120)
    private String remark;

    /**
     * 特殊处理搜索胶料类型为空的数据
     */
    @TableField(exist = false)
    private String glueTypeEmpty;

}
