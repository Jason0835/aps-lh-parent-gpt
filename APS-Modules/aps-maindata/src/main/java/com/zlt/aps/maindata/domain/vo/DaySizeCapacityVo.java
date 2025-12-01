package com.zlt.aps.maindata.domain.vo;

import com.zlt.aps.monthplan.api.domain.entity.SizeCapacityConfiguration;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 日寸口产能配置
 * 树形结构
 *
 * @author ZLT
 * @date 20250606
 */
@Data
public class DaySizeCapacityVo implements Serializable {
    /**
     * 父级 寸口|*|成型法|*|成型机类型
     */
    private String parentSizeCapacityTreeKey;
    /**
     * 寸口|*|成型法|*|成型机类型
     */
    private String sizeCapacityTreeKey;
    /**
     * 寸口
     */
    private BigDecimal proSize;
    /**
     * 成型法
     */
    private String mouldMethod;
    /**
     * 胎体布层级数 1 表示单层 2 表示多层(即2,3等)
     */
    private Integer tireFabricNumber;
    /**
     * 整月部分
     */
    private Integer intPart;
    /**
     * 非整月部分
     */
    private Integer decimalDays;
    /**
     * 数据内容
     */
    private SizeCapacityConfiguration data;
    /**
     * 下一部分
     */
    private DaySizeCapacityVo nextSize;
}
