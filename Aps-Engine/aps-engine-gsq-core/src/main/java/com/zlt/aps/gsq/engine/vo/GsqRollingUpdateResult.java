package com.zlt.aps.gsq.engine.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 钢丝圈排程滚动更新结果
 *
 * @author APS
 */
@Data
public class GsqRollingUpdateResult {

    /** 是否成功 */
    private boolean success;

    /** 滚动批次号 */
    private String batchNo;

    /** 影响的排程记录数 */
    private int affectedCount;

    /** 滚动前预计库存 */
    private double beforeStockQty;

    /** 滚动后预计库存 */
    private double afterStockQty;

    /** 变更明细列表 */
    private List<GsqRollingChangeDetail> changeDetails = new ArrayList<>();

    /** 错误信息（失败时） */
    private String errorMsg;

    /**
     * 构建成功结果
     */
    public static GsqRollingUpdateResult success(String batchNo, int affectedCount,
                                                  double beforeStock, double afterStock) {
        GsqRollingUpdateResult result = new GsqRollingUpdateResult();
        result.setSuccess(true);
        result.setBatchNo(batchNo);
        result.setAffectedCount(affectedCount);
        result.setBeforeStockQty(beforeStock);
        result.setAfterStockQty(afterStock);
        return result;
    }

    /**
     * 构建失败结果
     */
    public static GsqRollingUpdateResult fail(String batchNo, String errorMsg) {
        GsqRollingUpdateResult result = new GsqRollingUpdateResult();
        result.setSuccess(false);
        result.setBatchNo(batchNo);
        result.setErrorMsg(errorMsg);
        return result;
    }

    /**
     * 构建失败结果（无批次号，用于前置校验失败场景）
     */
    public static GsqRollingUpdateResult fail(String errorMsg) {
        return fail(null, errorMsg);
    }
}
