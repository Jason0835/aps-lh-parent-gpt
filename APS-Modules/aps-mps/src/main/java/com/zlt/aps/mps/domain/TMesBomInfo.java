package com.zlt.aps.mps.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * BOM信息同步接口
 * @TableName T_MES_BOM_INFO
 */
@Data
public class TMesBomInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 子物料品号 */
    private String childMaterialCode;

    /** 子物料名称 */
    private String childMaterialName;

    /** 子物料名称编码(名称中文映射) */
    private String childMaterialNameCode;

    /** 子物料代码 */
    private String childCode;

    /** 单位描述 */
    private String unit;

    /** 用量，单胎消耗量 */
    private Long dosage;

    /** 组成用量，单胎需要的数量 */
    private Long dosageForm;

    /** 父物料品号 */
    private String parentMaterialCode;

    /** 父物料名称 */
    private String parentMaterialName;

    /** 父物料代码 */
    private String parentCode;

    /** 生产阶段 */
    private String productionStage;

    /** 生产阶段中文映射（0：投产阶段；1试做阶段） */
    private String productionStageCode;

    /** SAP版本信息 */
    private String sapVersion;

    /** BOM信息版本 */
    private String bomVersion;

    /** 子物料版本 */
    private String childMaterialVersion;

    /** BOM类型 */
    private String bomType;

    /** 状态(1正常3废止) */
    private String status;

    /** MES系统创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date mesCreateDate;

    /** MES更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date mesUpdateDate;

    /** 版本号 */
    private String dataVersion;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date createDate;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date updateDate;

    /** 备注 */
    private String remark;

    /** 删除标识：0--正常，1-删除 */
    private Long isDelete;
}