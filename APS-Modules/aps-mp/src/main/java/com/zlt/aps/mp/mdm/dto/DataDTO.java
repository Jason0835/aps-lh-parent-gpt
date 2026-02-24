package com.zlt.aps.mp.mdm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.poi.ss.formula.functions.T;
import java.io.Serializable;

/**
 * 数据查询DTO
 * @author wengpc
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataDTO<T> implements Serializable {

    /**
     * 工厂编码
     */
    private String factoryCode;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 缓存Key
     */
    private String cacheKey;

    /**
     * 是否查询缓存
     */
    private boolean isQueryCache;

    /**
     * 查询对象
     */
    private T queryObject;


}
