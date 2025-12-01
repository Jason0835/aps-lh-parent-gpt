package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmConstructionInfo.java
 * 描    述：投产胎胚施工信息对象 t_mdm_construction_info
 *@author zlt
 *@date 2025-02-24
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "投产胎胚施工信息对象", description = "投产胎胚施工信息对象 ")
@Data
@TableName(value = "T_MDM_CONSTRUCTION_INFO")
public class MdmConstructionInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** SAP品号 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.sapCode")
    @ApiModelProperty(value = "SAP品号", name = "sapCode")
    @TableField(value = "SAP_CODE")
    private String sapCode;

    /** 规格代码 */
    @ApiModelProperty(value = "规格代码", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /** 胎胚代码(代码)施工号 */
    @Excel(name = "ui.data.column.mdmConstructionInfo.constructionCode")
    @ApiModelProperty(value = "胎胚代码(代码)施工号", name = "constructionCode")
    @TableField(value = "CONSTRUCTION_CODE")
    private String constructionCode;

    /** BOM版本(存储版本号) */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.constructionVersion")
    @ApiModelProperty(value = "BOM版本(存储版本号)", name = "constructionVersion")
    @TableField(value = "CONSTRUCTION_VERSION")
    private String constructionVersion;

    /** 分厂成型法 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.mouldMethod")
    @ApiModelProperty(value = "分厂成型法", name = "mouldMethod")
    @TableField(value = "MOULD_METHOD")
    private String mouldMethod;

    /** 寸口（保留2位小数） */
    @Excel(name = "ui.data.column.mdmConstructionInfo.proSize")
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

    /** 规格 */
    @Excel(name = "ui.data.column.mdmConstructionInfo.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /** 机头宽度 */
    @Excel(name = "ui.data.column.mdmConstructionInfo.headWidth")
    @ApiModelProperty(value = "机头宽度", name = "headWidth")
    @TableField(value = "HEAD_WIDTH")
    private BigDecimal headWidth;

    /** 扣圈盘直径 */
    @Excel(name = "ui.data.column.mdmConstructionInfo.bucklePlageDiameter")
    @ApiModelProperty(value = "扣圈盘直径", name = "bucklePlageDiameter")
    @TableField(value = "BUCKLE_PLAGE_DIAMETER")
    private BigDecimal bucklePlageDiameter;

    /** 断面宽 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.sectionWidth")
    @ApiModelProperty(value = "断面宽", name = "sectionWidth")
    @TableField(value = "SECTION_WIDTH")
    private BigDecimal sectionWidth;

    /** 贴合鼓周长 */
    @Excel(name = "ui.data.column.mdmConstructionInfo.fitDrumPerimeter")
    @ApiModelProperty(value = "贴合鼓周长", name = "fitDrumPerimeter")
    @TableField(value = "FIT_DRUM_PERIMETER")
    private BigDecimal fitDrumPerimeter;

    /** 卡盘直径 */
    @Excel(name = "ui.data.column.mdmConstructionInfo.chuckDiameter")
    @ApiModelProperty(value = "卡盘直径", name = "chuckDiameter")
    @TableField(value = "CHUCK_DIAMETER")
    private BigDecimal chuckDiameter;

    /** 拉伸宽度 */
    @Excel(name = "ui.data.column.mdmConstructionInfo.stretchWidth")
    @ApiModelProperty(value = "拉伸宽度", name = "stretchWidth")
    @TableField(value = "STRETCH_WIDTH")
    private BigDecimal stretchWidth;

    /** 定性宽度 */
    @Excel(name = "ui.data.column.mdmConstructionInfo.qualitativeWidth")
    @ApiModelProperty(value = "定性宽度", name = "qualitativeWidth")
    @TableField(value = "QUALITATIVE_WIDTH")
    private BigDecimal qualitativeWidth;

    /** 胎胚周长 */
    @Excel(name = "ui.data.column.mdmConstructionInfo.embryoCircle")
    @ApiModelProperty(value = "胎胚周长", name = "embryoCircle")
    @TableField(value = "EMBRYO_CIRCLE")
    private BigDecimal embryoCircle;

    /** 1#胎体布代号 */
    @Excel(name = "ui.data.column.mdmConstructionInfo.tireFabricCode1")
    @ApiModelProperty(value = "1#胎体布代号", name = "tireFabricCode1")
    @TableField(value = "TIRE_FABRIC_CODE1")
    private String tireFabricCode1;

    /** 1#胎体布SAP */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.tireFabricSap1")
    @ApiModelProperty(value = "1#胎体布SAP", name = "tireFabricSap1")
    @TableField(value = "TIRE_FABRIC_SAP1")
    private String tireFabricSap1;

    /** 1#胎体布BOM版本 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.tireFabric1Version")
    @ApiModelProperty(value = "1#胎体布BOM版本", name = "tireFabric1Version")
    @TableField(value = "TIRE_FABRIC1_VERSION")
    private String tireFabric1Version;

    /** 1#胎体布工艺 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.tireFabricCraft1")
    @ApiModelProperty(value = "1#胎体布工艺", name = "tireFabricCraft1")
    @TableField(value = "TIRE_FABRIC_CRAFT1")
    private String tireFabricCraft1;

    /** 2#胎体布代号 */
    @Excel(name = "ui.data.column.mdmConstructionInfo.tireFabricCode2")
    @ApiModelProperty(value = "2#胎体布代号", name = "tireFabricCode2")
    @TableField(value = "TIRE_FABRIC_CODE2")
    private String tireFabricCode2;

    /** 2#胎体布SAP */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.tireFabricSap2")
    @ApiModelProperty(value = "2#胎体布SAP", name = "tireFabricSap2")
    @TableField(value = "TIRE_FABRIC_SAP2")
    private String tireFabricSap2;

    /** 2#胎体布BOM版本 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.tireFabric2Version")
    @ApiModelProperty(value = "2#胎体布BOM版本", name = "tireFabric2Version")
    @TableField(value = "TIRE_FABRIC2_VERSION")
    private String tireFabric2Version;

    /** 2#胎体布工艺 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.tireFabricCraft2")
    @ApiModelProperty(value = "2#胎体布工艺", name = "tireFabricCraft2")
    @TableField(value = "TIRE_FABRIC_CRAFT2")
    private String tireFabricCraft2;

    /** 3#胎体布代号 */
    @Excel(name = "ui.data.column.mdmConstructionInfo.tireFabricCode3")
    @ApiModelProperty(value = "3#胎体布代号", name = "tireFabricCode3")
    @TableField(value = "TIRE_FABRIC_CODE3")
    private String tireFabricCode3;

    /** 3#胎体布SAP */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.tireFabricSap3")
    @ApiModelProperty(value = "3#胎体布SAP", name = "tireFabricSap3")
    @TableField(value = "TIRE_FABRIC_SAP3")
    private String tireFabricSap3;

    /** 3#胎体布BOM版本 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.tireFabric3Version")
    @ApiModelProperty(value = "3#胎体布BOM版本", name = "tireFabric3Version")
    @TableField(value = "TIRE_FABRIC3_VERSION")
    private String tireFabric3Version;

    /** 3#胎体布工艺 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.tireFabricCraft3")
    @ApiModelProperty(value = "3#胎体布工艺", name = "tireFabricCraft3")
    @TableField(value = "TIRE_FABRIC_CRAFT3")
    private String tireFabricCraft3;

    /** 原线代码 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.originalLineCode")
    @ApiModelProperty(value = "原线代码", name = "originalLineCode")
    @TableField(value = "ORIGINAL_LINE_CODE")
    private String originalLineCode;

    /** 帘线规格 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.cordSpec")
    @ApiModelProperty(value = "帘线规格", name = "cordSpec")
    @TableField(value = "CORD_SPEC")
    private String cordSpec;

    /** 帘布大卷SAP */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.cordSap")
    @ApiModelProperty(value = "帘布大卷SAP", name = "cordSap")
    @TableField(value = "CORD_SAP")
    private String cordSap;

    /** 帘布大卷SAP—BOM版本 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.cordVersion")
    @ApiModelProperty(value = "帘布大卷SAP—BOM版本", name = "cordVersion")
    @TableField(value = "CORD_VERSION")
    private String cordVersion;

    /** 补强/封口胶 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.reinforceSealGlue")
    @ApiModelProperty(value = "补强/封口胶", name = "reinforceSealGlue")
    @TableField(value = "REINFORCE_SEAL_GLUE")
    private String reinforceSealGlue;

    /** 内衬胶料 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.insideRubber")
    @ApiModelProperty(value = "内衬胶料", name = "insideRubber")
    @TableField(value = "INSIDE_RUBBER")
    private String insideRubber;

    /** 内衬代号 */
    @Excel(name = "ui.data.column.mdmConstructionInfo.insideCode")
    @ApiModelProperty(value = "内衬代号", name = "insideCode")
    @TableField(value = "INSIDE_CODE")
    private String insideCode;

    /** 内衬SAP */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.insideSap")
    @ApiModelProperty(value = "内衬SAP", name = "insideSap")
    @TableField(value = "INSIDE_SAP")
    private String insideSap;

    /** 内衬—BOM版本 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.insideVersion")
    @ApiModelProperty(value = "内衬—BOM版本", name = "insideVersion")
    @TableField(value = "INSIDE_VERSION")
    private String insideVersion;

    /** 内衬工艺 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.insideCraft")
    @ApiModelProperty(value = "内衬工艺", name = "insideCraft")
    @TableField(value = "INSIDE_CRAFT")
    private BigDecimal insideCraft;

    /** 胎侧代号 */
    @Excel(name = "ui.data.column.mdmConstructionInfo.sidewallCode")
    @ApiModelProperty(value = "胎侧代号", name = "sidewallCode")
    @TableField(value = "SIDEWALL_CODE")
    private String sidewallCode;

    /** 胎侧SAP */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.sidewallSap")
    @ApiModelProperty(value = "胎侧SAP", name = "sidewallSap")
    @TableField(value = "SIDEWALL_SAP")
    private String sidewallSap;

    /** 胎侧—BOM版本 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.sidewallVersion")
    @ApiModelProperty(value = "胎侧—BOM版本", name = "sidewallVersion")
    @TableField(value = "SIDEWALL_VERSION")
    private String sidewallVersion;

    /** 胎侧工艺 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.sidewallCraft")
    @ApiModelProperty(value = "胎侧工艺", name = "sidewallCraft")
    @TableField(value = "SIDEWALL_CRAFT")
    private BigDecimal sidewallCraft;

    /** 胎侧口型 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.sidewallMouthPlate")
    @ApiModelProperty(value = "胎侧口型", name = "sidewallMouthPlate")
    @TableField(value = "SIDEWALL_MOUTH_PLATE")
    private String sidewallMouthPlate;

    /** 胎侧居中 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.sidewallCenter")
    @ApiModelProperty(value = "胎侧居中", name = "sidewallCenter")
    @TableField(value = "SIDEWALL_CENTER")
    private String sidewallCenter;

    /** 胎侧长度 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.sidewallLength")
    @ApiModelProperty(value = "胎侧长度", name = "sidewallLength")
    @TableField(value = "SIDEWALL_LENGTH")
    private BigDecimal sidewallLength;

    /** 胎侧胶料 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.sidewallRubber")
    @ApiModelProperty(value = "胎侧胶料", name = "sidewallRubber")
    @TableField(value = "SIDEWALL_RUBBER")
    private String sidewallRubber;

    /** 胎侧胶重量 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.sidewallWeight")
    @ApiModelProperty(value = "胎侧胶重量", name = "sidewallWeight")
    @TableField(value = "SIDEWALL_WEIGHT")
    private String sidewallWeight;

    /** 胎侧耐磨胶重量 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.sidewallWearpRubberWeight")
    @ApiModelProperty(value = "胎侧耐磨胶重量", name = "sidewallWearpRubberWeight")
    @TableField(value = "SIDEWALL_WEARP_RUBBER_WEIGHT")
    private String sidewallWearpRubberWeight;

    /** 支撑胶代号 */
    @Excel(name = "ui.data.column.mdmConstructionInfo.supportCode")
    @ApiModelProperty(value = "支撑胶代号", name = "supportCode")
    @TableField(value = "SUPPORT_CODE")
    private String supportCode;

    /** 支撑胶料 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.supportRubberCode")
    @ApiModelProperty(value = "支撑胶料", name = "supportRubberCode")
    @TableField(value = "SUPPORT_RUBBER_CODE")
    private String supportRubberCode;

    /** 支撑胶长度 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.supportLength")
    @ApiModelProperty(value = "支撑胶长度", name = "supportLength")
    @TableField(value = "SUPPORT_LENGTH")
    private BigDecimal supportLength;

    /** 钢丝圈代码 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.beadCode")
    @ApiModelProperty(value = "钢丝圈代码", name = "beadCode")
    @TableField(value = "BEAD_CODE")
    private String beadCode;

    /** 钢丝圈SAP */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.beadSap")
    @ApiModelProperty(value = "钢丝圈SAP", name = "beadSap")
    @TableField(value = "BEAD_SAP")
    private String beadSap;

    /** 钢丝圈—BOM版本 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.beadVersion")
    @ApiModelProperty(value = "钢丝圈—BOM版本", name = "beadVersion")
    @TableField(value = "BEAD_VERSION")
    private String beadVersion;

    /** 钢丝圈排列 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.beadArrange")
    @ApiModelProperty(value = "钢丝圈排列", name = "beadArrange")
    @TableField(value = "BEAD_ARRANGE")
    private String beadArrange;

    /** 钢丝圈类型 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.beadType")
    @ApiModelProperty(value = "钢丝圈类型", name = "beadType")
    @TableField(value = "BEAD_TYPE")
    private String beadType;

    /** 胎圈代码 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.tireRingCode")
    @ApiModelProperty(value = "胎圈代码", name = "tireRingCode")
    @TableField(value = "TIRE_RING_CODE")
    private String tireRingCode;

    /** 胎圈SAP */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.tireRingSap")
    @ApiModelProperty(value = "胎圈SAP", name = "tireRingSap")
    @TableField(value = "TIRE_RING_SAP")
    private String tireRingSap;

    /** 胎圈—BOM版本 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.tireRingVersion")
    @ApiModelProperty(value = "胎圈—BOM版本", name = "tireRingVersion")
    @TableField(value = "TIRE_RING_VERSION")
    private String tireRingVersion;

    /** 三角胶代码 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.apexCode")
    @ApiModelProperty(value = "三角胶代码", name = "apexCode")
    @TableField(value = "APEX_CODE")
    private String apexCode;

    /** 六边形圈胶料 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.hexagonRubberCode")
    @ApiModelProperty(value = "六边形圈胶料", name = "hexagonRubberCode")
    @TableField(value = "HEXAGON_RUBBER_CODE")
    private String hexagonRubberCode;

    /** 六边形口型 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.hexagonMouthPlate")
    @ApiModelProperty(value = "六边形口型", name = "hexagonMouthPlate")
    @TableField(value = "HEXAGON_MOUTH_PLATE")
    private String hexagonMouthPlate;

    /** 六边形圈尺寸 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.hexagonRubberDimension")
    @ApiModelProperty(value = "六边形圈尺寸", name = "hexagonRubberDimension")
    @TableField(value = "HEXAGON_RUBBER_DIMENSION")
    private String hexagonRubberDimension;

    /** 三角胶重量 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.apexWeight")
    @ApiModelProperty(value = "三角胶重量", name = "apexWeight")
    @TableField(value = "APEX_WEIGHT")
    private BigDecimal apexWeight;

    /** 1#钢带代号 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.beltCode1")
    @ApiModelProperty(value = "1#钢带代号", name = "beltCode1")
    @TableField(value = "BELT_CODE1")
    private String beltCode1;

    /** 1#钢带SAP */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.beltSap1")
    @ApiModelProperty(value = "1#钢带SAP", name = "beltSap1")
    @TableField(value = "BELT_SAP1")
    private String beltSap1;

    /** 1#钢带BOM版本 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.belt1Version")
    @ApiModelProperty(value = "1#钢带BOM版本", name = "belt1Version")
    @TableField(value = "BELT1_VERSION")
    private String belt1Version;

    /** 1#钢带工艺 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.beltCraft1")
    @ApiModelProperty(value = "1#钢带工艺", name = "beltCraft1")
    @TableField(value = "BELT_CRAFT1")
    private BigDecimal beltCraft1;

    /** 1#钢带边胶 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.beltSideRubber1")
    @ApiModelProperty(value = "1#钢带边胶", name = "beltSideRubber1")
    @TableField(value = "BELT_SIDE_RUBBER1")
    private String beltSideRubber1;

    /** 1#钢带胶料 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.beltRubber1")
    @ApiModelProperty(value = "1#钢带胶料", name = "beltRubber1")
    @TableField(value = "BELT_RUBBER1")
    private String beltRubber1;

    /** 2#钢带代号 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.beltCode2")
    @ApiModelProperty(value = "2#钢带代号", name = "beltCode2")
    @TableField(value = "BELT_CODE2")
    private String beltCode2;

    /** 2#钢带SAP */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.beltSap2")
    @ApiModelProperty(value = "2#钢带SAP", name = "beltSap2")
    @TableField(value = "BELT_SAP2")
    private String beltSap2;

    /** 2#钢带BOM版本 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.belt2Version")
    @ApiModelProperty(value = "2#钢带BOM版本", name = "belt2Version")
    @TableField(value = "BELT2_VERSION")
    private String belt2Version;

    /** 2#钢带工艺 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.beltCraft2")
    @ApiModelProperty(value = "2#钢带工艺", name = "beltCraft2")
    @TableField(value = "BELT_CRAFT2")
    private BigDecimal beltCraft2;

    /** 2#钢带边胶 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.beltSideRubber2")
    @ApiModelProperty(value = "2#钢带边胶", name = "beltSideRubber2")
    @TableField(value = "BELT_SIDE_RUBBER2")
    private String beltSideRubber2;

    /** 2#钢带胶料 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.beltRubber2")
    @ApiModelProperty(value = "2#钢带胶料", name = "beltRubber2")
    @TableField(value = "BELT_RUBBER2")
    private String beltRubber2;

    /** 钢带裁断角度 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.beltCuttingAngle")
    @ApiModelProperty(value = "钢带裁断角度", name = "beltCuttingAngle")
    @TableField(value = "BELT_CUTTING_ANGLE")
    private String beltCuttingAngle;

    /** 钢带规格 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.articleCrownSpec")
    @ApiModelProperty(value = "钢带规格", name = "articleCrownSpec")
    @TableField(value = "ARTICLE_CROWN_SPEC")
    private String articleCrownSpec;

    /** 钢压大卷SAP */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.articleCrownSap")
    @ApiModelProperty(value = "钢压大卷SAP", name = "articleCrownSap")
    @TableField(value = "ARTICLE_CROWN_SAP")
    private String articleCrownSap;

    /** 钢压大卷BOM版本 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.articleCrownVersion")
    @ApiModelProperty(value = "钢压大卷BOM版本", name = "articleCrownVersion")
    @TableField(value = "ARTICLE_CROWN_VERSION")
    private String articleCrownVersion;

    /** 冠带条代号 */
    @Excel(name = "ui.data.column.mdmConstructionInfo.articleCrownCode")
    @ApiModelProperty(value = "冠带条代号", name = "articleCrownCode")
    @TableField(value = "ARTICLE_CROWN_CODE")
    private String articleCrownCode;

    /** 胎面代号 */
    @Excel(name = "ui.data.column.mdmConstructionInfo.treadCode")
    @ApiModelProperty(value = "胎面代号", name = "treadCode")
    @TableField(value = "TREAD_CODE")
    private String treadCode;

    /** 胎面SAP */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.treadSap")
    @ApiModelProperty(value = "胎面SAP", name = "treadSap")
    @TableField(value = "TREAD_SAP")
    private String treadSap;

    /** 胎面BOM版本 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.treadVersion")
    @ApiModelProperty(value = "胎面BOM版本", name = "treadVersion")
    @TableField(value = "TREAD_VERSION")
    private String treadVersion;

    /** 胎面宽 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.treadShoulderWidth")
    @ApiModelProperty(value = "胎面宽", name = "treadShoulderWidth")
    @TableField(value = "TREAD_SHOULDER_WIDTH")
    private BigDecimal treadShoulderWidth;

    /** 胎面肩宽 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.treadShoulderJwidth")
    @ApiModelProperty(value = "胎面肩宽", name = "treadShoulderJwidth")
    @TableField(value = "TREAD_SHOULDER_JWIDTH")
    private BigDecimal treadShoulderJwidth;

    /** 胎面长 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.treadShoulderLength")
    @ApiModelProperty(value = "胎面长", name = "treadShoulderLength")
    @TableField(value = "TREAD_SHOULDER_LENGTH")
    private BigDecimal treadShoulderLength;

    /** 胎面胶种 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.treadRubberCategory")
    @ApiModelProperty(value = "胎面胶种", name = "treadRubberCategory")
    @TableField(value = "TREAD_RUBBER_CATEGORY")
    private String treadRubberCategory;

    /** 重量kg/条（上胎冠） */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.tireCrownUpWidthWeight", readConverterExp = "上=胎冠")
    @ApiModelProperty(value = "重量kg/条", name = "tireCrownUpWidthWeight")
    @TableField(value = "TIRE_CROWN_UP_WIDTH_WEIGHT")
    private BigDecimal tireCrownUpWidthWeight;

    /** 重量kg/条（下胎冠） */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.tireCrownDownWidthWeight", readConverterExp = "下=胎冠")
    @ApiModelProperty(value = "重量kg/条", name = "tireCrownDownWidthWeight")
    @TableField(value = "TIRE_CROWN_DOWN_WIDTH_WEIGHT")
    private BigDecimal tireCrownDownWidthWeight;

    /** 重量kg/条（胎翼） */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.tireWingWidthWeight", readConverterExp = "胎=翼")
    @ApiModelProperty(value = "重量kg/条", name = "tireWingWidthWeight")
    @TableField(value = "TIRE_WING_WIDTH_WEIGHT")
    private BigDecimal tireWingWidthWeight;

    /** 重量kg/条（底胶） */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.primerWeight", readConverterExp = "底=胶")
    @ApiModelProperty(value = "重量kg/条", name = "primerWeight")
    @TableField(value = "PRIMER_WEIGHT")
    private BigDecimal primerWeight;

    /** 重量kg/条（导电胶） */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.conductingResinWeight", readConverterExp = "导=电胶")
    @ApiModelProperty(value = "重量kg/条", name = "conductingResinWeight")
    @TableField(value = "CONDUCTING_RESIN_WEIGHT")
    private BigDecimal conductingResinWeight;

    /** 胎面口型板 */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.treadMouthPlate")
    @ApiModelProperty(value = "胎面口型板", name = "treadMouthPlate")
    @TableField(value = "TREAD_MOUTH_PLATE")
    private String treadMouthPlate;

    /** 合模压力            PA */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.mouldClampingPressure")
    @ApiModelProperty(value = "合模压力            PA", name = "mouldClampingPressure")
    @TableField(value = "MOULD_CLAMPING_PRESSURE")
    private BigDecimal mouldClampingPressure;

    /** 机械硫化时间(秒)*/
//    @Excel(name = "ui.data.column.mdmConstructionInfo.curingTime")
    @ApiModelProperty(value = "机械硫化时间(秒)", name = "curingTime")
    @TableField(value = "CURING_TIME")
    private Integer curingTime;

    /**
     * 液压硫化时间(秒)
     */
//    @Excel(name = "ui.data.column.mdmProductInfo.hydraulicPressureCuringTime")
    @ApiModelProperty(value = "液压硫化时间(秒)", name = "hydraulicPressureCuringTime")
    @TableField(value = "HYDRAULIC_PRESSURE_CURING_TIME")
    private Integer hydraulicPressureCuringTime;

    /**
     * 模具型腔
     */
    @ApiModelProperty(value = "模具型腔", name = "moldCavity")
    @TableField(value = "MOLD_CAVITY")
    private String moldCavity;

    /** 生产阶段（0：投产阶段；1试制阶段） */
//    @Excel(name = "ui.data.column.mdmConstructionInfo.productionStage", readConverterExp = "0=投产阶段,1=试制阶段")
    @ApiModelProperty(value = "生产阶段", name = "productionStage")
    @TableField(value = "PRODUCTION_STAGE")
    private String productionStage;


}