package com.zlt.aps.common.engine.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 投产胎胚施工信息表
 * @TableName T_PRODUCT_CONSTRUCTION_INFO
 */
@ApiModel(value = "投产施工信息对象", description = "投产施工信息对象 ")
@Data
public class TProductConstructionInfo extends ApsBaseEntity {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 胎胚SAP品号(原外胎SAP调整为存储胎胚的SAP品号)
     */
    private String sapCode;

    /**
     * 胎胚代码(代码)施工号
     */
    private String embryoCode;

    /**
     * BOM版本(存储版本号)
     */
    private String embryoVersion;

    /**
     * 施工类型
     */
    private Integer consType;

    /**
     * 寸口信息
     */
    private BigDecimal dimension;

    /**
     * 规格描述
     */
    private String specDesc;

    /**
     * 机头宽度
     */
    private BigDecimal noseWidth;

    /**
     * 扣圈盘直径
     */
    private BigDecimal flipDiscDiameter;

    /**
     * 断面宽
     */
    private Integer sectionWidth;

    /**
     * 贴合鼓周长
     */
    private BigDecimal fitDrumPerimeter = BigDecimal.ZERO;

    /**
     * 卡盘直径
     */
    private BigDecimal chuckDiameter;

    /**
     * 拉伸宽度
     */
    private BigDecimal stretchWidth;

    /**
     * 定性宽度
     */
    private BigDecimal qualitativeWidth;

    /**
     * 胎胚周长
     */
    private BigDecimal embryoCircle;

    /**
     * 1#胎体布代号
     */
    private String tireFabricCode1;

    /**
     * 1#胎体布SAP
     */
    private String tireFabricSap1;

    /**
     * 1#胎体布BOM版本ID
     */
    private Long tireFabric1Version;

    /**
     * 1#胎体布工艺
     */
    private String tireFabricCraft1 = "0";

    /**
     * 2#胎体布代号
     */
    private String tireFabricCode2;

    /**
     * 2#胎体布SAP
     */
    private String tireFabricSap2;

    /**
     * 2#胎体布BOM版本ID
     */
    private Long tireFabric2Version;

    /**
     * 2#胎体布工艺
     */
    private String tireFabricCraft2 = "0";

    /**
     * 3#胎体布代号
     */
    private String tireFabricCode3;

    /**
     * 3#胎体布SAP
     */
    private String tireFabricSap3;

    /**
     * 3#胎体布BOM版本ID
     */
    private Long tireFabric3Version;

    /**
     * 3#胎体布工艺
     */
    private String tireFabricCraft3 = "0";

    /**
     * 原线代码
     */
    private String originalLineCode;

    /**
     * 帘线规格
     */
    private String cordSpec;

    /**
     * 帘布大卷SAP
     */
    private String cordSap;

    /**
     * 帘布大卷SAP—BOM版本ID
     */
    private Long cordVersion;

    /**
     * 补强/封口胶
     */
    private String reinforceSealGlue;

    /**
     * 内衬胶料
     */
    private String insideRubber;

    /**
     * 内衬代号
     */
    private String insideCode;

    /**
     * 内衬SAP
     */
    private String insideSap;

    /**
     * 内衬—BOM版本ID
     */
    private Long insideVersion;

    /**
     * 内衬工艺
     */
    private BigDecimal insideCraft;

    /**
     * 胎侧代号
     */
    private String sidewallCode;

    /**
     * 胎侧SAP
     */
    private String sidewallSap;

    /**
     * 胎侧—BOM版本ID
     */
    private Long sidewallVersion;

    /**
     * 胎侧工艺
     */
    private BigDecimal sidewallCraft;

    /**
     * 胎侧口型
     */
    private String sidewallMouthPlate;

    /**
     * 胎侧居中
     */
    private String sidewallCenter;

    /**
     * 胎侧长度
     */
    private BigDecimal sidewallLength = BigDecimal.ZERO;

    /**
     * 胎侧胶料
     */
    private String sidewallRubber;

    /**
     * 胎侧胶重量
     */
    private String sidewallWeight;

    /**
     * 胎侧耐磨胶重量
     */
    private String sidewallWearpRubberWeight;

    /**
     * 支撑胶代号
     */
    private String supportCode;

    /**
     * 支撑胶料
     */
    private String supportRubberCode;

    /**
     * 支撑胶长度
     */
    private BigDecimal supportLength;

    /**
     * 钢丝圈代码
     */
    private String beadCode;

    /**
     * 钢丝圈SAP
     */
    private String beadSap;

    /**
     * 钢丝圈—BOM版本ID
     */
    private String beadVersion;

    /**
     * 钢丝圈排列
     */
    private String beadArrange;

    /**
     * 钢丝圈类型
     */
    private String beadType;

    /**
     * 胎圈代码
     */
    private String tireRingCode;

    /**
     * 胎圈SAP
     */
    private String tireRingSap;

    /**
     * 胎圈—BOM版本ID
     */
    private Long tireRingVersion;

    /**
     * 三角胶代码
     */
    private String apexCode;

    /**
     * 六边形圈胶料
     */
    private String hexagonRubberCode;

    /**
     * 六边形口型
     */
    private String hexagonMouthPlate;

    /**
     * 六边形圈尺寸
     */
    private String hexagonRubberDimension;

    /**
     * 三角胶重量 
     */
    private BigDecimal apexWeight;

    /**
     * 1#钢带代号
     */
    private String beltCode1;

    /**
     * 1#钢带SAP
     */
    private String beltSap1;

    /**
     * 1#钢带BOM版本ID
     */
    private Long belt1Version;

    /**
     * 1#钢带工艺
     */
    private BigDecimal beltCraft1 = BigDecimal.ZERO;

    /**
     * 1#钢带边胶
     */
    private String beltSideRubber1;

    /**
     * 1#钢带胶料
     */
    private String beltRubber1;

    /**
     * 2#钢带代号
     */
    private String beltCode2;

    /**
     * 2#钢带SAP
     */
    private String beltSap2;

    /**
     * 2#钢带BOM版本ID
     */
    private Long belt2Version;

    /**
     * 2#钢带工艺
     */
    private BigDecimal beltCraft2 = BigDecimal.ZERO;

    /**
     * 2#钢带边胶
     */
    private String beltSideRubber2;

    /**
     * 2#钢带胶料
     */
    private String beltRubber2;

    /**
     * 钢带裁断角度
     */
    private String beltCuttingAngle;

    /**
     * 钢带规格
     */
    private String articleCrownSpec;

    /**
     * 钢压大卷SAP
     */
    private String articleCrownSap;

    /**
     * 钢压大卷BOM版本ID
     */
    private Long articleCrownVersion;

    /**
     * 冠带条代号
     */
    private String articleCrownCode;

    /**
     * 胎面代号
     */
    private String treadCode;

    /**
     * 胎面SAP
     */
    private String treadSap;

    /**
     * 胎面BOM版本ID
     */
    private Long treadVersion;

    /**
     * 胎面宽
     */
    private BigDecimal treadShoulderWidth;

    /**
     * 胎面肩宽
     */
    private BigDecimal treadShoulderJwidth;

    /**
     * 胎面长
     */
    private BigDecimal treadShoulderLength = BigDecimal.ZERO;

    /**
     * 胎面胶种
     */
    private String treadRubberCategory;

    /**
     * 重量kg/条（上胎冠）
     */
    private BigDecimal tireCrownUpWidthWeight;

    /**
     * 重量kg/条（下胎冠）
     */
    private BigDecimal tireCrownDownWidthWeight;

    /**
     * 重量kg/条（胎翼）
     */
    private BigDecimal tireWingWidthWeight;

    /**
     * 重量kg/条（底胶）
     */
    private BigDecimal primerWeight;

    /**
     * 重量kg/条（导电胶）
     */
    private BigDecimal conductingResinWeight;

    /**
     * 胎面口型板
     */
    private String treadMouthPlate;


    private static final long serialVersionUID = 1L;
}