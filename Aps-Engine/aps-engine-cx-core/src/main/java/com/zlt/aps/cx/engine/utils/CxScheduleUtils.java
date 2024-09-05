package com.zlt.aps.cx.engine.utils;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;
import com.zlt.aps.common.engine.utils.BeanConverUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.engine.constants.CxEngineConstants;
import com.zlt.aps.cx.engine.domain.CxAutoScheduleTask;
import com.zlt.aps.cx.engine.domain.CxEngineLastDaySupplePlan;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleResult;
import com.zlt.aps.cx.engine.domain.CxPlanProductStatus;
import com.zlt.aps.cx.engine.enums.ClassEnums;
import com.zlt.aps.cx.engine.exception.CxScheduleEngineException;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
  * 成型工序排程工具类
  * @ClassName CxScheduleUtils
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/23 16:29
  * @Version 1.0
**/
@Slf4j
public class CxScheduleUtils {

    /**
     * 后缀流水号长度默认4位
     */
    public static final int DEFAULT_LENGTH=4;

    /**
     * 复制前一天排程对象
     * @param source 原始对象
     */
    public static CxEngineScheduleResult copyCxScheduleTask(CxEngineScheduleResult source, String cxBatchNo, Date scheduleDate,boolean coverClass3PlannedQty){
        CxEngineScheduleResult target = BeanConverUtil.conver(source,CxEngineScheduleResult.class);
        target.setCxBatchNo(cxBatchNo);//成型批次号
        target.setLastOrderNo(source.getOrderNo());//2021-07-01 冗余昨天的工单号用于后面查找剩余任务量
        //target.setProductionStatus(CxEngineConstants.PRODUCTION_STATUS_UNDO);//设置未生产
        target.setScheduleDate(scheduleDate);//设置排程日期
        target.setTotalStock(source.getTotalStock()==null?0:source.getTotalStock());//总库存
        target.setCalcTotalStock(source.getCalcTotalStock()==null?0:source.getCalcTotalStock());//总库存按比例分配
        target.setClass1PlanQty(source.getClass4PlanQty()==null?0:source.getClass4PlanQty());//复制前一天次日一班计划量到今天
        target.setClass1AnalysisInput(target.getClass4AnalysisInput());//冗余前一天的手工原因分析
        target.setClass2PlanQty(0);
        target.setClass3PlanQty(0);
        target.setClass4PlanQty(0);
        target.setClass5PlanQty(0);
        target.setClass1Analysis("");//次日一班原因分析
        target.setClass2Analysis("");
        target.setClass3Analysis("");
        target.setClass4Analysis("");
        target.setClass5Analysis("");
        target.setClass2AnalysisInput("");
        target.setClass3AnalysisInput("");
        target.setClass4AnalysisInput("");
        target.setClass5AnalysisInput("");
        //Joran 2022-03-15如果进行覆盖的话从三班计划直接哪来覆盖start
        if(coverClass3PlannedQty){
            target.setClass3PlannedQty(source.getClass3PlanQty()==null?0:source.getClass3PlanQty());//将前一天的三班计划量存到排程日的三班（8-16点）计划中
        }
        //Joran 2022-03-15如果进行覆盖的话从三班计划直接哪来覆盖end

        target.setMonthRemainQty(source.getMonthRemainQty()==null?0:source.getMonthRemainQty());//月度剩余量
        target.setLastClass1PlanQty(source.getClass1PlanQty()==null?0:source.getClass1PlanQty());//冗余前一天的一班计划量
        target.setLastClass2PlanQty(source.getClass2PlanQty()==null?0:source.getClass2PlanQty());//冗余前一天的二班计划量
        target.setLastClass3PlanQty(source.getClass3PlanQty()==null?0:source.getClass3PlanQty());//冗余前一天三班的计划量
        target.setLastClass4PlanQty(source.getClass4PlanQty()==null?0:source.getClass4PlanQty());//冗余前一天次日一班的计划量
        target.setLastClass5PlanQty(source.getClass5PlanQty()==null?0:source.getClass5PlanQty());//冗余前一天次日二班的计划量
        target.setClass1Sort(source.getClass4Sort()==null?0:source.getClass4Sort());//冗余前一天的次日一班任务顺序
        target.setClass2Sort(source.getClass5Sort()==null?0:source.getClass5Sort());//冗余前一天的次日二班任务顺序
        target.setClass3Sort(0);
        target.setClass4Sort(0);
        target.setClass5Sort(0);
        target.setIsRelease(CxEngineConstants.IS_PUBLISH_NO);//未发布
        target.setDataSource(CxEngineConstants.CX_SCHEDULE_DATA_SOURCE_AUTO);//数据来源：自动排程
        target.setBomDataVersion(source.getBomDataVersion());//施工版本
        return target;
    }

    /**
     * 判断当前日期是否为月初第一天
     * @param scheduleDate
     * @return
     */
    public static boolean isFirstDayofMonth(Date scheduleDate){
        if (scheduleDate == null) {
            throw new IllegalArgumentException("scheduleDate cannot be null.");
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(scheduleDate);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        return dayOfMonth == 1;
    }

    /**
     * 将现有的任务列表根据成型机台进行拆分
     * @param taskList
     * @return
     */
    public static Map<String, List<CxEngineScheduleResult>> splitTaskByCxMachine(List<CxEngineScheduleResult> taskList){
        Map<String,List<CxEngineScheduleResult>> machineTaskMap=null;
        if(StringUtils.isNotEmpty(taskList)){
           machineTaskMap=taskList.stream().collect(Collectors.groupingBy(CxEngineScheduleResult::getCxMachineCode));
        }
        return machineTaskMap;
    }

    public static String getSequence(String prefix,long seq) {
        String str = String.valueOf(seq);
        int len = str.length();
        if (len > DEFAULT_LENGTH) {// 取决于业务规模,应该不会到达3
            return str;
        }
        int rest = DEFAULT_LENGTH - len;
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < rest; i++) {
            sb.append('0');
        }
        sb.append(str);
        return sb.toString();
    }

    /**
     * 根据入参组装key
     * @param keys
     * @return
     */
    public static String getMapKeyByInputString(String ... keys){
        StringBuilder sb=new StringBuilder();
        for (String key: keys) {
            sb.append(key).append(";");
        }
        return sb.toString();
    }

    /**
     * 计算可硫化班数
     * @param total 总量
     * @param single 单班硫化量
     * @param classShiftIndex 班次所属
     * @return
     */
    public static Double calcClassAvailableLhShift(Integer total,Integer single,int classShiftIndex){
        if(total==0||single==0){
            return BigDecimal.ZERO.doubleValue();
        }
        /**
         * 2025-05-23 需求变动通知单第一点
         *
         * 可硫化班次原方案：
         * 中班可硫化班次=（（8点库存＋前日白班计划量）/单班硫化量）
         * 夜班可硫化班次=（（8点库存＋白班计划量+中班计划量）/单班硫化量）-1
         * 白班可硫化班次=（（8点库存＋白班计划量+中班计划量+夜班计划量）/单班硫化量）-2
         * 次一可硫化班次=（（8点库存＋白班计划量+中班计划量+夜班计划量+白班计划量）/单班硫化量）-3
         * 次二可硫化班次=（（8点库存＋白班计划量+中班计划量+夜班计划量+白班计划量+次一计划量）/单班硫化量）-4
         *
         * 变更方案：
         * 中班可硫化班次=（（8点库存＋前日白班计划量）/单班硫化量）-1
         * 夜班可硫化班次=（（8点库存＋白班计划量+中班计划量）/单班硫化量）-2
         * 白班可硫化班次=（（8点库存＋白班计划量+中班计划量+夜班计划量）/单班硫化量）-3
         * 次一可硫化班次=（（8点库存＋白班计划量+中班计划量+夜班计划量+白班计划量）/单班硫化量）-4
         * 次二可硫化班次=（（8点库存＋白班计划量+中班计划量+夜班计划量+白班计划量+次一计划量）/单班硫化量）-5
         *
         */
        // BigDecimal oneDecimal=BigDecimal.ONE;  nick 注释 原来是减之前的班次不包括自己  现在扣除数量+1 改成 BigDecimal.ZERO
        BigDecimal oneDecimal=BigDecimal.ZERO;  //
        BigDecimal classIndex=BigDecimal.valueOf(classShiftIndex).subtract(oneDecimal);
        BigDecimal classAvailableLhShift= BigDecimal.valueOf((double)total/single).setScale(2, BigDecimal.ROUND_HALF_UP).subtract(classIndex);
        if(classAvailableLhShift.compareTo(BigDecimal.ZERO)<0){
            classAvailableLhShift=BigDecimal.ZERO;
        }
        return classAvailableLhShift.doubleValue();
    }

    /**
     * 计算可硫化班数
     * @param total 总量
     * @param single 单班硫化量
     * @param classShiftIndex 班次所属
     * @param startShiftIndex 开始扣减班次
     * @return
     */
    public static Double calcClassAvailableLhShiftByStartIndex(Integer total,Integer single,int classShiftIndex,int startShiftIndex){
        if(total==0||single==0){
            return BigDecimal.ZERO.doubleValue();
        }
        BigDecimal subShift=BigDecimal.valueOf(startShiftIndex);
        BigDecimal classIndex=BigDecimal.valueOf(classShiftIndex).subtract(subShift);
        BigDecimal classAvailableLhShift= BigDecimal.valueOf((double)total/single).setScale(2, BigDecimal.ROUND_HALF_UP).subtract(classIndex);
        if(classAvailableLhShift.compareTo(BigDecimal.ZERO)<0){
            classAvailableLhShift=BigDecimal.ZERO;
        }
        return classAvailableLhShift.doubleValue();
    }


    /**
     * 根据指定班次可硫化班次升序排序
     * 可硫化班次相同，按SAP品号进行升序
     * @param sortScheduleTaskList
     */
    public static void taskSortAscByAvailableLhShift(List<CxEngineScheduleResult> sortScheduleTaskList, int classIndex){
        if(StringUtils.isNotEmpty(sortScheduleTaskList)){

            //Joran 2022-02-25 添加前日三班可硫化班次排序start
             if(BigDecimal.ZERO.intValue()==classIndex){
                 log.debug("对前日三班的可硫化班次进行排序");
                 Comparator<CxEngineScheduleResult> class3PlannedAvailableLhShiftASC = Comparator.comparing(CxEngineScheduleResult::getClass3PlannedAvailableLhShift);
                 Collections.sort(sortScheduleTaskList,class3PlannedAvailableLhShiftASC);
                 return;
             }
            //Joran 2022-02-25 添加前日三班可硫化班次排序end


              ClassEnums enums=getClassEnumsByClassIndex(classIndex);
              if(enums==null){
                  log.error("传入班次下标错误，传入下标：{}",classIndex);
                  return;
              }
              //可硫化班次相同按单班硫化量进行降序排序
              Comparator<CxEngineScheduleResult> singleShiftLhQtyAsc = Comparator.comparing(CxEngineScheduleResult::getSingleShiftLhQty).reversed();
              switch (enums){
                  case CLASS_ONE:
                      //一班可硫化班次升序
                      Comparator<CxEngineScheduleResult> class1AvailableLhShiftASC = Comparator.comparing(CxEngineScheduleResult::getClass1AvailableLhShift);
                      //安排完第一规格后后续按照一班的倒序查找到下一个规格
                      Comparator<CxEngineScheduleResult> class1SortDesc = Comparator.comparing(CxEngineScheduleResult::getClass1Sort).reversed();
                      // 联合排序
                      Comparator<CxEngineScheduleResult> class1Comparator = class1AvailableLhShiftASC.thenComparing(class1SortDesc);
                      Collections.sort(sortScheduleTaskList,class1Comparator);
                      break;
                  case CLASS_TWO:
                      //二班可硫化班次升序
                      Comparator<CxEngineScheduleResult> class2AvailableLhShiftASC = Comparator.comparing(CxEngineScheduleResult::getClass2AvailableLhShift);
                      //Joran 2022-01-06 排二班的时候根据二班班次续作
                      Comparator<CxEngineScheduleResult> class2SortAsc = Comparator.comparing(CxEngineScheduleResult::getClass2Sort);
                      // 联合排序
                      Comparator<CxEngineScheduleResult> class2Comparator = class2AvailableLhShiftASC.thenComparing(class2SortAsc);
                      Collections.sort(sortScheduleTaskList,class2Comparator);
                      break;
                  case CLASS_THREE:
                      //三班可硫化班次升序
                      Comparator<CxEngineScheduleResult> class3AvailableLhShiftASC = Comparator.comparing(CxEngineScheduleResult::getClass3AvailableLhShift);
                      // 联合排序
                      Comparator<CxEngineScheduleResult> class3Comparator = class3AvailableLhShiftASC.thenComparing(singleShiftLhQtyAsc);
                      Collections.sort(sortScheduleTaskList,class3Comparator);
                      break;
                  case CLASS_FOUR:
                      //四班可硫化班次升序
                      Comparator<CxEngineScheduleResult> class4AvailableLhShiftASC = Comparator.comparing(CxEngineScheduleResult::getClass4AvailableLhShift);
                      // 联合排序
                      Comparator<CxEngineScheduleResult> class4Comparator = class4AvailableLhShiftASC.thenComparing(singleShiftLhQtyAsc);
                      Collections.sort(sortScheduleTaskList,class4Comparator);
                      break;
                  case CLASS_FIVE:
                      //四班可硫化班次升序
                      Comparator<CxEngineScheduleResult> class5AvailableLhShiftASC = Comparator.comparing(CxEngineScheduleResult::getClass5AvailableLhShift);
                      // 联合排序
                      Comparator<CxEngineScheduleResult> class5Comparator = class5AvailableLhShiftASC.thenComparing(singleShiftLhQtyAsc);
                      Collections.sort(sortScheduleTaskList,class5Comparator);
                      break;
                  default:break;
              }

        }
    }

    /**
     * 根据班次下标获取枚举
     * @param classIndex
     * @return
     */
    public static ClassEnums getClassEnumsByClassIndex(int classIndex){
        ClassEnums enums=ClassEnums.getClassEnums(classIndex);
        if(enums==null){
            throw new IllegalArgumentException("传入班次下标错误，班次下标："+classIndex);
        }
        return  enums;
    }

    /**
     * 计算各个班次可硫化班次
     * @param cxScheduleTask
     */
    public  static void calcAllClassAvailableLhShift(CxEngineScheduleResult cxScheduleTask) {
        //计算二班可硫化班次
        int classShiftIndex=ClassEnums.CLASS_ONE.getClassIndex();
        Integer totalStock=cxScheduleTask.getCalcTotalStock();//8点总库存
        if(totalStock==null){
            totalStock=0;
        }
        Integer planQty=0;
        Integer singleShiftLhQty=cxScheduleTask.getSingleShiftLhQty();
        //Joran 2022 -01-06 初始化各个班次的计划量
        cxScheduleTask.initPlanQty();
        //如果是新投产的规格,则从第一班开始
        boolean isNewSpec=cxScheduleTask.getNewSpecFlag();

        //Joran 2022-02-24 计算前一天三班可硫化班数
        cxScheduleTask.setClass3PlannedAvailableLhShift(calcClassAvailableLhShift(totalStock,singleShiftLhQty,classShiftIndex));

        if(!isNewSpec){ //非新规格
            planQty=cxScheduleTask.getClass3PlannedQty();//三班成型计划量
            do{
                ClassEnums enums= getClassEnumsByClassIndex(classShiftIndex);
                switch (enums){
                    case CLASS_ONE: //中班
                        totalStock += planQty;
                        cxScheduleTask.setClass1AvailableLhShift(calcClassAvailableLhShift(totalStock,singleShiftLhQty,classShiftIndex));
                        classShiftIndex++;
                        break;
                    case CLASS_TWO://夜班
                        planQty=cxScheduleTask.getClass1PlanQty();//前一班计划量
                        totalStock += planQty;
                        cxScheduleTask.setClass2AvailableLhShift(calcClassAvailableLhShift(totalStock,singleShiftLhQty,classShiftIndex));
                        classShiftIndex++;
                        break;
                    case CLASS_THREE://白班
                        planQty=cxScheduleTask.getClass2PlanQty();//前一班计划量
                        totalStock += planQty;
                        cxScheduleTask.setClass3AvailableLhShift(calcClassAvailableLhShift(totalStock,singleShiftLhQty,classShiftIndex));
                        classShiftIndex++;
                        break;
                    case CLASS_FOUR://次日一班
                        planQty=cxScheduleTask.getClass3PlanQty();//前一班计划量
                        totalStock += planQty;
                        cxScheduleTask.setClass4AvailableLhShift(calcClassAvailableLhShift(totalStock,singleShiftLhQty,classShiftIndex));
                        classShiftIndex++;
                        break;
                    case CLASS_FIVE://次日二班
                        planQty=cxScheduleTask.getClass4PlanQty();//前一班计划量
                        totalStock += planQty;
                        cxScheduleTask.setClass5AvailableLhShift(calcClassAvailableLhShift(totalStock,singleShiftLhQty,classShiftIndex));
                        classShiftIndex++;
                        break;
                    default:{
                        break;
                    }
                }
            }while(classShiftIndex <= ClassEnums.CLASS_FIVE.getClassIndex());
        }else{
            int startIndex=0;//开始计算班次
            if(cxScheduleTask.getClass3PlannedQty()>0){//昨天三班就有计划
                startIndex=ClassEnums.CLASS_ONE.getClassIndex();
            }else if(cxScheduleTask.getClass1PlanQty()>0){
                startIndex=ClassEnums.CLASS_ONE.getClassIndex();//从1班开始
            }else if(cxScheduleTask.getClass2PlanQty()>0){
                startIndex=ClassEnums.CLASS_TWO.getClassIndex();//从2班开始
            }else if(cxScheduleTask.getClass3PlanQty()>0){
                startIndex=ClassEnums.CLASS_THREE.getClassIndex();//从3班开始
            }else if(cxScheduleTask.getClass4PlanQty()>0){
                startIndex=ClassEnums.CLASS_FOUR.getClassIndex();//从次一班开始
            }else if(cxScheduleTask.getClass5PlanQty()>0){
                startIndex=ClassEnums.CLASS_FIVE.getClassIndex();//从次二班开始
            }

            do{
                ClassEnums enums= getClassEnumsByClassIndex(classShiftIndex);
                switch (enums){
                    case CLASS_ONE: //中班
                        if(classShiftIndex<startIndex){
                            cxScheduleTask.setClass1AvailableLhShift(0D);
                        }else{
                            planQty=cxScheduleTask.getClass3PlannedQty();//前一班计划量
                            totalStock+=(planQty==null?0:planQty);
                            cxScheduleTask.setClass1AvailableLhShift(calcClassAvailableLhShiftByStartIndex(totalStock,singleShiftLhQty,classShiftIndex,startIndex));
                        }
                        classShiftIndex++;
                        break;
                    case CLASS_TWO://夜班
                        if(classShiftIndex<=startIndex){
                            cxScheduleTask.setClass2AvailableLhShift(0D);
                        }else{
                            planQty=cxScheduleTask.getClass1PlanQty();//前一班计划量
                            totalStock+=(planQty==null?0:planQty);
                            cxScheduleTask.setClass2AvailableLhShift(calcClassAvailableLhShiftByStartIndex(totalStock,singleShiftLhQty,classShiftIndex,startIndex));
                        }

                        classShiftIndex++;
                        break;
                    case CLASS_THREE://白班
                        if(classShiftIndex<=startIndex){
                            cxScheduleTask.setClass3AvailableLhShift(0D);
                        }else{
                            planQty=cxScheduleTask.getClass2PlanQty();//前一班计划量
                            totalStock+=(planQty==null?0:planQty);
                            cxScheduleTask.setClass3AvailableLhShift(calcClassAvailableLhShiftByStartIndex(totalStock,singleShiftLhQty,classShiftIndex,startIndex));
                        }
                        classShiftIndex++;
                        break;
                    case CLASS_FOUR://次日一班
                        if(classShiftIndex<=startIndex){
                            cxScheduleTask.setClass4AvailableLhShift(0D);
                        }else{
                            planQty=cxScheduleTask.getClass3PlanQty();//前一班计划量
                            totalStock+=(planQty==null?0:planQty);
                            cxScheduleTask.setClass4AvailableLhShift(calcClassAvailableLhShiftByStartIndex(totalStock,singleShiftLhQty,classShiftIndex,startIndex));
                        }
                        classShiftIndex++;
                        break;
                    case CLASS_FIVE://次日二班
                        if(classShiftIndex<=startIndex){
                            cxScheduleTask.setClass4AvailableLhShift(0D);
                        }else{
                            planQty=cxScheduleTask.getClass4PlanQty();//前一班计划量
                            totalStock+=(planQty==null?0:planQty);
                            cxScheduleTask.setClass5AvailableLhShift(calcClassAvailableLhShiftByStartIndex(totalStock,singleShiftLhQty,classShiftIndex,startIndex));
                        }
                        classShiftIndex++;
                        break;
                    default:{
                        break;
                    }
                }
            }while(classShiftIndex <= ClassEnums.CLASS_FIVE.getClassIndex());
        }


    }


    /**
     *  处理日期格式，时分秒处理为0
     * @Author Joran.Zhang
     * @Description
     * @Date 2021/6/23 11:24
     * @Param
     * @Return
     */
    public static Date formatDateByZero(Date date){
        date= DateUtils.setHours(date,0);
        date=DateUtils.setMinutes(date,0);
        date=DateUtils.setSeconds(date,0);
        DateUtils.setMilliseconds(date,0);
        return date;
    }

    /**
     * 根据指定班次进行任务顺序升序排序
     * @param sortScheduleTaskList
     * @param cls
     */
    public static void resultSortAscByClassShiftSort(List<CxEngineScheduleResult> sortScheduleTaskList, ClassEnums cls){
        if(StringUtils.isNotEmpty(sortScheduleTaskList)&&cls!=null){
            Comparator<CxEngineScheduleResult> createTimeAsc = Comparator.comparing(CxEngineScheduleResult::getCreateTime);
            switch (cls){
                case CLASS_ONE:
                    Comparator<CxEngineScheduleResult> class1AscSort = Comparator.comparing(CxEngineScheduleResult::getClass1Sort);
                    //按照下一个班次投产顺序
                    Comparator<CxEngineScheduleResult> nextClassAscSort = Comparator.comparing(CxEngineScheduleResult::getClass2Sort);
                    // 联合排序
                    Comparator<CxEngineScheduleResult> class1Comparator = class1AscSort.thenComparing(nextClassAscSort);
                    Collections.sort(sortScheduleTaskList,class1Comparator);
                    break;
                case CLASS_TWO:
                    Comparator<CxEngineScheduleResult> class2AscSort = Comparator.comparing(CxEngineScheduleResult::getClass2Sort);
                    // 联合排序
                    Comparator<CxEngineScheduleResult> class2Comparator = class2AscSort.thenComparing(createTimeAsc);
                    Collections.sort(sortScheduleTaskList,class2Comparator);
                    break;
                case CLASS_THREE:
                    Comparator<CxEngineScheduleResult> class3AscSort = Comparator.comparing(CxEngineScheduleResult::getClass3Sort);
                    // 联合排序
                    Comparator<CxEngineScheduleResult> class3Comparator = class3AscSort.thenComparing(createTimeAsc);
                    Collections.sort(sortScheduleTaskList,class3Comparator);
                    break;
                case CLASS_FOUR:
                    Comparator<CxEngineScheduleResult> class4AscSort = Comparator.comparing(CxEngineScheduleResult::getClass4Sort);
                    // 联合排序
                    Comparator<CxEngineScheduleResult> class4Comparator = class4AscSort.thenComparing(createTimeAsc);
                    Collections.sort(sortScheduleTaskList,class4Comparator);
                    break;
                case CLASS_FIVE:
                    Comparator<CxEngineScheduleResult> class5AscSort = Comparator.comparing(CxEngineScheduleResult::getClass5Sort);
                    //可硫化班次
                    Comparator<CxEngineScheduleResult> class5Comparator = class5AscSort.thenComparing(createTimeAsc);
                    Collections.sort(sortScheduleTaskList,class5Comparator);
                    break;
                default:break;
            }
        }
    }

    /**
     * 创建自动排程任务
     * @param cxEngineScheduleResult
     * @return
     */
    public static CxAutoScheduleTask createScheduleTask(CxEngineScheduleResult cxEngineScheduleResult,Integer classIndex,int taskQty,Integer continuePlanQty,Double shiftHour){
        CxAutoScheduleTask cxAutoScheduleTask=new CxAutoScheduleTask();
        cxAutoScheduleTask.setCxMachineCode(cxEngineScheduleResult.getCxMachineCode());
        cxAutoScheduleTask.setEmbryoCode(cxEngineScheduleResult.getEmbryoCode());
        //Joran 2021-12-01 添加施工版本信息
        cxAutoScheduleTask.setBomDataVersion(cxEngineScheduleResult.getBomDataVersion());
        cxAutoScheduleTask.setClassShift(classIndex);
        cxAutoScheduleTask.setTaskTotalQty(taskQty);//任务总量
        cxAutoScheduleTask.setClassShiftHour(shiftHour);//班次总时长
        cxAutoScheduleTask.setRemainTaskQty(taskQty-continuePlanQty);//任务剩余量
        return cxAutoScheduleTask;
    }

    /**
     * 根据任务安排的班次顺序降序排序
     * @param sortScheduleTaskList
     */
    public static void sortDescByScheduleTaskClassShift(List<CxAutoScheduleTask> sortScheduleTaskList){
        if(StringUtils.isNotEmpty(sortScheduleTaskList)){
            Comparator<CxAutoScheduleTask> classShiftDescSort = Comparator.comparing(CxAutoScheduleTask::getClassShift).reversed();
            Collections.sort(sortScheduleTaskList,classShiftDescSort);
        }
    }

    /**
     * 根据任务安排的班次顺序升排序
     * @param sortScheduleTaskList
     */
    public static void sortAscByScheduleTaskClassShift(List<CxAutoScheduleTask> sortScheduleTaskList){
        if(StringUtils.isNotEmpty(sortScheduleTaskList)){
            Comparator<CxAutoScheduleTask> classShiftDescSort = Comparator.comparing(CxAutoScheduleTask::getClassShift);
            Collections.sort(sortScheduleTaskList,classShiftDescSort);
        }
    }

    /**
     * 获取指定年月的第一天
     * @param scheduleDate
     * @return
     */
    public static Date getFirstDayOfMonth(Date scheduleDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(scheduleDate); //将Calendar的时间改成日期的时间
        int year = cal.get(Calendar.YEAR); //获取当前年份
        int month = cal.get(Calendar.MONTH);//获取当前月份
        //设置年份
        cal.set(Calendar.YEAR, year);
        //设置月份
        cal.set(Calendar.MONTH, month-1);
        //获取某月最小天数
        int firstDay = cal.getMinimum(Calendar.DATE);
        //设置日历中月份的最小天数
        cal.set(Calendar.DAY_OF_MONTH,firstDay);
        Date date =cal.getTime();
        date= DateUtils.setHours(date,0);
        date=DateUtils.setMinutes(date,0);
        date=DateUtils.setSeconds(date,0);
        DateUtils.setMilliseconds(date,0);
        return date;
    }

    /**
     * 获取指定年月的最后一天
     * @param scheduleDate
     * @return
     */
    public static Date getLastDayOfMonth(Date scheduleDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(scheduleDate); //将Calendar的时间改成日期的时间
        int year = cal.get(Calendar.YEAR); //获取当前年份
        int month = cal.get(Calendar.MONTH);//获取当前月份
        //设置年份
        cal.set(Calendar.YEAR, year);
        //设置月份
        cal.set(Calendar.MONTH, month);
        //获取某月最大天数
        int lastDay = cal.getActualMaximum(Calendar.DATE);
        //设置日历中月份的最大天数
        cal.set(Calendar.DAY_OF_MONTH, lastDay);
        Date date =cal.getTime();
        date= DateUtils.setHours(date,23);
        date=DateUtils.setMinutes(date,59);
        date=DateUtils.setSeconds(date,59);
        return date;
    }

    /**
     * 根据任务列表和班次进行计算平均可硫化班次
     * @param lastDayScheduleResultList
     * @param cls
     * @return
     */
    public static Double calcAvgAvailableLhShift(List<CxEngineScheduleResult> lastDayScheduleResultList, ClassEnums cls) {
        Double avgAvalableLhShift=CxEngineConstants.ZERO;
        BigDecimal totalAvalableLhShiftDic=BigDecimal.ZERO;
        List<CxEngineScheduleResult> toProductList=new ArrayList<>();
        //Joran 2021-12-18 进行投产标记类型为是的数据筛选start
        for(CxEngineScheduleResult cxEngineScheduleResult:lastDayScheduleResultList){
            if(CxEngineConstants.TO_PRODUCT_YES.equals(cxEngineScheduleResult.getToProduct())){
                toProductList.add(cxEngineScheduleResult);
            }
        }
        //Joran 2021-12-18 进行投产标记类型为是的数据筛选end

        //Joran 2021-12-18 只对标记投产的排程计划进行处理 start
        if(StringUtils.isNotEmpty(toProductList)){
            int total=toProductList.size();
            for(CxEngineScheduleResult cxEngineScheduleResult:toProductList){
                Double classAvailableLhShift=CxEngineConstants.ZERO;
                switch (cls){
                    case CLASS_ONE:
                        classAvailableLhShift=cxEngineScheduleResult.getClass1AvailableLhShift()==null?CxEngineConstants.ZERO:cxEngineScheduleResult.getClass1AvailableLhShift();
                        break;
                    case CLASS_TWO:
                        classAvailableLhShift=cxEngineScheduleResult.getClass2AvailableLhShift()==null?CxEngineConstants.ZERO:cxEngineScheduleResult.getClass2AvailableLhShift();
                        break;
                    case CLASS_THREE:
                        classAvailableLhShift=cxEngineScheduleResult.getClass3AvailableLhShift()==null?CxEngineConstants.ZERO:cxEngineScheduleResult.getClass3AvailableLhShift();
                        break;
                    case CLASS_FOUR:
                        classAvailableLhShift=cxEngineScheduleResult.getClass4AvailableLhShift()==null?CxEngineConstants.ZERO:cxEngineScheduleResult.getClass4AvailableLhShift();
                        break;
                    case CLASS_FIVE:
                        classAvailableLhShift=cxEngineScheduleResult.getClass5AvailableLhShift()==null?CxEngineConstants.ZERO:cxEngineScheduleResult.getClass5AvailableLhShift();
                        break;
                    default:break;
                }
                totalAvalableLhShiftDic=totalAvalableLhShiftDic.add(BigDecimal.valueOf(classAvailableLhShift));
            }
            avgAvalableLhShift=totalAvalableLhShiftDic.divide(BigDecimal.valueOf(total),CxEngineConstants.TWO_SCALE,BigDecimal.ROUND_HALF_UP).doubleValue();
        }
        //Joran 2021-12-18 只对标记投产的排程计划进行处理 end

        return avgAvalableLhShift;
    }

    /**
     * 根据班次下标进行对应下标的班次平均可硫化班次计算
     * @param lastDayScheduleResultList
     * @param clsIndex 班次下标开始
     * @return
     */
    public static Double calcAvgAvailableLhShiftIndex(List<CxEngineScheduleResult> lastDayScheduleResultList, Integer clsIndex){
        Double avgAvalableLhShift=CxEngineConstants.ZERO;
        BigDecimal totalAvalableLhShiftDic=BigDecimal.ZERO;
        List<CxEngineScheduleResult> toProductList=new ArrayList<>();
        //Joran 2021-12-18 进行投产标记类型为是的数据筛选start
        for(CxEngineScheduleResult cxEngineScheduleResult:lastDayScheduleResultList){
            if(CxEngineConstants.TO_PRODUCT_YES.equals(cxEngineScheduleResult.getToProduct())){
                toProductList.add(cxEngineScheduleResult);
            }
        }
        //Joran 2021-12-18 进行投产标记类型为是的数据筛选end
        if(StringUtils.isNotEmpty(toProductList)){
            int total=toProductList.size();
            if(clsIndex>0){
                ClassEnums cls=ClassEnums.getClassEnums(clsIndex);
                if(cls!=null){
                    return calcAvgAvailableLhShift(lastDayScheduleResultList,cls);
                }
                return  avgAvalableLhShift;
            }else{
                for(CxEngineScheduleResult cxEngineScheduleResult:toProductList){
                    Double classAvailableLhShift=cxEngineScheduleResult.getClass3PlannedAvailableLhShift();
                    totalAvalableLhShiftDic=totalAvalableLhShiftDic.add(BigDecimal.valueOf(classAvailableLhShift));
                }
                avgAvalableLhShift=totalAvalableLhShiftDic.divide(BigDecimal.valueOf(total),CxEngineConstants.TWO_SCALE,BigDecimal.ROUND_HALF_UP).doubleValue();

            }
        }
        return avgAvalableLhShift;
    }


    /**
     * 获取指定班次的可硫化班数
     * @param cxEngineScheduleResult
     * @param cls
     * @return
     */
    private static Double getAvailableLhShiftByClassShift(CxEngineScheduleResult cxEngineScheduleResult,ClassEnums cls){
        Double availableLhShift=CxEngineConstants.ZERO;
        switch (cls){
            case CLASS_ONE:
                availableLhShift=cxEngineScheduleResult.getClass1AvailableLhShift()==null?CxEngineConstants.ZERO:cxEngineScheduleResult.getClass1AvailableLhShift();
                break;
            case CLASS_TWO:
                availableLhShift=cxEngineScheduleResult.getClass2AvailableLhShift()==null?CxEngineConstants.ZERO:cxEngineScheduleResult.getClass2AvailableLhShift();
                break;
            case CLASS_THREE:
                availableLhShift=cxEngineScheduleResult.getClass3AvailableLhShift()==null?CxEngineConstants.ZERO:cxEngineScheduleResult.getClass3AvailableLhShift();;
                break;
            case CLASS_FOUR:
                availableLhShift=cxEngineScheduleResult.getClass4AvailableLhShift()==null?CxEngineConstants.ZERO:cxEngineScheduleResult.getClass4AvailableLhShift();;
                break;
            case CLASS_FIVE:
                availableLhShift=cxEngineScheduleResult.getClass5AvailableLhShift()==null?CxEngineConstants.ZERO:cxEngineScheduleResult.getClass5AvailableLhShift();;
                break;
            default:break;
        }
        return availableLhShift;
    }

    /**
     * 获取班次下标对应的可硫化班次数(包含前日三班的可硫化班次)
     * @param cxEngineScheduleResult 当前成型计划
     * @param clsIndex 班次下标（获取前日三班传入0即可）
     * @return
     */
    public static Double getAvailableLhShiftByClassShiftIndex(CxEngineScheduleResult cxEngineScheduleResult,Integer clsIndex){
        Double availableLhShift=CxEngineConstants.ZERO;
        if(BigDecimal.ZERO.intValue()==clsIndex){//昨日三班可硫化班次
            return cxEngineScheduleResult.getClass3PlannedAvailableLhShift();
        }
        ClassEnums cls =ClassEnums.getClassEnums(clsIndex);
        if(cls==null){
            return availableLhShift;
        }
        return getAvailableLhShiftByClassShift(cxEngineScheduleResult,cls);
    }



    /**
     * 计算剩余时间
     * @param autoScheduleTask
     * @param quota
     * @param planQty
     */
    public static void calcRemainTime(CxAutoScheduleTask autoScheduleTask, int quota, Integer planQty) {
        StringBuilder sb=new StringBuilder();
        sb.append("计算剩余时间calcRemainTime：")
                .append("任务对象：【").append(autoScheduleTask.toString()).append("】").append("\n")
                .append("入参定额参数：").append(quota)
                .append(",入参计划数量:").append(planQty);
        BigDecimal hourCountBig=BigDecimal.valueOf(quota).divide(BigDecimal.valueOf(CxEngineConstants.CLASS_SHIFT_HOUR)); //一个小时生产多少
        sb.append(",小时产量：").append(hourCountBig);
        int remainQuota=quota-planQty;//剩余定额量 等于定额扣掉任务剩余量
        sb.append("，扣减计划量剩余定额：").append(remainQuota);
        Double usedTime =null;//已经消耗的时间
        Double remainTime=null;//剩余时间
        if(remainQuota>0){
            BigDecimal planQtyBig=BigDecimal.valueOf(planQty);
            usedTime=planQtyBig.divide(hourCountBig,3, BigDecimal.ROUND_DOWN).doubleValue();
            sb.append("，计划量/小时产量保留两位小数：").append(usedTime);
            remainTime=autoScheduleTask.getClassShiftHour()-usedTime; //班次时长扣减掉已经安排的时长算出剩余时间
            sb.append("，剩余时间（含更换工装时间）：").append(remainTime);
            autoScheduleTask.setRemainTime(remainTime);//设置剩余时长
        }else{//刚好可以扣除
            sb.append("【产能排完】剩余任务量刚好扣除任务量剩余时间等于0：").append(remainTime);
            autoScheduleTask.setRemainTime(BigDecimal.ZERO.doubleValue());
        }
        log.debug(sb.toString());
    }

    /**
     * 根据班次设置对应的班次原因分析
     * @param cxEngineScheduleResult
     * @param cls
     */
    public static void setClassAnalysis(CxEngineScheduleResult cxEngineScheduleResult,ClassEnums cls,String classAnalsis){
        if(cls==null){
            log.error("设置班次原因分析，对应的班次信息为空");
            return;
        }
        String alreadyAnalsis="";
        switch (cls){
            case CLASS_ONE:
                alreadyAnalsis=cxEngineScheduleResult.getClass1Analysis();
                if(StringUtils.isNotEmpty(alreadyAnalsis)&&!alreadyAnalsis.equals(classAnalsis)){
                    classAnalsis=StringUtils.isEmpty(alreadyAnalsis)?classAnalsis:alreadyAnalsis+";"+classAnalsis;
                }
                cxEngineScheduleResult.setClass1Analysis(classAnalsis);
                break;
            case CLASS_TWO:
                alreadyAnalsis=cxEngineScheduleResult.getClass2Analysis();
                if(StringUtils.isNotEmpty(alreadyAnalsis)&&!alreadyAnalsis.equals(classAnalsis)){
                    classAnalsis=StringUtils.isEmpty(alreadyAnalsis)?classAnalsis:alreadyAnalsis+";"+classAnalsis;
                }
                cxEngineScheduleResult.setClass2Analysis(classAnalsis);
                break;
            case CLASS_THREE:
                alreadyAnalsis=cxEngineScheduleResult.getClass3Analysis();
                if(StringUtils.isNotEmpty(alreadyAnalsis)&&!alreadyAnalsis.equals(classAnalsis)){
                    classAnalsis=StringUtils.isEmpty(alreadyAnalsis)?classAnalsis:alreadyAnalsis+";"+classAnalsis;
                }
                cxEngineScheduleResult.setClass3Analysis(classAnalsis);
                break;
            case CLASS_FOUR:
                alreadyAnalsis=cxEngineScheduleResult.getClass4Analysis();
                if(StringUtils.isNotEmpty(alreadyAnalsis)&&!alreadyAnalsis.equals(classAnalsis)){
                    classAnalsis=StringUtils.isEmpty(alreadyAnalsis)?classAnalsis:alreadyAnalsis+";"+classAnalsis;
                }
                cxEngineScheduleResult.setClass4Analysis(classAnalsis);
                break;
            case CLASS_FIVE:
                alreadyAnalsis=cxEngineScheduleResult.getClass5Analysis();
                if(StringUtils.isNotEmpty(alreadyAnalsis)&&!alreadyAnalsis.equals(classAnalsis)){
                    classAnalsis=StringUtils.isEmpty(alreadyAnalsis)?classAnalsis:alreadyAnalsis+";"+classAnalsis;
                }
                cxEngineScheduleResult.setClass5Analysis(classAnalsis);
                break;
            default:break;
        }

    }

    /**
     * 获取班次原因分析
     * @param cxEngineScheduleResult
     * @param cls
     * @return
     */
    public static String getClassAnalysis(CxEngineScheduleResult cxEngineScheduleResult,ClassEnums cls){
        String analysis="";
        if(cls==null){
            log.debug("获取班次异常>机台编号："+cxEngineScheduleResult.getCxMachineCode()+"，胎胚代码："+cxEngineScheduleResult.getEmbryoCode());
            return analysis;
        }
        switch (cls){
            case CLASS_ONE:
                analysis=cxEngineScheduleResult.getClass1Analysis();
                break;
            case CLASS_TWO:
                analysis=cxEngineScheduleResult.getClass2Analysis();
                break;
            case CLASS_THREE:
                analysis=cxEngineScheduleResult.getClass3Analysis();
                break;
            case CLASS_FOUR:
                analysis=cxEngineScheduleResult.getClass4Analysis();
                break;
            case CLASS_FIVE:
                analysis=cxEngineScheduleResult.getClass5Analysis();
                break;
            default:break;
        }
        return analysis;
    }


    /**
     * 重新设置任务量和原因分析
     * @param cxEngineScheduleResult
     * @param cls
     * @return
     */
    public static void reSetPlanQtyAndAnalysis(CxEngineScheduleResult cxEngineScheduleResult,ClassEnums cls,Integer differentPlan,String analysis){
        Integer planQty=differentPlan;
        boolean updateAnalysis=false;
        if(getBeforeClassPlanQty(cxEngineScheduleResult,cls)>0){
            updateAnalysis=true;
        }
        switch (cls){
            case CLASS_ONE:
                planQty+=cxEngineScheduleResult.getClass1PlanQty();
                cxEngineScheduleResult.setClass1PlanQty(planQty);
                if(updateAnalysis){
                    cxEngineScheduleResult.setClass1Analysis(analysis);
                }

                break;
            case CLASS_TWO:
                planQty+=cxEngineScheduleResult.getClass2PlanQty();
                cxEngineScheduleResult.setClass2PlanQty(planQty);
                if(updateAnalysis){
                    cxEngineScheduleResult.setClass2Analysis(analysis);
                }

                break;
            case CLASS_THREE:
                planQty+=cxEngineScheduleResult.getClass3PlanQty();
                cxEngineScheduleResult.setClass3PlanQty(planQty);
                if(updateAnalysis){
                    cxEngineScheduleResult.setClass3Analysis(analysis);
                }
                break;
            case CLASS_FOUR:
                planQty+=cxEngineScheduleResult.getClass4PlanQty();
                cxEngineScheduleResult.setClass4PlanQty(planQty);
                if(updateAnalysis){
                    cxEngineScheduleResult.setClass4Analysis(analysis);
                }
                break;
            case CLASS_FIVE:
                planQty+=cxEngineScheduleResult.getClass5PlanQty();
                cxEngineScheduleResult.setClass5PlanQty(planQty);
                if(updateAnalysis){
                    cxEngineScheduleResult.setClass5Analysis(analysis);
                }
                break;
            default:break;
        }
    }

    /**
     * 依据条件判断是否需要进行更换工装原因分析
     * @param cxEngineScheduleResult
     * @param classIndex
     * @param changeMoldAnalysis
     */
    public static void setChangeMoldConditionByClassIndex(CxEngineScheduleResult cxEngineScheduleResult, Integer classIndex, String changeMoldAnalysis,StringBuilder logDetail) {
        logDetail.append(StringUtils.format("设置更换工装原因分析：【{}】，进行更换工装原因分析。班次下标：【{}】，原因分析：【{}】",cxEngineScheduleResult.getEmbryoCode(),classIndex,changeMoldAnalysis));
        if(classIndex==BigDecimal.ZERO.intValue()){
            logDetail.append(StringUtils.format("设置更换工装原因分析：【{}】，当前是昨日三班，不需要进行原因分析",cxEngineScheduleResult.getEmbryoCode()));
            return;
        }
        ClassEnums cls= ClassEnums.getClassEnums(classIndex);
        //前规格是否有安排计划量
        Integer beforePlanQty=CxScheduleUtils.getBeforeClassPlanQty(cxEngineScheduleResult,cls);
        switch (cls){
            case CLASS_ONE:
                if(beforePlanQty==0){
                    cxEngineScheduleResult.setClass1Analysis(changeMoldAnalysis);
                    CxScheduleUtils.setClassAnalysis(cxEngineScheduleResult,cls,changeMoldAnalysis);
                }
                break;
            case CLASS_TWO:
                if(beforePlanQty==0&&StringUtils.isEmpty(cxEngineScheduleResult.getClass1Analysis())){
                    cxEngineScheduleResult.setClass2Analysis(changeMoldAnalysis);
                    CxScheduleUtils.setClassAnalysis(cxEngineScheduleResult,cls,changeMoldAnalysis);
                }
                break;
            case CLASS_THREE:
                if(beforePlanQty==0&&StringUtils.isEmpty(cxEngineScheduleResult.getClass2Analysis())){
                    cxEngineScheduleResult.setClass3Analysis(changeMoldAnalysis);
                    CxScheduleUtils.setClassAnalysis(cxEngineScheduleResult,cls,changeMoldAnalysis);
                }
                break;
            case CLASS_FOUR:
                if(beforePlanQty==0&&StringUtils.isEmpty(cxEngineScheduleResult.getClass3Analysis())){
                    cxEngineScheduleResult.setClass4Analysis(changeMoldAnalysis);
                    CxScheduleUtils.setClassAnalysis(cxEngineScheduleResult,cls,changeMoldAnalysis);
                }
                break;
            case CLASS_FIVE:
                if(beforePlanQty==0&&StringUtils.isEmpty(cxEngineScheduleResult.getClass4Analysis())){
                    cxEngineScheduleResult.setClass5Analysis(changeMoldAnalysis);
                    CxScheduleUtils.setClassAnalysis(cxEngineScheduleResult,cls,changeMoldAnalysis);
                }
                break;
            default:break;
        }
    }


    /**
     * 重置计划量和原因分析根据下标（从昨日三班开始）
     * @param cxEngineScheduleResult
     * @param classIndex
     * @param differentPlan
     * @param analysis
     */
    public static void reSetPlanQtyAndAnalysisByShiftIndex(CxEngineScheduleResult cxEngineScheduleResult,Integer classIndex,Integer differentPlan,String analysis){
        Integer planQty=differentPlan;

        //处理昨日三班计划 start
        if(classIndex==BigDecimal.ZERO.intValue()){
            planQty+=cxEngineScheduleResult.getClass3PlannedQty();
            cxEngineScheduleResult.setLastClass3PlanQty(planQty);
            cxEngineScheduleResult.setClass3PlannedQty(planQty);
            return;
        }
        //处理昨日三班计划 end

        ClassEnums cls =ClassEnums.getClassEnums(classIndex);
        boolean updateAnalysis=false;
        if(getBeforeClassPlanQty(cxEngineScheduleResult,cls)>0){
            updateAnalysis=true;
        }
        switch (cls){
            case CLASS_ONE:
                planQty+=cxEngineScheduleResult.getClass1PlanQty();
                cxEngineScheduleResult.setClass1PlanQty(planQty);
                if(updateAnalysis){
                    cxEngineScheduleResult.setClass1Analysis(analysis);
                }

                break;
            case CLASS_TWO:
                planQty+=cxEngineScheduleResult.getClass2PlanQty();
                cxEngineScheduleResult.setClass2PlanQty(planQty);
                if(updateAnalysis){
                    cxEngineScheduleResult.setClass2Analysis(analysis);
                }

                break;
            case CLASS_THREE:
                planQty+=cxEngineScheduleResult.getClass3PlanQty();
                cxEngineScheduleResult.setClass3PlanQty(planQty);
                if(updateAnalysis){
                    cxEngineScheduleResult.setClass3Analysis(analysis);
                }
                break;
            case CLASS_FOUR:
                planQty+=cxEngineScheduleResult.getClass4PlanQty();
                cxEngineScheduleResult.setClass4PlanQty(planQty);
                if(updateAnalysis){
                    cxEngineScheduleResult.setClass4Analysis(analysis);
                }
                break;
            case CLASS_FIVE:
                planQty+=cxEngineScheduleResult.getClass5PlanQty();
                cxEngineScheduleResult.setClass5PlanQty(planQty);
                if(updateAnalysis){
                    cxEngineScheduleResult.setClass5Analysis(analysis);
                }
                break;
            default:break;
        }
    }

    /**
     * 获取前班次计划量
     * @param cxEngineScheduleResult
     * @param cls
     * @return
     */
    public static Integer getBeforeClassPlanQty(CxEngineScheduleResult cxEngineScheduleResult,ClassEnums cls){
        Integer lastClassPlanQty=0;
        switch (cls){
            case CLASS_ONE:
                lastClassPlanQty=cxEngineScheduleResult.getClass3PlannedQty();
                break;
            case CLASS_TWO:
                lastClassPlanQty=cxEngineScheduleResult.getClass1PlanQty();
                break;
            case CLASS_THREE:
                lastClassPlanQty=cxEngineScheduleResult.getClass2PlanQty();
                break;
            case CLASS_FOUR:
                lastClassPlanQty=cxEngineScheduleResult.getClass3PlanQty();
                break;
            case CLASS_FIVE:
                lastClassPlanQty=cxEngineScheduleResult.getClass4PlanQty();
                break;
            default:break;
        }
        return lastClassPlanQty;
    }

    /**
     * 获取当前班次的计划量
     * @param cxEngineScheduleResult
     * @param cls
     * @return
     */
    public static Integer getCurrentClassPlanQty(CxEngineScheduleResult cxEngineScheduleResult,ClassEnums cls){
        Integer planQty=0;
        if(cls==null){
            return 0;
        }
        switch (cls){
            case CLASS_ONE:
                planQty=cxEngineScheduleResult.getClass1PlanQty();
                break;
            case CLASS_TWO:
                planQty=cxEngineScheduleResult.getClass2PlanQty();
                break;
            case CLASS_THREE:
                planQty=cxEngineScheduleResult.getClass3PlanQty();
                break;
            case CLASS_FOUR:
                planQty=cxEngineScheduleResult.getClass4PlanQty();
                break;
            case CLASS_FIVE:
                planQty=cxEngineScheduleResult.getClass5PlanQty();
                break;
            default:break;
        }
        return planQty;
    }

    /**
     * 获取当前班次的计划量（从前日三班开始获取）
     * @param cxEngineScheduleResult
     * @param classIndex
     * @return
     */
    public static Integer getCurrentClassPlanQtyByShiftIndex(CxEngineScheduleResult cxEngineScheduleResult,Integer classIndex){
        if(classIndex>0){
            ClassEnums cls =ClassEnums.getClassEnums(classIndex);
            return getCurrentClassPlanQty(cxEngineScheduleResult,cls);
        }
        return cxEngineScheduleResult.getClass3PlannedQty();
    }


    /**
     * 获取当前班次的计划量
     * @param cxScheduleResult
     * @param cls
     * @return
     */
    public static Integer getCurrentClassPlanQty(CxScheduleResult cxScheduleResult, ClassEnums cls){
        Integer planQty=0;
        switch (cls){
            case CLASS_ONE:
                planQty=cxScheduleResult.getClass1PlanQty();
                break;
            case CLASS_TWO:
                planQty=cxScheduleResult.getClass2PlanQty();
                break;
            case CLASS_THREE:
                planQty=cxScheduleResult.getClass3PlanQty();
                break;
            case CLASS_FOUR:
                planQty=cxScheduleResult.getClass4PlanQty();
                break;
            case CLASS_FIVE:
                planQty=cxScheduleResult.getClass5PlanQty();
                break;
            default:break;
        }
        return planQty;
    }

    /**
     * 获取规格更换工装时长
     * @param cxMachineType 成型机台类型，如果是一次法不校验扣圈盘直径
     * @param beforeSpec
     * @param afterSpec
     * @param minChangeSpecTimeMin
     * @return
     */
    public static Double changeSpecTime(String cxMachineType, EngineProductConstructionInfo beforeSpec, EngineProductConstructionInfo afterSpec, Double minChangeSpecTimeMin){
        if(afterSpec==null||beforeSpec==null){
            throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.auto.change.spec.time.error"), beforeSpec==null?"":beforeSpec.getEmbryoCode(), afterSpec==null?"":afterSpec.getEmbryoCode()));
        }
        Double beforeNoseWidth=beforeSpec.getNoseWidth();//机头宽度
        Double beforeFlipDiscDiameter=null;
        Double afterFlipDiscDiameter=null;
        if(!CxEngineConstants.MACHINE_TYPE_ONCE.equals(cxMachineType)){
            beforeFlipDiscDiameter=beforeSpec.getFlipDiscDiameter();//扣圈盘直径
            if(beforeFlipDiscDiameter==null){
                throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.insert.beforeFlipDiscDiameter.empty.error"));
            }
        }

        if(beforeNoseWidth==null){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.insert.beforeNoseWidth.empty.error"));
        }
        Double afterNoseWidth=afterSpec.getNoseWidth();//机头宽度
        if(!CxEngineConstants.MACHINE_TYPE_ONCE.equals(cxMachineType)){
             afterFlipDiscDiameter=afterSpec.getFlipDiscDiameter();//扣圈盘直径
            //扣圈盘直径和机头宽度完全一样，不需要进行工装更换
            if(beforeFlipDiscDiameter.equals(afterFlipDiscDiameter)&&beforeNoseWidth.equals(afterNoseWidth)){
                return CxEngineConstants.ZERO;
            }
        }else{
            //机头宽度完全一样，不需要进行工装更换
            if(beforeNoseWidth.equals(afterNoseWidth)){
                return CxEngineConstants.ZERO;
            }
        }
        //根据配置获取更换规格切换时间
        Double minChangeSpecHour=BigDecimal.valueOf(minChangeSpecTimeMin/CxEngineConstants.ONE_MINUTE_SECOND).setScale(CxEngineConstants.TWO_SCALE, BigDecimal.ROUND_HALF_UP).doubleValue();
        log.debug("【计算更换工装时长】前规格胎胚："+beforeSpec.getEmbryoCode()+",机头宽度："+beforeNoseWidth+",扣圈盘直径:"+beforeFlipDiscDiameter+"。后规格胎胚："+afterSpec+",机头宽度："+afterNoseWidth+",扣圈盘直径:"+afterFlipDiscDiameter+"，更换工装时长："+minChangeSpecHour+"(小时)");
        return minChangeSpecHour;
    }


    /**
     * 成型排程任务列表根据胎胚代码进行分组
     * @param taskList
     * @return
     */
    public static Map<String, List<CxEngineScheduleResult>> splitTaskByEmbryoCode(List<CxEngineScheduleResult> taskList){
        Map<String,List<CxEngineScheduleResult>> embryoCodeTaskMap=null;
        if(StringUtils.isNotEmpty(taskList)){
            embryoCodeTaskMap=taskList.stream().collect(Collectors.groupingBy(CxEngineScheduleResult::getEmbryoCode));
        }
        return embryoCodeTaskMap;
    }

    /**
     *  对排程的单班硫化量进行合并汇总
     * @param lastDayScheduleResultList
     */
    public static void calcMachineSpecLhShiftCount(List<CxEngineScheduleResult> lastDayScheduleResultList) {
        log.debug("开始对单机台任务，同胎胚单班硫化量进行合并汇总start");
        if(StringUtils.isNotEmpty(lastDayScheduleResultList)){
            //1、根据胎胚进行分组
            Map<String, List<CxEngineScheduleResult>> embryoCodeTaskListMap= CxScheduleUtils.splitTaskByEmbryoCode(lastDayScheduleResultList);
            if(StringUtils.isNotEmpty(embryoCodeTaskListMap)){
                for(Map.Entry<String, List<CxEngineScheduleResult>> entry:embryoCodeTaskListMap.entrySet()){
                    //胎胚任务列表
                    List<CxEngineScheduleResult> embryoCodeTaskList =entry.getValue();
                    //根据是否计划安排进行倒序
                    Comparator<CxEngineScheduleResult> toProductSort = Comparator.comparing(CxEngineScheduleResult::getToProduct).reversed();
                    Collections.sort(embryoCodeTaskList,toProductSort);
                    //遍历处理进行累加单班硫化量，如果标记为不排产的单班硫化量置0，单班硫化量累加到投产胎胚上
                    int totalSingleLhShiftCount=0;
                    for(CxEngineScheduleResult task:embryoCodeTaskList){
                        if(task.getLhMachineQty()==null||task.getLhMachineQty()==BigDecimal.ZERO.doubleValue()){
                            continue;
                        }
                        totalSingleLhShiftCount+=task.getSingleShiftLhQty();
                        task.setBeforeSingleShiftLhQty(task.getSingleShiftLhQty());//留存
                        if(CxEngineConstants.TO_PRODUCT_NO.equals(task.getToProduct())){
                            task.setSingleShiftLhQty(0);
                        }else{
                            task.setSingleShiftLhQty(totalSingleLhShiftCount);
                            break;
                        }
                    }
                }
            }
        }
        log.debug("开始对单机台任务，同胎胚单班硫化量进行合并汇总end");
    }

    /**
     * 根据班制进行计划量设置
     * @param cxEngineScheduleResult
     * @param cls
     * @param planQty
     */
    public static void setClassShiftPlanQty(CxEngineScheduleResult cxEngineScheduleResult,ClassEnums cls,Integer planQty){
        switch (cls){
            case CLASS_ONE:
                cxEngineScheduleResult.setClass1PlanQty(planQty);
                break;
            case CLASS_TWO:
                cxEngineScheduleResult.setClass2PlanQty(planQty);
                break;
            case CLASS_THREE:
                cxEngineScheduleResult.setClass3PlanQty(planQty);
                break;
            case CLASS_FOUR:
                cxEngineScheduleResult.setClass4PlanQty(planQty);
                break;
            case CLASS_FIVE:
                cxEngineScheduleResult.setClass5PlanQty(planQty);
                break;
            default:break;
        }
    }

    /**
     * 增加一个下标对班次的任务量进行设置（新增从前日三班计划开始进行设置）
     * @param cxEngineScheduleResult
     * @param clsIndex
     * @param planQty
     */
    public static void setClassShiftPlanQtyByShiftIndex(CxEngineScheduleResult cxEngineScheduleResult,int clsIndex,Integer planQty){
        if(clsIndex==BigDecimal.ZERO.intValue()){
            cxEngineScheduleResult.setClass3PlannedQty(planQty);
            cxEngineScheduleResult.setLastClass3PlanQty(planQty);
            return;
        }
        ClassEnums cls=ClassEnums.getClassEnums(clsIndex);
        if(cls==null){
            log.error("设置班次计划量，下标="+clsIndex+",不存在的枚举");
            return;
        }
        setClassShiftPlanQty(cxEngineScheduleResult,cls,planQty);
    }

    /**
     * 获取是否换工装原因分析标识
     * @param cxEngineScheduleResult
     * @param cls
     * @return
     */
    public static boolean  getAnalysisFlag(CxEngineScheduleResult cxEngineScheduleResult,ClassEnums cls){
        if(ClassEnums.CLASS_ONE.equals(cls)){
            return true;
        }else{
            Integer currentSort=getClassClassSort(cxEngineScheduleResult,cls,CxEngineConstants.CURRENT_SHIFT_TYPE);
            Integer beforeSort=getClassClassSort(cxEngineScheduleResult,cls,CxEngineConstants.BEFORE_SHIFT_TYPE);
            return currentSort.equals(beforeSort);
        }
    }

    /**
     * 获取前班次计划量
     * @param cxEngineScheduleResult
     * @param cls
     * @param  type  0:当前班次顺序，-1前班制顺序
     * @return
     */
    public static Integer getClassClassSort(CxEngineScheduleResult cxEngineScheduleResult,ClassEnums cls,String type){
        Integer classSort=0;
        if(CxEngineConstants.CURRENT_SHIFT_TYPE.equals(type)){
            switch (cls){
                case CLASS_TWO:
                    classSort=cxEngineScheduleResult.getClass2Sort();
                    break;
                case CLASS_THREE:
                    classSort=cxEngineScheduleResult.getClass3Sort();
                    break;
                case CLASS_FOUR:
                    classSort=cxEngineScheduleResult.getClass4Sort();
                    break;
                case CLASS_FIVE:
                    classSort=cxEngineScheduleResult.getClass5Sort();
                    break;
                default:break;
            }
        }else if(CxEngineConstants.BEFORE_SHIFT_TYPE.equals(type)){
            switch (cls){
                case CLASS_TWO:
                    classSort=cxEngineScheduleResult.getClass1Sort();
                    break;
                case CLASS_THREE:
                    classSort=cxEngineScheduleResult.getClass2Sort();
                    break;
                case CLASS_FOUR:
                    classSort=cxEngineScheduleResult.getClass3Sort();
                    break;
                case CLASS_FIVE:
                    classSort=cxEngineScheduleResult.getClass4Sort();
                    break;
                default:break;
            }
        }

        return classSort;
    }

    /**
     * 根据班次任务安排情况获取前规格任务
     * @param taskList
     * @param cls
     * @param classSort
     * @return
     */
    public static CxEngineScheduleResult getBeforeScheduleResultByClassSort(List<CxEngineScheduleResult> taskList,ClassEnums cls,Integer classSort){
        if(StringUtils.isNotEmpty(taskList)){
            for(CxEngineScheduleResult cxEngineScheduleResult:taskList){
                Integer firstClassSort=getClassClassSort(cxEngineScheduleResult,cls,CxEngineConstants.CURRENT_SHIFT_TYPE);
                if(classSort.equals(firstClassSort)){
                    return cxEngineScheduleResult;
                }
            }
        }
        return null;
    }

    /**
     * 将原始集合中可自动安排的计划添加到目标列表中
     * @param sourceList
     * @param targetList
     */
    public static void addProductSourceToTarget(List<CxEngineScheduleResult> sourceList,List<CxEngineScheduleResult> targetList){
        for(CxEngineScheduleResult cxEngineScheduleResult:sourceList){
            if(CxEngineConstants.TO_PRODUCT_YES.equals(cxEngineScheduleResult.getToProduct())){
                targetList.add(cxEngineScheduleResult);
            }
        }
    }

    /**
     * 自动排程添加新规格时工具方法
     * @param lastEngineScheduleResult
     * @return
     */
    public static CxEngineScheduleResult createNewScheduleResultTask(CxEngineScheduleResult lastEngineScheduleResult, CxPlanProductStatus nextProductPlan,EngineProductConstructionInfo newSpecConstructionInfo){
        CxEngineScheduleResult newSpecPlanResult=new CxEngineScheduleResult();
        String cxBatchNo=lastEngineScheduleResult.getCxBatchNo();//获取成型批次号
        String machineCode=lastEngineScheduleResult.getCxMachineCode();//获取成型机台
        newSpecPlanResult.setCxBatchNo(cxBatchNo);
        newSpecPlanResult.setCxMachineCode(machineCode);//成型机台编号
        newSpecPlanResult.setCxMachineType(lastEngineScheduleResult.getCxMachineType());//成型机机型
        newSpecPlanResult.setCxMachineName(lastEngineScheduleResult.getCxMachineName());//成型机台名称
        newSpecPlanResult.setCxMachineType(lastEngineScheduleResult.getCxMachineType());//2021-12-15 成型机台类型
        newSpecPlanResult.setScheduleDate(lastEngineScheduleResult.getScheduleDate());
        newSpecPlanResult.setWorkShifts(lastEngineScheduleResult.getWorkShifts());//班制
        newSpecPlanResult.setTotalStock(0);//库存
        newSpecPlanResult.setCxMonthFinishQty(0);//成型产量
        newSpecPlanResult.setClass1PlanQty(0);
        newSpecPlanResult.setClass2PlanQty(0);
        newSpecPlanResult.setClass3PlanQty(0);
        newSpecPlanResult.setClass4PlanQty(0);
        newSpecPlanResult.setClass5PlanQty(0);
        newSpecPlanResult.setLastClass1PlanQty(0);
        newSpecPlanResult.setLastClass2PlanQty(0);
        newSpecPlanResult.setLastClass3PlanQty(0);
        newSpecPlanResult.setLastClass4PlanQty(0);
        newSpecPlanResult.setLastClass5PlanQty(0);
        newSpecPlanResult.setClass3PlannedQty(0);
        newSpecPlanResult.setClass1Sort(0);
        newSpecPlanResult.setClass2Sort(0);
        newSpecPlanResult.setClass3Sort(0);
        newSpecPlanResult.setClass4Sort(0);
        newSpecPlanResult.setClass5Sort(0);
        newSpecPlanResult.setMonthFinishQty(0);//月度完成量为0
        newSpecPlanResult.setNewSpecFlag(true);//标注为添加新规格
        //Joran 2021-10-14 实际超欠产数据填充
        newSpecPlanResult.setSapCode(nextProductPlan.getSapCode());//SAP品号
        newSpecPlanResult.setEmbryoCode(nextProductPlan.getEmbryoCode());//胎胚代码
        newSpecPlanResult.setActualOverProduction(0-nextProductPlan.getMonthPlanTotalQty());
        newSpecPlanResult.setSpecDimension(newSpecConstructionInfo.getDimension());//寸口
        newSpecPlanResult.setTaskType(CxEngineConstants.TASK_TYPE_TODO);//设置待投产指还没安排硫化机
        newSpecPlanResult.setProductionStatus(CxEngineConstants.PRODUCTION_STATUS_UNDO);//设置未生产
        newSpecPlanResult.setIsRelease(CxEngineConstants.IS_PUBLISH_NO);//未发布
        newSpecPlanResult.setDataSource(CxEngineConstants.CX_SCHEDULE_DATA_SOURCE_AUTO);//数据来源：自动排程
        newSpecPlanResult.setBomDataVersion(nextProductPlan.getBomDataVersion());//施工版本信息
        newSpecPlanResult.setSpecialRequirements(nextProductPlan.getSpecialRequirements());//特殊要求
        //Joran 2022-04-25 添加创建人创建时间
        newSpecPlanResult.setBaseVale(null);
        return newSpecPlanResult;
    }

    /**
     * 根据寸口进行任务拆分
     * @param taskList
     * @return
     */
    public static Map<String, List<CxEngineScheduleResult>> splitTaskByDimension(String prefix,List<CxEngineScheduleResult> taskList){
        Map<String,List<CxEngineScheduleResult>> dimensionTaskMap=null;
        if(StringUtils.isNotEmpty(taskList)){
            dimensionTaskMap=taskList.stream().collect(Collectors.groupingBy(cxEngineScheduleResult -> GenerageMapKeyUtils.createMapKey(prefix,cxEngineScheduleResult.getSpecDimension()+"")));
        }
        return dimensionTaskMap;
    }

    /**
     * 根据各个班次的任务量来进行机台任务顺序排序
     * @param sameMachineScheduleList 同机台任务列表
     */
    public static void scheduleTaskMachinePlanSort(List<CxEngineScheduleResult> sameMachineScheduleList){
        if(StringUtils.isEmpty(sameMachineScheduleList)){
            return;
        }else if(sameMachineScheduleList.size()==1){ //只有一条记录时其实生产顺序就是1
            CxEngineScheduleResult cxEngineScheduleResult=sameMachineScheduleList.get(0);
            cxEngineScheduleResult.setPlanSort(1);
            return;
        }
        //工单对应的生产顺序
        Map<String,Integer> orderNoPlanSort= new HashMap<>();
        Integer machinePlanSort=0;
   /*     Comparator<CxEngineScheduleResult> class1SortAsc = Comparator.comparing(CxEngineScheduleResult::getClass1Sort);
        Collections.sort(sameMachineScheduleList,class1SortAsc);*/
        //根据一班都需要特殊处理一下
        for(CxEngineScheduleResult cxEngineScheduleResult:sameMachineScheduleList){
            if(cxEngineScheduleResult.getClass1Sort()!=null && cxEngineScheduleResult.getClass1Sort()>0){
                machinePlanSort=cxEngineScheduleResult.getClass1Sort();
                cxEngineScheduleResult.setPlanSort(machinePlanSort);
                orderNoPlanSort.put(cxEngineScheduleResult.getOrderNo(),machinePlanSort);
            }
        }
        //根据二班都需要特殊处理一下
        for(CxEngineScheduleResult cxEngineScheduleResult:sameMachineScheduleList){
            if(cxEngineScheduleResult.getClass2Sort()!=null && cxEngineScheduleResult.getClass2Sort()>0 && !orderNoPlanSort.containsKey(cxEngineScheduleResult.getOrderNo())){
                machinePlanSort+=1;
                cxEngineScheduleResult.setPlanSort(machinePlanSort);
                orderNoPlanSort.put(cxEngineScheduleResult.getOrderNo(),machinePlanSort);
            }
        }
        //根据三班都需要特殊处理一下
        for(CxEngineScheduleResult cxEngineScheduleResult:sameMachineScheduleList){
            if(cxEngineScheduleResult.getClass3Sort()!=null && cxEngineScheduleResult.getClass3Sort()>0 && !orderNoPlanSort.containsKey(cxEngineScheduleResult.getOrderNo())){
                machinePlanSort+=1;
                cxEngineScheduleResult.setPlanSort(machinePlanSort);
                orderNoPlanSort.put(cxEngineScheduleResult.getOrderNo(),machinePlanSort);
            }

        }
        //次一班
        for(CxEngineScheduleResult cxEngineScheduleResult:sameMachineScheduleList){
            if(cxEngineScheduleResult.getClass4Sort()!=null && cxEngineScheduleResult.getClass4Sort()>0 && !orderNoPlanSort.containsKey(cxEngineScheduleResult.getOrderNo())){
                machinePlanSort+=1;
                cxEngineScheduleResult.setPlanSort(machinePlanSort);
                orderNoPlanSort.put(cxEngineScheduleResult.getOrderNo(),machinePlanSort);
            }

        }
        //次二班
        for(CxEngineScheduleResult cxEngineScheduleResult:sameMachineScheduleList){
            if(cxEngineScheduleResult.getClass5Sort()!=null && cxEngineScheduleResult.getClass5Sort()>0 && !orderNoPlanSort.containsKey(cxEngineScheduleResult.getOrderNo())){
                machinePlanSort+=1;
                cxEngineScheduleResult.setPlanSort(machinePlanSort);
                orderNoPlanSort.put(cxEngineScheduleResult.getOrderNo(),machinePlanSort);
            }
        }

        //最后增补顺序
        for(CxEngineScheduleResult cxEngineScheduleResult:sameMachineScheduleList){
            if(cxEngineScheduleResult.getPlanSort()==null){
                machinePlanSort+=1;
                cxEngineScheduleResult.setPlanSort(machinePlanSort);
                orderNoPlanSort.put(cxEngineScheduleResult.getOrderNo(),machinePlanSort);
            }
        }
        //根据生产顺序进行排序
        sortByPlanSort(sameMachineScheduleList);
    }

    /**
     * 根据生产顺序进行排序
     * @param sameMachineScheduleList
     */
    public static void sortByPlanSort(List<CxEngineScheduleResult> sameMachineScheduleList) {
        Comparator<CxEngineScheduleResult> planSortAsc = Comparator.comparing(CxEngineScheduleResult::getPlanSort);
        Collections.sort(sameMachineScheduleList,planSortAsc);
    }

    /**
     * 从三班开始往后移动规格班次计划量和原因分析，用于做平移调整
     * @param cxEngineScheduleResult
     * @param taskShiftPlanQty
     * @param taskShiftAnalysis
     */
    public static void cacheFromClass3PlanQtyAndAnalysis(CxEngineScheduleResult cxEngineScheduleResult,Map<String,Integer> taskShiftPlanQty,Map<String,String> taskShiftAnalysis){
        for (ClassEnums cls : ClassEnums.values()) {
            String key= GenerageMapKeyUtils.createMapKey(cxEngineScheduleResult.getOrderNo(),cls.getClassIndex()+"");
            switch (cls){
                case CLASS_ONE:
                case CLASS_TWO:
                    continue;
            }
            if(!taskShiftPlanQty.containsKey(key)){
                taskShiftPlanQty.put(key,getCurrentClassPlanQty(cxEngineScheduleResult,cls));
            }

            if(!taskShiftAnalysis.containsKey(key)){
                taskShiftAnalysis.put(key,getClassAnalysis(cxEngineScheduleResult,cls));
            }

        }
    }

    /**
     * 清空和置零从三班开始后的班次计划量和原因分析
     * @param cxEngineScheduleResult
     */
    public static void cleanFromClass3PlanQtyAndAnalysis(CxEngineScheduleResult cxEngineScheduleResult){
        for (ClassEnums cls : ClassEnums.values()) {
            switch (cls){
                case CLASS_ONE:
                case CLASS_TWO:
                    continue;
            }
            //清空班次
            setClassShiftPlanQty(cxEngineScheduleResult,cls,0);
            setClassShiftEmptyAnalysis(cxEngineScheduleResult,cls);

        }
    }

    /**
     *  班次原因分析清空
     * @param cxEngineScheduleResult
     * @param cls
     */
    public static void setClassShiftEmptyAnalysis(CxEngineScheduleResult cxEngineScheduleResult,ClassEnums cls){
        switch (cls){
            case CLASS_ONE:
                cxEngineScheduleResult.setClass1Analysis("");
                break;
            case CLASS_TWO:
                cxEngineScheduleResult.setClass2Analysis("");
                break;
            case CLASS_THREE:
                cxEngineScheduleResult.setClass3Analysis("");
                break;
            case CLASS_FOUR:
                cxEngineScheduleResult.setClass4Analysis("");
                break;
            case CLASS_FIVE:
                cxEngineScheduleResult.setClass5Analysis("");
                break;
            default:break;
        }
    }

    /**
     * 原因分析平移
     * @param currentCxScheduleResultCopy 当前排程计划
     * @param currentClassIndex 设定的班次
     * @param analysisBeginIndex 原因迁移开始班次
     * @param analysisStep 当前步骤
     * @param taskShiftAnalysis 原因分析集合
     */
    public static void setClassAnalysisByMap(CxEngineScheduleResult currentCxScheduleResultCopy, Integer currentClassIndex, Integer analysisBeginIndex, Integer analysisStep, Map<String, String> taskShiftAnalysis) {
        ClassEnums cls=ClassEnums.getClassEnums(currentClassIndex);
        if(currentCxScheduleResultCopy==null){
            return;
        }
        if(cls==null){
            return;
        }
        if(StringUtils.isEmpty(taskShiftAnalysis)){
            return;
        }
        Integer analysisIndex=analysisBeginIndex+analysisStep;
        String key=GenerageMapKeyUtils.createMapKey(currentCxScheduleResultCopy.getOrderNo(),analysisIndex+"");
        if(taskShiftAnalysis.containsKey(key)){
            String analysis=taskShiftAnalysis.get(key);
            setClassAnalysis(currentCxScheduleResultCopy,cls,analysis);
        }
    }


    /**
     * 清空和置零从三班开始后的班次计划量和原因分析
     * @param cxEngineScheduleResult
     */
    public static void cleanFromClass3PlannedQty(CxEngineScheduleResult cxEngineScheduleResult){
    cxEngineScheduleResult.setClass3PlannedQty(0);
    cxEngineScheduleResult.setClass1PlanQty(0);
    cxEngineScheduleResult.setClass1Sort(0);
    cxEngineScheduleResult.setClass1Analysis("");
    cxEngineScheduleResult.setClass2PlanQty(0);
    cxEngineScheduleResult.setClass2Sort(0);
    cxEngineScheduleResult.setClass2Analysis("");
    }

    /**
     * 对机台全部的单班硫化量进行汇总
     * @param toProductList
     * @return
     */
    public static Integer sumSingleLhShiftQty(List<CxEngineScheduleResult> toProductList,String machineCode,StringBuilder logDetail,String division) {
        logDetail.append(StringUtils.format("机台编号：{}，进行汇总总的单班硫化总量",machineCode)).append(division);
        Integer totalLhSingleShift=BigDecimal.ZERO.intValue();
        for(CxEngineScheduleResult cxEngineScheduleResult:toProductList){
            totalLhSingleShift+=cxEngineScheduleResult.getSingleShiftLhQty();
        }
        logDetail.append(StringUtils.format("机台编号：{}，进行汇总总的单班硫化总量={}",machineCode,totalLhSingleShift)).append(division);
        return totalLhSingleShift;
    }

    /**
     *  对排程的单班硫化量进行合并汇总
     * @param lastDaySupplePlanList
     */
    public static void calcMachineSpecLhShiftCountBySupplePlan(List<CxEngineLastDaySupplePlan> lastDaySupplePlanList) {
        log.debug("开始对单机台增补计划处理，同胎胚单班硫化量进行合并汇总start");
        if(StringUtils.isNotEmpty(lastDaySupplePlanList)){
            //1、根据胎胚进行分组
            Map<String, List<CxEngineLastDaySupplePlan>> embryoCodeTaskListMap= CxScheduleUtils.splitSuppleTaskByEmbryoCode(lastDaySupplePlanList);
            if(StringUtils.isNotEmpty(embryoCodeTaskListMap)){
                for(Map.Entry<String, List<CxEngineLastDaySupplePlan>> entry:embryoCodeTaskListMap.entrySet()){
                    //胎胚任务列表
                    List<CxEngineLastDaySupplePlan> embryoCodeTaskList =entry.getValue();
                    //根据是否计划安排进行倒序
                    Comparator<CxEngineLastDaySupplePlan> toProductSort = Comparator.comparing(CxEngineLastDaySupplePlan::getToProduct).reversed();
                    Collections.sort(embryoCodeTaskList,toProductSort);
                    //遍历处理进行累加单班硫化量，如果标记为不排产的单班硫化量置0，单班硫化量累加到投产胎胚上
                    int totalSingleLhShiftCount=0;
                    for(CxEngineLastDaySupplePlan task:embryoCodeTaskList){
                        if(task.getLhMachineQty()==null||task.getLhMachineQty()==BigDecimal.ZERO.doubleValue()){
                            continue;
                        }
                        totalSingleLhShiftCount+=task.getSingleShiftLhQty();
                        task.setBeforeSingleShiftLhQty(task.getSingleShiftLhQty());//留存
                        if(CxEngineConstants.TO_PRODUCT_NO.equals(task.getToProduct())){
                            task.setSingleShiftLhQty(0);
                        }else{
                            task.setSingleShiftLhQty(totalSingleLhShiftCount);
                            break;
                        }
                    }
                }
            }
        }
        log.debug("开始对单机台增补计划处理，同胎胚单班硫化量进行合并汇总end");
    }

    /**
     * 成型排程任务列表根据胎胚代码进行分组
     * @param taskList
     * @return
     */
    public static Map<String, List<CxEngineLastDaySupplePlan>> splitSuppleTaskByEmbryoCode(List<CxEngineLastDaySupplePlan> taskList){
        Map<String,List<CxEngineLastDaySupplePlan>> embryoCodeTaskMap=null;
        if(StringUtils.isNotEmpty(taskList)){
            embryoCodeTaskMap=taskList.stream().collect(Collectors.groupingBy(CxEngineLastDaySupplePlan::getEmbryoCode));
        }
        return embryoCodeTaskMap;
    }



}
