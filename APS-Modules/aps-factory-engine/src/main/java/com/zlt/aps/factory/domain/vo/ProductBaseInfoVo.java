package com.zlt.aps.factory.domain.vo;

import com.alibaba.fastjson.JSON;
import com.zlt.aps.monthplan.api.domain.vo.MaterialInfoGrossRateJsonVo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 物料的基础信息
 *
 * @author ZLT
 * 20251211
 */
@Data
public class ProductBaseInfoVo implements Serializable {
    /**
     * 工厂编码
     */
    private String factoryCode;

    /**
     * 物料编号
     */
    private String materialCode;

    /**
     * 物料描述
     */
    private String materialDesc;

    /**
     * 英寸
     */
    private String proSize;

    /**
     * 结构
     */
    private String structureName;
    /**
     * 产品品类 TBR 全钢 PCR 半钢
     */
    private String productTypeCode;

    /**
     * 规格
     */
    private String specifications;

    /**
     * 主花纹
     */
    private String mainPattern;

    /**
     * 花纹
     */
    private String pattern;
    /**
     * 轮胎类型
     */
    private String tireType;

    /**
     * 公用类型 取数据字典 biz_common_type的编码 1 公用规格 2 外销专用 3 内销专用 4 OE专用
     */
    private String commonType;

    /**
     * 不可生产
     */
    private String cantProduce;

    /**
     * 废停
     */
    private String forbidTag;

    /**
     * 毛利率Json
     */
    private String grossRateJson;

    /**
     * 利率值
     */
    List<MaterialInfoGrossRateJsonVo> rateList;

    /**
     * 获取物料的毛利率值
     *
     * @return
     */
    public List<MaterialInfoGrossRateJsonVo> getRateList() {
        if (StringUtils.isBlank(grossRateJson)) {
            return Collections.emptyList();
        }
        return JSON.parseArray(grossRateJson, MaterialInfoGrossRateJsonVo.class);
    }
}
