package com.zlt.aps.factory.domain.vo;

import com.tlt.aps.enums.ConstructionStageEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 搭配排产配置信息
 *
 * @author ZLT
 * @date 20250829
 */
@Data
public class MatchingProductionConfigurationVo implements Serializable {
    /**
     * 生产物料编号
     */
    @ApiModelProperty(value = "生产物料编号", name = "productCode")
    private String productCode;
    /**
     * 成型法
     */
    @ApiModelProperty(value = "成型法", name = "mouldMethod")
    private String mouldMethod;
    /**
     * 生胎代码
     */
    @ApiModelProperty(value = "生胎代码", name = "embryoCode")
    private String embryoCode;
    /**
     * 规格代号
     */
    @ApiModelProperty(value = "规格代号", name = "specCode")
    private String specCode;
    /**
     * 全规格代号信息 包含规格代号及对应的成型法
     */
    @ApiModelProperty(value = "全规格代号信息", name = "specCodeInfo")
    private String specCodeInfo;
    /**
     * 生产规格描述
     */
    @ApiModelProperty(value = "生产规格描述", name = "productDesc")
    private String productDesc;
    /**
     * 施工代号，可转换成施工阶段
     */
    private String constructionCode;
    /**
     * 施工阶段
     */
    @ApiModelProperty(value = "施工阶段", name = "constructionStage")
    private Integer constructionStage;
    /**
     * 施工阶段枚举实例
     */
    private ConstructionStageEnum constructionStageType;
    /**
     * 品牌
     */
    @ApiModelProperty(value = "品牌", name = "brand")
    private String brand;

    /**
     * 寸口
     */
    @ApiModelProperty(value = "寸口", name = "proSize")
    private BigDecimal proSize;

    /**
     * 规格
     */
    @ApiModelProperty(value = "规格", name = "specifications")
    private String specifications;

    /**
     * 花纹
     */
    @ApiModelProperty(value = "花纹", name = "pattern")
    private String pattern;

    /**
     * 层级
     */
    @ApiModelProperty(value = "层级", name = "hierarchy")
    private String hierarchy;

    /**
     * 硫化时间--到秒
     */
    @ApiModelProperty(value = "硫化时间-到秒", name = "curingTime")
    private BigDecimal curingTime;

    /**
     * 库位类别 1 内销 2 外销 3 OE
     */
    @ApiModelProperty(value = "库位类别 1 内销 2 外销 3 OE", name = "locationType")
    private String locationType;
    /**
     * 胎体布层级数 1 表示单层 2 表示多层(即2,3等)单层可使用多层，多层不能使用单层 即1可变成2,2不能变成1
     */
    private Integer tireFabricNumber;
    /**
     * 合模压力--拼模时使用
     */
    private BigDecimal mouldClampingPressure;
    /**
     * 模具行腔--拼模时使用
     */
    private String moldCavity;

    /**
     * 得到分组值
     * 寸口|*|成型法|*|胎体布层级
     *
     * @return
     */
    public String getGroupKey() {
        String groupKey = "%s|*|%s|*|%s";
        return String.format(groupKey, proSize, mouldMethod, tireFabricNumber);
    }
}
