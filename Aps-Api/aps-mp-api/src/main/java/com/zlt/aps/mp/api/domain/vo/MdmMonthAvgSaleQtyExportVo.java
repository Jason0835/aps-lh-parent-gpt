package com.zlt.aps.mp.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.mp.api.domain.entity.MpHistorySaleRecord;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * 月均销量导出Vo
 *
 * @author Chen
 * @since 2025/12/19
 */
public class MdmMonthAvgSaleQtyExportVo extends BaseEntity {

    /**
     * 工厂
     */
    private String factoryCode;

    /**
     * 产品品类
     */
    private String productTypeCode;

    /**
     * 内外销
     */
    private String locationType;

    /**
     * 品牌
     */
    private String brand;

    /**
     * 物料编码
     */
    private String materialCode;

    /**
     * 物料描述
     */
    private String materialDesc;

    /**
     * 滚动12个月销量
     */
    private Long rollTwelveMonthSaleQty;

    /**
     * 月均销量
     */
    private Long averageSaleQty;

    /**
     * 近3个月均销量
     */
    private Long passThreeMonthSaleQty;

    /**
     * 近6个月均销量
     */
    private Long passSixMonthSaleQty;

    /**
     * 近12个月的发货频次
     */
    private Integer deliveryFrequency;

    /**
     * 适销区域，多个英文逗号分隔
     */
    private String saleArea;

    /**
     * 适销区域，多个英文逗号分隔
     */
    private String saleAreaName;

    @ApiModelProperty(value = "区域(往前12个月所有区域)月均销量总和", name = "areaGroupList")
    @TableField(exist = false)
    private List<MpHistorySaleRecord> areaGroupList;

    @ApiModelProperty(value = "月份(往前12个月)月均销量总和", name = "monthGroupList")
    @TableField(exist = false)
    private List<MpHistorySaleRecord> monthGroupList;
}
