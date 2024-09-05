package com.zlt.aps.lh.engine.util;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.lh.engine.domain.LhEngineScheduleResult;
import com.zlt.aps.lh.engine.enums.AnalysisCodeEnum;
import com.zlt.aps.lh.engine.enums.LhClassShiftEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * 硫化自动排程工具类
 */
public class LhEngineScheduleUtils {

   public static final String DAY="DAY";

   public static final String HOUR="HOUR";

   public static final String MINUTES="MINUTES";

    /**
     * 根据班次枚举进行开始时间设置
     * @param lhEngineScheduleResult
     * @param cls
     * @param dateTime
     */
    public static void setClassShiftStartTime(LhEngineScheduleResult lhEngineScheduleResult,LhClassShiftEnum cls,Date dateTime){
        if(cls==null){
            return;
        }
        switch (cls){
            case ONE_CLASS_SHIFT:
                lhEngineScheduleResult.setClass1StartTime(dateTime);
                break;
            case TWO_CLASS_SHIFT:
                lhEngineScheduleResult.setClass2StartTime(dateTime);
                break;
            case THREE_CLASS_SHIFT:
                lhEngineScheduleResult.setClass3StartTime(dateTime);
                break;
            default:break;
        }
    }

    /**
     * 根据班次枚举进行结束时间设置
     * @param lhEngineScheduleResult
     * @param cls
     * @param dateTime
     */
    public static void setClassShiftEndTime(LhEngineScheduleResult lhEngineScheduleResult,LhClassShiftEnum cls,Date dateTime){
        if(cls==null){
            return;
        }
        switch (cls){
            case ONE_CLASS_SHIFT:
                lhEngineScheduleResult.setClass1EndTime(dateTime);
                break;
            case TWO_CLASS_SHIFT:
                lhEngineScheduleResult.setClass2EndTime(dateTime);
                break;
            case THREE_CLASS_SHIFT:
                lhEngineScheduleResult.setClass3EndTime(dateTime);
                break;
            default:break;
        }
    }


    /**
     * 根据班制进行硫化班次计划量设置
     * @param lhEngineScheduleResult
     * @param cls
     * @param planQty
     */
    public static void setClassShiftPlanQty(LhEngineScheduleResult lhEngineScheduleResult,LhClassShiftEnum cls,Integer planQty){
        if(cls==null){
            return;
        }
        switch (cls){
            case ONE_CLASS_SHIFT:
                lhEngineScheduleResult.setClass1PlanQty(planQty);
                break;
            case TWO_CLASS_SHIFT:
                lhEngineScheduleResult.setClass2PlanQty(planQty);
                break;
            case THREE_CLASS_SHIFT:
                lhEngineScheduleResult.setClass3PlanQty(planQty);
                break;
            default:break;
        }
    }

    /**
     * 根据班次获取前一班硫化班次计划
     * @param lhEngineScheduleResult
     * @param cls
     */
    public static void getBeforeClassShiftPlanQty(LhEngineScheduleResult lhEngineScheduleResult,LhClassShiftEnum cls){
        if(cls==null){
            return;
        }
        switch (cls){
            case ONE_CLASS_SHIFT:
                //如果是白班的计划则不处理直接返回，初始化会扣除昨日三班的计划
                 break;
            case TWO_CLASS_SHIFT:
                lhEngineScheduleResult.getClass1PlanQty();
                break;
            case THREE_CLASS_SHIFT:
                lhEngineScheduleResult.getClass2PlanQty();
                break;
            default:break;
        }
    }

    /**
     * 获取硫化前班次计划量
     * @param lhEngineScheduleResult
     * @param cls
     * @return
     */
    public static Integer getLhClassPlanQty(LhEngineScheduleResult lhEngineScheduleResult, LhClassShiftEnum cls){
        Integer classPlanQty=0;
        switch (cls){
            case ONE_CLASS_SHIFT:
                classPlanQty=lhEngineScheduleResult.getClass1PlanQty();
                break;
            case TWO_CLASS_SHIFT:
                classPlanQty=lhEngineScheduleResult.getClass2PlanQty();
                break;
            case THREE_CLASS_SHIFT:
                classPlanQty=lhEngineScheduleResult.getClass3PlanQty();
                break;
            default:break;
        }
        return classPlanQty;
    }

    /**
     * 获取成型前班次计划量
     * @param cxScheduleResult
     * @param cls
     * @return
     */
    public static Integer getCxBeforeClassPlanQty(CxScheduleResult cxScheduleResult,LhClassShiftEnum cls){
        Integer lastClassPlanQty=0;
        switch (cls){
            case ONE_CLASS_SHIFT:
                lastClassPlanQty=cxScheduleResult.getClass3PlannedQty();
                break;
            case TWO_CLASS_SHIFT:
                lastClassPlanQty=cxScheduleResult.getClass1PlanQty();
                break;
            case THREE_CLASS_SHIFT:
                lastClassPlanQty=cxScheduleResult.getClass2PlanQty();
                break;
            default:break;
        }
        return lastClassPlanQty;
    }

    /**
     * 根据硫化班次读取成型对应班次的计划量
     * @param cxScheduleResult
     * @param cls
     * @return
     */
    public static Integer getCxCurrentClassPlanQty(CxScheduleResult cxScheduleResult, LhClassShiftEnum cls){
        Integer planQty=0;
        switch (cls){
            case ONE_CLASS_SHIFT:
                planQty=cxScheduleResult.getClass1PlanQty();
                break;
            case TWO_CLASS_SHIFT:
                planQty=cxScheduleResult.getClass2PlanQty();
                break;
            case THREE_CLASS_SHIFT:
                planQty=cxScheduleResult.getClass3PlanQty();
                break;
            default:break;
        }
        return planQty;
    }

    /**
     * 判断两个时间段是否有交集
     * @param leftStartDate 第一个时间段的开始时间
     * @param leftEndDate  第一个时间段段的结束时间
     * @param rightStartDate 第二个时间段的开始时间
     * @param rightEndDate  第二个时间段段的结束时间
     * @return 若有交集, 返回true, 否则返回false
     */
    public static boolean hasOverlap(Date leftStartDate, Date leftEndDate, Date rightStartDate, Date rightEndDate) {
        return !(leftEndDate.getTime()<rightStartDate.getTime()||leftStartDate.getTime()>rightEndDate.getTime());
    }

    /**
     * 格式化日期
     * @param date yyyy-MM-dd
     * @return yyyy-MM-dd 00:00:00
     */
    public static Date formatDateByZero(Date date){
        date= DateUtils.setHours(date,0);
        date=DateUtils.setMinutes(date,0);
        date=DateUtils.setSeconds(date,0);
        DateUtils.setMilliseconds(date,0);
        return date;
    }

    /**
     * 两个日期的时间小时差
     * @param startTime
     * @param endTime
     * @return
     */
    public static long diffDate(Date startTime,Date endTime,String type){
        if(StringUtils.isEmpty(type)){
            type=HOUR;
        }
        Long dateDiff=null;
        Instant startInstant = startTime.toInstant();
        Instant endInstant = endTime.toInstant();
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDateTime start = startInstant.atZone(zoneId).toLocalDateTime();
        LocalDateTime end = endInstant.atZone(zoneId).toLocalDateTime();
        if(DAY.equals(type)){
            dateDiff= Duration.between(start, end).toDays();
        }else if(HOUR.equals(type)){
            dateDiff = Duration.between(start, end).toHours();
        }else if(MINUTES.equals(type)){
            dateDiff = Duration.between(start, end).toMinutes();
        }
        return dateDiff;
    }


    /**
     * 根据班制进行硫化班次计划量设置
     * @param lhEngineScheduleResult
     * @param cls
     * @param openStream
     */
    public static void setClassShiftOpenStreamFlag(LhEngineScheduleResult lhEngineScheduleResult,LhClassShiftEnum cls,boolean openStream){
        if(cls==null){
            return;
        }
        switch (cls){
            case ONE_CLASS_SHIFT:
                lhEngineScheduleResult.setClass1OpenStream(openStream);
                break;
            case TWO_CLASS_SHIFT:
                lhEngineScheduleResult.setClass2OpenStream(openStream);
                break;
            case THREE_CLASS_SHIFT:
                lhEngineScheduleResult.setClass3OpenStream(openStream);
                break;
            default:break;
        }
    }

    /**
     * 根据班制进行硫化班次计划量上限设置
     * @param lhEngineScheduleResult
     * @param cls
     * @param planQty
     */
    public static void setClassShiftMaxPlanQty(LhEngineScheduleResult lhEngineScheduleResult,LhClassShiftEnum cls,Integer planQty){
        if(cls==null){
            return;
        }
        switch (cls){
            case ONE_CLASS_SHIFT:
                lhEngineScheduleResult.setClass1MaxPlanQty(planQty);
                break;
            case TWO_CLASS_SHIFT:
                lhEngineScheduleResult.setClass2MaxPlanQty(planQty);
                break;
            case THREE_CLASS_SHIFT:
                lhEngineScheduleResult.setClass3MaxPlanQty(planQty);
                break;
            default:break;
        }
    }

    /**
     * 开班设置
     * @param lhEngineScheduleResult
     * @param cls
     * @param openShift
     */
    public static void setClassShiftOpenShiftFlag(LhEngineScheduleResult lhEngineScheduleResult,LhClassShiftEnum cls,boolean openShift){
        if(cls==null){
            return;
        }
        switch (cls){
            case ONE_CLASS_SHIFT:
                lhEngineScheduleResult.setClass1OpenShift(openShift);
                break;
            case TWO_CLASS_SHIFT:
                lhEngineScheduleResult.setClass2OpenShift(openShift);
                break;
            case THREE_CLASS_SHIFT:
                lhEngineScheduleResult.setClass3OpenShift(openShift);
                break;
            default:break;
        }
    }

    /**
     *  硫化排程原因编码设置
     * @param lhEngineScheduleResult
     * @param cls
     * @param analysisCodeEnum 原因分析枚举
     */
    public static void setAnalysisCode(LhEngineScheduleResult lhEngineScheduleResult, LhClassShiftEnum cls, AnalysisCodeEnum analysisCodeEnum) {
        String analysisCode="";
        switch (cls){
            case ONE_CLASS_SHIFT:
                analysisCode=StringUtils.isEmpty(lhEngineScheduleResult.getClass1AnalysisCode())?"":lhEngineScheduleResult.getClass1AnalysisCode();
                analysisCode+=StringUtils.isNotEmpty(analysisCode)?":"+analysisCodeEnum.getAnalysisCode(): analysisCodeEnum.getAnalysisCode();
                lhEngineScheduleResult.setClass1AnalysisCode(analysisCode);
                break;
            case TWO_CLASS_SHIFT:
                analysisCode=StringUtils.isEmpty(lhEngineScheduleResult.getClass2AnalysisCode())?"":lhEngineScheduleResult.getClass2AnalysisCode();
                analysisCode += StringUtils.isNotEmpty(analysisCode)?":"+analysisCodeEnum.getAnalysisCode(): analysisCodeEnum.getAnalysisCode();
                lhEngineScheduleResult.setClass2AnalysisCode(analysisCode);
                break;
            case THREE_CLASS_SHIFT:
                analysisCode=StringUtils.isEmpty(lhEngineScheduleResult.getClass3AnalysisCode())?"":lhEngineScheduleResult.getClass3AnalysisCode();
                analysisCode += StringUtils.isNotEmpty(analysisCode)?":"+analysisCodeEnum.getAnalysisCode(): analysisCodeEnum.getAnalysisCode();
                lhEngineScheduleResult.setClass3AnalysisCode(analysisCode);
                break;
            default:break;
        }
    }

    /**
     * 停汽设置
     * @param lhEngineScheduleResult
     * @param cls
     * @param stopStream
     */
    public static void setClassShiftOcclusionFlag(LhEngineScheduleResult lhEngineScheduleResult,LhClassShiftEnum cls,boolean stopStream){
        if(cls==null){
            return;
        }
        switch (cls){
            case ONE_CLASS_SHIFT:
                lhEngineScheduleResult.setClass1Occlusion(stopStream);
                break;
            case TWO_CLASS_SHIFT:
                lhEngineScheduleResult.setClass2Occlusion(stopStream);
                break;
            case THREE_CLASS_SHIFT:
                lhEngineScheduleResult.setClass3Occlusion(stopStream);
                break;
            default:break;
        }
    }

    /**
     *  硫化排程原因编码设置
     * @param lhEngineScheduleResult
     * @param cls
     * @param analysis 原因分析
     */
    public static void setClassShiftAnalysis(LhEngineScheduleResult lhEngineScheduleResult, LhClassShiftEnum cls, String analysis) {
        if(cls==null){
            return;
        }
        switch (cls){
            case ONE_CLASS_SHIFT:
                lhEngineScheduleResult.setClass1Analysis(analysis);
                break;
            case TWO_CLASS_SHIFT:
                lhEngineScheduleResult.setClass2Analysis(analysis);
                break;
            case THREE_CLASS_SHIFT:
                lhEngineScheduleResult.setClass3Analysis(analysis);
                break;
            default:break;
        }
    }

    /**
     * 获取班次开班标记
     * @param lhEngineScheduleResult
     * @param cls
     */
    public static Boolean getClassShiftOpenShiftFlag(LhEngineScheduleResult lhEngineScheduleResult,LhClassShiftEnum cls){
        Boolean isOpenShift=false;
        if(cls==null){
            return false;
        }
        switch (cls){
            case ONE_CLASS_SHIFT:
                isOpenShift=lhEngineScheduleResult.getClass1OpenShift();
                break;
            case TWO_CLASS_SHIFT:
                isOpenShift=lhEngineScheduleResult.getClass2OpenShift();
                break;
            case THREE_CLASS_SHIFT:
                isOpenShift=lhEngineScheduleResult.getClass3OpenShift();
                break;
            default:break;
        }
        return isOpenShift;
    }


    /**
     * 获取班次换模标记
     * @param lhEngineScheduleResult
     * @param cls
     */
    public static Boolean getClassShiftChangeMoldFlag(LhEngineScheduleResult lhEngineScheduleResult,LhClassShiftEnum cls){
        Boolean isChangeMold=false;
        if(cls==null){
            return false;
        }
        switch (cls){
            case ONE_CLASS_SHIFT:
                isChangeMold=lhEngineScheduleResult.getClass1ChangeMold();
                break;
            case TWO_CLASS_SHIFT:
                isChangeMold=lhEngineScheduleResult.getClass2ChangeMold();
                break;
            case THREE_CLASS_SHIFT:
                isChangeMold=lhEngineScheduleResult.getClass3ChangeMold();
                break;
            default:break;
        }
        return isChangeMold;
    }

    /**
     * 获取班次开班标记
     * @param lhEngineScheduleResult
     * @param cls
     */
    public static Boolean getClassShiftOpenStreamFlag(LhEngineScheduleResult lhEngineScheduleResult,LhClassShiftEnum cls){
        Boolean isOpenStream=false;
        if(cls==null){
            return false;
        }
        switch (cls){
            case ONE_CLASS_SHIFT:
                isOpenStream=lhEngineScheduleResult.getClass1OpenStream();
                break;
            case TWO_CLASS_SHIFT:
                isOpenStream=lhEngineScheduleResult.getClass2OpenStream();
                break;
            case THREE_CLASS_SHIFT:
                isOpenStream=lhEngineScheduleResult.getClass3OpenStream();
                break;
            default:break;
        }
        return isOpenStream;
    }

    /**
     * 获取班次计划量上限设置
     * @param lhEngineScheduleResult
     * @param cls
     */
    public static Integer getClassShiftMaxPlanQty(LhEngineScheduleResult lhEngineScheduleResult,LhClassShiftEnum cls){
        Integer classMaxPlanQty=0;
        if(cls==null){
            return 0;
        }
        switch (cls){
            case ONE_CLASS_SHIFT:
                classMaxPlanQty=lhEngineScheduleResult.getClass1MaxPlanQty();
                break;
            case TWO_CLASS_SHIFT:
                classMaxPlanQty=lhEngineScheduleResult.getClass2MaxPlanQty();
                break;
            case THREE_CLASS_SHIFT:
                classMaxPlanQty=lhEngineScheduleResult.getClass3MaxPlanQty();
                break;
            default:break;
        }
        return classMaxPlanQty;
    }

    /**
     *  计算总消耗时间(秒数)
     * @param lhEngineScheduleResult
     * @return
     */
    public static Integer useTotalSecond(LhEngineScheduleResult lhEngineScheduleResult,Integer totalPlanQty, Integer brushBagTime){
        Double singleTireTime=lhEngineScheduleResult.getLhTime();
        Integer useMoldNumber=lhEngineScheduleResult.getUseMoldNumber();
        Double totalTime=0D;
        if(totalPlanQty>0){
            //按单班硫化计算单胎时间+2分钟刷囊时间
            singleTireTime+=brushBagTime;
            if(totalPlanQty%useMoldNumber==0){
                totalTime=singleTireTime * (totalPlanQty / useMoldNumber);
            }else{
                Integer remainStock=totalPlanQty%useMoldNumber;
                totalPlanQty-=remainStock;
                //计算整除部分的时间+剩余部分的时间
                totalTime=singleTireTime * (totalPlanQty / useMoldNumber);
                //剩余时间
                Double remainQtyTime=remainStock * singleTireTime;
                totalTime+=remainQtyTime;
            }
        }else{
            return 0;
        }
        //换算成秒
        Integer totalSecond= BigDecimal.valueOf(totalTime * 60D ).setScale(1, RoundingMode.UP).intValue();
        return totalSecond;
    }

    /**
     * 分组允许空值
     * @param classifier
     * @param <T>
     * @param <A>
     * @return
     */
    public static <T, A> Collector<T, ?, Map<A, List<T>>> groupByWithNullKeys(Function<? super T, ? extends A> classifier) {
        return Collectors.toMap(
                classifier,
                Collections::singletonList,
                (List<T> oldList, List<T> newEl) -> {
                    List<T> newList = new ArrayList<>(oldList.size() + 1);
                    newList.addAll(oldList);
                    newList.addAll(newEl);
                    return newList;
                }
        );
    }

    /**
     * 换模标记设置
     * @param lhEngineScheduleResult
     * @param cls
     * @param isChangeMold
     */
    public static void setClassShiftChangeMoldFlag(LhEngineScheduleResult lhEngineScheduleResult,LhClassShiftEnum cls,boolean isChangeMold){
        if(cls==null){
            return;
        }
        switch (cls){
            case ONE_CLASS_SHIFT:
                lhEngineScheduleResult.setClass1ChangeMold(isChangeMold);
                break;
            case TWO_CLASS_SHIFT:
                lhEngineScheduleResult.setClass2ChangeMold(isChangeMold);
                break;
            case THREE_CLASS_SHIFT:
                lhEngineScheduleResult.setClass3ChangeMold(isChangeMold);
                break;
            default:break;
        }
    }

    /**
     * 成型待料标记标记设置
     * @param lhEngineScheduleResult
     * @param cls
     * @param classWaitMaterialFlag
     */
    public static void setClassWaitMaterialFlag(LhEngineScheduleResult lhEngineScheduleResult,LhClassShiftEnum cls,boolean classWaitMaterialFlag){
        if(cls==null){
            return;
        }
        switch (cls){
            case ONE_CLASS_SHIFT:
                lhEngineScheduleResult.setClass1WaitMaterialFlag(classWaitMaterialFlag);
                break;
            case TWO_CLASS_SHIFT:
                lhEngineScheduleResult.setClass2WaitMaterialFlag(classWaitMaterialFlag);
                break;
            case THREE_CLASS_SHIFT:
                lhEngineScheduleResult.setClass3WaitMaterialFlag(classWaitMaterialFlag);
                break;
            default:break;
        }
    }

    /**
     * 获取班次待料标记
     * @param lhEngineScheduleResult
     * @param cls
     */
    public static Boolean getClassWaitMaterialFlag(LhEngineScheduleResult lhEngineScheduleResult,LhClassShiftEnum cls){
        Boolean waitMaterialFlag=false;
        if(cls==null){
            return false;
        }
        switch (cls){
            case ONE_CLASS_SHIFT:
                waitMaterialFlag=lhEngineScheduleResult.getClass1WaitMaterialFlag();
                break;
            case TWO_CLASS_SHIFT:
                waitMaterialFlag=lhEngineScheduleResult.getClass2WaitMaterialFlag();
                break;
            case THREE_CLASS_SHIFT:
                waitMaterialFlag=lhEngineScheduleResult.getClass3WaitMaterialFlag();
                break;
            default:break;
        }
        return waitMaterialFlag;
    }

    /**
     * 最大可安排计划量结合模数进行调整
     * @param maxPlanQty
     * @param useMoldNumber
     * @return
     */
    public static Integer getMaxPlanQtyByMoldNumber(Integer maxPlanQty,Integer useMoldNumber){
        if(maxPlanQty==null||useMoldNumber==null){
            return maxPlanQty;
        }
        if(maxPlanQty.equals(BigDecimal.ZERO.intValue())){
            return maxPlanQty;
        }
        Integer remainPlan=maxPlanQty%useMoldNumber;
        Integer updateMaxPlanQty=maxPlanQty - remainPlan;
        return updateMaxPlanQty;
    }

}
