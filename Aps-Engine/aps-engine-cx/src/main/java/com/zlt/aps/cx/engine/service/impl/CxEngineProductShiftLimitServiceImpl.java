package com.zlt.aps.cx.engine.service.impl;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cx.engine.common.CommonCacheService;
import com.zlt.aps.cx.engine.constants.CxEngineConstants;
import com.zlt.aps.cx.engine.domain.CxEngineProductDimensionLimit;
import com.zlt.aps.cx.engine.domain.CxEngineProductMachineLimit;
import com.zlt.aps.cx.engine.domain.CxEngineProductStockLimit;
import com.zlt.aps.cx.engine.exception.CxScheduleEngineException;
import com.zlt.aps.cx.engine.mapper.CxEngineProductShiftLimitMapper;
import com.zlt.aps.cx.engine.service.CxEngineProductShiftLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;

@Service("cxEngineProductShiftLimitService")
@Slf4j
public class CxEngineProductShiftLimitServiceImpl implements CxEngineProductShiftLimitService {

    @Autowired
    private CxEngineProductShiftLimitMapper cxEngineProductShiftLimitMapper;
    @Autowired
    private CommonCacheService commonCacheService;

    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符

    @Override
    public List<CxEngineProductStockLimit> selectCxProductShiftStockLimitList(CxEngineProductStockLimit cxEngineProductStockLimit) {
        return cxEngineProductShiftLimitMapper.selectCxProductShiftStockLimitList(cxEngineProductStockLimit);
    }

    @Override
    public List<CxEngineProductDimensionLimit> selectCxEngineProductDimensionLimitList(CxEngineProductDimensionLimit cxEngineProductDimensionLimit) {
        return cxEngineProductShiftLimitMapper.selectCxEngineProductDimensionLimitList(cxEngineProductDimensionLimit);
    }

    @Override
    public List<CxEngineProductMachineLimit> selectCxEngineProductMachineLimitList(CxEngineProductMachineLimit cxEngineProductMachineLimit) {
        return cxEngineProductShiftLimitMapper.selectCxEngineProductMachineLimitList(cxEngineProductMachineLimit);
    }

    /**
     * 当前排产的规格信息
     * @param key 轮胎类型
     * @param embryoCodeTypeTotalMap 胎胚类型对应的库存数
     * @return
     */
    @Override
    public Double adjustLhShiftCountByStock(String key, Map<String, Integer> embryoCodeTypeTotalMap,Map<String,String> cxParams,List<CxEngineProductStockLimit> productStockLimits,StringBuilder logDetail) {
        logDetail.append("adjustLhShiftCountByStock,进行调整班数获取").append(division);
        Double adjustLhShiftCount= BigDecimal.ZERO.doubleValue();
        if(StringUtils.isNotEmpty(key)&&StringUtils.isNotEmpty(embryoCodeTypeTotalMap)){
            if(StringUtils.isNotEmpty(productStockLimits)&&embryoCodeTypeTotalMap.containsKey(key)){
                //当前胎胚类型对应的库存数
                Integer embryoTypeStockNum=embryoCodeTypeTotalMap.get(key);
                logDetail.append("轮胎类型前缀：【").append(key).append("】,类型库存=").append(embryoTypeStockNum).append(division);
                //根据限制类型来分别处理
                Map<String,CxEngineProductStockLimit> productStockLimitMap = productStockLimits.stream().collect(Collectors.toMap(CxEngineProductStockLimit::getLimitType, cxEngineProductStockLimit -> cxEngineProductStockLimit,(limit1, limit2)->limit1));
                logDetail.append("轮胎类型设置信息:").append(toJSONString(productStockLimitMap)).append(division);
               if(StringUtils.isNotEmpty(productStockLimitMap)){
                   //1.库存大于上限，加上设置的班数
                   if(productStockLimitMap.containsKey(CxEngineConstants.STOCK_UP_LIMIT)){
                       //库存上限
                       CxEngineProductStockLimit upLimit=productStockLimitMap.get(CxEngineConstants.STOCK_UP_LIMIT);
                       if(!productStockLimitMap.containsKey(CxEngineConstants.STOCK_UP_LIMIT_WARNING)){ //上限预警设置校验
                           throw new CxScheduleEngineException(I18nUtil.getMessage("cx.schedule.stock.up.limit.warning.empty"));
                       }
                       //库存上限预警
                       CxEngineProductStockLimit upWarningLimit=productStockLimitMap.get(CxEngineConstants.STOCK_UP_LIMIT_WARNING);
                       if(embryoTypeStockNum>=upLimit.getStockNum()){ //大于等于上限 取上限调整班数
                           adjustLhShiftCount=upLimit.getShiftParams();
                           logDetail.append("大于等于上限调整班数=").append(adjustLhShiftCount).append(division);
                       }else if(embryoTypeStockNum>=upWarningLimit.getStockNum()){ //大于等于上限预警,小于上限 取上限预警调整班数
                           adjustLhShiftCount=upWarningLimit.getShiftParams();
                           logDetail.append("大于等于上限预警,小于上限取上限预警调整班数=").append(adjustLhShiftCount).append(division);
                       }
                   }else{
                       throw new CxScheduleEngineException(I18nUtil.getMessage("cx.schedule.stock.up.limit.empty"));
                   }

                   //2.库存预警下限
                   if(productStockLimitMap.containsKey(CxEngineConstants.STOCK_BOTTOM_LIMIT_WARNING)){
                       CxEngineProductStockLimit bottomWarningLimit=productStockLimitMap.get(CxEngineConstants.STOCK_BOTTOM_LIMIT_WARNING);
                       if(!productStockLimitMap.containsKey(CxEngineConstants.STOCK_BOTTOM_LIMIT)){ //库存下限设置校验
                           throw new CxScheduleEngineException(I18nUtil.getMessage("cx.schedule.stock.bottom.limit.empty"));
                       }
                       CxEngineProductStockLimit bottomLimit=productStockLimitMap.get(CxEngineConstants.STOCK_BOTTOM_LIMIT);
                       if(embryoTypeStockNum>=bottomLimit.getStockNum()&&embryoTypeStockNum<=bottomWarningLimit.getStockNum()){ //小于下限预警大于下限 取下限预警班数
                           adjustLhShiftCount=bottomWarningLimit.getShiftParams();
                           logDetail.append("小于下限预警大于下限 取下限预警班数=").append(adjustLhShiftCount).append(division);
                       }else if(embryoTypeStockNum<bottomLimit.getStockNum()){//小于库存下限
                           adjustLhShiftCount=bottomLimit.getShiftParams();
                           logDetail.append("小于库存下限取下限班数=").append(adjustLhShiftCount).append(division);
                       }
                   }else{
                       throw new CxScheduleEngineException(I18nUtil.getMessage("cx.schedule.stock.bottom.limit.warning.empty"));
                   }
               }



                //1.库存下限设定(2022-01-11 下限预警大于下限)
               /* if(productStockLimitMap.containsKey(CxEngineConstants.STOCK_BOTTOM_LIMIT_WARNING)){//下限预警
                    CxEngineProductStockLimit bottomLimitWarning=productStockLimitMap.get(CxEngineConstants.STOCK_BOTTOM_LIMIT_WARNING);
                    if(embryoTypeStockNum<=bottomLimitWarning.getStockNum()){//低于预警
                        adjustLhShiftCount=bottomLimitWarning.getShiftParams();//取下限预警
                    }else if(productStockLimitMap.containsKey(CxEngineConstants.STOCK_BOTTOM_LIMIT)){//高于预警低于下限
                        CxEngineProductStockLimit bottomLimit=productStockLimitMap.get(CxEngineConstants.STOCK_BOTTOM_LIMIT);
                        if(embryoTypeStockNum<=bottomLimit.getStockNum()){
                            adjustLhShiftCount=bottomLimit.getShiftParams();//取下限
                        }
                    }
                }*/
                //2.库存上限设定
               /* if(productStockLimitMap.containsKey(CxEngineConstants.STOCK_UP_LIMIT_WARNING)){
                    CxEngineProductStockLimit upLimitWarning=productStockLimitMap.get(CxEngineConstants.STOCK_UP_LIMIT_WARNING);
                    //2.1高于上限预警低于上限
                    if(embryoTypeStockNum>upLimitWarning.getStockNum()){ //高于上限预警
                        if( productStockLimitMap.containsKey(CxEngineConstants.STOCK_UP_LIMIT)){//是否存在上限
                            CxEngineProductStockLimit upLimit=productStockLimitMap.get(CxEngineConstants.STOCK_UP_LIMIT);
                            if(embryoTypeStockNum<=upLimit.getStockNum()){
                                adjustLhShiftCount=upLimit.getShiftParams();//取上限调整班数
                            }
                        }
                    }else if(productStockLimitMap.containsKey(CxEngineConstants.STOCK_BOTTOM_LIMIT)){ //低于上限预警 高于下限
                        CxEngineProductStockLimit bottomLimit=productStockLimitMap.get(CxEngineConstants.STOCK_BOTTOM_LIMIT);
                        if(embryoTypeStockNum>bottomLimit.getStockNum()){
                            adjustLhShiftCount=upLimitWarning.getShiftParams();//取上限预警
                        }
                    }
                }*/

            }
        }else{
            logDetail.append("没有匹配，调整班次默认为O，不进行调整").append(division);
        }
        return adjustLhShiftCount;
    }

    /**
     * 同寸口一班平均可硫化班次调整量设定
     * @param dimension
     * @param dimensionAvgLhShift
     * @param productDimensionLimits
     * @return
     */
    @Override
    public Double adjustLhShiftCountByDimension(Double dimension, Double dimensionAvgLhShift, List<CxEngineProductDimensionLimit> productDimensionLimits,StringBuilder logDetail) {
        logDetail.append("【开始进行第二轮班次调整】》").append("寸口=").append(dimension).append(",白班平均可硫化班次=").append(dimensionAvgLhShift).append(division);
        Double adjustLhShiftCount =BigDecimal.ZERO.doubleValue();
        if(StringUtils.isNotEmpty(productDimensionLimits)){
            for(CxEngineProductDimensionLimit cxEngineProductDimensionLimit:productDimensionLimits){
                if(cxEngineProductDimensionLimit.getMinAvgShift()<=dimensionAvgLhShift&&cxEngineProductDimensionLimit.getMaxAvgShift()>dimensionAvgLhShift){
                    logDetail.append("平均班次大于等于设置的最小班次=").append(cxEngineProductDimensionLimit.getMinAvgShift()).append(division);
                    logDetail.append("平均班次小于设置的最大班次=").append(cxEngineProductDimensionLimit.getMaxAvgShift()).append(division);
                    adjustLhShiftCount=cxEngineProductDimensionLimit.getShiftParams();
                    logDetail.append("调整的班数为=").append(adjustLhShiftCount).append(division);
                    break;
                }
            }
        }
        return adjustLhShiftCount;
    }

    /**
     * 同机台一班可硫化班次
     * @param avgAvailableLhShift
     * @param productMachineLimits
     * @return
     */
    @Override
    public Double adjustLhShiftCountByMachine(Double avgAvailableLhShift, List<CxEngineProductMachineLimit> productMachineLimits,StringBuilder logDetail) {
        logDetail.append("【开始进行第三轮机台平均可硫化班次班次调整】》").append(",白班平均可硫化班次=").append(avgAvailableLhShift).append(division);
        Double adjustLhShiftCount =BigDecimal.ZERO.doubleValue();
        if(StringUtils.isNotEmpty(productMachineLimits)){
            for(CxEngineProductMachineLimit cxEngineProductMachineLimit:productMachineLimits){
                if(cxEngineProductMachineLimit.getMinAvgShift()<=avgAvailableLhShift&&cxEngineProductMachineLimit.getMaxAvgShift()>avgAvailableLhShift){
                    logDetail.append("平均班次大于等于设置的最小班次=").append(cxEngineProductMachineLimit.getMinAvgShift()).append(division);
                    logDetail.append("平均班次小于设置的最大班次=").append(cxEngineProductMachineLimit.getMaxAvgShift()).append(division);
                    logDetail.append("调整的班数为=").append(cxEngineProductMachineLimit.getShiftParams()).append(division);
                    adjustLhShiftCount=cxEngineProductMachineLimit.getShiftParams();
                    break;
                }
            }
        }
        return adjustLhShiftCount;
    }
}
