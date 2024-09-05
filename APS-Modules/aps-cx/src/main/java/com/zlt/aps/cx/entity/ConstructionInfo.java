package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import oracle.sql.NUMBER;

import java.math.BigDecimal;

import static org.apache.ibatis.type.JdbcType.DATE;
import static org.apache.ibatis.type.JdbcType.DECIMAL;

/**
 * <p>
 * 施工信息表
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-11
 */
@Data
@TableName("T_CONSTRUCTION_VERSION_INFO")
@ApiModel(value = "ConstructionInfo对象", description = "施工信息表")
@KeySequence(value = "SEQ_CONSTRUCTION_INFO", clazz = Long.class)
public class ConstructionInfo extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "SAP品号")
    @TableField("SAP_CODE")
    private String sapCode;

    @ApiModelProperty(value = "胎胚代码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    @ApiModelProperty(value = "寸口信息")
    @TableField(value = "DIMENSION", updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal dimension;

    @ApiModelProperty(value = "规格描述")
    @TableField("SPEC_DESC")
    private String specDesc;

    @ApiModelProperty(value = "机头宽度")
    @TableField(value = "NOSE_WIDTH", updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal noseWidth;

    @ApiModelProperty(value = "扣圈盘直径")
    @TableField(value = "FLIP_DISC_DIAMETER", updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal flipDiscDiameter;

    @ApiModelProperty(value = "贴合鼓周长")
    @TableField(value = "FIT_DRUM_PERIMETER", updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal fitDrumPerimeter;

    @ApiModelProperty(value = "卡盘直径")
    @TableField(value = "CHUCK_DIAMETER", updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal chuckDiameter;

    @ApiModelProperty(value = "拉伸宽度")
    @TableField(value = "STRETCH_WIDTH" , updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal stretchWidth;

    @ApiModelProperty(value = "定性宽度")
    @TableField(value = "QUALITATIVE_WIDTH", updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal qualitativeWidth;

    @ApiModelProperty(value = "胎胚周长")
    @TableField(value = "EMBRYO_CIRCLE", updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal embryoCircle;

    @ApiModelProperty(value = "1#胎体布SAP")
    @TableField("TIRE_FABRIC_SAP1")
    private String tireFabricSap1;

    @ApiModelProperty(value = "1#胎体布代号")
    @TableField("TIRE_FABRIC_CODE1")
    private String tireFabricCode1;

    @ApiModelProperty(value = "1#胎体布工艺")
    @TableField("TIRE_FABRIC_CRAFT1")
    private String tireFabricCraft1;

    @ApiModelProperty(value = "2#胎体布SAP")
    @TableField("TIRE_FABRIC_SAP2")
    private String tireFabricSap2;

    @ApiModelProperty(value = "2#胎体布代号")
    @TableField("TIRE_FABRIC_CODE2")
    private String tireFabricCode2;

    @ApiModelProperty(value = "2#胎体布工艺")
    @TableField("TIRE_FABRIC_CRAFT2")
    private String tireFabricCraft2;

    @ApiModelProperty(value = "3#胎体布SAP")
    @TableField("TIRE_FABRIC_SAP3")
    private String tireFabricSap3;

    @ApiModelProperty(value = "3#胎体布代号")
    @TableField("TIRE_FABRIC_CODE3")
    private String tireFabricCode3;

    @ApiModelProperty(value = "3#胎体布工艺")
    @TableField("TIRE_FABRIC_CRAFT3")
    private String tireFabricCraft3;

    @ApiModelProperty(value = "帘线SAP")
    @TableField("CORD_SAP")
    private String cordSap;

    @ApiModelProperty(value = "帘线规格")
    @TableField("CORD_SPEC")
    private String cordSpec;

    @ApiModelProperty(value = "补强/封口胶")
    @TableField("REINFORCE_SEAL_GLUE")
    private String reinforceSealGlue;

    @ApiModelProperty(value = "内衬胶料")
    @TableField("INSIDE_RUBBER")
    private String insideRubber;

    @ApiModelProperty(value = "内衬SAP")
    @TableField("INSIDE_SAP")
    private String insideSap;

    @ApiModelProperty(value = "内衬代号")
    @TableField("INSIDE_CODE")
    private String insideCode;

    @ApiModelProperty(value = "内衬工艺")
    @TableField(value = "INSIDE_CRAFT", updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal insideCraft;

    @ApiModelProperty(value = "胎侧胶料")
    @TableField("SIDEWALL_RUBBER")
    private String sidewallRubber;

    @ApiModelProperty(value = "胎侧SAP")
    @TableField("SIDEWALL_SAP")
    private String sidewallSap;

    @ApiModelProperty(value = "胎侧代号")
    @TableField("SIDEWALL_CODE")
    private String sidewallCode;

    @ApiModelProperty(value = "胎侧口型")
    @TableField("SIDEWALL_MOUTH_PLATE")
    private String sidewallMouthPlate;

    @ApiModelProperty(value = "胎侧工艺")
    @TableField(value = "SIDEWALL_CRAFT" , updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal sidewallCraft;

    @ApiModelProperty(value = "胎侧居中")
    @TableField("SIDEWALL_CENTER")
    private String sidewallCenter;

    @ApiModelProperty(value = "胎侧长度")
    @TableField(value = "SIDEWALL_LENGTH", updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal sidewallLength;

    @ApiModelProperty(value = "胎侧胶重量")
    @TableField("SIDEWALL_WEIGHT")
    private String sidewallWeight;

    @ApiModelProperty(value = "胎侧耐磨胶重量")
    @TableField("SIDEWALL_WEARP_RUBBER_WEIGHT")
    private String sidewallWearpRubberWeight;

    @ApiModelProperty(value = "支撑胶料")
    @TableField("SUPPORT_RUBBER_CODE")
    private String supportRubberCode;

    @ApiModelProperty(value = "支撑胶代号")
    @TableField("SUPPORT_CODE")
    private String supportCode;

    @ApiModelProperty(value = "支撑胶长度")
    @TableField(value = "SUPPORT_LENGTH", updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal supportLength;

    @ApiModelProperty(value = "钢丝圈SAP")
    @TableField("BEAD_SAP")
    private String beadSap;

    @ApiModelProperty(value = "钢丝圈代码")
    @TableField("BEAD_CODE")
    private String beadCode;

    @ApiModelProperty(value = "钢丝圈排列")
    @TableField("BEAD_ARRANGE")
    private String beadArrange;

    @ApiModelProperty(value = "钢丝圈类型")
    @TableField("BEAD_TYPE")
    private String beadType;

    @ApiModelProperty(value = "六边形圈胶料")
    @TableField("HEXAGON_RUBBER_CODE")
    private String hexagonRubberCode;

    @ApiModelProperty(value = "六边形圈口型")
    @TableField("HEXAGON_MOUTH_PLATE")
    private String hexagonMouthPlate;

    @ApiModelProperty(value = "胎圈SAP")
    @TableField("TIRE_RING_SAP")
    private String tireRingSap;

    @ApiModelProperty(value = "胎圈代码")
    @TableField("TIRE_RING_CODE")
    private String tireRingCode;

    @ApiModelProperty(value = "三角胶代码")
    @TableField("APEX_CODE")
    private String apexCode;

    @ApiModelProperty(value = "六边形圈尺寸")
    @TableField("HEXAGON_RUBBER_DIMENSION")
    private String hexagonRubberDimension;

    @ApiModelProperty(value = "三角胶重量 ")
    @TableField(value = "APEX_WEIGHT", updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal apexWeight;

    @ApiModelProperty(value = "1#钢带SAP")
    @TableField("BELT_SAP1")
    private String beltSap1;

    @ApiModelProperty(value = "1#钢带代号")
    @TableField("BELT_CODE1")
    private String beltCode1;

    @ApiModelProperty(value = "1#钢带工艺")
    @TableField(value = "BELT_CRAFT1", updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal beltCraft1;

    @ApiModelProperty(value = "1#钢带边胶")
    @TableField("BELT_SIDE_RUBBER1")
    private String beltSideRubber1;

    @ApiModelProperty(value = "1#钢带胶料")
    @TableField("BELT_RUBBER1")
    private String beltRubber1;

    @ApiModelProperty(value = "2#钢带SAP")
    @TableField("BELT_SAP2")
    private String beltSap2;

    @ApiModelProperty(value = "2#钢带代号")
    @TableField("BELT_CODE2")
    private String beltCode2;

    @ApiModelProperty(value = "2#钢带工艺")
    @TableField(value = "BELT_CRAFT2", updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal beltCraft2;

    @ApiModelProperty(value = "2#钢带边胶")
    @TableField("BELT_SIDE_RUBBER2")
    private String beltSideRubber2;

    @ApiModelProperty(value = "2#钢带胶料")
    @TableField("BELT_RUBBER2")
    private String beltRubber2;

    @ApiModelProperty(value = "钢带裁断角度")
    @TableField("BELT_CUTTING_ANGLE")
    private String beltCuttingAngle;

    @ApiModelProperty(value = "钢带SAP")
    @TableField("ARTICLE_CROWN_SAP")
    private String articleCrownSap;

    @ApiModelProperty(value = "钢带规格")
    @TableField("ARTICLE_CROWN_SPEC")
    private String articleCrownSpec;

    @ApiModelProperty(value = "冠带条代号")
    @TableField("ARTICLE_CROWN_CODE")
    private String articleCrownCode;

    @ApiModelProperty(value = "胎面SAP")
    @TableField("TREAD_SAP")
    private String treadSap;

    @ApiModelProperty(value = "胎面代号")
    @TableField("TREAD_CODE")
    private String treadCode;

//    @ApiModelProperty(value = "胎面宽.肩宽.长")
//    @TableField("TREAD_SHOULDER_WIDTH")
//    private String treadShoulderWidth;

    @ApiModelProperty(value = "胎面宽")
    @TableField("TREAD_SHOULDER_WIDTH")
    private String treadShoulderWidth;

    @ApiModelProperty(value = "胎面肩宽")
    @TableField("TREAD_SHOULDER_JWIDTH")
    private String treadShoulderJWidth;

    @ApiModelProperty(value = "胎面长")
    @TableField("TREAD_SHOULDER_LENGTH")
    private String treadShoulderLength;


    @ApiModelProperty(value = "胎面胶种")
    @TableField("TREAD_RUBBER_CATEGORY")
    private String treadRubberCategory;

    @ApiModelProperty(value = "重量kg/条（上胎冠）")
    @TableField(value = "TIRE_CROWN_UP_WIDTH_WEIGHT", updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal tireCrownUpWidthWeight;

    @ApiModelProperty(value = "重量kg/条（下胎冠）")
    @TableField(value = "TIRE_CROWN_DOWN_WIDTH_WEIGHT", updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal tireCrownDownWidthWeight;

    @ApiModelProperty(value = "重量kg/条（胎翼）")
    @TableField(value = "TIRE_WING_WIDTH_WEIGHT", updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal tireWingWidthWeight;

    @ApiModelProperty(value = "重量kg/条（底胶）")
    @TableField(value = "PRIMER_WEIGHT", updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal primerWeight;

    @ApiModelProperty(value = "重量kg/条（导电胶）")
    @TableField(value = "CONDUCTING_RESIN_WEIGHT", updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal conductingResinWeight;

    @ApiModelProperty(value = "口型板")
    @TableField("TREAD_MOUTH_PLATE")
    private String treadMouthPlate;

    @ApiModelProperty(value = "断面宽")
    @TableField(value = "SECTION_WIDTH", updateStrategy = FieldStrategy.IGNORED, jdbcType = DECIMAL)
    private BigDecimal sectionWidth;

    @ApiModelProperty(value = "胎胚BOM版本")
    @TableField(value = "EMBRYO_VERSION")
    private String embryoVersion;

    @ApiModelProperty(value = "1#胎体布BOM版本")
    @TableField(value = "TIRE_FABRIC1_VERSION")
    private String tireFabric1Version;

    @ApiModelProperty(value = "2#胎体布BOM版本")
    @TableField(value = "TIRE_FABRIC2_VERSION")
    private String tireFabric2Version;

    @ApiModelProperty(value = "3#胎体布BOM版本")
    @TableField(value = "TIRE_FABRIC3_VERSION")
    private String tireFabric3Version;

    @ApiModelProperty(value = "帘布大卷SAP—BOM版本")
    @TableField(value = "CORD_VERSION")
    private String cordVersion;

    @ApiModelProperty(value = "内衬—BOM版本")
    @TableField(value = "INSIDE_VERSION")
    private String insideVersion;

    @ApiModelProperty(value = "胎侧—BOM版本")
    @TableField(value = "SIDEWALL_VERSION")
    private String sidewallVersion;

    @ApiModelProperty(value = "钢丝圈—BOM版本")
    @TableField(value = "BEAD_VERSION")
    private String beadVersion;

    @ApiModelProperty(value = "胎圈—BOM版本")
    @TableField(value = "TIRE_RING_VERSION")
    private String tireRingVersion;

    @ApiModelProperty(value = "1#钢带BOM版本")
    @TableField(value = "BELT1_VERSION")
    private String belt1Version;

    @ApiModelProperty(value = "2#钢带BOM版本")
    @TableField(value = "BELT2_VERSION")
    private String belt2Version;

    @ApiModelProperty(value = "钢压大卷BOM版本")
    @TableField(value = "ARTICLE_CROWN_VERSION")
    private String articleCrownVersion;

    @ApiModelProperty(value = "胎面BOM版本")
    @TableField(value = "TREAD_VERSION")
    private String treadVersion;
    
	/** 帘布边胶 */
	@ApiModelProperty(value = "帘布边胶")
	@TableField(value = "TIRE_FABRIC_SIDE_RUBBER")
	private String tireFabricSideRubber;
}
