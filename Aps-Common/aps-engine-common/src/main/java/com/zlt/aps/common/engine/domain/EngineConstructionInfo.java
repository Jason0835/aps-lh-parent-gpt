package com.zlt.aps.common.engine.domain;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

/**
 * 施工信息对象 t_construction_info
 * 
 * @author Joran.zhang
 * @date 2021-06-30
 */
@ApiModel(value = "施工信息对象", description = "施工信息对象 ")
@Data
public class EngineConstructionInfo extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID ，序列SEQ_CONSTRUCTION_INFO*/
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 胎胚代码 */
    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    /** 寸口信息 */
    @ApiModelProperty(value = "寸口信息")
    private Double dimension;

    /** 规格描述 */
    @ApiModelProperty(value = "规格描述")
    private String specDesc;

    /** 机头宽度 */
    @ApiModelProperty(value = "机头宽度")
    private Double noseWidth;

    /** 扣圈盘直径 */
    @ApiModelProperty(value = "扣圈盘直径")
    private Double flipDiscDiameter;

    /** 贴合鼓周长 */
    @ApiModelProperty(value = "贴合鼓周长")
    private Long fitDrumPerimeter = 0L;

    /** 卡盘直径 */
    @ApiModelProperty(value = "卡盘直径")
    private Double chuckDiameter;

    /** 拉伸宽度 */
    @ApiModelProperty(value = "拉伸宽度")
    private Double stretchWidth;

    /** 定性宽度 */
    @ApiModelProperty(value = "定性宽度")
    private Double qualitativeWidth;

    /** 胎胚周长 */
    @ApiModelProperty(value = "胎胚周长")
    private Double embryoCircle;

    /** 1#胎体布代号 */
    @ApiModelProperty(value = "1#胎体布代号")
    private String tireFabricCode1;

    /** 1#胎体布工艺 */
    @ApiModelProperty(value = "1#胎体布工艺")
    private String tireFabricCraft1 = "0";

    /** 2#胎体布代号 */
    @ApiModelProperty(value = "2#胎体布代号")
    private String tireFabricCode2;

    /** 2#胎体布工艺 */
    @ApiModelProperty(value = "2#胎体布工艺")
    private String tireFabricCraft2 = "0";

    /** 3#胎体布代号 */
    @ApiModelProperty(value = "3#胎体布代号")
    private String tireFabricCode3;

    /** 3#胎体布工艺 */
    @ApiModelProperty(value = "3#胎体布工艺")
    private String tireFabricCraft3 = "0";

    /** 帘线规格 */
    @ApiModelProperty(value = "帘线规格")
    private String cordSpec;

    /** 补强/封口胶 */
    @ApiModelProperty(value = "补强/封口胶")
    private String reinforceSealGlue;

    /** 内衬胶料 */
    @ApiModelProperty(value = "内衬胶料")
    private String insideRubber;

    /** 内衬代号 */
    @ApiModelProperty(value = "内衬代号")
    private String insideCode;

    /** 内衬工艺 */
    @ApiModelProperty(value = "内衬工艺")
    private Double insideCraft;

    /** 胎侧代号 */
    @ApiModelProperty(value = "胎侧代号")
    private String sidewallCode;

    /** 胎侧工艺 */
    @ApiModelProperty(value = "胎侧工艺")
    private Double sidewallCraft;

    /** 胎侧口型 */
    @ApiModelProperty(value = "胎侧口型")
    private String sidewallMouthPlate;

    /** 胎侧居中 */
    @ApiModelProperty(value = "胎侧居中")
    private String sidewallCenter;

    /** 胎侧长度 */
    @ApiModelProperty(value = "胎侧长度")
    private Double sidewallLength = 0D;

    /** 胎侧胶料 */
    @ApiModelProperty(value = "胎侧胶料")
    private String sidewallRubber;

    /** 胎侧胶重量 */
    @ApiModelProperty(value = "胎侧胶重量")
    private String sidewallWeight;

    /** 胎侧耐磨胶重量 */
    @ApiModelProperty(value = "胎侧耐磨胶重量")
    private String sidewallWearpRubberWeight;

    /** 支撑胶代号 */
    @ApiModelProperty(value = "支撑胶代号")
    private String supportCode;

    /** 支撑胶料 */
    @ApiModelProperty(value = "支撑胶料")
    private String supportRubberCode;

    /** 支撑胶长度 */
    @ApiModelProperty(value = "支撑胶长度")
    private Double supportLength;

    /** 钢丝圈代码 */
    @ApiModelProperty(value = "钢丝圈代码")
    private String beadCode;

    /** 钢丝圈排列 */
    @ApiModelProperty(value = "钢丝圈排列")
    private String beadArrange;

    /** 钢丝圈类型 */
    @ApiModelProperty(value = "钢丝圈类型")
    private String beadType;

    /** 胎圈代码 */
    @ApiModelProperty(value = "胎圈代码")
    private String tireRingCode;

    /** 三角胶代码 */
    @ApiModelProperty(value = "三角胶代码")
    private String apexCode;

    /** 六边形圈胶料 */
    @ApiModelProperty(value = "六边形圈胶料")
    private String hexagonRubberCode;

    /** 六边形口型 */
    @ApiModelProperty(value = "六边形口型")
    private String hexagonMouthPlate;

    /** 六边形圈尺寸 */
    @ApiModelProperty(value = "六边形圈尺寸")
    private String hexagonRubberDimension;

    /** 三角胶重量 */
    @ApiModelProperty(value = "三角胶重量")
    private Double apexWeight;

    /** 1#钢带代号 */
    @ApiModelProperty(value = "1#钢带代号")
    private String beltCode1;

    /** 1#钢带工艺 */
    @ApiModelProperty(value = "1#钢带工艺")
    private Double beltCraft1 = 0D;

    /** 1#钢带边胶 */
    @ApiModelProperty(value = "1#钢带边胶")
    private String beltSideRubber1;

    /** 1#钢带胶料 */
    @ApiModelProperty(value = "1#钢带胶料")
    private String beltRubber1;

    /** 2#钢带代号 */
    @ApiModelProperty(value = "2#钢带代号")
    private String beltCode2;

    /** 2#钢带工艺 */
    @ApiModelProperty(value = "2#钢带工艺")
    private Double beltCraft2 = 0D;

    /** 2#钢带边胶 */
    @ApiModelProperty(value = "2#钢带边胶")
    private String beltSideRubber2;

    /** 2#钢带胶料 */
    @ApiModelProperty(value = "2#钢带胶料")
    private String beltRubber2;

    /** 钢带裁断角度 */
    @ApiModelProperty(value = "钢带裁断角度")
    private String beltCuttingAngle;

    /** 钢带规格 */
    @ApiModelProperty(value = "钢带规格")
    private String articleCrownSpec;

    /** 冠带条代号 */
    @ApiModelProperty(value = "冠带条代号")
    private String articleCrownCode;

    /** 胎面代号 */
    @ApiModelProperty(value = "胎面代号")
    private String treadCode;

    /** 胎面宽 */
    @ApiModelProperty(value = "胎面宽")
    private Double treadShoulderWidth;

    /** 胎面肩宽 */
    @ApiModelProperty(value = "胎面肩宽")
    private Double treadShoulderJwidth;

    /** 胎面长 */
    @ApiModelProperty(value = "胎面长")
    private Double treadShoulderLength = 0D;

    /** 胎面胶种 */
    @ApiModelProperty(value = "胎面胶种")
    private String treadRubberCategory;

    /** 重量kg/条（上胎冠） */
    @ApiModelProperty(value = "重量kg/条")
    private Double tireCrownUpWidthWeight;

    /** 重量kg/条（下胎冠） */
    @ApiModelProperty(value = "重量kg/条")
    private Double tireCrownDownWidthWeight;

    /** 重量kg/条（胎翼） */
    @ApiModelProperty(value = "重量kg/条")
    private Double tireWingWidthWeight;

    /** 重量kg/条（底胶） */
    @ApiModelProperty(value = "重量kg/条")
    private Double primerWeight;

    /** 重量kg/条（导电胶） */
    @ApiModelProperty(value = "重量kg/条")
    private Double conductingResinWeight;

    /** 胎面口型板 */
    @ApiModelProperty(value = "胎面口型板")
    private String treadMouthPlate;

    /**
     * 断面宽
     */
    @ApiModelProperty(value = "断面宽")
    private Double sectionWidth;

    /**
     * 施工类型
     */
    @ApiModelProperty(value = "施工类型")
    private Integer consType;

    /**
     * 合模压力
     */
    @ApiModelProperty(value = "合模压力")
    private BigDecimal clampingPressure;

    /**
     * 硫化时间
     */
    @ApiModelProperty(value = "硫化时间")
    private Integer curingTime;

    /**
     * BOM版本信息
     */
    @ApiModelProperty(value = "BOM版本信息")
    private String bomDataVersion;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("embryoCode", getEmbryoCode())
            .append("dimension", getDimension())
            .append("specDesc", getSpecDesc())
            .append("noseWidth", getNoseWidth())
            .append("flipDiscDiameter", getFlipDiscDiameter())
            .append("fitDrumPerimeter", getFitDrumPerimeter())
            .append("chuckDiameter", getChuckDiameter())
            .append("stretchWidth", getStretchWidth())
            .append("qualitativeWidth", getQualitativeWidth())
            .append("embryoCircle", getEmbryoCircle())
            .append("tireFabricCode1", getTireFabricCode1())
            .append("tireFabricCraft1", getTireFabricCraft1())
            .append("tireFabricCode2", getTireFabricCode2())
            .append("tireFabricCraft2", getTireFabricCraft2())
            .append("tireFabricCode3", getTireFabricCode3())
            .append("tireFabricCraft3", getTireFabricCraft3())
            .append("cordSpec",getCordSpec())
            .append("reinforceSealGlue", getReinforceSealGlue())
            .append("insideRubber", getInsideRubber())
            .append("insideCode", getInsideCode())
            .append("insideCraft", getInsideCraft())
            .append("sidewallCode", getSidewallCode())
            .append("sidewallCraft", getSidewallCraft())
            .append("sidewallMouthPlate", getSidewallMouthPlate())
            .append("sidewallCenter", getSidewallCenter())
            .append("sidewallLength", getSidewallLength())
            .append("sidewallRubber", getSidewallRubber())
            .append("sidewallWeight", getSidewallWeight())
            .append("sidewallWearpRubberWeight", getSidewallWearpRubberWeight())
            .append("supportCode", getSupportCode())
            .append("supportRubberCode", getSupportRubberCode())
            .append("supportLength", getSupportLength())
            .append("beadCode", getBeadCode())
            .append("beadArrange", getBeadArrange())
            .append("beadType", getBeadType())
            .append("tireRingCode", getTireRingCode())
            .append("apexCode", getApexCode())
            .append("hexagonRubberCode", getHexagonRubberCode())
            .append("hexagonMouthPlate", getHexagonMouthPlate())
            .append("hexagonRubberDimension", getHexagonRubberDimension())
            .append("apexWeight", getApexWeight())
            .append("beltCode1", getBeltCode1())
            .append("beltCraft1", getBeltCraft1())
            .append("beltSideRubber1", getBeltSideRubber1())
            .append("beltRubber1", getBeltRubber1())
            .append("beltCode2", getBeltCode2())
            .append("beltCraft2", getBeltCraft2())
            .append("beltSideRubber2", getBeltSideRubber2())
            .append("beltRubber2", getBeltRubber2())
            .append("beltCuttingAngle", getBeltCuttingAngle())
            .append("articleCrownSpec", getArticleCrownSpec())
            .append("articleCrownCode", getArticleCrownCode())
            .append("treadCode", getTreadCode())
            .append("treadShoulderWidth", getTreadShoulderWidth())
            .append("treadShoulderJwidth", getTreadShoulderJwidth())
            .append("treadShoulderLength", getTreadShoulderLength())
            .append("treadRubberCategory", getTreadRubberCategory())
            .append("tireCrownUpWidthWeight", getTireCrownUpWidthWeight())
            .append("tireCrownDownWidthWeight", getTireCrownDownWidthWeight())
            .append("tireWingWidthWeight", getTireWingWidthWeight())
            .append("primerWeight", getPrimerWeight())
            .append("conductingResinWeight", getConductingResinWeight())
            .append("treadMouthPlate", getTreadMouthPlate())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("delFlag", getDelFlag())
            .append("remark", getRemark())
            .append("sectionWidth", getSectionWidth())
            .toString();
    }


        

}
