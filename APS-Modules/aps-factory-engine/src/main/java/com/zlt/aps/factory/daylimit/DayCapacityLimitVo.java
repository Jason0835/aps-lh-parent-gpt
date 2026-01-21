package com.zlt.aps.factory.daylimit;

import com.zlt.aps.factory.domain.Context;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Map;

/**
 * 日产能控制对象
 * 排产日、最大产能上限、最低产能，产能比例
 *
 * @author ZLT
 * @date 20250106
 */
@Getter
public class DayCapacityLimitVo implements Serializable {
    /**
     * 日产能控制信息
     * key=排产日 : value=日排产控制信息
     */
    private Map<Integer, DayCapacityLimitHelper> dayCapacityLimitMap;

    /**
     * 构建日产能控制对象
     *
     * @param dayCapacityLimitMap
     */
    public DayCapacityLimitVo(Map<Integer, DayCapacityLimitHelper> dayCapacityLimitMap) {
        this.dayCapacityLimitMap = dayCapacityLimitMap;
    }

    /**
     * 更新日排产信息
     *
     * @param dayCapacityLimitMap
     */
    public void updateWholeDayLimitInfo(Map<Integer, DayCapacityLimitHelper> dayCapacityLimitMap) {
        this.dayCapacityLimitMap = dayCapacityLimitMap;
    }

    /**
     * 增加每日结构切换使用次数
     *
     * @param context              排产上下文
     * @param changeProductionDate 切换日
     * @param groupName            分组名
     * @param cxMachineCode        成型机台
     */
    public void addChangeGroupNameUsedQty(Context context, Integer changeProductionDate, String groupName, String cxMachineCode) {
        if(null == changeProductionDate || StringUtils.isBlank(groupName) || StringUtils.isBlank(cxMachineCode)){
            return;
        }
        if(CollectionUtils.isEmpty(dayCapacityLimitMap)){
            return ;
        }
        DayCapacityLimitHelper dayLimit = dayCapacityLimitMap.get(changeProductionDate);
        if(null == dayLimit){
            return ;
        }

    }
}
