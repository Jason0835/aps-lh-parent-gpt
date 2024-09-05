package com.zlt.aps.lh.engine.common;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.domain.LhEngineTireConstructionInfo;
import com.zlt.aps.common.engine.domain.MdmMonthPlanMain;
import com.zlt.aps.common.engine.mapper.MdmMonthPlanMainMapper;
import com.zlt.aps.common.engine.service.LhEngineTireConstructionInfoService;
import com.zlt.aps.common.engine.service.MdmMonthPlanMainService;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.cx.api.domain.dto.CxParamsDto;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.lh.engine.constants.LhEngineParamCodeConstants;
import com.zlt.aps.lh.engine.mapper.CommonCxEngineMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 硫化工序相关公共部分处理
 */
@Component("lhCommonService")
@Slf4j
public class LhCommonService {
    @Autowired
    private CommonCxEngineMapper commonCxEngineMapper;

    @Autowired
    private IncrementService incrementService;

    @Autowired
    private LhEngineTireConstructionInfoService lhEngineTireConstructionInfoService;

    @Autowired
    private MdmMonthPlanMainMapper mdmMonthPlanMainMapper;

    /**
     * 成型排程是否投产：否
     */
    private static final String TO_PRODUCT_NO="1";
    /**
     * 更新库存信息
     * @param cxScheduleResultList
     * @param scheduleDate
     */
    public void updateLastDayTaskStock(List<CxScheduleResult> cxScheduleResultList, String scheduleDate) {
        Date date = DateUtils.parseDate(scheduleDate);
        String lastDateStr= DateUtils.parseDateToStr("yyyy-MM-dd",DateUtils.addDays(date,-1));
        CxStock stockCondition=new CxStock();
        stockCondition.setStockDateStr(lastDateStr);
        List<CxStock> stockList =this.commonCxEngineMapper.selectCxStockList(stockCondition);
        Map<String,Integer> stockMap=null;
        if(StringUtils.isNotEmpty(stockList)){
            stockMap=new HashMap<>();
            for (CxStock cxEnginStock:stockList) {
                String key = GenerageMapKeyUtils.createMapKey(cxEnginStock.getEmbryoCode(),cxEnginStock.getBomDataVersion());
                stockMap.put(key,cxEnginStock.getStockRealNum());
            }
        }else{ //没有库存信息时则默认都为0
            for(CxScheduleResult cxScheduleResult:cxScheduleResultList){
                cxScheduleResult.setTotalStock(0);//没有库存，默认为0
                cxScheduleResult.setCalcTotalStock(0);//没有库存，默认为0
            }
            log.debug("硫化自动排程没有库存信息，默认库存初始化为0");
            return;
        }
        if(StringUtils.isNotEmpty(stockMap)){
            //计算胎胚模数
            Map<String,Integer> embryoCodeMoldNum=new HashMap<>();
            //汇总同胎胚记录数
            Map<String,List<CxScheduleResult>> embryoCodeListMap=new HashMap<>();
            //遍历汇总同胎胚总模数、总记录数start
            calcEmbryoCodeMap(embryoCodeMoldNum,embryoCodeListMap,cxScheduleResultList);
            //遍历汇总同胎胚总模数、总记录数end

            if(StringUtils.isNotEmpty(embryoCodeListMap)){
                for(Map.Entry<String,List<CxScheduleResult>> entry:embryoCodeListMap.entrySet()){
                    String key=entry.getKey();//胎胚代码+施工版本
                    Integer totalMoldNum=embryoCodeMoldNum.get(key);//拿到总模数
                    List<CxScheduleResult> embryoCodeList=entry.getValue();//结果集
                    if (embryoCodeList.size()>1){
                        //遍历按比例分库存
                        int index=0;
                        int calcTotalStock=0;//参与计算的库存
                        for(CxScheduleResult embryoCodeResult:embryoCodeList){

                            //成型未投产的，不参与分配 by pancd+ 20230830
                            if (TO_PRODUCT_NO.equals(embryoCodeResult.getToProduct())){
                                log.debug("【模数比例库存设置】胎胚代码："+embryoCodeResult.getEmbryoCode()+",比例计算库存："+0+",原因：未投产");
                                embryoCodeResult.setCalcTotalStock(0);
                                if(stockMap.containsKey(key)){
                                    embryoCodeResult.setTotalStock(stockMap.get(key));
                                }
                                continue;
                            }else {
                                embryoCodeResult.setTotalStock(stockMap.get(key));
                            }

                            if(stockMap.containsKey(key)){
                                Integer stockCount=stockMap.get(key);
                                Integer calcStock=0;
                                embryoCodeResult.setTotalStock(stockCount);
                                if(index==embryoCodeList.size()-1){
                                    calcStock=stockCount-calcTotalStock;
                                }else{
                                    Integer calcMoldNum=embryoCodeResult.getCalcMoldNum();//计算模数
                                    calcStock= BigDecimal.valueOf((double)calcMoldNum/totalMoldNum * stockCount).setScale(0, RoundingMode.UP).intValue();
                                    calcTotalStock+=calcStock;
                                }
                                log.debug("【模数比例库存设置】胎胚代码："+embryoCodeResult.getEmbryoCode()+",总库存："+stockCount+",比例计算库存："+calcStock);
                                embryoCodeResult.setCalcTotalStock(calcStock);
                            }else{
                                embryoCodeResult.setCalcTotalStock(0);//没有库存，默认为0
                                log.debug("【模数比例库存设置】胎胚代码："+embryoCodeResult.getEmbryoCode()+",总库存："+0+",比例计算库存："+0+",原因：未找到库存信息");
                            }
                            index++;
                        }

                    }else{
                        CxScheduleResult embryoCodeResult=embryoCodeList.get(0);
                        Integer totalStock=0;
                        embryoCodeResult.setTotalStock(0);//没有库存，默认为0
                        embryoCodeResult.setCalcTotalStock(0);//没有库存，默认为0
                        if(stockMap.containsKey(key)){
                            totalStock=stockMap.get(key);
                            embryoCodeResult.setTotalStock(totalStock);
                            embryoCodeResult.setCalcTotalStock(totalStock);
                        }
                        log.debug("【模数比例库存设置】胎胚代码："+embryoCodeResult.getEmbryoCode()+",总库存："+totalStock+",比例计算库存："+totalStock);
                    }
                }

            }
        }
    }

    /**
     * 组装同胎胚模数和记录数信息
     * @param embryoCodeMoldNum
     * @param embryoCodeListMap
     * @param lastDayTaskList
     */
    private void calcEmbryoCodeMap(Map<String, Integer> embryoCodeMoldNum, Map<String, List<CxScheduleResult>> embryoCodeListMap, List<CxScheduleResult> lastDayTaskList) {
        List<CxScheduleResult> cxScheduleResultList=null;
        for(CxScheduleResult cxScheduleResult:lastDayTaskList){
            cxScheduleResultList=new ArrayList<>();
            Integer calcTotalMoldNum=0;
            String embryoCode=cxScheduleResult.getEmbryoCode();
            String bomDataVersion=cxScheduleResult.getBomDataVersion();
            String key=GenerageMapKeyUtils.createMapKey(embryoCode,bomDataVersion);
            if(embryoCodeMoldNum.containsKey(key)){
                calcTotalMoldNum=embryoCodeMoldNum.get(key);
            }
            if(embryoCodeListMap.containsKey(key)){
                cxScheduleResultList=embryoCodeListMap.get(key);
            }
            Integer moldNum=1;
            Double lhMachineQty=cxScheduleResult.getLhMachineQty();
            //如果没有选择硫化机则按1个模进行计算
            if(lhMachineQty!=null&&lhMachineQty>0){
                moldNum=new BigDecimal(lhMachineQty).intValue();
            }
            cxScheduleResult.setCalcMoldNum(moldNum);//保存计算的模数
            calcTotalMoldNum+=moldNum;
            cxScheduleResultList.add(cxScheduleResult);
            embryoCodeMoldNum.put(key,calcTotalMoldNum);
            embryoCodeListMap.put(key,cxScheduleResultList);
        }
    }

    /**
     * 创建批次号
     * @param scheduleDate
     * @return
     */
    public String createBatchNo(String prefix,String scheduleDate) {
        scheduleDate = scheduleDate.replace("-", "");
        return incrementService.getSequence4(prefix + scheduleDate);
    }

    /**
     * 创建工单号
     * @param scheduleDate 排程日期
     * @return
     */
    public String createOrderNo(String prefix,String scheduleDate) {
        scheduleDate = scheduleDate.replace("-", "");
        return incrementService.getSequence4(prefix +scheduleDate);
    }

    /**
     * 根据SPA品号获取单条硫化时长
     * //单条硫化时间后续调整为从BOM信息中获取
     * @param sapCode
     * @return
     */
    public Double getSingleLhTime(String sapCode) {
        if(StringUtils.isEmpty(sapCode)){
            log.error("【获取单条硫化时间】通过SAP品号获取单条硫化时长异常");
            return BigDecimal.ZERO.doubleValue();
        }
        Double lhTime=lhEngineTireConstructionInfoService.getLhTireTimeBySapCode(sapCode,null);
        // 先预设单条硫化时间3分钟
        log.debug("【获取单条硫化时间】SAP品号："+sapCode+"--->单条硫化时长="+lhTime);
        return lhTime;
    }

    /**
     * 根据生产的规格进行硫化机定额
     * @param singleLhTime 单胎硫化时长
     * @return 班产定额
     */
    public Integer calcLhShiftQuota(Double singleLhTime){
        //加载成型参数
        Map<String,String>  cxParamsMap=loadCxParams();
        Integer shiftTime=getShiftTime(cxParamsMap);
        Integer brushBagTime=getBrushBagTime(cxParamsMap);
        BigDecimal moldNumDecimal=BigDecimal.valueOf(2);
        BigDecimal singleShiftLhQtyDecimal=BigDecimal.valueOf(((double)shiftTime/(singleLhTime + brushBagTime))).setScale(0, BigDecimal.ROUND_DOWN).multiply(moldNumDecimal);
        log.debug("【计算单班硫化量】硫化机*2模计算，单条硫化时长："+singleLhTime+"，硫化机模数："+2+"，计算可硫化单班硫化量="+singleShiftLhQtyDecimal.intValue());
        return singleShiftLhQtyDecimal.intValue();
    }

    /**
     * 传入成型参数结合使用模数计算机台定额
     * @param cxParams
     * @param singleLhTime
     * @param useMoldNumber
     * @return
     */
    public Integer calcLhShiftQuotaByMoldNumber(Map<String,String> cxParams,Double singleLhTime,Integer useMoldNumber){
        if(singleLhTime==null){
            log.error("单胎硫化时长为空,默认返回定额1条");
            return BigDecimal.ONE.intValue();
        }
        if(useMoldNumber==null){
            //如果使用模数为空的话默认为2模
            useMoldNumber=2;
        }
        if(StringUtils.isEmpty(cxParams)){
            cxParams=loadCxParams();
        }
        Integer shiftTime=getShiftTime(cxParams);
        Integer brushBagTime=getBrushBagTime(cxParams);
        BigDecimal moldNumDecimal=BigDecimal.valueOf(useMoldNumber);
        BigDecimal singleShiftLhQtyDecimal=BigDecimal.valueOf(((double)shiftTime/(singleLhTime + brushBagTime))).setScale(0, BigDecimal.ROUND_DOWN).multiply(moldNumDecimal);
        log.debug("【计算单班硫化量】硫化机*2模计算，单条硫化时长："+singleLhTime+"，硫化机模数："+2+"，计算可硫化单班硫化量="+singleShiftLhQtyDecimal.intValue());
        return singleShiftLhQtyDecimal.intValue();
    }

    /**
     * 获取班次总时长，班次总时长-固定损耗时长
     * @return
     */
    public Integer getShiftTime(Map<String,String> cxParams) {
        if(StringUtils.isNotEmpty(cxParams)&&cxParams.containsKey(LhEngineParamCodeConstants.CLASS_SHIFT_MAX_TIME)&&cxParams.containsKey(LhEngineParamCodeConstants.CLASS_LOSSRATE_TIME)){
            String classShiftTimeParams=cxParams.get(LhEngineParamCodeConstants.CLASS_SHIFT_MAX_TIME);
            Integer classShiftTime=null;
            if(StringUtils.isNotEmpty(classShiftTimeParams)){
                classShiftTime=Integer.valueOf(classShiftTimeParams);
            }
            Integer classShiftLossTime=null;
            String classShiftLossTimeParams=cxParams.get(LhEngineParamCodeConstants.CLASS_LOSSRATE_TIME);
            if(StringUtils.isNotEmpty(classShiftLossTimeParams)){
                classShiftLossTime=Integer.valueOf(classShiftLossTimeParams);
            }
            if(classShiftTime!=null&&classShiftLossTime!=null){
                return classShiftTime - classShiftLossTime;
            }
        }
        return 480-10;
    }

    /**
     * 获取刷囊时间
     * @return
     */
    public Integer getBrushBagTime(Map<String,String>  cxParamsMap){
        if(StringUtils.isNotEmpty(cxParamsMap)&&cxParamsMap.containsKey(LhEngineParamCodeConstants.BRUSH_BAG_TIME)){
            String brushBagTimeParams=cxParamsMap.get(LhEngineParamCodeConstants.BRUSH_BAG_TIME);
            if(StringUtils.isNotEmpty(brushBagTimeParams)){
                return Integer.valueOf(brushBagTimeParams);
            }
        }
        return 2;
    }

    public Map<String,String> loadCxParams(){
        Map<String,String> cxParamsMap=new HashMap<>();
        List<CxParamsDto> cxParamsDtoList=this.commonCxEngineMapper.selectCxParamsList(new CxParamsDto());
        if(StringUtils.isNotEmpty(cxParamsDtoList)){
            cxParamsMap=cxParamsDtoList.stream().collect(Collectors.toMap(CxParamsDto::getParamCode,CxParamsDto::getParamValue));
        }
        return cxParamsMap;
    }

    /**
     * 根据SPA品号获取单条硫化时长
     * //单条硫化时间后续调整为从BOM信息中获取
     * @param sapCode
     * @return
     */
    public Double getSingleLhTime(String sapCode,Map<String, List<LhEngineTireConstructionInfo>> sapTireConstructionListMap) {
        if(StringUtils.isEmpty(sapCode)){
            log.error("【获取单条硫化时间】通过SAP品号获取单条硫化时长异常");
            return BigDecimal.ZERO.doubleValue();
        }

        if(StringUtils.isEmpty(sapTireConstructionListMap)){
            log.error("【硫化外胎施工异常】：当前外胎施工信息为空");
            return BigDecimal.ZERO.doubleValue();
        }
        Double lhTime=lhEngineTireConstructionInfoService.getSingleTireTimeBySap(sapCode,null,sapTireConstructionListMap);
        return lhTime;
    }

    /**
     * 根据外胎SAP品号和胎胚代码获取硫化时长
     * @param sapCode
     * @param embryoCode
     * @param sapTireConstructionListMap
     * @return
     */
    public Double getSapEmbryoCodeSingleLhTime(String sapCode,String embryoCode,Map<String, List<LhEngineTireConstructionInfo>> sapTireConstructionListMap) {
        if(StringUtils.isEmpty(sapCode)){
            log.error("【获取单条硫化时间】通过SAP品号获取单条硫化时长异常");
            return BigDecimal.ZERO.doubleValue();
        }

        if(StringUtils.isEmpty(sapTireConstructionListMap)){
            log.error("【硫化外胎施工异常】：当前外胎施工信息为空");
            return BigDecimal.ZERO.doubleValue();
        }
        Double lhTime=lhEngineTireConstructionInfoService.getSingleTireTimeBySap(sapCode,embryoCode,sapTireConstructionListMap);
        return lhTime;
    }

    /**
     * 根据日期获取月度计划版本信息
     * @param scheduleDate
     * @return
     */
    public MdmMonthPlanMain getValidPlanMainVersion(Date scheduleDate) {
        String year=DateUtils.parseDateToStr(DateUtils.YYYY,scheduleDate);
        String month=DateUtils.parseDateToStr("MM",scheduleDate);
        MdmMonthPlanMain condition=new MdmMonthPlanMain();
        condition.setIsFinalized("0");//定稿数据
        condition.setYear(year);
        condition.setMonth(month);
        return mdmMonthPlanMainMapper.getValidPlanMainVersion(condition);
    }

}
