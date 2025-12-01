package com.zlt.aps.factory.domain.vo;

import com.zlt.aps.factory.domain.dto.MouldTableInfoDto;
import lombok.Getter;

import java.io.Serializable;

/**
 * 单模排产--选中的模台对象辅助类
 *
 * @author ZLT
 * @date 20250718
 */
@Getter
public class SingleMouldSelectedMouldTableHelper implements Serializable {

    /**
     * 选中的模台 -- 可能是单模台分组，也可能是双模台中的一个模台
     * 双模台可能是拼模，也可能不是拼模
     */
    private MouldTableInfoDto selectedMouldTableInfo;

    /**
     * 起始时间
     */
    private Integer productionGroupStartDate;
    /**
     * 结束日期
     */
    private Integer productionGroupEndDate;

    /**
     * 构造函数--单模排产选中的模台辅助类
     *
     * @param selectedMouldTableInfo   选中的模台信息对象
     * @param productionGroupStartDate 开始排产日
     * @param productionGroupEndDate   结束排产日
     */
    public SingleMouldSelectedMouldTableHelper(MouldTableInfoDto selectedMouldTableInfo, Integer productionGroupStartDate, Integer productionGroupEndDate) {
        this.selectedMouldTableInfo = selectedMouldTableInfo;
        this.productionGroupStartDate = productionGroupStartDate;
        this.productionGroupEndDate = productionGroupEndDate;
    }
}
