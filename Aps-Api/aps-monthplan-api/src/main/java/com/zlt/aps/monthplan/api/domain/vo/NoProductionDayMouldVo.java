package com.zlt.aps.monthplan.api.domain.vo;

import com.zlt.aps.monthplan.api.enums.MouldNoProductionType;
import lombok.Data;

import java.io.Serializable;

/**
 * 模具不可排产日信息
 *
 * @author ZLT
 * @date 20250219
 */
@Data
public class NoProductionDayMouldVo implements Serializable {
    /**
     * 日期
     */
    private Integer day;
    /**
     * 不可排产类型 停工、维修、洗模
     */
    private MouldNoProductionType noProductionType;
}
