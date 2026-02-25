package com.zlt.aps.mp.api.domain.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 投产施工信息对象 t_product_construction_info
 *
 * @author zlt
 * @date 2021-12-02
 */
@ApiModel(value = "投产施工信息对象", description = "投产施工信息对象 ")
@Data
public class CxProductConstructionInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 胎胚SAP
     */
    @Excel(name = "ui.data.column.productConstruction.sapCode")
    @ApiModelProperty(value = "胎胚SAP")
    @TableField(value = "SAP_CODE")
    private String sapCode;

    /**
     * 胎胚代码
     */
    @Excel(name = "ui.data.column.productConstruction.embryoCode")
    @ApiModelProperty(value = "胎胚代码")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /**
     * BOM版本
     */
    @Excel(name = "ui.data.column.productConstruction.embryoVersion")
    @ApiModelProperty(value = "BOM版本")
    @TableField(value = "EMBRYO_VERSION")
    private String embryoVersion;

    /**
     * 生产阶段（0：投产阶段；1试做阶段）
     */
    @Excel(name = "ui.data.column.productConstruction.productionStage", dictType = "PRODUCTION_STAGE")
    @ApiModelProperty(value = "生产阶段（0：投产阶段；1试做阶段）")
    @TableField(value = "PRODUCTION_STAGE")
    private String productionStage;

    /**
     * 施工类型
     */
    @Excel(name = "ui.data.column.productConstruction.consType")
    @ApiModelProperty(value = "施工类型")
    @TableField(value = "CONS_TYPE")
    private Integer consType;

    /**
     * 寸口信息
     */
    @Excel(name = "ui.data.column.productConstruction.dimension")
    @ApiModelProperty(value = "寸口信息")
    @TableField(value = "DIMENSION")
    private Double dimension;

    /**
     * 规格描述
     */
    @Excel(name = "ui.data.column.productConstruction.specDesc")
    @ApiModelProperty(value = "规格描述")
    @TableField(value = "SPEC_DESC")
    private String specDesc;

    /**
     * 机头宽度
     */
    @Excel(name = "ui.data.column.productConstruction.noseWidth")
    @ApiModelProperty(value = "机头宽度")
    @TableField(value = "NOSE_WIDTH")
    private Double noseWidth;

    /**
     * 扣圈盘直径
     */
    @Excel(name = "ui.data.column.productConstruction.flipDiscDiameter")
    @ApiModelProperty(value = "扣圈盘直径")
    @TableField(value = "FLIP_DISC_DIAMETER")
    private Double flipDiscDiameter;

    /**
     * 断面宽
     */
    @Excel(name = "ui.data.column.productConstruction.sectionWidth")
    @ApiModelProperty(value = "断面宽")
    @TableField(value = "SECTION_WIDTH")
    private Double sectionWidth;

    /**
     * 贴合鼓周长
     */
    @Excel(name = "ui.data.column.productConstruction.fitDrumPerimeter")
    @ApiModelProperty(value = "贴合鼓周长")
    @TableField(value = "FIT_DRUM_PERIMETER")
    private Double fitDrumPerimeter;

    /**
     * 卡盘直径
     */
    @Excel(name = "ui.data.column.productConstruction.chuckDiameter")
    @ApiModelProperty(value = "卡盘直径")
    @TableField(value = "CHUCK_DIAMETER")
    private Double chuckDiameter;

    /**
     * 拉伸宽度
     */
    @Excel(name = "ui.data.column.productConstruction.stretchWidth")
    @ApiModelProperty(value = "拉伸宽度")
    @TableField(value = "STRETCH_WIDTH")
    private Double stretchWidth;

    /**
     * 定性宽度
     */
    @Excel(name = "ui.data.column.productConstruction.qualitativeWidth")
    @ApiModelProperty(value = "定性宽度")
    @TableField(value = "QUALITATIVE_WIDTH")
    private Double qualitativeWidth;

    /**
     * 胎胚周长
     */
    @Excel(name = "ui.data.column.productConstruction.embryoCircle")
    @ApiModelProperty(value = "胎胚周长")
    @TableField(value = "EMBRYO_CIRCLE")
    private Double embryoCircle;

    /**
     * 1#胎体布代号
     */
    @Excel(name = "ui.data.column.productConstruction.tireFabricCode1")
    @ApiModelProperty(value = "1#胎体布代号")
    @TableField(value = "TIRE_FABRIC_CODE1")
    private String tireFabricCode1;

    /**
     * 1#胎体布SAP
     */
    @Excel(name = "ui.data.column.productConstruction.tireFabricSap1")
    @ApiModelProperty(value = "1#胎体布SAP")
    @TableField(value = "TIRE_FABRIC_SAP1")
    private String tireFabricSap1;

    /**
     * 1#胎体布BOM版本ID
     */
    @Excel(name = "ui.data.column.productConstruction.tireFabric1Version")
    @ApiModelProperty(value = "1#胎体布BOM版本ID")
    @TableField(value = "TIRE_FABRIC1_VERSION")
    private String tireFabric1Version;

    /**
     * 1#胎体布工艺
     */
    @Excel(name = "ui.data.column.productConstruction.tireFabricCraft1")
    @ApiModelProperty(value = "1#胎体布工艺")
    @TableField(value = "TIRE_FABRIC_CRAFT1")
    private String tireFabricCraft1;

    /**
     * 帘布边胶
     */
    @Excel(name = "ui.data.column.productConstruction.tireFabricSideRubber")
    @ApiModelProperty(value = "帘布边胶")
    @TableField(value = "TIRE_FABRIC_SIDE_RUBBER")
    private String tireFabricSideRubber;

    /**
     * 2#胎体布代号
     */
    @Excel(name = "ui.data.column.productConstruction.tireFabricCode2")
    @ApiModelProperty(value = "2#胎体布代号")
    @TableField(value = "TIRE_FABRIC_CODE2")
    private String tireFabricCode2;

    /**
     * 2#胎体布SAP
     */
    @Excel(name = "ui.data.column.productConstruction.tireFabricSap2")
    @ApiModelProperty(value = "2#胎体布SAP")
    @TableField(value = "TIRE_FABRIC_SAP2")
    private String tireFabricSap2;

    /**
     * 2#胎体布BOM版本ID
     */
    @Excel(name = "ui.data.column.productConstruction.tireFabric2Version")
    @ApiModelProperty(value = "2#胎体布BOM版本ID")
    @TableField(value = "TIRE_FABRIC2_VERSION")
    private String tireFabric2Version;

    /**
     * 2#胎体布工艺
     */
    @Excel(name = "ui.data.column.productConstruction.tireFabricCraft2")
    @ApiModelProperty(value = "2#胎体布工艺")
    @TableField(value = "TIRE_FABRIC_CRAFT2")
    private String tireFabricCraft2;

    /**
     * 3#胎体布代号
     */
    @Excel(name = "ui.data.column.productConstruction.tireFabricCode3")
    @ApiModelProperty(value = "3#胎体布代号")
    @TableField(value = "TIRE_FABRIC_CODE3")
    private String tireFabricCode3;

    /**
     * 3#胎体布SAP
     */
    @Excel(name = "ui.data.column.productConstruction.tireFabricSap3")
    @ApiModelProperty(value = "3#胎体布SAP")
    @TableField(value = "TIRE_FABRIC_SAP3")
    private String tireFabricSap3;

    /**
     * 3#胎体布BOM版本ID
     */
    @Excel(name = "ui.data.column.productConstruction.tireFabric3Version")
    @ApiModelProperty(value = "3#胎体布BOM版本ID")
    @TableField(value = "TIRE_FABRIC3_VERSION")
    private String tireFabric3Version;

    /**
     * 3#胎体布工艺
     */
    @Excel(name = "ui.data.column.productConstruction.tireFabricCraft3")
    @ApiModelProperty(value = "3#胎体布工艺")
    @TableField(value = "TIRE_FABRIC_CRAFT3")
    private String tireFabricCraft3;

    /**
     * 原线代码
     */
    @Excel(name = "ui.data.column.productConstruction.originalLineCode")
    @ApiModelProperty(value = "原线代码")
    @TableField(value = "ORIGINAL_LINE_CODE")
    private String originalLineCode;

    /**
     * 帘线规格
     */
    @Excel(name = "ui.data.column.productConstruction.cordSpec")
    @ApiModelProperty(value = "帘线规格")
    @TableField(value = "CORD_SPEC")
    private String cordSpec;

    /**
     * 帘布大卷SAP
     */
    @Excel(name = "ui.data.column.productConstruction.cordSap")
    @ApiModelProperty(value = "帘布大卷SAP")
    @TableField(value = "CORD_SAP")
    private String cordSap;

    /**
     * 帘布大卷SAP—BOM版本ID
     */
    @Excel(name = "ui.data.column.productConstruction.cordVersion")
    @ApiModelProperty(value = "帘布大卷SAP—BOM版本ID")
    @TableField(value = "CORD_VERSION")
    private String cordVersion;

    /**
     * 补强/封口胶
     */
    @Excel(name = "ui.data.column.productConstruction.reinforceSealGlue")
    @ApiModelProperty(value = "补强/封口胶")
    @TableField(value = "REINFORCE_SEAL_GLUE")
    private String reinforceSealGlue;

    /**
     * 内衬胶料
     */
    @Excel(name = "ui.data.column.productConstruction.insideRubber")
    @ApiModelProperty(value = "内衬胶料")
    @TableField(value = "INSIDE_RUBBER")
    private String insideRubber;

    /**
     * 内衬代号
     */
    @Excel(name = "ui.data.column.productConstruction.insideCode")
    @ApiModelProperty(value = "内衬代号")
    @TableField(value = "INSIDE_CODE")
    private String insideCode;

    /**
     * 内衬SAP
     */
    @Excel(name = "ui.data.column.productConstruction.insideSap")
    @ApiModelProperty(value = "内衬SAP")
    @TableField(value = "INSIDE_SAP")
    private String insideSap;

    /**
     * 内衬BOM版本ID
     */
    @Excel(name = "ui.data.column.productConstruction.insideVersion")
    @ApiModelProperty(value = "内衬BOM版本ID")
    @TableField(value = "INSIDE_VERSION")
    private String insideVersion;

    /**
     * 内衬工艺
     */
    @Excel(name = "ui.data.column.productConstruction.insideCraft")
    @ApiModelProperty(value = "内衬工艺")
    @TableField(value = "INSIDE_CRAFT")
    private Double insideCraft;

    /**
     * 胎侧代号
     */
    @Excel(name = "ui.data.column.productConstruction.sidewallCode")
    @ApiModelProperty(value = "胎侧代号")
    @TableField(value = "SIDEWALL_CODE")
    private String sidewallCode;

    /**
     * 胎侧SAP
     */
    @Excel(name = "ui.data.column.productConstruction.sidewallSap")
    @ApiModelProperty(value = "胎侧SAP")
    @TableField(value = "SIDEWALL_SAP")
    private String sidewallSap;

    /**
     * 胎侧BOM版本ID
     */
    @Excel(name = "ui.data.column.productConstruction.sidewallVersion")
    @ApiModelProperty(value = "胎侧BOM版本ID")
    @TableField(value = "SIDEWALL_VERSION")
    private String sidewallVersion;

    /**
     * 胎侧工艺
     */
    @Excel(name = "ui.data.column.productConstruction.sidewallCraft")
    @ApiModelProperty(value = "胎侧工艺")
    @TableField(value = "SIDEWALL_CRAFT")
    private Long sidewallCraft;

    /**
     * 胎侧口型
     */
    @Excel(name = "ui.data.column.productConstruction.sidewallMouthPlate")
    @ApiModelProperty(value = "胎侧口型")
    @TableField(value = "SIDEWALL_MOUTH_PLATE")
    private String sidewallMouthPlate;

    /**
     * 胎侧居中
     */
    @Excel(name = "ui.data.column.productConstruction.sidewallCenter")
    @ApiModelProperty(value = "胎侧居中")
    @TableField(value = "SIDEWALL_CENTER")
    private String sidewallCenter;

    /**
     * 胎侧长度
     */
    @Excel(name = "ui.data.column.productConstruction.sidewallLength")
    @ApiModelProperty(value = "胎侧长度")
    @TableField(value = "SIDEWALL_LENGTH")
    private Double sidewallLength;

    /**
     * 胎侧胶料
     */
    @Excel(name = "ui.data.column.productConstruction.sidewallRubber")
    @ApiModelProperty(value = "胎侧胶料")
    @TableField(value = "SIDEWALL_RUBBER")
    private String sidewallRubber;

    /**
     * 胎侧胶重量
     */
    @Excel(name = "ui.data.column.productConstruction.sidewallWeight")
    @ApiModelProperty(value = "胎侧胶重量")
    @TableField(value = "SIDEWALL_WEIGHT")
    private String sidewallWeight;

    /**
     * 胎侧耐磨胶重量
     */
    @Excel(name = "ui.data.column.productConstruction.sidewallWearpRubberWeight")
    @ApiModelProperty(value = "胎侧耐磨胶重量")
    @TableField(value = "SIDEWALL_WEARP_RUBBER_WEIGHT")
    private String sidewallWearpRubberWeight;

    /**
     * 支撑胶代号
     */
    @Excel(name = "ui.data.column.productConstruction.supportCode")
    @ApiModelProperty(value = "支撑胶代号")
    @TableField(value = "SUPPORT_CODE")
    private String supportCode;

    /**
     * 支撑胶料
     */
    @Excel(name = "ui.data.column.productConstruction.supportRubberCode")
    @ApiModelProperty(value = "支撑胶料")
    @TableField(value = "SUPPORT_RUBBER_CODE")
    private String supportRubberCode;

    /**
     * 支撑胶长度
     */
    @Excel(name = "ui.data.column.productConstruction.supportLength")
    @ApiModelProperty(value = "支撑胶长度")
    @TableField(value = "SUPPORT_LENGTH")
    private Long supportLength;

    /**
     * 钢丝圈代码
     */
    @Excel(name = "ui.data.column.productConstruction.beadCode")
    @ApiModelProperty(value = "钢丝圈代码")
    @TableField(value = "BEAD_CODE")
    private String beadCode;

    /**
     * 钢丝圈SAP
     */
    @Excel(name = "ui.data.column.productConstruction.beadSap")
    @ApiModelProperty(value = "钢丝圈SAP")
    @TableField(value = "BEAD_SAP")
    private String beadSap;

    /**
     * 钢丝圈BOM版本ID
     */
    @Excel(name = "ui.data.column.productConstruction.beadVersion")
    @ApiModelProperty(value = "钢丝圈BOM版本ID")
    @TableField(value = "BEAD_VERSION")
    private Long beadVersion;

    /**
     * 钢丝圈排列
     */
    @Excel(name = "ui.data.column.productConstruction.beadArrange")
    @ApiModelProperty(value = "钢丝圈排列")
    @TableField(value = "BEAD_ARRANGE")
    private String beadArrange;

    /**
     * 钢丝圈类型
     */
    @Excel(name = "ui.data.column.productConstruction.beadType")
    @ApiModelProperty(value = "钢丝圈类型")
    @TableField(value = "BEAD_TYPE")
    private String beadType;

    /**
     * 胎圈代码
     */
    @Excel(name = "ui.data.column.productConstruction.tireRingCode")
    @ApiModelProperty(value = "胎圈代码")
    @TableField(value = "TIRE_RING_CODE")
    private String tireRingCode;

    /**
     * 胎圈SAP
     */
    @Excel(name = "ui.data.column.productConstruction.tireRingSap")
    @ApiModelProperty(value = "胎圈SAP")
    @TableField(value = "TIRE_RING_SAP")
    private String tireRingSap;

    /**
     * 胎圈BOM版本ID
     */
    @Excel(name = "ui.data.column.productConstruction.tireRingVersion")
    @ApiModelProperty(value = "胎圈BOM版本ID")
    @TableField(value = "TIRE_RING_VERSION")
    private String tireRingVersion;

    /**
     * 三角胶代码
     */
    @Excel(name = "ui.data.column.productConstruction.apexCode")
    @ApiModelProperty(value = "三角胶代码")
    @TableField(value = "APEX_CODE")
    private String apexCode;

    /**
     * 六边形圈胶料
     */
    @Excel(name = "ui.data.column.productConstruction.hexagonRubberCode")
    @ApiModelProperty(value = "六边形圈胶料")
    @TableField(value = "HEXAGON_RUBBER_CODE")
    private String hexagonRubberCode;

    /**
     * 六边形口型
     */
    @Excel(name = "ui.data.column.productConstruction.hexagonMouthPlate")
    @ApiModelProperty(value = "六边形口型")
    @TableField(value = "HEXAGON_MOUTH_PLATE")
    private String hexagonMouthPlate;

    /**
     * 六边形圈尺寸
     */
    @Excel(name = "ui.data.column.productConstruction.hexagonRubberDimension")
    @ApiModelProperty(value = "六边形圈尺寸")
    @TableField(value = "HEXAGON_RUBBER_DIMENSION")
    private String hexagonRubberDimension;

    /**
     * 三角胶重量
     */
    @Excel(name = "ui.data.column.productConstruction.apexWeight")
    @ApiModelProperty(value = "三角胶重量")
    @TableField(value = "APEX_WEIGHT")
    private Double apexWeight;

    /**
     * 1#钢带代号
     */
    @Excel(name = "ui.data.column.productConstruction.beltCode1")
    @ApiModelProperty(value = "1#钢带代号")
    @TableField(value = "BELT_CODE1")
    private String beltCode1;

    /**
     * 1#钢带SAP
     */
    @Excel(name = "ui.data.column.productConstruction.beltSap1")
    @ApiModelProperty(value = "1#钢带SAP")
    @TableField(value = "BELT_SAP1")
    private String beltSap1;

    /**
     * 1#钢带BOM版本ID
     */
    @Excel(name = "ui.data.column.productConstruction.belt1Version")
    @ApiModelProperty(value = "1#钢带BOM版本ID")
    @TableField(value = "BELT1_VERSION")
    private String belt1Version;

    /**
     * 1#钢带工艺
     */
    @Excel(name = "ui.data.column.productConstruction.beltCraft1")
    @ApiModelProperty(value = "1#钢带工艺")
    @TableField(value = "BELT_CRAFT1")
    private Double beltCraft1;

    /**
     * 1#钢带边胶
     */
    @Excel(name = "ui.data.column.productConstruction.beltSideRubber1")
    @ApiModelProperty(value = "1#钢带边胶")
    @TableField(value = "BELT_SIDE_RUBBER1")
    private String beltSideRubber1;

    /**
     * 1#钢带胶料
     */
    @Excel(name = "ui.data.column.productConstruction.beltRubber1")
    @ApiModelProperty(value = "1#钢带胶料")
    @TableField(value = "BELT_RUBBER1")
    private String beltRubber1;

    /**
     * 2#钢带代号
     */
    @Excel(name = "ui.data.column.productConstruction.beltCode2")
    @ApiModelProperty(value = "2#钢带代号")
    @TableField(value = "BELT_CODE2")
    private String beltCode2;

    /**
     * 2#钢带SAP
     */
    @Excel(name = "ui.data.column.productConstruction.beltSap2")
    @ApiModelProperty(value = "2#钢带SAP")
    @TableField(value = "BELT_SAP2")
    private String beltSap2;

    /**
     * 2#钢带BOM版本ID
     */
    @Excel(name = "ui.data.column.productConstruction.belt2Version")
    @ApiModelProperty(value = "2#钢带BOM版本ID")
    @TableField(value = "BELT2_VERSION")
    private String belt2Version;

    /**
     * 2#钢带工艺
     */
    @Excel(name = "ui.data.column.productConstruction.beltCraft2")
    @ApiModelProperty(value = "2#钢带工艺")
    @TableField(value = "BELT_CRAFT2")
    private Double beltCraft2;

    /**
     * 2#钢带边胶
     */
    @Excel(name = "ui.data.column.productConstruction.beltSideRubber2")
    @ApiModelProperty(value = "2#钢带边胶")
    @TableField(value = "BELT_SIDE_RUBBER2")
    private String beltSideRubber2;

    /**
     * 2#钢带胶料
     */
    @Excel(name = "ui.data.column.productConstruction.beltRubber2")
    @ApiModelProperty(value = "2#钢带胶料")
    @TableField(value = "BELT_RUBBER2")
    private String beltRubber2;

    /**
     * 钢带裁断角度
     */
    @Excel(name = "ui.data.column.productConstruction.beltCuttingAngle")
    @ApiModelProperty(value = "钢带裁断角度")
    @TableField(value = "BELT_CUTTING_ANGLE")
    private String beltCuttingAngle;

    /**
     * 钢带规格
     */
    @Excel(name = "ui.data.column.productConstruction.articleCrownSpec")
    @ApiModelProperty(value = "钢带规格")
    @TableField(value = "ARTICLE_CROWN_SPEC")
    private String articleCrownSpec;

    /**
     * 钢压大卷SAP
     */
    @Excel(name = "ui.data.column.productConstruction.articleCrownSap")
    @ApiModelProperty(value = "钢压大卷SAP")
    @TableField(value = "ARTICLE_CROWN_SAP")
    private String articleCrownSap;

    /**
     * 钢压大卷BOM版本ID
     */
    @Excel(name = "ui.data.column.productConstruction.articleCrownVersion")
    @ApiModelProperty(value = "钢压大卷BOM版本ID")
    @TableField(value = "ARTICLE_CROWN_VERSION")
    private String articleCrownVersion;

    /**
     * 冠带条代号
     */
    @Excel(name = "ui.data.column.productConstruction.articleCrownCode")
    @ApiModelProperty(value = "冠带条代号")
    @TableField(value = "ARTICLE_CROWN_CODE")
    private String articleCrownCode;

    /**
     * 胎面代号
     */
    @Excel(name = "ui.data.column.productConstruction.treadCode")
    @ApiModelProperty(value = "胎面代号")
    @TableField(value = "TREAD_CODE")
    private String treadCode;

    /**
     * 胎面SAP
     */
    @Excel(name = "ui.data.column.productConstruction.treadSap")
    @ApiModelProperty(value = "胎面SAP")
    @TableField(value = "TREAD_SAP")
    private String treadSap;

    /**
     * 胎面BOM版本ID
     */
    @Excel(name = "ui.data.column.productConstruction.treadVersion")
    @ApiModelProperty(value = "胎面BOM版本ID")
    @TableField(value = "TREAD_VERSION")
    private String treadVersion;

    /**
     * 胎面宽
     */
    @Excel(name = "ui.data.column.productConstruction.treadShoulderWidth")
    @ApiModelProperty(value = "胎面宽")
    @TableField(value = "TREAD_SHOULDER_WIDTH")
    private Double treadShoulderWidth;

    /**
     * 胎面肩宽
     */
    @Excel(name = "ui.data.column.productConstruction.treadShoulderJwidth")
    @ApiModelProperty(value = "胎面肩宽")
    @TableField(value = "TREAD_SHOULDER_JWIDTH")
    private Double treadShoulderJwidth;

    /**
     * 胎面长
     */
    @Excel(name = "ui.data.column.productConstruction.treadShoulderLength")
    @ApiModelProperty(value = "胎面长")
    @TableField(value = "TREAD_SHOULDER_LENGTH")
    private Double treadShoulderLength;

    /**
     * 胎面胶种
     */
    @Excel(name = "ui.data.column.productConstruction.treadRubberCategory")
    @ApiModelProperty(value = "胎面胶种")
    @TableField(value = "TREAD_RUBBER_CATEGORY")
    private String treadRubberCategory;

    /**
     * 重量kg/条（上胎冠）
     */
    @Excel(name = "ui.data.column.productConstruction.tireCrownUpWidthWeight")
    @ApiModelProperty(value = "重量kg/条")
    @TableField(value = "TIRE_CROWN_UP_WIDTH_WEIGHT")
    private Double tireCrownUpWidthWeight;

    /**
     * 重量kg/条（下胎冠）
     */
    @Excel(name = "ui.data.column.productConstruction.tireCrownDownWidthWeight")
    @ApiModelProperty(value = "重量kg/条")
    @TableField(value = "TIRE_CROWN_DOWN_WIDTH_WEIGHT")
    private Double tireCrownDownWidthWeight;

    /**
     * 重量kg/条（胎翼）
     */
    @Excel(name = "ui.data.column.productConstruction.tireWingWidthWeight")
    @ApiModelProperty(value = "重量kg/条")
    @TableField(value = "TIRE_WING_WIDTH_WEIGHT")
    private Long tireWingWidthWeight;

    /**
     * 重量kg/条（底胶）
     */
    @Excel(name = "ui.data.column.productConstruction.primerWeight")
    @ApiModelProperty(value = "重量kg/条")
    @TableField(value = "PRIMER_WEIGHT")
    private Double primerWeight;

    /**
     * 重量kg/条（导电胶）
     */
    @Excel(name = "ui.data.column.productConstruction.conductingResinWeight")
    @ApiModelProperty(value = "重量kg/条")
    @TableField(value = "CONDUCTING_RESIN_WEIGHT")
    private Double conductingResinWeight;

    /**
     * 胎面口型板
     */
    @Excel(name = "ui.data.column.productConstruction.treadMouthPlate")
    @ApiModelProperty(value = "胎面口型板")
    @TableField(value = "TREAD_MOUTH_PLATE")
    private String treadMouthPlate;

    /**
     * 删除标识
     */
    @ApiModelProperty(value = "删除标识")
    @TableField(value = "DEL_FLAG")
    private String delFlag;

    /**
     * 查询类型 胎面：TREAD 胎侧：SIDEWALL 内衬：INSIDE 胎圈：TIRE_RING 钢丝圈：BEAD 1#钢带：BELT1
     * 2#钢带：BELT2 钢压大卷：ARTICLE_CROWN 1#胎体布：TIRE_FABRIC1 2#胎体布：TIRE_FABRIC2
     * 3#胎体布：TIRE_FABRIC3 帘布大卷：CORD
     */
    @ApiModelProperty(value = "查询类型")
    private String queryType;

    /**
     * 半部件查询类型
     * 数据字典配置：HALF_PARTS_CODE
     */
    @ApiModelProperty(value = "半部件查询类型")
    private String halfPartsQueryType;

    /**
     * 半部件查询代码
     */
    @ApiModelProperty(value = "半部件查询代码")
    private String halfPartsQueryCode;

    /**
     * 成型鼓
     */
    @ApiModelProperty(value = "成型鼓")
    @TableField(value = "MOLDING_DRUM")
    private String moldingDrum;

}
