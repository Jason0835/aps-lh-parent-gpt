package com.zlt.aps.cd15.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：HalfCdImportBak.java
 * 描    述：裁断线下计划导入导出对象 half_cd_import_bak
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-05-29
 */

@ApiModel(value = "裁断线下计划导入导出对象", description = "裁断线下计划导入导出对象 ")
@Data
@TableName(value = "HALF_CD_IMPORT_BAK_EXPORT_DATA")
public class HalfCdImportBakExportData extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableField(exist = false)
    private Long id;
    @TableField(exist = false)
    private String searchValue;
    @TableField(exist = false)
    private String createBy;
    @TableField(exist = false)
    private Date createTime;
    @TableField(exist = false)
    private String updateBy;
    @TableField(exist = false)
    private Date updateTime;
    @TableField(exist = false)
    private String remark;
    @TableField(exist = false)
    private Integer isDelete = 0;
    @TableField(exist = false)
    private Map<String, Object> params;
    @TableField(exist = false)
    private RowStateEnum rowState;

    /**
     * 序号
     */
    @Excel(name = "ui.data.column.halfCdImportBak.cx1")
    @ApiModelProperty(value = "序号", name = "cx1")
    @TableField(value = "CX1")
    private Double cx1;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.halfCdImportBak.cx2")
    @ApiModelProperty(value = "规格", name = "cx2")
    @TableField(value = "CX2")
    private String cx2;

    /**
     * 规格代码
     */
    @Excel(name = "ui.data.column.halfCdImportBak.cx3")
    @ApiModelProperty(value = "规格代码", name = "cx3")
    @TableField(value = "CX3")
    private String cx3;

    /**
     * 施工卡
     */
    @Excel(name = "ui.data.column.halfCdImportBak.cx4")
    @ApiModelProperty(value = "施工卡", name = "cx4")
    @TableField(value = "CX4")
    private String cx4;

    /**
     * 成型早班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.cx5")
    @ApiModelProperty(value = "成型早班", name = "cx5")
    @TableField(value = "CX5")
    private Double cx5;

    /**
     * 成型夜班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.cx6")
    @ApiModelProperty(value = "成型夜班", name = "cx6")
    @TableField(value = "CX6")
    private Double cx6;

    /**
     * 成型合计
     */
    @Excel(name = "ui.data.column.halfCdImportBak.cx7")
    @ApiModelProperty(value = "成型合计", name = "cx7")
    @TableField(value = "CX7")
    private Double cx7;

    /**
     * 成型完成
     */
    @Excel(name = "ui.data.column.halfCdImportBak.cx8")
    @ApiModelProperty(value = "成型完成", name = "cx8")
    @TableField(value = "CX8")
    private Double cx8;

    /**
     * 胎身帘布编号
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb1")
    @ApiModelProperty(value = "胎身帘布编号", name = "lb1")
    @TableField(value = "LB1")
    private String lb1;

    /**
     * 条米
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb2")
    @ApiModelProperty(value = "条米", name = "lb2")
    @TableField(value = "LB2")
    private Double lb2;

    /**
     * 卷长
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb3")
    @ApiModelProperty(value = "卷长", name = "lb3")
    @TableField(value = "LB3")
    private Double lb3;

    /**
     * 理论库存
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb4")
    @ApiModelProperty(value = "理论库存", name = "lb4")
    @TableField(value = "LB4")
    private Double lb4;

    /**
     * 实际库存
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb5")
    @ApiModelProperty(value = "实际库存", name = "lb5")
    @TableField(value = "LB5")
    private Double lb5;

    /**
     * 计划用量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb6")
    @ApiModelProperty(value = "计划用量", name = "lb6")
    @TableField(value = "LB6")
    private Double lb6;

    /**
     * 用量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb7")
    @ApiModelProperty(value = "用量", name = "lb7")
    @TableField(value = "LB7")
    private Double lb7;

    /**
     * 早班顺位
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb8")
    @ApiModelProperty(value = "早班顺位", name = "lb8")
    @TableField(value = "LB8")
    private Double lb8;

    /**
     * 1#直裁早班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb9")
    @ApiModelProperty(value = "1#直裁早班", name = "lb9")
    @TableField(value = "LB9")
    private Double lb9;

    /**
     * 2#直裁早班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb10")
    @ApiModelProperty(value = "2#直裁早班", name = "lb10")
    @TableField(value = "LB10")
    private Double lb10;

    /**
     * 3#直裁早班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb11")
    @ApiModelProperty(value = "3#直裁早班", name = "lb11")
    @TableField(value = "LB11")
    private Double lb11;

    /**
     * 4#直裁早班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb12")
    @ApiModelProperty(value = "4#直裁早班", name = "lb12")
    @TableField(value = "LB12")
    private Double lb12;

    /**
     * 早班计划完成
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb13")
    @ApiModelProperty(value = "早班计划完成", name = "lb13")
    @TableField(value = "LB13")
    private Double lb13;

    /**
     * 夜班顺位
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb14")
    @ApiModelProperty(value = "夜班顺位", name = "lb14")
    @TableField(value = "LB14")
    private Double lb14;

    /**
     * 1#直裁夜班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb15")
    @ApiModelProperty(value = "1#直裁夜班", name = "lb15")
    @TableField(value = "LB15")
    private Double lb15;

    /**
     * 2#直裁夜班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb16")
    @ApiModelProperty(value = "2#直裁夜班", name = "lb16")
    @TableField(value = "LB16")
    private Double lb16;

    /**
     * 3#直裁夜班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb17")
    @ApiModelProperty(value = "3#直裁夜班", name = "lb17")
    @TableField(value = "LB17")
    private Double lb17;

    /**
     * 4#直裁夜班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb18")
    @ApiModelProperty(value = "4#直裁夜班", name = "lb18")
    @TableField(value = "LB18")
    private Double lb18;

    /**
     * 夜班计划完成
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb19")
    @ApiModelProperty(value = "夜班计划完成", name = "lb19")
    @TableField(value = "LB19")
    private Double lb19;

    /**
     * 理论交班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb20")
    @ApiModelProperty(value = "理论交班", name = "lb20")
    @TableField(value = "LB20")
    private Double lb20;

    /**
     * 实际交班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lb21")
    @ApiModelProperty(value = "实际交班", name = "lb21")
    @TableField(value = "LB21")
    private Double lb21;

    /**
     * 2号胎身帘布编号
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt1")
    @ApiModelProperty(value = "2号胎身帘布编号", name = "lbt1")
    @TableField(value = "LBT1")
    private String lbt1;

    /**
     * 2号条米
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt2")
    @ApiModelProperty(value = "2号条米", name = "lbt2")
    @TableField(value = "LBT2")
    private Double lbt2;

    /**
     * 2号卷长
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt3")
    @ApiModelProperty(value = "2号卷长", name = "lbt3")
    @TableField(value = "LBT3")
    private Double lbt3;

    /**
     * 2号理论库存
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt4")
    @ApiModelProperty(value = "2号理论库存", name = "lbt4")
    @TableField(value = "LBT4")
    private Double lbt4;

    /**
     * 2号实际库存
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt5")
    @ApiModelProperty(value = "2号实际库存", name = "lbt5")
    @TableField(value = "LBT5")
    private Double lbt5;

    /**
     * 2号计划用量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt6")
    @ApiModelProperty(value = "2号计划用量", name = "lbt6")
    @TableField(value = "LBT6")
    private Double lbt6;

    /**
     * 2号用量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt7")
    @ApiModelProperty(value = "2号用量", name = "lbt7")
    @TableField(value = "LBT7")
    private Double lbt7;

    /**
     * 2号早班顺位
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt8")
    @ApiModelProperty(value = "2号早班顺位", name = "lbt8")
    @TableField(value = "LBT8")
    private Double lbt8;

    /**
     * 2号1#直裁早班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt9")
    @ApiModelProperty(value = "2号1#直裁早班", name = "lbt9")
    @TableField(value = "LBT9")
    private Double lbt9;

    /**
     * 2号2#直裁早班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt10")
    @ApiModelProperty(value = "2号2#直裁早班", name = "lbt10")
    @TableField(value = "LBT10")
    private Double lbt10;

    /**
     * 2号3#直裁早班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt11")
    @ApiModelProperty(value = "2号3#直裁早班", name = "lbt11")
    @TableField(value = "LBT11")
    private Double lbt11;

    /**
     * 2号4#直裁早班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt12")
    @ApiModelProperty(value = "2号4#直裁早班", name = "lbt12")
    @TableField(value = "LBT12")
    private Double lbt12;

    /**
     * 2号早班计划完成
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt13")
    @ApiModelProperty(value = "2号早班计划完成", name = "lbt13")
    @TableField(value = "LBT13")
    private Double lbt13;

    /**
     * 2号夜班顺序
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt14")
    @ApiModelProperty(value = "2号夜班顺序", name = "lbt14")
    @TableField(value = "LBT14")
    private Double lbt14;

    /**
     * 2号1#直裁夜班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt15")
    @ApiModelProperty(value = "2号1#直裁夜班", name = "lbt15")
    @TableField(value = "LBT15")
    private Double lbt15;

    /**
     * 2号2#直裁夜班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt16")
    @ApiModelProperty(value = "2号2#直裁夜班", name = "lbt16")
    @TableField(value = "LBT16")
    private Double lbt16;

    /**
     * 2号3#直裁夜班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt17")
    @ApiModelProperty(value = "2号3#直裁夜班", name = "lbt17")
    @TableField(value = "LBT17")
    private Double lbt17;

    /**
     * 2号4#直裁夜班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt18")
    @ApiModelProperty(value = "2号4#直裁夜班", name = "lbt18")
    @TableField(value = "LBT18")
    private Double lbt18;

    /**
     * 2号夜班计划完成
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt19")
    @ApiModelProperty(value = "2号夜班计划完成", name = "lbt19")
    @TableField(value = "LBT19")
    private Double lbt19;

    /**
     * 理论交班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt20")
    @ApiModelProperty(value = "理论交班", name = "lbt20")
    @TableField(value = "LBT20")
    private Double lbt20;

    /**
     * 实际交班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.lbt21")
    @ApiModelProperty(value = "实际交班", name = "lbt21")
    @TableField(value = "LBT21")
    private Double lbt21;

    /**
     * 内衬层编号
     */
    @Excel(name = "ui.data.column.halfCdImportBak.nc1")
    @ApiModelProperty(value = "内衬层编号", name = "nc1")
    @TableField(value = "NC1")
    private String nc1;

    /**
     * 条米
     */
    @Excel(name = "ui.data.column.halfCdImportBak.nc2")
    @ApiModelProperty(value = "条米", name = "nc2")
    @TableField(value = "NC2")
    private Double nc2;

    /**
     * 卷长
     */
    @Excel(name = "ui.data.column.halfCdImportBak.nc3")
    @ApiModelProperty(value = "卷长", name = "nc3")
    @TableField(value = "NC3")
    private Double nc3;

    /**
     * 理论库存
     */
    @Excel(name = "ui.data.column.halfCdImportBak.nc4")
    @ApiModelProperty(value = "理论库存", name = "nc4")
    @TableField(value = "NC4")
    private Double nc4;

    /**
     * 实际存量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.nc5")
    @ApiModelProperty(value = "实际存量", name = "nc5")
    @TableField(value = "NC5")
    private Double nc5;

    /**
     * 计划用量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.nc6")
    @ApiModelProperty(value = "计划用量", name = "nc6")
    @TableField(value = "NC6")
    private Double nc6;

    /**
     * 用量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.nc7")
    @ApiModelProperty(value = "用量", name = "nc7")
    @TableField(value = "NC7")
    private Double nc7;

    /**
     * NB11库存米数
     */
    @Excel(name = "ui.data.column.halfCdImportBak.nc8")
    @ApiModelProperty(value = "NB11库存米数", name = "nc8")
    @TableField(value = "NC8")
    private Double nc8;

    /**
     * NB11计划米数
     */
    @Excel(name = "ui.data.column.halfCdImportBak.nc9")
    @ApiModelProperty(value = "NB11计划米数", name = "nc9")
    @TableField(value = "NC9")
    private Double nc9;

    /**
     * 早班计划顺序
     */
    @Excel(name = "ui.data.column.halfCdImportBak.nc10")
    @ApiModelProperty(value = "早班计划顺序", name = "nc10")
    @TableField(value = "NC10")
    private Double nc10;

    /**
     * 早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.nc11")
    @ApiModelProperty(value = "早班计划", name = "nc11")
    @TableField(value = "NC11")
    private Double nc11;

    /**
     *
     */
    @Excel(name = "ui.data.column.halfCdImportBak.nc12")
    @ApiModelProperty(value = "", name = "nc12")
    @TableField(value = "NC12")
    private Double nc12;

    /**
     * 夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.nc13")
    @ApiModelProperty(value = "夜班计划", name = "nc13")
    @TableField(value = "NC13")
    private Double nc13;

    /**
     * 早班计划完成
     */
    @Excel(name = "ui.data.column.halfCdImportBak.nc14")
    @ApiModelProperty(value = "早班计划完成", name = "nc14")
    @TableField(value = "NC14")
    private Double nc14;

    /**
     *
     */
    @Excel(name = "ui.data.column.halfCdImportBak.nc15")
    @ApiModelProperty(value = "", name = "nc15")
    @TableField(value = "NC15")
    private Double nc15;

    /**
     * 夜班计划完成
     */
    @Excel(name = "ui.data.column.halfCdImportBak.nc16")
    @ApiModelProperty(value = "夜班计划完成", name = "nc16")
    @TableField(value = "NC16")
    private Double nc16;

    /**
     * 理论交班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.nc17")
    @ApiModelProperty(value = "理论交班", name = "nc17")
    @TableField(value = "NC17")
    private Double nc17;

    /**
     * 实际交班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.nc18")
    @ApiModelProperty(value = "实际交班", name = "nc18")
    @TableField(value = "NC18")
    private Double nc18;

    /**
     * 1#带束层编号
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd1")
    @ApiModelProperty(value = "1#带束层编号", name = "gd1")
    @TableField(value = "GD1")
    private String gd1;

    /**
     * 条米
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd2")
    @ApiModelProperty(value = "条米", name = "gd2")
    @TableField(value = "GD2")
    private Double gd2;

    /**
     * 卷长
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd3")
    @ApiModelProperty(value = "卷长", name = "gd3")
    @TableField(value = "GD3")
    private Double gd3;

    /**
     * 理论库存
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd4")
    @ApiModelProperty(value = "理论库存", name = "gd4")
    @TableField(value = "GD4")
    private Double gd4;

    /**
     * 实际存量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd5")
    @ApiModelProperty(value = "实际存量", name = "gd5")
    @TableField(value = "GD5")
    private Double gd5;

    /**
     * 计划用量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd6")
    @ApiModelProperty(value = "计划用量", name = "gd6")
    @TableField(value = "GD6")
    private Double gd6;

    /**
     * 用量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd7")
    @ApiModelProperty(value = "用量", name = "gd7")
    @TableField(value = "GD7")
    private Double gd7;

    /**
     * 早班计划顺序
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd8")
    @ApiModelProperty(value = "早班计划顺序", name = "gd8")
    @TableField(value = "GD8")
    private Double gd8;

    /**
     * 1#机早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd9")
    @ApiModelProperty(value = "1#机早班计划", name = "gd9")
    @TableField(value = "GD9")
    private Double gd9;

    /**
     * 2#机早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd10")
    @ApiModelProperty(value = "2#机早班计划", name = "gd10")
    @TableField(value = "GD10")
    private Double gd10;

    /**
     * 3#机早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd11")
    @ApiModelProperty(value = "3#机早班计划", name = "gd11")
    @TableField(value = "GD11")
    private Double gd11;

    /**
     * 4#机早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd12")
    @ApiModelProperty(value = "4#机早班计划", name = "gd12")
    @TableField(value = "GD12")
    private Double gd12;

    /**
     * 早班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd13")
    @ApiModelProperty(value = "早班", name = "gd13")
    @TableField(value = "GD13")
    private Double gd13;

    /**
     * 早班计划完成
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd14")
    @ApiModelProperty(value = "早班计划完成", name = "gd14")
    @TableField(value = "GD14")
    private Double gd14;

    /**
     * 1#机夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd15")
    @ApiModelProperty(value = "1#机夜班计划", name = "gd15")
    @TableField(value = "GD15")
    private Double gd15;

    /**
     * 2#机夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd16")
    @ApiModelProperty(value = "2#机夜班计划", name = "gd16")
    @TableField(value = "GD16")
    private Double gd16;

    /**
     * 3#机夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd17")
    @ApiModelProperty(value = "3#机夜班计划", name = "gd17")
    @TableField(value = "GD17")
    private Double gd17;

    /**
     * 4#机夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd18")
    @ApiModelProperty(value = "4#机夜班计划", name = "gd18")
    @TableField(value = "GD18")
    private Double gd18;

    /**
     * 夜班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd19")
    @ApiModelProperty(value = "夜班", name = "gd19")
    @TableField(value = "GD19")
    private Double gd19;

    /**
     * 夜班计划完成
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd20")
    @ApiModelProperty(value = "夜班计划完成", name = "gd20")
    @TableField(value = "GD20")
    private Double gd20;

    /**
     * 理论交班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd21")
    @ApiModelProperty(value = "理论交班", name = "gd21")
    @TableField(value = "GD21")
    private Double gd21;

    /**
     * 实际交班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gd22")
    @ApiModelProperty(value = "实际交班", name = "gd22")
    @TableField(value = "GD22")
    private Double gd22;

    /**
     * 2#带束层编号
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt1")
    @ApiModelProperty(value = "2#带束层编号", name = "gdt1")
    @TableField(value = "GDT1")
    private String gdt1;

    /**
     * 条米
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt2")
    @ApiModelProperty(value = "条米", name = "gdt2")
    @TableField(value = "GDT2")
    private Double gdt2;

    /**
     * 卷长
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt3")
    @ApiModelProperty(value = "卷长", name = "gdt3")
    @TableField(value = "GDT3")
    private Double gdt3;

    /**
     * 理论库存
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt4")
    @ApiModelProperty(value = "理论库存", name = "gdt4")
    @TableField(value = "GDT4")
    private Double gdt4;

    /**
     * 实际存量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt5")
    @ApiModelProperty(value = "实际存量", name = "gdt5")
    @TableField(value = "GDT5")
    private Double gdt5;

    /**
     * 计划用量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt6")
    @ApiModelProperty(value = "计划用量", name = "gdt6")
    @TableField(value = "GDT6")
    private Double gdt6;

    /**
     * 用量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt7")
    @ApiModelProperty(value = "用量", name = "gdt7")
    @TableField(value = "GDT7")
    private Double gdt7;

    /**
     * 早班计划顺序
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt8")
    @ApiModelProperty(value = "早班计划顺序", name = "gdt8")
    @TableField(value = "GDT8")
    private Double gdt8;

    /**
     * 1#机早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt9")
    @ApiModelProperty(value = "1#机早班计划", name = "gdt9")
    @TableField(value = "GDT9")
    private Double gdt9;

    /**
     * 2#机早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt10")
    @ApiModelProperty(value = "2#机早班计划", name = "gdt10")
    @TableField(value = "GDT10")
    private Double gdt10;

    /**
     * 3#机早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt11")
    @ApiModelProperty(value = "3#机早班计划", name = "gdt11")
    @TableField(value = "GDT11")
    private Double gdt11;

    /**
     * 4#机早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt12")
    @ApiModelProperty(value = "4#机早班计划", name = "gdt12")
    @TableField(value = "GDT12")
    private Double gdt12;

    /**
     * 早班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt13")
    @ApiModelProperty(value = "早班", name = "gdt13")
    @TableField(value = "GDT13")
    private Double gdt13;

    /**
     * 早班计划完成
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt14")
    @ApiModelProperty(value = "早班计划完成", name = "gdt14")
    @TableField(value = "GDT14")
    private Double gdt14;

    /**
     * 1#机夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt15")
    @ApiModelProperty(value = "1#机夜班计划", name = "gdt15")
    @TableField(value = "GDT15")
    private Double gdt15;

    /**
     * 2#机夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt16")
    @ApiModelProperty(value = "2#机夜班计划", name = "gdt16")
    @TableField(value = "GDT16")
    private Double gdt16;

    /**
     * 3#机夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt17")
    @ApiModelProperty(value = "3#机夜班计划", name = "gdt17")
    @TableField(value = "GDT17")
    private Double gdt17;

    /**
     * 4#机夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt18")
    @ApiModelProperty(value = "4#机夜班计划", name = "gdt18")
    @TableField(value = "GDT18")
    private Double gdt18;

    /**
     * 夜班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt19")
    @ApiModelProperty(value = "夜班", name = "gdt19")
    @TableField(value = "GDT19")
    private Double gdt19;

    /**
     * 夜班计划完成
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt20")
    @ApiModelProperty(value = "夜班计划完成", name = "gdt20")
    @TableField(value = "GDT20")
    private Double gdt20;

    /**
     * 理论交班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt21")
    @ApiModelProperty(value = "理论交班", name = "gdt21")
    @TableField(value = "GDT21")
    private Double gdt21;

    /**
     * 实际交班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gdt22")
    @ApiModelProperty(value = "实际交班", name = "gdt22")
    @TableField(value = "GDT22")
    private Double gdt22;

    /**
     * 子口布
     */
    @Excel(name = "ui.data.column.halfCdImportBak.zk1")
    @ApiModelProperty(value = "子口布", name = "zk1")
    @TableField(value = "ZK1")
    private String zk1;

    /**
     * 条米
     */
    @Excel(name = "ui.data.column.halfCdImportBak.zk2")
    @ApiModelProperty(value = "条米", name = "zk2")
    @TableField(value = "ZK2")
    private Double zk2;

    /**
     * 卷长
     */
    @Excel(name = "ui.data.column.halfCdImportBak.zk3")
    @ApiModelProperty(value = "卷长", name = "zk3")
    @TableField(value = "ZK3")
    private Double zk3;

    /**
     * 内衬存量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.zk4")
    @ApiModelProperty(value = "内衬存量", name = "zk4")
    @TableField(value = "ZK4")
    private String zk4;

    /**
     * 计划用量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.zk5")
    @ApiModelProperty(value = "计划用量", name = "zk5")
    @TableField(value = "ZK5")
    private String zk5;

    /**
     * 子口包布存量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.zk6")
    @ApiModelProperty(value = "子口包布存量", name = "zk6")
    @TableField(value = "ZK6")
    private String zk6;

    /**
     * 早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.zk7")
    @ApiModelProperty(value = "早班计划", name = "zk7")
    @TableField(value = "ZK7")
    private String zk7;

    /**
     *
     */
    @Excel(name = "ui.data.column.halfCdImportBak.zk8")
    @ApiModelProperty(value = "", name = "zk8")
    @TableField(value = "ZK8")
    private String zk8;

    /**
     * 夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.zk9")
    @ApiModelProperty(value = "夜班计划", name = "zk9")
    @TableField(value = "ZK9")
    private String zk9;

    /**
     * 早班计划完成
     */
    @Excel(name = "ui.data.column.halfCdImportBak.zk10")
    @ApiModelProperty(value = "早班计划完成", name = "zk10")
    @TableField(value = "ZK10")
    private String zk10;

    /**
     *
     */
    @Excel(name = "ui.data.column.halfCdImportBak.zk11")
    @ApiModelProperty(value = "", name = "zk11")
    @TableField(value = "ZK11")
    private String zk11;

    /**
     * 夜班计划完成
     */
    @Excel(name = "ui.data.column.halfCdImportBak.zk12")
    @ApiModelProperty(value = "夜班计划完成", name = "zk12")
    @TableField(value = "ZK12")
    private String zk12;

    /**
     * NB11总量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.zk13")
    @ApiModelProperty(value = "NB11总量", name = "zk13")
    @TableField(value = "ZK13")
    private String zk13;

    /**
     * 存量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.zk14")
    @ApiModelProperty(value = "存量", name = "zk14")
    @TableField(value = "ZK14")
    private String zk14;

    /**
     * 裸胎圈编号
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gsq1")
    @ApiModelProperty(value = "裸胎圈编号", name = "gsq1")
    @TableField(value = "GSQ1")
    private String gsq1;

    /**
     * 理论库存
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gsq2")
    @ApiModelProperty(value = "理论库存", name = "gsq2")
    @TableField(value = "GSQ2")
    private Double gsq2;

    /**
     * 实际存量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gsq3")
    @ApiModelProperty(value = "实际存量", name = "gsq3")
    @TableField(value = "GSQ3")
    private Double gsq3;

    /**
     * 计划用量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gsq4")
    @ApiModelProperty(value = "计划用量", name = "gsq4")
    @TableField(value = "GSQ4")
    private Double gsq4;

    /**
     * 用量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gsq5")
    @ApiModelProperty(value = "用量", name = "gsq5")
    @TableField(value = "GSQ5")
    private Double gsq5;

    /**
     * 1#机早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gsq6")
    @ApiModelProperty(value = "1#机早班计划", name = "gsq6")
    @TableField(value = "GSQ6")
    private Double gsq6;

    /**
     * 3#机早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gsq7")
    @ApiModelProperty(value = "3#机早班计划", name = "gsq7")
    @TableField(value = "GSQ7")
    private Double gsq7;

    /**
     * 4#机早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gsq8")
    @ApiModelProperty(value = "4#机早班计划", name = "gsq8")
    @TableField(value = "GSQ8")
    private Double gsq8;

    /**
     * 早班计划完成
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gsq9")
    @ApiModelProperty(value = "早班计划完成", name = "gsq9")
    @TableField(value = "GSQ9")
    private Double gsq9;

    /**
     * 1#机夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gsq10")
    @ApiModelProperty(value = "1#机夜班计划", name = "gsq10")
    @TableField(value = "GSQ10")
    private Double gsq10;

    /**
     * 3#机夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gsq11")
    @ApiModelProperty(value = "3#机夜班计划", name = "gsq11")
    @TableField(value = "GSQ11")
    private Double gsq11;

    /**
     * 4#机夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gsq12")
    @ApiModelProperty(value = "4#机夜班计划", name = "gsq12")
    @TableField(value = "GSQ12")
    private Double gsq12;

    /**
     * 夜班计划完成
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gsq13")
    @ApiModelProperty(value = "夜班计划完成", name = "gsq13")
    @TableField(value = "GSQ13")
    private Double gsq13;

    /**
     * 合计
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gsq14")
    @ApiModelProperty(value = "合计", name = "gsq14")
    @TableField(value = "GSQ14")
    private Double gsq14;

    /**
     * 交班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.gsq15")
    @ApiModelProperty(value = "交班", name = "gsq15")
    @TableField(value = "GSQ15")
    private Double gsq15;

    /**
     * 胎圈编号
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq1")
    @ApiModelProperty(value = "胎圈编号", name = "tq1")
    @TableField(value = "TQ1")
    private String tq1;

    /**
     * 宽度
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq2")
    @ApiModelProperty(value = "宽度", name = "tq2")
    @TableField(value = "TQ2")
    private Double tq2;

    /**
     * 理论库存
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq3")
    @ApiModelProperty(value = "理论库存", name = "tq3")
    @TableField(value = "TQ3")
    private Double tq3;

    /**
     * 实际存量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq4")
    @ApiModelProperty(value = "实际存量", name = "tq4")
    @TableField(value = "TQ4")
    private Double tq4;

    /**
     * 计划用量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq5")
    @ApiModelProperty(value = "计划用量", name = "tq5")
    @TableField(value = "TQ5")
    private Double tq5;

    /**
     * 用量
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq6")
    @ApiModelProperty(value = "用量", name = "tq6")
    @TableField(value = "TQ6")
    private Double tq6;

    /**
     * 早班计划顺序
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq7")
    @ApiModelProperty(value = "早班计划顺序", name = "tq7")
    @TableField(value = "TQ7")
    private Double tq7;

    /**
     * 1号早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq8")
    @ApiModelProperty(value = "1号早班计划", name = "tq8")
    @TableField(value = "TQ8")
    private Double tq8;

    /**
     * 2号早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq9")
    @ApiModelProperty(value = "2号早班计划", name = "tq9")
    @TableField(value = "TQ9")
    private Double tq9;

    /**
     * 3号早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq10")
    @ApiModelProperty(value = "3号早班计划", name = "tq10")
    @TableField(value = "TQ10")
    private Double tq10;

    /**
     * 4号早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq11")
    @ApiModelProperty(value = "4号早班计划", name = "tq11")
    @TableField(value = "TQ11")
    private Double tq11;

    /**
     * 5号早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq12")
    @ApiModelProperty(value = "5号早班计划", name = "tq12")
    @TableField(value = "TQ12")
    private Double tq12;

    /**
     * 7号早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq13")
    @ApiModelProperty(value = "7号早班计划", name = "tq13")
    @TableField(value = "TQ13")
    private Double tq13;

    /**
     * 8号早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq14")
    @ApiModelProperty(value = "8号早班计划", name = "tq14")
    @TableField(value = "TQ14")
    private Double tq14;

    /**
     * 9号早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq15")
    @ApiModelProperty(value = "9号早班计划", name = "tq15")
    @TableField(value = "TQ15")
    private Double tq15;

    /**
     * 12号早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq16")
    @ApiModelProperty(value = "12号早班计划", name = "tq16")
    @TableField(value = "TQ16")
    private Double tq16;

    /**
     * 13号早班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq17")
    @ApiModelProperty(value = "13号早班计划", name = "tq17")
    @TableField(value = "TQ17")
    private Double tq17;

    /**
     * 早班计划完成
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq18")
    @ApiModelProperty(value = "早班计划完成", name = "tq18")
    @TableField(value = "TQ18")
    private Double tq18;

    /**
     * 1号夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq19")
    @ApiModelProperty(value = "1号夜班计划", name = "tq19")
    @TableField(value = "TQ19")
    private Double tq19;

    /**
     * 2号夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq20")
    @ApiModelProperty(value = "2号夜班计划", name = "tq20")
    @TableField(value = "TQ20")
    private Double tq20;

    /**
     * 3号夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq21")
    @ApiModelProperty(value = "3号夜班计划", name = "tq21")
    @TableField(value = "TQ21")
    private Double tq21;

    /**
     * 4号夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq22")
    @ApiModelProperty(value = "4号夜班计划", name = "tq22")
    @TableField(value = "TQ22")
    private Double tq22;

    /**
     * 5号夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq23")
    @ApiModelProperty(value = "5号夜班计划", name = "tq23")
    @TableField(value = "TQ23")
    private Double tq23;

    /**
     * 7号夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq24")
    @ApiModelProperty(value = "7号夜班计划", name = "tq24")
    @TableField(value = "TQ24")
    private Double tq24;

    /**
     * 8号夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq25")
    @ApiModelProperty(value = "8号夜班计划", name = "tq25")
    @TableField(value = "TQ25")
    private Double tq25;

    /**
     * 9号夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq26")
    @ApiModelProperty(value = "9号夜班计划", name = "tq26")
    @TableField(value = "TQ26")
    private Double tq26;

    /**
     * 12号夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq27")
    @ApiModelProperty(value = "12号夜班计划", name = "tq27")
    @TableField(value = "TQ27")
    private Double tq27;

    /**
     * 13号夜班计划
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq28")
    @ApiModelProperty(value = "13号夜班计划", name = "tq28")
    @TableField(value = "TQ28")
    private Double tq28;

    /**
     * 夜班计划完成
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq29")
    @ApiModelProperty(value = "夜班计划完成", name = "tq29")
    @TableField(value = "TQ29")
    private Double tq29;

    /**
     * 合计
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq30")
    @ApiModelProperty(value = "合计", name = "tq30")
    @TableField(value = "TQ30")
    private Double tq30;

    /**
     * 交班
     */
    @Excel(name = "ui.data.column.halfCdImportBak.tq31")
    @ApiModelProperty(value = "交班", name = "tq31")
    @TableField(value = "TQ31")
    private Double tq31;

    /**
     * 库存日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.halfCdImportBak.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "库存日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;


}