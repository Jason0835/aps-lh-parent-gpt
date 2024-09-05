package com.zlt.aps.cx.api.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * <p>
 * 施工信息表DTO
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-11
 */
@Data
@ApiModel(value = "ConstructionInfo对象", description = "施工信息表")
public class ConstructionInfoDto extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "SAP品号")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.data.column.scheduleResult.sapCode",sort = 1)
    private String sapCode;

    @ApiModelProperty(value = "胎胚代码")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.embryoCode",sort = 2)
    private String embryoCode;

    @ApiModelProperty(value = "寸口信息")
    @Excel(name = "ui.construction.dimension")
    @ImportValidated(number = true, min = 0, max = 999999)
    private BigDecimal dimension;

    @ApiModelProperty(value = "规格描述")
    @Excel(name = "ui.construction.spec")
    @ImportValidated(maxLength = 66)
    private String specDesc;

    @ApiModelProperty(value = "机头宽度")
    @Excel(name = "ui.construction.noseWidth")
    @ImportValidated(number = true, min = 0, max = 99999)
    private BigDecimal noseWidth;

    @ApiModelProperty(value = "扣圈盘直径")
    @Excel(name = "ui.construction.flipDiscDiameter")
    @ImportValidated(number = true, min = 0, max = 99999)
    private BigDecimal flipDiscDiameter;

    @ApiModelProperty(value = "贴合鼓周长")
    @Excel(name = "ui.construction.fitDrumPerimeter")
    @ImportValidated(number = true, min = 0, max = 99999)
    private BigDecimal fitDrumPerimeter;

    @ApiModelProperty(value = "卡盘直径")
    @Excel(name = "ui.construction.chuckDiameter")
    @ImportValidated(number = true, min = 0, max = 99999)
    private BigDecimal chuckDiameter;

    @ApiModelProperty(value = "拉伸宽度")
    @Excel(name = "ui.construction.stretchWidth")
    @ImportValidated(number = true, min = 0, max = 99999)
    private BigDecimal stretchWidth;

    @ApiModelProperty(value = "定性宽度")
    @Excel(name = "ui.construction.qualitativeWidth")
    @ImportValidated(number = true, min = 0, max = 99999)
    private BigDecimal qualitativeWidth;

    @ApiModelProperty(value = "胎胚周长")
    @Excel(name = "ui.construction.embryoCircle")
    @ImportValidated(number = true, min = 0, max = 99999)
    private BigDecimal embryoCircle;

    @ApiModelProperty(value = "断面宽")
    @ImportValidated(maxLength = 8, digits = true)
    @Excel(name = "ui.construction.sectionWidth")
    private BigDecimal sectionWidth;

    @ApiModelProperty(value = "1#胎体布SAP")
    @Excel(name = "ui.construction.tireFabricSap1")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    private String tireFabricSap1;

    @ApiModelProperty(value = "1#胎体布代号")
    @Excel(name = "ui.construction.tireFabricCode1")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    private String tireFabricCode1;

    @ApiModelProperty(value = "1#胎体布工艺")
    @Excel(name = "ui.construction.tireFabricCraft1")
    @ImportValidated(number = true, min = 0, max = 99999)
    private String tireFabricCraft1;

    @ApiModelProperty(value = "2#胎体布SAP")
    @Excel(name = "ui.construction.tireFabricSap2")
    @ImportValidated(maxLength = 20, isCode = true)
    private String tireFabricSap2;

    @ApiModelProperty(value = "2#胎体布代号")
    @Excel(name = "ui.construction.tireFabricCode2")
    @ImportValidated(maxLength = 20, isCode = true)
    private String tireFabricCode2;

    @ApiModelProperty(value = "2#胎体布工艺")
    @ImportValidated(number = true, min = 0, max = 99999)
    @Excel(name = "ui.construction.tireFabricCraft2")


    private String tireFabricCraft2;

    @ApiModelProperty(value = "3#胎体布SAP")
    @ImportValidated(maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.tireFabricSap3")
    private String tireFabricSap3;

    @ApiModelProperty(value = "3#胎体布代号")
    @ImportValidated(maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.tireFabricCode3")
    private String tireFabricCode3;

    @ApiModelProperty(value = "3#胎体布工艺")
    @ImportValidated(number = true, min = 0, max = 99999)
    @Excel(name = "ui.construction.tireFabricCraft3")
    private String tireFabricCraft3;

    @ApiModelProperty(value = "原线代码")
//    @ImportValidated(number = true, min = 0, max = 99999)
    @Excel(name = "ui.construction.originalLineCode")
    private String originalLineCode;

    @ApiModelProperty(value = "帘线SAP")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.cordSap")
    private String cordSap;

    @ApiModelProperty(value = "帘线规格")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.cordSpec")
    private String cordSpec;

    @ApiModelProperty(value = "补强/封口胶")
    @ImportValidated(maxLength = 100, isCode = true)
    @Excel(name = "ui.construction.reinforceSealGlue")
    private String reinforceSealGlue;

    @ApiModelProperty(value = "内衬胶料")
    @ImportValidated(maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.insideRubber")
    private String insideRubber;

    @ApiModelProperty(value = "内衬SAP")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.insideSap")
    private String insideSap;

    @ApiModelProperty(value = "内衬代号")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.insideCode")
    private String insideCode;

    @ApiModelProperty(value = "内衬工艺")
    @ImportValidated(number = true, min = 0, max = 99999)
    @Excel(name = "ui.construction.insideCraft")
    private BigDecimal insideCraft;

    @ApiModelProperty(value = "胎侧SAP")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.sidewallSap")
    private String sidewallSap;

    @ApiModelProperty(value = "胎侧代号")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.sidewallCode")
    private String sidewallCode;

    @ApiModelProperty(value = "胎侧工艺")
    @ImportValidated(number = true, min = 0, max = 99999)
    @Excel(name = "ui.construction.sidewallCraft")
    private BigDecimal sidewallCraft;

    @ApiModelProperty(value = "胎侧口型")
    @ImportValidated(maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.sidewallMouthPlate")
    private String sidewallMouthPlate;

    @ApiModelProperty(value = "胎侧居中")
    @ImportValidated(maxLength = 20)
    @Excel(name = "ui.construction.sidewallCenter")
    private String sidewallCenter;

    @ApiModelProperty(value = "胎侧长度")
    @ImportValidated(number = true, min = 0, max = 99999)
    @Excel(name = "ui.construction.sidewallLength")
    private BigDecimal sidewallLength;

    @ApiModelProperty(value = "胎侧胶料")
    @ImportValidated(maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.sidewallRubber")
    private String sidewallRubber;

    @ApiModelProperty(value = "胎侧胶重量")
    @ImportValidated(number = true, min = 0, max = 99999)
    @Excel(name = "ui.construction.sidewallWeight")
    private String sidewallWeight;

    @ApiModelProperty(value = "胎侧耐磨胶重量")
    @ImportValidated(number = true, min = 0, max = 99999)
    @Excel(name = "ui.construction.sidewallWearpRubberWeight")
    private String sidewallWearpRubberWeight;

    @ApiModelProperty(value = "支撑胶代号")
    @ImportValidated(maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.supportCode")
    private String supportCode;

    @ApiModelProperty(value = "支撑胶料")
    @ImportValidated(maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.supportRubberCode")
    private String supportRubberCode;

    @ApiModelProperty(value = "支撑胶长度")
    @ImportValidated(number = true, min = 0, max = 99999)
    @Excel(name = "ui.construction.supportLength")
    private BigDecimal supportLength;

    @ApiModelProperty(value = "钢丝圈SAP")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.beadSap")
    private String beadSap;

    @ApiModelProperty(value = "钢丝圈代码")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.beadCode")
    private String beadCode;

    @ApiModelProperty(value = "钢丝圈排列")
    @ImportValidated(maxLength = 50, isCode = true)
    @Excel(name = "ui.construction.beadArrange")
    private String beadArrange;

    @ApiModelProperty(value = "钢丝圈类型")
    @ImportValidated(maxLength = 16)
    @Excel(name = "ui.construction.beadType")
    private String beadType;

    @ApiModelProperty(value = "胎圈SAP")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.tireRingSap")
    private String tireRingSap;

    @ApiModelProperty(value = "胎圈代码")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.tireRingCode")
    private String tireRingCode;

    @ApiModelProperty(value = "三角胶代码")
    @ImportValidated(maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.apexCode")
    private String apexCode;

    @ApiModelProperty(value = "六边形圈胶料")
    @ImportValidated(maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.hexagonRubberCode")
    private String hexagonRubberCode;

    @ApiModelProperty(value = "六边形圈口型")
    @ImportValidated(maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.hexagonMouthPlate")
    private String hexagonMouthPlate;

    @ApiModelProperty(value = "六边形圈尺寸")
    @ImportValidated(maxLength = 16)
    @Excel(name = "ui.construction.hexagonRubberDimension")
    private String hexagonRubberDimension;

    @ApiModelProperty(value = "三角胶重量 ")
    @ImportValidated(number = true, min = 0, max = 99999)
    @Excel(name = "ui.construction.apexWeight")
    private BigDecimal apexWeight;

    @ApiModelProperty(value = "1#钢带SAP")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.beltSap1")
    private String beltSap1;

    @ApiModelProperty(value = "1#钢带代号")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.beltCode1")
    private String beltCode1;

    @ApiModelProperty(value = "1#钢带工艺")
    @ImportValidated(maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.beltCraft1")
    private BigDecimal beltCraft1;

    @ApiModelProperty(value = "1#钢带边胶")
    @ImportValidated(maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.beltSideRubber1")
    private String beltSideRubber1;

    @ApiModelProperty(value = "1#钢带胶料")
    @ImportValidated(maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.beltRubber1")
    private String beltRubber1;

    @ApiModelProperty(value = "2#钢带SAP")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.beltSap2")
    private String beltSap2;

    @ApiModelProperty(value = "2#钢带代号")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.beltCode2")
    private String beltCode2;

    @ApiModelProperty(value = "2#钢带工艺")
    @ImportValidated(number = true, min = 0, max = 99999)
    @Excel(name = "ui.construction.beltCraft2")
    private BigDecimal beltCraft2;

    @ApiModelProperty(value = "2#钢带边胶")
    @ImportValidated(maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.beltSideRubber2")
    private String beltSideRubber2;

    @ApiModelProperty(value = "2#钢带胶料")
    @ImportValidated(maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.beltRubber2")
    private String beltRubber2;

    @ApiModelProperty(value = "钢带裁断角度")
    @ImportValidated(maxLength = 10)
    @Excel(name = "ui.construction.beltCuttingAngle")
    private String beltCuttingAngle;

    @ApiModelProperty(value = "钢带SAP")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.articleCrownSap")
    private String articleCrownSap;

    @ApiModelProperty(value = "钢带规格")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.articleCrownSpec")
    private String articleCrownSpec;

    @ApiModelProperty(value = "冠带条代号")
    @ImportValidated(maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.articleCrownCode")
    private String articleCrownCode;

    @ApiModelProperty(value = "胎面SAP")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.treadSap")
    private String treadSap;

    @ApiModelProperty(value = "胎面代号")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.treadCode")
    private String treadCode;

    @ApiModelProperty(value = "胎面宽")
    @ImportValidated(number = true, min = 0, max = 99999)
    @Excel(name = "ui.construction.treadShoulderWidth")
    private String treadShoulderWidth;

    @ApiModelProperty(value = "胎面肩宽")
    @ImportValidated(number = true, min = 0, max = 99999)
    @Excel(name = "ui.construction.treadShoulderJWidth")
    private String treadShoulderJWidth;

    @ApiModelProperty(value = "胎面长")
    @ImportValidated(number = true, min = 0, max = 99999)
    @Excel(name = "ui.construction.treadShoulderLength")
    private String treadShoulderLength;

    @ApiModelProperty(value = "胎面胶种")
    @ImportValidated(maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.treadRubberCategory")
    private String treadRubberCategory;

    @ApiModelProperty(value = "重量kg/条（上胎冠）")
    @ImportValidated(number = true, min = 0, max = 99999)
    @Excel(name = "ui.construction.tireCrownUpWidthWeight")
    private BigDecimal tireCrownUpWidthWeight;

    @ApiModelProperty(value = "重量kg/条（下胎冠）")
    @ImportValidated(number = true, min = 0, max = 99999)
    @Excel(name = "ui.construction.tireCrownDownWidthWeight")
    private BigDecimal tireCrownDownWidthWeight;

    @ApiModelProperty(value = "重量kg/条（胎翼）")
    @ImportValidated(number = true, min = 0, max = 99999)
    @Excel(name = "ui.construction.tireWingWidthWeight")
    private BigDecimal tireWingWidthWeight;

    @ApiModelProperty(value = "重量kg/条（底胶）")
    @ImportValidated(number = true, min = 0, max = 99999)
    @Excel(name = "ui.construction.primerWeight")
    private BigDecimal primerWeight;

    @ApiModelProperty(value = "重量kg/条（导电胶）")
    @ImportValidated(number = true, min = 0, max = 99999)
    @Excel(name = "ui.construction.conductingResinWeight")
    private BigDecimal conductingResinWeight;

    @ApiModelProperty(value = "胎侧口型")
    @ImportValidated(maxLength = 20, isCode = true)
    @Excel(name = "ui.construction.treadMouthPlate")
    private String treadMouthPlate;


    @ApiModelProperty(value = "胎胚BOM版本")
    @ImportValidated(required = true,maxLength = 30)
    @Excel(name = "ui.construction.embryoVersion",sort = 3)
    private String embryoVersion;

    @ApiModelProperty(value = "1#胎体布BOM版本")
    private String tireFabric1Version;

    @ApiModelProperty(value = "2#胎体布BOM版本")
    private String tireFabric2Version;

    @ApiModelProperty(value = "3#胎体布BOM版本")
    private String tireFabric3Version;

    @ApiModelProperty(value = "帘布大卷BOM版本")
    private String cordVersion;

    @ApiModelProperty(value = "内衬—BOM版本")
    private String insideVersion;

    @ApiModelProperty(value = "胎侧—BOM版本")
    private String sidewallVersion;

    @ApiModelProperty(value = "钢丝圈—BOM版本")
    private String beadVersion;

    @ApiModelProperty(value = "胎圈—BOM版本")
    private String tireRingVersion;

    @ApiModelProperty(value = "1#钢带BOM版本")
    private String belt1Version;

    @ApiModelProperty(value = "2#钢带BOM版本")
    private String belt2Version;

    @ApiModelProperty(value = "钢压大卷BOM版本")
    private String articleCrownVersion;

    @ApiModelProperty(value = "胎面BOM版本")
    private String treadVersion;

	/** 生产阶段（0：投产阶段；1试做阶段） */
	@ApiModelProperty(value = "生产阶段（0：投产阶段；1试做阶段）")
	private String productionStage;
    
	/** 帘布边胶 */
	@ApiModelProperty(value = "帘布边胶")
	private String tireFabricSideRubber;

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
}
