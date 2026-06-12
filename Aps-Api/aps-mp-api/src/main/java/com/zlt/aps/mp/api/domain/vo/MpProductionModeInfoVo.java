package com.zlt.aps.mp.api.domain.vo;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.mp.api.enums.ProductionModeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 排产模式信息对象
 *
 * @author zlt
 * @since 20260611
 */
@Data
@ApiModel(value = "排产模式信息对象", description = "排产模式信息对象")
public class MpProductionModeInfoVo implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 工厂编码
     */
    @ApiModelProperty(notes = "工厂编码", name = "factoryCode")
    private String factoryCode;
    /**
     * 产品品类 半钢，全钢
     */
    @ApiModelProperty(value = "产品品类 半钢，全钢", name = "productTypeCode")
    private String productTypeCode;
    /**
     * 排产模式
     */
    @ApiModelProperty(value = "排产模式", name = "排产模式")
    private Integer productionMode;
    /**
     * 说明
     */
    @ApiModelProperty(value = "说明", name = "desc")
    private String desc;
    /**
     * 是否校验切换模式
     */
    @ApiModelProperty(value = "是否校验切换模式 1 校验 0 忽略", name = "isCheckChange")
    private Integer isCheckChange;
    /**
     * 排产模式参数
     */
    private String productionModeParamCode;

    /**
     * 根据排产模式，构建排产模式信息对象
     *
     * @param productionMode     排产模式
     * @param factoryCode        工厂
     * @param productionTypeCode 产品品类
     * @return
     */
    public static MpProductionModeInfoVo build(ProductionModeEnum productionMode, String factoryCode, String productionTypeCode) {
        if (null == productionMode) {
            return null;
        }
        MpProductionModeInfoVo modeInfo = new MpProductionModeInfoVo();
        modeInfo.setProductionMode(productionMode.getMode());
        modeInfo.setFactoryCode(factoryCode);
        modeInfo.setProductTypeCode(productionTypeCode);
        String i18nKey = productionMode.getI18nKey();
        String desc = I18nUtil.getMessage(i18nKey);
        modeInfo.setDesc(desc);
        return modeInfo;
    }
}
