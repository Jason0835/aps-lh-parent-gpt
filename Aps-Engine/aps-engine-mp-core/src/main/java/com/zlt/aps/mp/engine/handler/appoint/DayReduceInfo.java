package com.zlt.aps.mp.engine.handler.appoint;

import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;

/**
 * 在机结构在某天强行下机时，理论需要减的胎胚、硫化机台等信息
 * 指定机台时间段业务辅助类
 *
 * @author ZLT
 * @date 20260810
 */
@Getter
public class DayReduceInfo implements Serializable {
    /**
     * 强行下机日
     */
    private Integer day;
    /**
     * 当日扣减的成型机台信息
     */
    private Set<CxMachineBaseInfoVo> dayCxMachineInfoSet;
    /**
     * 最大胎胚种类数
     */
    private Integer maxEmbryoCodeCount;
    /**
     * 最大硫化机台数
     */
    private Integer maxLhMachines;
    /**
     * 扣减的胎胚种类数
     */
    private Integer embryoCodeCount;
    /**
     * 扣减的硫化机台数
     */
    private Integer lhMachines;
    /**
     * 扣减的成型机台
     */
    private Set<String> cxMachineSet;

    /**
     * 构造函数
     *
     * @param day                 扣减日
     * @param dayCxMachineInfoSet 当日扣减的成型机台
     * @param maxEmbryoCodeCount  最大胎胚种类数
     * @param maxLhMachines       最大硫化机台数
     * @param embryoCodeCount     最大可扣减胎胚种类数
     * @param lhMachines          最大可扣减硫化机台数
     * @param cxMachineSet        成型机台编号集合
     */
    public DayReduceInfo(Integer day,
                         Set<CxMachineBaseInfoVo> dayCxMachineInfoSet,
                         Integer maxEmbryoCodeCount,
                         Integer maxLhMachines,
                         Integer embryoCodeCount,
                         Integer lhMachines,
                         Set<String> cxMachineSet) {
        this.day = day;
        this.dayCxMachineInfoSet = dayCxMachineInfoSet;
        this.maxEmbryoCodeCount = maxEmbryoCodeCount;
        this.maxLhMachines = maxLhMachines;
        this.embryoCodeCount = embryoCodeCount;
        this.lhMachines = lhMachines;
        this.cxMachineSet = cxMachineSet;
    }

    /**
     * 最大剩余胎胚种类数
     *
     * @return
     */
    public Integer getLeftOverMaxEmbryoCodeCount() {
        if (null == maxEmbryoCodeCount || maxEmbryoCodeCount < BigDecimal.ONE.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        if (null == embryoCodeCount || embryoCodeCount < BigDecimal.ONE.intValue()) {
            return maxEmbryoCodeCount;
        }
        Integer leftOver = maxEmbryoCodeCount - embryoCodeCount;
        if (leftOver < BigDecimal.ONE.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        return leftOver;
    }

    /**
     * 最大剩余硫化机台数
     *
     * @return
     */
    public Integer getLeftOverMaxLhMachines() {
        if (null == maxLhMachines || maxLhMachines < BigDecimal.ONE.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        if (null == lhMachines || lhMachines < BigDecimal.ONE.intValue()) {
            return maxLhMachines;
        }
        Integer leftOver = maxLhMachines - lhMachines;
        if (leftOver < BigDecimal.ONE.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        return leftOver;
    }
}
