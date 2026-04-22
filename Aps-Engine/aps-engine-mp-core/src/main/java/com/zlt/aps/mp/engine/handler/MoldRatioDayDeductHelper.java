package com.zlt.aps.mp.engine.handler;

import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 模具分配比例-日降膜次数对象对象
 * 用以辅助判断续作Sku模具分配比例调整时的降膜处理
 *
 * @author ZLT
 * @date 20260420
 */
@Getter
public class MoldRatioDayDeductHelper implements Serializable {

    private Integer deductDay;

    private Integer deductCount;

    public MoldRatioDayDeductHelper(Integer deductDay, Integer deductCount) {
        this.deductDay = deductDay;
        this.deductCount = deductCount;
    }

    /**
     * 增加一次降膜次数
     */
    public void addDeductOneCount() {
        if (null == this.deductCount) {
            this.deductCount = BigDecimal.ZERO.intValue();
        }
        this.deductCount = this.deductCount + BigDecimal.ONE.intValue();
    }
}
