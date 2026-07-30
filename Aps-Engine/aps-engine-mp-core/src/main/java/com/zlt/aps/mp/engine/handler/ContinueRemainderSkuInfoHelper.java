package com.zlt.aps.mp.engine.handler;

import com.zlt.aps.mp.engine.domain.dto.CxContinueSkuInfoHelper;
import lombok.Getter;

import java.io.Serializable;
import java.util.Map;

/**
 * 可续作接活字块Sku信息对象
 * 值传递辅助类
 *
 * @author ZLT
 * @date 20260730
 */
@Getter
public class ContinueRemainderSkuInfoHelper implements Serializable {
    /**
     * 分组key 规格+花纹或是主花纹
     */
    private String groupKey;
    /**
     * 总的硫化机台数--模具数
     */
    private Integer sumMoldNumber;
    /**
     * 对应的续作Sku信息
     */
    private Map<String, CxContinueSkuInfoHelper> continueSkuInfo;

    /**
     * 构建对应可接活字块的续作Sku信息
     *
     * @param groupKey        分组Key 规格+花纹 或是主花纹
     * @param sumMoldNumber   硫化机台数转化成模具数
     * @param continueSkuInfo 对应的续作Sku信息
     * @return
     */
    public static ContinueRemainderSkuInfoHelper buildContinueRemainderSkuInfo(String groupKey,
                                                                               Integer sumMoldNumber,
                                                                               Map<String, CxContinueSkuInfoHelper> continueSkuInfo) {
        ContinueRemainderSkuInfoHelper remainderInfo = new ContinueRemainderSkuInfoHelper();
        remainderInfo.groupKey = groupKey;
        remainderInfo.sumMoldNumber = sumMoldNumber;
        remainderInfo.continueSkuInfo = continueSkuInfo;
        return remainderInfo;
    }

}
