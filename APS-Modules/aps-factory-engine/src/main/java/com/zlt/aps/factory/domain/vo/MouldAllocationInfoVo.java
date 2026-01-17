package com.zlt.aps.factory.domain.vo;

import com.zlt.aps.factory.domain.dto.MouldAllocationDayInfoHelper;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 工厂模具分配比例
 *
 * @author ZLT
 * @date 20251217
 */
@Data
public class MouldAllocationInfoVo implements Serializable {

    /**
     * 工厂编码
     */
    private String factoryCode;
    /**
     * 主花纹
     */
    private String mainPattern;
    /**
     * 花纹
     */
    private String pattern;
    /**
     * 结构名
     */
    private String structureName;
    /**
     * 规格
     */
    private String specifications;
    /**
     * 分配数量
     */
    private Integer allocationQty;
    
    /**
     * 日限制信息集合
     */
    private Map<Integer, MouldAllocationDayInfoHelper> dayLimitInfoMap;

    /**
     * 业务重复键：结构|*|主花纹
     *
     * @return
     */
    public String getDuplicateKey() {
        String keyFormat = "%s|*|%s";
        return String.format(keyFormat, structureName, mainPattern);
    }
}
