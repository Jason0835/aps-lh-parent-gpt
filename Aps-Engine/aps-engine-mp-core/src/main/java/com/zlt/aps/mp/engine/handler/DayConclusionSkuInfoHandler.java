package com.zlt.aps.mp.engine.handler;

import com.zlt.aps.mp.engine.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.SkuDayProductionInfoHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 日收尾余量Sku处理
 *
 * @author ZLT
 * @date 20260326
 */
@Slf4j
public class DayConclusionSkuInfoHandler {
    /**
     * 获取可释放硫化机台，同时可以进行拼模的SKu及余量信息
     *
     * @param context
     * @param currentDayInfo
     * @param nextDayInfo
     */
    public void getConclusionSkuInfo(Context context, GroupPlanCxLhCapacityLimitHelper currentDayInfo, GroupPlanCxLhCapacityLimitHelper nextDayInfo){
        if(null == currentDayInfo){
            return ;
        }
        //表示最后一天
        if(null == nextDayInfo){

        }
    }

    /**
     * 最后一天可收尾释放硫化机台的Sku信息
     *
     * @param context
     * @param currentDayInfo
     */
    private void getLastDayConclusionSkuInfo(Context context, GroupPlanCxLhCapacityLimitHelper currentDayInfo){
        Map<String, List<SkuDayProductionInfoHelper>> skuProductionDetailInfo = currentDayInfo.getSkuProductionDetailInfo();
        if(CollectionUtils.isEmpty(skuProductionDetailInfo)){
            //表示没有排产
            return ;
        }
        Integer productionDay = currentDayInfo.getDay();
        Set<Integer> openSet = Optional.ofNullable(context.getProductionDayAfterStop()).orElse(Collections.emptySet());
        boolean isOpenDay = openSet.contains(productionDay);
        //开产日，没有能衔接的
        if(isOpenDay){

        }
        Map<String, List<SkuDayProductionInfoHelper>> canLeftOverMap = new HashMap<>();
        Map<String, List<SkuDayProductionInfoHelper>> occupyInfoMap = new HashMap<>();
        skuProductionDetailInfo.forEach((materialDesc, detailList) ->{
            List<SkuDayProductionInfoHelper> conclusionInfoList = new ArrayList<>();
            List<SkuDayProductionInfoHelper> occupyInfoList = new ArrayList<>();
            detailList.forEach(singleLhMachine ->{
                Integer dayMaxQty = singleLhMachine.getDayLhMachineQty();
                Integer dayProductionQty = singleLhMachine.getSumProductionQty();
                Integer lossQty = singleLhMachine.getLossQty();
                if(dayProductionQty < dayMaxQty && lossQty == BigDecimal.ZERO.intValue()){
                    conclusionInfoList.add(singleLhMachine);
                }
                if(dayProductionQty < dayMaxQty && lossQty > BigDecimal.ZERO.intValue()){
                    occupyInfoList.add(singleLhMachine);
                }
            });
            if(!CollectionUtils.isEmpty(conclusionInfoList)){
                canLeftOverMap.put(materialDesc, conclusionInfoList);
            }
            if(!CollectionUtils.isEmpty(occupyInfoList)){
                occupyInfoMap.put(materialDesc, occupyInfoList);
            }
        });
        Integer leftOverCount = BigDecimal.ZERO.intValue();
        if(!CollectionUtils.isEmpty(canLeftOverMap)){

        }




    }

    private void setPairInfo(){

    }

}
