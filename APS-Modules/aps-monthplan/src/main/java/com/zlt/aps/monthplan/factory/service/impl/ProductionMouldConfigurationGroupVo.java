package com.zlt.aps.monthplan.factory.service.impl;

import lombok.Getter;

import java.io.Serializable;

/**
 * 模具正在生产品种辅助类
 *
 * @author ZLT
 * @date 20250715
 */
@Getter
public class ProductionMouldConfigurationGroupVo implements Serializable {

    /**
     * 模台数
     */
    private Integer moldQty;
    /**
     * 本身模台数
     */
    private Integer mouldNumber;
    /**
     * 是否拼模
     */
    private boolean assemble;

    public ProductionMouldConfigurationGroupVo(Integer moldQty, Integer mouldNumber) {
        this.moldQty = moldQty;
        this.mouldNumber = mouldNumber;
        if (moldQty < mouldNumber) {
            assemble = true;
        } else {
            assemble = false;
        }
    }
}
