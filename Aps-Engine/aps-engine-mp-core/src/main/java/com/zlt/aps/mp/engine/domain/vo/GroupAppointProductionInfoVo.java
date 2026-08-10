package com.zlt.aps.mp.engine.domain.vo;

import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * 分组指定生产信息对象
 *
 * @author ZLT
 * @date 20260713
 */
@Data
@Slf4j
public class GroupAppointProductionInfoVo implements Serializable {
    /**
     * 分组名称：必须项
     */
    private String groupName;
    /**
     * 成型机台编号
     */
    private String cxMachineCode;
    /**
     * 周期第N天：排产周期所处天数
     */
    private Integer monthStartDay;
    /**
     * 最大可分配生产天数
     */
    private Integer maxAllocationDay;

    /**
     * 是否有效配置:必须指定分组名(TBR-结构)
     * 1、没有指定机台，不能指定时间且必须指定最大可分配生产天数
     * 2、有指定机台，则必须指定上机日，可不指定最大可分配生产天数
     *
     * @param context
     * @return
     */
    public boolean isEffectiveConfiguration(Context context) {
        if (StringUtils.isBlank(groupName)) {
            return false;
        }
        if (StringUtils.isBlank(cxMachineCode)) {
            //没有指定机台，则表示控制结构最大分配天数，不能指定上机日
            if (null != monthStartDay) {
                return false;
            }
            return null != maxAllocationDay;
        }
        //指定机台，则必须指定上机日
        return isEffectiveMonthStartDay(context);
    }

    /**
     * 是否指定机台
     *
     * @return
     */
    public boolean isAppointCxMachine(Context context) {
        if (StringUtils.isBlank(cxMachineCode)) {
            return false;
        }
        return isEffectiveMonthStartDay(context);
    }

    /**
     * 是否为有效上机日
     *
     * @param context 排产上下文
     * @return
     */
    private boolean isEffectiveMonthStartDay(Context context) {
        if (null == monthStartDay) {
            return false;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer cycleStartDay = FactoryConstant.MONTH_START_DAY;
        Integer cycleEndDay = productionContext.getMonthDays();
        if (monthStartDay < cycleStartDay || monthStartDay > cycleEndDay) {
            return false;
        }
        return true;
    }
}
