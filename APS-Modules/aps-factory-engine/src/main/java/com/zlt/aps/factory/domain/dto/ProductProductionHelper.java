package com.zlt.aps.factory.domain.dto;

import com.zlt.aps.factory.domain.vo.ProductConstructionInfoVo;
import com.zlt.aps.monthplan.api.domain.vo.ProductSpecInfoVo;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 物料排产辅助类信息
 *
 * @author ZLT
 * @date 20250430
 */
@Getter
public class ProductProductionHelper {
    /**
     * 物料编码
     */
    private String productCode;

    /**
     * 硫化规格代号
     */
    private String specCode;

    /**
     * 施工代号，可转换成施工阶段
     */
    private String constructionCode;
    /**
     * 生胎代号
     */
    private String embryoCode;
    /**
     * 合模压力
     */
    private BigDecimal mouldClampingPressure;
    /**
     * 成型法: MACHINE_TYPE
     * 1-1次法
     * 2-2次法
     */
    private String mouldMethod;
    /**
     * 模具型腔
     */
    private String moldCavity;
    /**
     * 硫化规格代号、胚胎代码、成型法-json格式
     */
    private String specCodeInfo;
    /**
     * 施工配置
     */
    private ProductConstructionInfoVo productConstructionInfo;
    /**
     * 空对象
     */
    private final static ProductProductionHelper EMPTY = new ProductProductionHelper("", null, "", null);

    /**
     * 构造函数
     *
     * @param productCode  物料编码
     * @param specInfo     物料与施工关系，包含规格代号、施工代号、生胎代码、合模压力，模具行腔，成形法
     * @param specCodeInfo 所有硫化规格
     */
    public ProductProductionHelper(String productCode, ProductSpecInfoVo specInfo, String specCodeInfo, ProductConstructionInfoVo productConstructionInfo) {
        this.productCode = productCode;
        if (null == specInfo) {
            this.specCode = "";
            this.constructionCode = "";
            this.embryoCode = "";
            this.mouldClampingPressure = null;
            this.mouldMethod = "";
            this.moldCavity = "";
            this.productConstructionInfo = null;
        } else {
            this.specCode = specInfo.getSpecCode();
            this.constructionCode = specInfo.getConstructionCode();
            this.embryoCode = specInfo.getEmbryoCode();
            this.mouldClampingPressure = specInfo.getMouldClampingPressure();
            this.mouldMethod = specInfo.getMouldMethod();
            this.moldCavity = specInfo.getMoldCavity();
            this.productConstructionInfo = productConstructionInfo;
        }
        this.specCodeInfo = specCodeInfo;
    }

    /**
     * 空数据对象
     *
     * @return
     */
    public static ProductProductionHelper buildEmpty() {
        return EMPTY;
    }
}
