package com.zlt.aps.factory.domain.vo;

import com.alibaba.fastjson.JSON;
import com.zlt.aps.monthplan.api.domain.vo.MaterialInfoGrossRateJsonVo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 物料的基础信息
 *
 * @author ZLT
 * 20250308
 */
@Data
public class ProductBaseInfoVo implements Serializable {
    /**
     * 分厂编码
     */
    private String factoryCode;

    /**
     * 物料编号
     */
    private String productCode;

    /**
     * 轮胎类型
     */
    private String tireType;

    /**
     * 规格描述
     */
    private String productDesc;
    /**
     * 寸口（保留2位小数）
     */
    private BigDecimal proSize;
    /**
     * 模具大类
     */
    private String mouldCategory;

    /**
     * 硫化时间(单位秒-second)
     */
    private Integer curingTime;
    /**
     * 公用类型 取数据字典 biz_common_type的编码 1 公用规格 2 外销专用 3 内销专用 4 OE专用
     */
    private String commonType;

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
