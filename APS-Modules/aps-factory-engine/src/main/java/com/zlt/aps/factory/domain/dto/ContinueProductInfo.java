package com.zlt.aps.factory.domain.dto;

import com.tlt.aps.constant.StringConstant;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * 排产计划-续作信息
 *
 * @author ZLT
 * @date 20251215
 */
@Data
public class ContinueProductInfo implements Serializable {

    /**
     * 分组信息--TBR结构名
     */
    private String groupName;

    /**
     * 成型机台编号 多个以,拼接
     */
    private String cxMachineCode;

    /**
     * 物料编码
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
     * 胎胚号
     */
    private String embryoCode;

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
     * 模具使用数量变化信息
     */
    private String mouldChangeInfo;
    /**
     * 续作在机机台-从结构转产表中获取
     */
    private Set<String> continueCxMachineCodeSet;

    /**
     * 获取对应的硫化机台数
     *
     * @return
     */
    public Integer getLhMachineCount() {
        if (StringUtils.isBlank(mouldChangeInfo)) {
            return BigDecimal.ZERO.intValue();
        }
        String[] changeArray = mouldChangeInfo.split(StringConstant.DASH);
        if (changeArray.length <= BigDecimal.ONE.intValue()) {
            return Integer.parseInt(changeArray[0]);
        }
        return Integer.parseInt(changeArray[changeArray.length - 1]);
    }

    /**
     * 提取有效的成型机台，放入cxMachineCodeSet集合中
     *
     * @param cxMachineCodeSet 有效成型机台集合
     * @param allCxMachineMap  所有成型机台
     */
    public void extractEffectiveCxMachineCode(Set<String> cxMachineCodeSet, Map<String, CxMachineBaseInfoVo> allCxMachineMap) {
        if (null == cxMachineCodeSet || null == allCxMachineMap) {
            return;
        }
        if (CollectionUtils.isEmpty(continueCxMachineCodeSet)) {
            return;
        }
        continueCxMachineCodeSet.forEach(singleCxMachineCode -> {
            if (allCxMachineMap.containsKey(singleCxMachineCode)) {
                cxMachineCodeSet.add(singleCxMachineCode);
            }
        });
    }

    /**
     * 提取续作Sku的续作排产模具数，并放入continueSkuMouldNumberMap集合中
     * continueSkuMouldNumberMap : key=materialDesc : value=续作sku信息
     *
     * @param continueSkuMouldNumberMap 存储续作Sku的续作模具数
     */
    public void extractSkuProductionMouldNumber(Map<String, CxContinueSkuInfoHelper> continueSkuMouldNumberMap) {
        if (null == continueSkuMouldNumberMap) {
            return;
        }
        Integer currentMouldNumber = getLhMachineCount();
        if (null == currentMouldNumber || currentMouldNumber <= BigDecimal.ZERO.intValue()) {
            currentMouldNumber = BigDecimal.ZERO.intValue();
        }
        CxContinueSkuInfoHelper continueSkuInfo = continueSkuMouldNumberMap.get(materialDesc);
        if (null == continueSkuInfo) {
            continueSkuInfo = builderEmpty();
            continueSkuMouldNumberMap.put(materialDesc, continueSkuInfo);
        }
        Integer sumNumber = continueSkuInfo.getMouldNumber();
        if (null == sumNumber) {
            sumNumber = BigDecimal.ZERO.intValue();
        }
        sumNumber = sumNumber + currentMouldNumber;
        continueSkuInfo.setMouldNumber(sumNumber);
    }

    /**
     * 创建带有基础信息-没有模具数的续作sku信息
     *
     * @return
     */
    private CxContinueSkuInfoHelper builderEmpty() {
        CxContinueSkuInfoHelper continueSkuInfo = new CxContinueSkuInfoHelper();
        BeanUtils.copyProperties(this, continueSkuInfo);
        continueSkuInfo.setMouldNumber(BigDecimal.ZERO.intValue());
        return continueSkuInfo;
    }
}
