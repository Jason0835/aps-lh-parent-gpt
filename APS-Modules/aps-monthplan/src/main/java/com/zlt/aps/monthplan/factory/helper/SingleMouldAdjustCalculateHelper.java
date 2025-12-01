package com.zlt.aps.monthplan.factory.helper;

import com.zlt.aps.monthplan.factory.dto.MouldProductRelationDto;
import lombok.Data;

import java.util.Map;
import java.util.Set;

/**
 * 单模具计划调增辅助类
 * 计划排产开始~结束日期
 * 循环起始~结束日
 * 模具编号、还需增加的量
 * 存储已排模具、存储新增模具排产、最大可用模具信息
 *
 * @author ZLT
 * @date 20250407
 */
@Data
public class SingleMouldAdjustCalculateHelper {

    /**
     * 计划排产开始日期
     */
    private Integer beginDate;
    /**
     * 计划排产结束日期
     */
    private Integer endDay;
    /**
     * 循环起始日
     */
    private Integer startDay;
    /**
     * 循环结束日期
     */
    private Integer maxDay;
    /**
     * 模具编号
     */
    private String mouldCode;
    /**
     * 还需增加的量
     */
    private Long needAddQty;
    /**
     * 存储已排模具--临时存储
     */
    private Set<String> productionMouldSet;
    /**
     * 存储新增模具排产--临时存储
     */
    private Set<String> addMouldSet;
    /**
     * 最大可用模具信息--临时存储
     */
    private Map<String, MouldProductRelationDto> maxEnableMouldMap;

    /**
     * @param beginDate  计划排产开始日期
     * @param endDay     计划排产结束日期
     * @param startDay   循环起始日
     * @param maxDay     循环结束日期
     * @param mouldCode  模具编号
     * @param needAddQty 还需增加的量
     */
    public SingleMouldAdjustCalculateHelper(Integer beginDate, Integer endDay, Integer startDay, Integer maxDay, String mouldCode, Long needAddQty) {
        this.beginDate = beginDate;
        this.endDay = endDay;
        this.startDay = startDay;
        this.maxDay = maxDay;
        this.mouldCode = mouldCode;
        this.needAddQty = needAddQty;
    }
}
