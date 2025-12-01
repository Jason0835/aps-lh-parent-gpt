package com.zlt.mix.schedule.engine.service.materialschedule.impl;

import com.github.pagehelper.util.StringUtil;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.BigDecimalUtil;
import com.zlt.mix.common.engine.constants.EngineConstants;
import com.zlt.mix.common.engine.utils.CollectionUtil;
import com.zlt.mix.schedule.api.domain.entity.MaterialScheduleResult;
import com.zlt.mix.schedule.engine.service.materialschedule.MaterialClassEngineService;
import com.zlt.mix.schedule.engine.util.CombinedMapKey;
import com.zlt.mix.schedule.engine.vo.MaterialLastClassVo;
import com.zlt.mix.schedule.engine.vo.MaterialScheduleResultVo;
import com.zlt.mix.setting.api.domain.entity.LhflMachine;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.mix.common.core.utils.MixCommonUtil.getDouble;
import static com.zlt.mix.common.core.utils.MixCommonUtil.getInt;

/**
 * 硫磺辅料日计划排程各班计划量、完成时间、排程等信息Service业务层处理
 */
@Slf4j
@Service
public class MaterialClassEngineServiceImpl implements MaterialClassEngineService {

    /**
     * 计算各班计划量、计划开始时间、计划完成时间、生产顺序
     * @param scheduleList
     * @param params  参数map
     * @param machineMap  机台信息map
     * @param publishScheduleList	已发布计划，用于确定每个机台每个班的初始编号
     */
    public void staScheduleClassInfo(List<MaterialScheduleResultVo> scheduleList, Map<String, String> params, 
    		Map<String,LhflMachine> machineMap, List<MaterialScheduleResultVo> publishScheduleList) {
        //把排程记录按机台进行分组
        Map<String, List<MaterialScheduleResultVo>> scheduleMap = scheduleList.stream().collect(Collectors.groupingBy(r->r.getMachineCode()));
        scheduleList.clear();
        
        Map<CombinedMapKey, MaterialLastClassVo> latestOrderMap = this.buildLatestScheduleMap(publishScheduleList); // 各机台各班已发布记录的最后一个排程
        int materialIntervalTime = getInt(params.get(EngineConstants.MATERIAL_INTERVAL_TIME));  //不同规格之间的间隔时间（单位：分）
        for(Map.Entry<String, List<MaterialScheduleResultVo>> entry : scheduleMap.entrySet()) {
            String machineCode = entry.getKey();  //机台code
            LhflMachine machine = machineMap.get(machineCode);
            int classShift = (machine != null ? machine.getClassShift() : EngineConstants.CLASS_SHIFT_THREE);  //机台班制
            // 判断各班机台是否启用
            boolean isMidEnable = Optional.ofNullable(machine).map(r -> ZltConstant.STATUS_ENABLE.equals(r.getMidStatus())).orElse(true);
            boolean isNightEnable = Optional.ofNullable(machine).map(r -> ZltConstant.STATUS_ENABLE.equals(r.getNightStatus())).orElse(true);
            boolean isDayEnable = Optional.ofNullable(machine).map(r -> ZltConstant.STATUS_ENABLE.equals(r.getDayStatus())).orElse(true);

            List<MaterialScheduleResultVo> scheduleGroupList = entry.getValue();
            //同一个机台下的排产按中班需求计划时间生效排序（需求计划时间为空的，放最后）,在按夜班需求计划时间排程，最后在按白班需求计划时间排序
            scheduleGroupList = scheduleGroupList.stream().sorted(this.compareDataSource() // 先按数据类型排序，跨区接收的放在前面
            		.thenComparing(MaterialScheduleResultVo::getGlueMidExpectStartTime, Comparator.nullsLast(Date::compareTo))
                    .thenComparing(MaterialScheduleResultVo::getGlueNightExpectStartTime, Comparator.nullsLast(Date::compareTo))
                    .thenComparing(MaterialScheduleResultVo::getGlueDayExpectStartTime, Comparator.nullsLast(Date::compareTo))).collect(Collectors.toList());
            if(classShift == EngineConstants.CLASS_SHIFT_ONE) {     //长白班的情况
            	if (isDayEnable) {
            		MaterialLastClassVo dayLatestClass = latestOrderMap.get(CombinedMapKey.createKey(machineCode,  EngineConstants.CLASS_DAY)); // 白班初始序号
                	this.longDayClassSystem(scheduleGroupList, materialIntervalTime, dayLatestClass);
            	}
            } else if(classShift == EngineConstants.CLASS_SHIFT_TWO) {       //两班制的情况
            	if (isMidEnable) {
            		//计算中班的计划量、预计完成时间、顺序等；并且根据机台剩余产能，以及在保证能按时给“终炼母炼”供料的基础上，把夜班、白班计划量 合并到中班
            		MaterialLastClassVo midLatestClass = latestOrderMap.get(CombinedMapKey.createKey(machineCode,  EngineConstants.CLASS_MID)); // 中班初始序号
            		this.midDayClass(scheduleGroupList, materialIntervalTime, midLatestClass);
            	}
                if (isNightEnable) {
                	//计算夜班的计划量、预计完成时间、顺序等；并且根据机台剩余产能，以及在保证能按时给“终炼母炼”供料的基础上，把白班计划量 合并到夜班
                	MaterialLastClassVo nightLatestClass = latestOrderMap.get(CombinedMapKey.createKey(machineCode,  EngineConstants.CLASS_NIGHT)); // 夜班初始序号
                	this.nightDayClass(scheduleGroupList, materialIntervalTime, nightLatestClass);
                }
            	//两部制的情况下：把白班计划量不为0的记录，全部移到中班最后称重；如果中班产能已经满了，则全部移到夜班最后称重
            	this.dayTwoDayClass(scheduleGroupList, materialIntervalTime);
            } else {     //三班制的情况
            	if (isMidEnable) {
            		//计算中班的计划量、预计完成时间、顺序等；并且根据机台剩余产能，以及在保证能按时给“终炼母炼”供料的基础上，把夜班、白班计划量 合并到中班
            		MaterialLastClassVo midLatestClass = latestOrderMap.get(CombinedMapKey.createKey(machineCode,  EngineConstants.CLASS_MID)); // 中班初始序号
            		this.midDayClass(scheduleGroupList, materialIntervalTime, midLatestClass);
            	}
                if (isNightEnable) {
                	//计算夜班的计划量、预计完成时间、顺序等；并且根据机台剩余产能，以及在保证能按时给“终炼母炼”供料的基础上，把白班计划量 合并到夜班
                	MaterialLastClassVo nightLatestClass = latestOrderMap.get(CombinedMapKey.createKey(machineCode,  EngineConstants.CLASS_NIGHT)); // 夜班初始序号
                	this.nightDayClass(scheduleGroupList, materialIntervalTime, nightLatestClass);
                }
            	if (isDayEnable) {
            		//计算白班的计划量、预计完成时间、顺序等
            		MaterialLastClassVo dayLatestClass = latestOrderMap.get(CombinedMapKey.createKey(machineCode,  EngineConstants.CLASS_DAY)); // 白班初始序号
            		this.dayThreeDayClass(scheduleGroupList, materialIntervalTime, dayLatestClass);
            	}
            }
            scheduleList.addAll(scheduleGroupList);
        }
    }
    
    /**
     * 按数据排程的数据来源排序，跨区接收的最优先
     * @return
     */
    private Comparator<MaterialScheduleResultVo> compareDataSource() {
    	return new Comparator<MaterialScheduleResultVo>() {
			@Override
			public int compare(MaterialScheduleResultVo o1, MaterialScheduleResultVo o2) {
				String dataSource1 = o1.getDataSource();
				String dataSource2 = o2.getDataSource();
				if (Objects.equals(dataSource1, dataSource2)) {
					return 0;
				}
				// 跨区接收产生排程排在最前头
				if (ZltConstant.MATERIAL_SCHEDULE_SOURCE_RECEIVE.equals(dataSource1)) {
					return -1;
				}
				if (ZltConstant.MATERIAL_SCHEDULE_SOURCE_RECEIVE.equals(dataSource2)) {
					return 1;
				}
				// 如果两个都不是，顺序不变
				return 0;
			}
		};
    }

    /**
     * 构建各机台各班已发布记录的最后一个排程
     * @param publishScheduleList	已发布计划列表
     * @return
     */
	private Map<CombinedMapKey, MaterialLastClassVo> buildLatestScheduleMap(List<MaterialScheduleResultVo> publishScheduleList) {
		// 统计各机台各班已发布记录的最后一个序号
        Map<CombinedMapKey, MaterialLastClassVo> latestOrderMap = new HashMap<>();
        // 按机台分组
        Map<String, List<MaterialScheduleResultVo>> publishMap = publishScheduleList.stream().collect(Collectors.groupingBy(MaterialScheduleResultVo::getMachineCode));
        BigDecimal orderMultiple = BigDecimalUtil.valueOf(EngineConstants.ORDER_MULTIPLE); // 顺序之间的倍数
        for (Entry<String, List<MaterialScheduleResultVo>> entry: publishMap.entrySet()) {
        	String machineCode = entry.getKey();
        	List<MaterialScheduleResultVo> publishList = entry.getValue();
        	// 取出各班已发布记录大的序号
        	MaterialScheduleResultVo midLatestSchedule = publishList.stream().filter(r -> r.getMidProduceOrder() != null).max(Comparator.comparing(MaterialScheduleResultVo::getMidProduceOrder)).orElse(null);
        	MaterialScheduleResultVo nightLatestSchedule = publishList.stream().filter(r -> r.getNightProduceOrder() != null).max(Comparator.comparing(MaterialScheduleResultVo::getNightProduceOrder)).orElse(null);
        	MaterialScheduleResultVo dayLatestSchedule = publishList.stream().filter(r -> r.getDayProduceOrder() != null).max(Comparator.comparing(MaterialScheduleResultVo::getDayProduceOrder)).orElse(null);
        	
        	if (midLatestSchedule != null) {
        		Integer midProduceOrder = midLatestSchedule.getMidProduceOrder();
        		Integer produceOrder = BigDecimalUtil.valueOf(midProduceOrder).divide(orderMultiple, 0, RoundingMode.DOWN).intValue();
        		MaterialLastClassVo lastClass = new MaterialLastClassVo(midLatestSchedule.getMaterialName(), produceOrder + 1, null);
        		latestOrderMap.put(CombinedMapKey.createKey(machineCode, EngineConstants.CLASS_MID), lastClass);
        	}
        	if (nightLatestSchedule != null) {
        		Integer nightProduceOrder = nightLatestSchedule.getNightProduceOrder();
        		Integer produceOrder = BigDecimalUtil.valueOf(nightProduceOrder).divide(orderMultiple, 0, RoundingMode.DOWN).intValue();
        		MaterialLastClassVo lastClass = new MaterialLastClassVo(nightLatestSchedule.getMaterialName(), produceOrder + 1, null);
        		latestOrderMap.put(CombinedMapKey.createKey(machineCode, EngineConstants.CLASS_NIGHT), lastClass);
        	}
        	if (dayLatestSchedule != null) {
        		Integer dayProduceOrder = dayLatestSchedule.getDayProduceOrder();
        		Integer produceOrder = BigDecimalUtil.valueOf(dayProduceOrder).divide(orderMultiple, 0, RoundingMode.DOWN).intValue();
        		MaterialLastClassVo lastClass = new MaterialLastClassVo(dayLatestSchedule.getMaterialName(), produceOrder + 1, null);
        		latestOrderMap.put(CombinedMapKey.createKey(machineCode, EngineConstants.CLASS_DAY), lastClass);
        	}
        	
        }
        return latestOrderMap;
	}

    /**
     * 长白班的情况，计划量都移到早班去
     * @param scheduleGroupList
     * @param materialIntervalTime 不同规格之间的间隔时间（单位：分）
     */
    private void longDayClassSystem(List<MaterialScheduleResultVo> scheduleGroupList, int materialIntervalTime, MaterialLastClassVo dayLatestClass) {
        Date produceTime = null;
        String preMaterialName = "";  //上一个胶料名称
        int order = 1; //顺序
        if (dayLatestClass != null) {
        	preMaterialName = dayLatestClass.getMaterialName();
        	order = dayLatestClass.getProduceOrder();
        }
        for(MaterialScheduleResultVo schedule : scheduleGroupList) {
            Double dayPlanQty = BigDecimalUtil.add(schedule.getMidPlanQty(), schedule.getNightPlanQty(), schedule.getDayPlanQty());
            schedule.setMidPlanQty(0D);   //中班计划量
            schedule.setNightPlanQty(0D);   //夜班计划量
            schedule.setDayPlanQty(dayPlanQty);     //白班计划量
            if(dayPlanQty > 0) {
                //计算计划开始称重时间
                produceTime = countStartTime(produceTime, schedule, preMaterialName, materialIntervalTime, EngineConstants.CLASS_DAY);  //计算出预计开始时间
                schedule.setDayExpectStartTime(produceTime);
                //计算计划完成时间
                long produceConsume = dayPlanQty.longValue() * schedule.getSingleCarTime();  //计算总消耗时间
                produceTime = addTimeMillis(produceTime, produceConsume);  //计划称重完成时间
                schedule.setDayExpectFinishTime(produceTime);   //白班预计完成时间
                schedule.setDayProduceOrder(order * EngineConstants.ORDER_MULTIPLE);  //生产顺序
                order++;
            }
        }
    }

    /**
     * 计算中班的计划量、预计完成时间、顺序等；并且根据机台剩余产能，以及在保证能按时给“终炼母炼”供料的基础上，把夜班、白班计划量 合并到中班
     * @param scheduleGroupList
     * @param materialIntervalTime
     */
    private void midDayClass(List<MaterialScheduleResultVo> scheduleGroupList, int materialIntervalTime, MaterialLastClassVo dayLatestClass) {
        Date produceTime = null;
        String preMaterialName = "";  //上一个物料名称
        int order = 1; //顺序
        if (dayLatestClass != null) {
        	preMaterialName = dayLatestClass.getMaterialName();
        	order = dayLatestClass.getProduceOrder();
        }
        for(int i=0;i<scheduleGroupList.size();i++) {
            MaterialScheduleResultVo schedule = scheduleGroupList.get(i);
            Date endTimeMidClass = schedule.getMidClassEndTime();  //中班结束的时间
            Date glueStartTime = schedule.getGlueMidExpectStartTime(); //终炼母炼计划中班开始生产时间
            glueStartTime = (glueStartTime == null ? endTimeMidClass : glueStartTime);
            Double midPlanQty = schedule.getMidPlanQty();   //中班计划量
            Double nightPlanQty = schedule.getNightPlanQty();  //夜班计划量
            Double dayPlanQty = schedule.getDayPlanQty();   //白班计划量
            long singleCarTime = schedule.getSingleCarTime();  //单车称重消耗时间（毫秒）

            long produceTimeMillis = 0;
            produceTime = countStartTime(produceTime, schedule, preMaterialName, materialIntervalTime, EngineConstants.CLASS_MID);  //计算出预计开始时间
            schedule.setMidExpectStartTime(produceTime);
            if(midPlanQty > 0) {
                produceTimeMillis = midPlanQty.longValue() * singleCarTime;  //中班全部称重完需要消耗的时间（毫秒）
                produceTime = addTimeMillis(produceTime, produceTimeMillis);  //计算出中班硫磺辅料称重完毕后的时间
                schedule.setMidExpectFinishTime(produceTime);  //设置计划完成时间
            }

            produceTimeMillis = nightPlanQty.longValue() * singleCarTime;  //夜班全部称重完需要消耗的时间（毫秒）
            Date middleTime = addTimeMillis(produceTime, produceTimeMillis);
            if(nightPlanQty > 0 && glueStartTime.after(middleTime) && endTimeMidClass.after(middleTime)) {
                //如果夜班计划量大于0，则判断如果把夜班的计划放到中班后，是否会有超时风险，并且完成时间是否会超过中班结束时间（无法准时给终炼母炼提供硫磺辅料）；
                //如果不会则把夜班计划量移到中班一起称重
                midPlanQty = BigDecimalUtil.add(midPlanQty, nightPlanQty);
                schedule.setMidPlanQty(midPlanQty);

                nightPlanQty = 0D;
                schedule.setNightPlanQty(nightPlanQty);
                schedule.setGlueNightExpectStartTime(null);
                produceTime = middleTime;
                schedule.setMidExpectFinishTime(produceTime);  //设置计划完成时间
            }

            produceTimeMillis = dayPlanQty.longValue() * singleCarTime;  //白班全部称重完需要消耗的时间（毫秒）
            middleTime = addTimeMillis(produceTime, produceTimeMillis);
            if(dayPlanQty > 0 && glueStartTime.after(middleTime) && endTimeMidClass.after(middleTime)) {
                //如果白班计划量大于0，则判断如果把白班的计划放到中班后，是否会有超时风险，并且完成时间是否会超过中班结束时间（无法准时给终炼母炼提供硫磺辅料）；
                //如果不会则把白班计划量移到中班一起称重
                midPlanQty = BigDecimalUtil.add(midPlanQty, dayPlanQty);
                schedule.setMidPlanQty(midPlanQty);

                dayPlanQty = 0D;
                schedule.setDayPlanQty(dayPlanQty);
                schedule.setGlueDayExpectStartTime(null);
                produceTime = middleTime;
                schedule.setMidExpectFinishTime(produceTime);  //设置计划完成时间
            }

            if(midPlanQty > 0) {
                schedule.setMidProduceOrder(order * EngineConstants.ORDER_MULTIPLE);  //设置中班生产顺序
                order++;
            } else {
                schedule.setMidExpectStartTime(null);
            }
            preMaterialName = schedule.getMaterialName();
        }
    }

    /**
     * 计算夜班的计划量、预计完成时间、顺序等；并且根据机台剩余产能，以及在保证能按时给“终炼母炼”供料的基础上，把白班计划量 合并到中班
     * @param scheduleGroupList
     * @param materialIntervalTime
     */
    private void nightDayClass(List<MaterialScheduleResultVo> scheduleGroupList, int materialIntervalTime, MaterialLastClassVo dayLatestClass) {
        //同一个机台下的排产按夜班需求计划时间排程，最后在按白班需求计划时间排序
        scheduleGroupList = scheduleGroupList.stream().sorted(this.compareDataSource()
        		.thenComparing(MaterialScheduleResultVo::getGlueNightExpectStartTime, Comparator.nullsLast(Date::compareTo))
                .thenComparing(MaterialScheduleResultVo::getGlueDayExpectStartTime, Comparator.nullsLast(Date::compareTo))).collect(Collectors.toList());

        Date produceTime = null;
        String preMaterialName = "";  //上一个物料名称
        int order = 1; //顺序
        if (dayLatestClass != null) {
        	preMaterialName = dayLatestClass.getMaterialName();
        	order = dayLatestClass.getProduceOrder();
        }
        for(MaterialScheduleResultVo schedule : scheduleGroupList) {
            Date endTimeNightClass = schedule.getNightClassEndTime();  //夜班结束的时间
            Date glueStartTime = schedule.getGlueNightExpectStartTime(); //终炼母炼计划夜班开始生产时间
            glueStartTime = (glueStartTime == null ? endTimeNightClass : glueStartTime);
            Double nightPlanQty = schedule.getNightPlanQty();  //夜班计划量
            Double dayPlanQty = schedule.getDayPlanQty();   //白班计划量
            long singleCarTime = schedule.getSingleCarTime();  //单车称重消耗时间（毫秒）

            long produceTimeMillis = 0;
            produceTime = countStartTime(produceTime, schedule, preMaterialName, materialIntervalTime, EngineConstants.CLASS_NIGHT);  //计算出预计开始时间
            schedule.setNightExpectStartTime(produceTime);
            if(nightPlanQty > 0) {
                produceTimeMillis = nightPlanQty.longValue() * singleCarTime;  //夜班全部称重完需要消耗的时间（毫秒）
                produceTime = addTimeMillis(produceTime, produceTimeMillis);  //计算出夜班硫磺辅料称重完毕后的时间
                schedule.setNightExpectFinishTime(produceTime);  //设置计划完成时间
            }

            produceTimeMillis = dayPlanQty.longValue() * singleCarTime;  //白班全部称重完需要消耗的时间（毫秒）
            Date middleTime = addTimeMillis(produceTime, produceTimeMillis);
            if(dayPlanQty > 0 && glueStartTime.after(middleTime) && endTimeNightClass.after(middleTime)) {
                //如果白班计划量大于0，则判断如果把白班的计划放到夜班后，是否会有超时风险，并且完成时间是否会超过夜班结束时间（无法准时给终炼母炼提供硫磺辅料）；
                //如果不会则把白班计划量移到夜班一起称重
                nightPlanQty = BigDecimalUtil.add(nightPlanQty, dayPlanQty);
                schedule.setNightPlanQty(nightPlanQty);

                dayPlanQty = 0D;
                schedule.setDayPlanQty(dayPlanQty);
                schedule.setGlueDayExpectStartTime(null);
                produceTime = middleTime;
                schedule.setNightExpectFinishTime(produceTime);  //设置计划完成时间
            }

            if(nightPlanQty > 0 ) {
                schedule.setNightProduceOrder(order * EngineConstants.ORDER_MULTIPLE);  //设置夜班生产顺序
                order++;
            } else {
                schedule.setNightExpectStartTime(null);
            }
            preMaterialName = schedule.getMaterialName();
        }
    }

    /**
     * 三班制的情况下：计算白班的计划量、预计完成时间、顺序等；
     * @param scheduleGroupList
     * @param materialIntervalTime
     */
    private void dayThreeDayClass(List<MaterialScheduleResultVo> scheduleGroupList, int materialIntervalTime, MaterialLastClassVo dayLatestClass) {
        //同一个机台下的排产按白班需求计划时间排程
        scheduleGroupList = scheduleGroupList.stream().sorted(this.compareDataSource()
        		.thenComparing(MaterialScheduleResultVo::getGlueDayExpectStartTime, Comparator.nullsLast(Date::compareTo))).collect(Collectors.toList());

        Date produceTime = null;
        String preMaterialName = "";  //上一个物料名称
        int order = 1; //顺序
        if (dayLatestClass != null) {
        	preMaterialName = dayLatestClass.getMaterialName();
        	order = dayLatestClass.getProduceOrder();
        }
        for(MaterialScheduleResultVo schedule : scheduleGroupList) {
            Double dayPlanQty = schedule.getDayPlanQty();   //白班计划量
            long singleCarTime = schedule.getSingleCarTime();  //单车称重消耗时间（毫秒）

            long produceTimeMillis = 0;
            if(dayPlanQty > 0) {
                produceTime = countStartTime(produceTime, schedule, preMaterialName, materialIntervalTime, EngineConstants.CLASS_DAY);  //计算出预计开始时间
                schedule.setDayExpectStartTime(produceTime);

                produceTimeMillis = dayPlanQty.longValue() * singleCarTime;  //白班全部称重完需要消耗的时间（毫秒）
                produceTime = addTimeMillis(produceTime, produceTimeMillis);  //计算出白班硫磺辅料称重完毕后的时间
                schedule.setDayExpectFinishTime(produceTime);  //设置计划完成时间

                schedule.setDayProduceOrder(order * EngineConstants.ORDER_MULTIPLE);  //设置白班生产顺序
                order++;
            }
            preMaterialName = schedule.getMaterialName();
        }
    }

    /**
     * 两部制的情况下：把白班计划量不为0的记录，全部移到中班最后称重；如果中班产能已经满了，则全部移到夜班最后称重
     * @param scheduleGroupList
     * @param materialIntervalTime
     */
    private void dayTwoDayClass(List<MaterialScheduleResultVo> scheduleGroupList, int materialIntervalTime) {
        //过滤出白班计划量大于0的记录，在按时间升序排
        List<MaterialScheduleResultVo> list = scheduleGroupList.stream().filter(r->r.getDayPlanQty() > 0).sorted(Comparator.comparing(MaterialScheduleResultVo::getGlueDayExpectStartTime)).collect(Collectors.toList());
        if(scheduleGroupList.isEmpty()) {
            return;
        }

        MaterialLastClassVo midLastClass =  this.maxMidClassInfo(scheduleGroupList); //获取中班的 生产顺序值最大的 排程信息
        MaterialLastClassVo nightLastClass =  this.maxNightClassInfo(scheduleGroupList);  //获取夜班的 生产顺序值最大的 排程信息
        for(MaterialScheduleResultVo schedule : list) {
            Double dayPlanQty = schedule.getDayPlanQty();   //白班计划量
            long singleCarTime = schedule.getSingleCarTime();  //单车称重消耗时间（毫秒）
            String[] demandPlanning = schedule.getDemandPlanning().split(",");  //需求计划
            String dayDemandPlanning = demandPlanning[demandPlanning.length-1];  //解析出胶料白班的需求计划
            schedule.setDemandPlanning(dayDemandPlanning);  //设置需求计划
            schedule.setTotalPlanQty(dayPlanQty);   //设置总计划量

            Date endTimeMidClass = schedule.getMidClassEndTime();  //白班结束的时间

            long produceTimeMillis = 0;
            String preMaterialName = midLastClass.getMaterialName();  //上一个物料名称
            Date produceTime = midLastClass.getExpectFinishTime();   //中班里面最大的预计完成时间
            produceTime = countStartTime(midLastClass.getExpectFinishTime(), schedule, preMaterialName, materialIntervalTime, EngineConstants.CLASS_MID);  //计算出预计开始时间
            produceTimeMillis = dayPlanQty.longValue() * singleCarTime;  //白班全部称重完需要消耗的时间（毫秒）
            Date middleTime = addTimeMillis(produceTime, produceTimeMillis);
            if(endTimeMidClass.after(middleTime)) {
                //把白班的计划量挪到中班后，完成时间不会超过中班结束时间，则可以白班的计划挪到中班
                int order = midLastClass.getProduceOrder() + EngineConstants.ORDER_MULTIPLE;
                schedule.setMidPlanQty(dayPlanQty);
                schedule.setMidProduceOrder(order);
                schedule.setMidExpectStartTime(produceTime);
                schedule.setMidExpectFinishTime(middleTime);

                //重新设置中班排在最后的排程信息
                midLastClass.setMaterialName(schedule.getMaterialName());
                midLastClass.setProduceOrder(order);
                midLastClass.setExpectFinishTime(middleTime);

                //重置夜班、白班的字段
                schedule.setNightPlanQty(0D);
                schedule.setNightProduceOrder(null);
                schedule.setNightExpectStartTime(null);
                schedule.setNightExpectFinishTime(null);
                schedule.setNightRemark(null);
                schedule.setDayPlanQty(0D);
                schedule.setDayProduceOrder(null);
                schedule.setDayExpectStartTime(null);
                schedule.setDayExpectFinishTime(null);
                schedule.setDayRemark(null);
            } else {
                //如果机台在中班已经排满，则白班的计划放到夜班去
                int order = nightLastClass.getProduceOrder() + EngineConstants.ORDER_MULTIPLE;
                schedule.setNightPlanQty(dayPlanQty);
                schedule.setNightProduceOrder(order);

                produceTime = nightLastClass.getExpectFinishTime();   //夜班里面最大的预计完成时间
                produceTime = countStartTime(produceTime, schedule, preMaterialName, materialIntervalTime, EngineConstants.CLASS_MID);  //计算出预计开始时间
                Date nightTime = addTimeMillis(produceTime, produceTimeMillis);
                schedule.setNightExpectStartTime(produceTime);
                schedule.setNightExpectFinishTime(nightTime);

                //重新设置中班排在最后的排程信息
                nightLastClass.setMaterialName(schedule.getMaterialName());
                nightLastClass.setProduceOrder(order);
                nightLastClass.setExpectFinishTime(nightTime);

                //重置中班、白班的字段
                schedule.setMidPlanQty(0D);
                schedule.setMidProduceOrder(null);
                schedule.setMidExpectStartTime(null);
                schedule.setMidExpectFinishTime(null);
                schedule.setMidRemark(null);
                schedule.setDayPlanQty(0D);
                schedule.setDayProduceOrder(null);
                schedule.setDayExpectStartTime(null);
                schedule.setDayExpectFinishTime(null);
                schedule.setDayRemark(null);
            }
            scheduleGroupList.add(schedule);
        }
    }

    /**
     * 创建常用规格安全库存的排产记录。（常用规格保持每天用量的{safeStockRate}的安全库存。    ）
     * @param safeStockScheduleList 安全库存列表
     * @param scheduleList	排产列表
     * @param machineMap 机台列表
     * @param materialIntervalTime 不同胶料的间隔时间
     */
    public void createSafeStockSchedule(List<MaterialScheduleResultVo> safeStockScheduleList, List<MaterialScheduleResultVo> scheduleList, Map<String, LhflMachine> machineMap, int materialIntervalTime) {
        if(CollectionUtil.isEmpty(safeStockScheduleList)) {
            return;
        }
        Map<String, MaterialScheduleResultVo> scheduleMap = scheduleList.stream().collect(Collectors.toMap(MaterialScheduleResultVo::getMaterialName, Function.identity(), (r1, r2) -> r1));
        Map<String, Double> materialTotalDemandMap = this.mapMaterialTotalDemand(scheduleList);  //计算出物料对应的总需求了
        Map<String, MaterialLastClassVo> lastClassMaxInfoMap = this.mapLastClassMaxInfo(safeStockScheduleList, scheduleList, machineMap);  //获取排程最后一班排序最大的排班信息
        List<MaterialScheduleResultVo> safeStockList = new ArrayList<>();
        for(MaterialScheduleResultVo schedule: safeStockScheduleList) {
            String materialName = schedule.getMaterialName();
            Double safeStockQty = schedule.getSafeStockQty(); // 安全库存
            if (StringUtil.isEmpty(schedule.getMachineCode())) {
            	continue; // 没有排上机台的也没有办法排安全库存
            }
            Double demandQty = materialTotalDemandMap.getOrDefault(materialName, 0D);  //总需求量
            Double stockQty = schedule.getStockQty(); // 库存
            Double safeStock = stockQty > demandQty? safeStockQty - (stockQty - demandQty): safeStockQty; // 实际应生产的安全库存 
            if (safeStock <= 0) {
            	continue;
            }
            MaterialScheduleResultVo orgSchedule = scheduleMap.getOrDefault(materialName, schedule); // 优先以本次有排的物料信息为准；没有则以安全库存的为准
            MaterialScheduleResultVo safeStockSchedule = this.buildBaseSchedule(orgSchedule);
    		safeStockSchedule.setDemandQty(safeStock);// 临时存放实际需要排产的安全库存量
    		safeStockSchedule.setSafeStockQty(safeStockQty);
			safeStockSchedule.setStockQty(stockQty);
            safeStockList.add(safeStockSchedule); // 构建安全库存排程记录
        }
		String safeStockRemark = I18nUtil.getMessage("engine.material.safe.stock.remark"); // 排产班次的备注信息
        this.insertScheduleAtLastClass(safeStockList, scheduleList, lastClassMaxInfoMap, materialIntervalTime, safeStockRemark);// 将安全库存记录插入到当前排产列表的最后一班
    }
    
    /**
     * 将待排记录插入到当前排产列表的最后一班
     * @param baseScheduleList	待排记录
     * @param scheduleList	本次已排记录
     * @param lastClassMaxInfoMap	各机台最后一班排产请开给你
     * @param materialIntervalTime	不同胶料的间隔时间
     */
	private void insertScheduleAtLastClass(List<MaterialScheduleResultVo> baseScheduleList,
			List<MaterialScheduleResultVo> scheduleList, Map<String, MaterialLastClassVo> lastClassMaxInfoMap,
			int materialIntervalTime, String remark) {
		Map<String, List<MaterialScheduleResultVo>> spanReceiveScheduleMap = baseScheduleList.stream()
				.collect(Collectors.groupingBy(MaterialScheduleResultVo::getMachineCode));
		for (Map.Entry<String, List<MaterialScheduleResultVo>> entry : spanReceiveScheduleMap.entrySet()) {
			String machineCode = entry.getKey();
			List<MaterialScheduleResultVo> spanReceiveGroupingList = entry.getValue();
			MaterialLastClassVo lastClass = lastClassMaxInfoMap.get(machineCode); // 获取机台下的最后一班生产顺序最大的那条排程白班信息
			if (lastClass == null) {
				continue;
			}
			List<MaterialScheduleResultVo> scheduleGroupList = spanReceiveGroupingList.stream()
					.sorted(Comparator.comparing(MaterialScheduleResultVo::getDemandQty, Comparator.reverseOrder()))
					.collect(Collectors.toList()); // 按待排产量（安全库存）排序（倒序）

			// 通过剩余时长筛选可排计划
			while (CollectionUtils.isNotEmpty(scheduleGroupList)) {
				MaterialScheduleResultVo safeStockSchedule = this.getCurrentSchedule(scheduleGroupList, lastClass,
						materialIntervalTime, remark);
				if (safeStockSchedule != null && safeStockSchedule.getDemandQty() <= 0) { // 只有安全库存全部排完的才算处理完成
					for (int i = scheduleGroupList.size() - 1; i >= 0; i--) {
						if (scheduleGroupList.get(i) == safeStockSchedule) {
							scheduleGroupList.remove(i); // 从待排列表中移除该排程记录
						}
					}
					scheduleList.add(safeStockSchedule); // 把常用规格的安全库存排程加入到排程计划中
				}
			}
		}
	}
    
	/**
	 * 重新构建库存基础排程记录
	 * 
	 * @param orgSchedule  来源排程记录
	 * @return
	 */
	private MaterialScheduleResultVo buildBaseSchedule(MaterialScheduleResultVo orgSchedule) {
		MaterialScheduleResultVo baseSchedule = new MaterialScheduleResultVo();
		BeanUtils.copyProperties(orgSchedule, baseSchedule);
		baseSchedule.setId(null);
		// 排程属性初始化
		this.resetPlanField(baseSchedule);
		return baseSchedule;
	}

	/**
	 * 重置排程计划相关字段
	 * @param baseSchedule
	 */
	private void resetPlanField(MaterialScheduleResultVo baseSchedule) {
		baseSchedule.setDemandPlanning(null);
		baseSchedule.setMidProduceOrder(null);
		baseSchedule.setMidPlanQty(0D);
		baseSchedule.setMidExpectStartTime(null);
		baseSchedule.setMidExpectFinishTime(null);
		baseSchedule.setMidRemark(null);
		baseSchedule.setGlueMidPlanQty(0D);
		baseSchedule.setGlueMidExpectStartTime(null);
		baseSchedule.setNightProduceOrder(null);
		baseSchedule.setNightPlanQty(0D);
		baseSchedule.setNightExpectStartTime(null);
		baseSchedule.setNightExpectFinishTime(null);
		baseSchedule.setNightRemark(null);
		baseSchedule.setGlueNightPlanQty(0D);
		baseSchedule.setGlueNightExpectStartTime(null);
		baseSchedule.setDayProduceOrder(null);
		baseSchedule.setDayPlanQty(0D);
		baseSchedule.setTotalPlanQty(0D);
		baseSchedule.setDayExpectStartTime(null);
		baseSchedule.setDayExpectFinishTime(null);
		baseSchedule.setDayRemark(null);
		baseSchedule.setGlueDayPlanQty(0D);
		baseSchedule.setGlueDayExpectStartTime(null);
	}

    /**
     * 获取本次排产计划
     * @param scheduleList	排程列表
     * @param lastClass	机台上一个排程的信息
     * @param materialIntervalTime	切换物料的间隔时间
     * @param scheduleRemark	备注信息
     * @return
     */
	private MaterialScheduleResultVo getCurrentSchedule(List<MaterialScheduleResultVo> scheduleList,
			MaterialLastClassVo lastClass, int materialIntervalTime, String scheduleRemark) {
		MaterialScheduleResultVo currentSchedule = null;
		String preMaterialName = lastClass.getMaterialName(); //上一个物料名称
		MaterialScheduleResultVo firstSchedule = CollectionUtil.firstElement(scheduleList);
		Date produceTime = lastClass.getExpectFinishTime() != null? lastClass.getExpectFinishTime(): lastClass.getClassStartTime(); // 上一个物料的生产时间
		String classType = lastClass.getClassType();  // 最晚一笔规格的排产班次
		Integer classShift = lastClass.getClassShift(); // 班制
		long surplusTime = 0; // 剩余时长
		int order = Optional.ofNullable(lastClass.getProduceOrder()).orElse(0);  //机台下当班最大的生产顺序
		order = order + EngineConstants.ORDER_MULTIPLE; // 本次排产的顺序为上次加10
		boolean isProductAll = false;
		for (MaterialScheduleResultVo schedule: scheduleList) {
		    Double demandQty = schedule.getDemandQty();
		    long produceTimeMillis = demandQty.longValue() * schedule.getSingleCarTime(); // 排产时间
			long intervalTime = this.getMaterialIntervalTime(schedule.getMaterialName(), preMaterialName, materialIntervalTime);
		    surplusTime = lastClass.getClassEndTime().getTime() - produceTime.getTime() - intervalTime; // 重算剩余时长，如果有换物料，需要再扣除切换时间
			if (surplusTime >= produceTimeMillis) { // 检查本班的剩余可排产时间是否足够排下完整的计划量
		    	currentSchedule = schedule;
		    	isProductAll = true;
		    	break;
			}
		}
		if (currentSchedule == null) {
			if (this.isLatestClass(classType, classShift, lastClass)) { // 如果是最后一班，要在当班排完所有的计划量
				isProductAll = true;
			} else { // 如果不是最后一班，则只排剩余产能可排的量
				long intervalTime = this.getMaterialIntervalTime(firstSchedule.getMaterialName(), preMaterialName, materialIntervalTime);
				long productOneTime = firstSchedule.getSingleCarTime() + intervalTime; // 生产1车的时长；如果有换物料，需要加上切换时间
			    surplusTime = lastClass.getClassEndTime().getTime() - produceTime.getTime() - intervalTime; // 重算剩余时长，如果有换物料，需要再扣除切换时间
				if (surplusTime < productOneTime) { // 如果剩余产能1车都排不上，直接将机台切换到下一个班别
				    this.switchClassType(lastClass, firstSchedule);
			        return null;
				}
			}
			currentSchedule = firstSchedule;
		}
		
		Double demandQty = currentSchedule.getDemandQty();	// 安全库存，即是本次排产的计划量
		long singleCarTime = currentSchedule.getSingleCarTime();  //单车称重消耗时间（毫秒）
		Double planQty;
		if (isProductAll) { // 产能充足，直接将未排的计划量全部排上
			planQty = demandQty;
		} else { // 产能不足，则只排剩余产能可排的量，= 剩余时间 / 每车所需时间，结果向下取整。
			planQty = BigDecimalUtil.valueOf(surplusTime).divide(BigDecimalUtil.valueOf(singleCarTime), 0, RoundingMode.DOWN).doubleValue();
		}
		long produceTimeMillis = planQty.longValue() * singleCarTime;
		Date planStartTime = countStartTime(produceTime, currentSchedule, preMaterialName, materialIntervalTime, classType);  //计算出预计开始时间
		Date planEndTime = addTimeMillis(planStartTime, produceTimeMillis);  //计算出中班硫磺辅料称重完毕后的时间
		
		if (EngineConstants.CLASS_MID.equals(classType)) {
		    currentSchedule.setMidPlanQty(planQty);
		    currentSchedule.setMidProduceOrder(order);
		    currentSchedule.setMidExpectStartTime(planStartTime);
		    currentSchedule.setMidExpectFinishTime(planEndTime);
		    currentSchedule.setMidRemark(scheduleRemark);
		} else if(EngineConstants.CLASS_NIGHT.equals(classType)) {
		    currentSchedule.setNightPlanQty(planQty);
		    currentSchedule.setNightProduceOrder(order);
		    currentSchedule.setNightExpectStartTime(planStartTime);
		    currentSchedule.setNightExpectFinishTime(planEndTime);
		    currentSchedule.setNightRemark(scheduleRemark);
		} else {
		    currentSchedule.setDayPlanQty(planQty);
		    currentSchedule.setDayProduceOrder(order);
		    currentSchedule.setDayExpectStartTime(planStartTime);
		    currentSchedule.setDayExpectFinishTime(planEndTime);
		    currentSchedule.setDayRemark(scheduleRemark);
		}
		currentSchedule.setDemandQty(BigDecimalUtil.sub(currentSchedule.getDemandQty(), planQty));
		currentSchedule.setTotalPlanQty(BigDecimalUtil.add(currentSchedule.getMidPlanQty(),
				currentSchedule.getNightPlanQty(), currentSchedule.getDayPlanQty())); // 总计划量
		currentSchedule.setRemark(scheduleRemark);
		
		// 重新设置此机台下的最后一班排产信息
		lastClass.setMaterialName(currentSchedule.getMaterialName());
		lastClass.setExpectFinishTime(planEndTime);
		lastClass.setProduceOrder(order);
		
		return currentSchedule;
	}

	/**
	 * 根据物料判断是否有切换时间
	 * @param materialName	本次排产物料名
	 * @param preMaterialName	上次排产物料名
	 * @param materialIntervalTime	间隔时间
	 * @return
	 */
	private int getMaterialIntervalTime(String materialName, String preMaterialName, int materialIntervalTime) {
		return Objects.equals(materialName, preMaterialName)? 0: materialIntervalTime * 60 * 1000;
	}

	/**
	 * 切换最后一班排产信息的班次
	 * @param lastClass	最后一班排产信息
	 * @param scheduleResult 排产信息，主要获取班次开始/结束时间
	 * @return 是否有下一班
	 */
	private boolean switchClassType(MaterialLastClassVo lastClass, MaterialScheduleResultVo scheduleResult) {
		String classType = lastClass.getClassType();  // 最晚一笔规格的排产班次
		Integer classShift = lastClass.getClassShift(); // 班制
		String nextClassType = this.getNextClass(classType, classShift, lastClass); // 下一个班次
		if (EngineConstants.CLASS_NIGHT.equals(nextClassType)) { // 切换至夜班
		    lastClass.setClassType(nextClassType);
		    lastClass.setExpectFinishTime(null);
		    lastClass.setProduceOrder(null);
		    lastClass.setClassStartTime(scheduleResult.getNightClassStartTime());
		    lastClass.setClassEndTime(scheduleResult.getNightClassEndTime());
		} else if (EngineConstants.CLASS_DAY.equals(nextClassType)) { // 切换至白班
		    lastClass.setClassType(EngineConstants.CLASS_DAY);
		    lastClass.setExpectFinishTime(null);
		    lastClass.setProduceOrder(null);
		    lastClass.setClassStartTime(scheduleResult.getDayClassStartTime());
		    lastClass.setClassEndTime(scheduleResult.getDayClassEndTime());
		}
		
		return true;
	}
	
	/**
	 * 判断是否最后一班
	 * 
	 * @param classType  班次
	 * @param classShift 班制
	 * @param lastClass  最后一个排程信息
	 * @return
	 */
	private boolean isLatestClass(String classType, Integer classShift, MaterialLastClassVo lastClass) {
		return getNextClass(classType, classShift, lastClass) == null;
	}

	/**
	 * 获取当班的下一个班次，如果是最后一班，则直接返回空
	 * 
	 * @param classType  当班班次
	 * @param classShift 班制
	 * @param lastClass  最后一个排程信息
	 * @return
	 */
	private String getNextClass(String classType, Integer classShift, MaterialLastClassVo lastClass) {
		if (EngineConstants.CLASS_MID.equals(classType)) { // 原先是中班则切换至夜班（三班制或者两班制）
			if (!lastClass.isMachineNightEnable()) { // 如果夜班不可用，则判断是三班还是两班
				if (classShift == EngineConstants.CLASS_SHIFT_TWO || !lastClass.isMachineDayEnable()) { // 三班制，或者白班补可用，则说明中班就是最后一班
					return null;
				}
				if (lastClass.isMachineDayEnable()) { // 三班制，夜班不可用但白班可用，则返回白班
					return EngineConstants.CLASS_DAY;
				}
			}
			return EngineConstants.CLASS_NIGHT;
		} else if (EngineConstants.CLASS_NIGHT.equals(classType) && classShift == EngineConstants.CLASS_SHIFT_THREE) { // 原先是夜班则切换至白班（三班制）
			if (!lastClass.isMachineDayEnable()) { // 如果白班不可用，则说明夜班是最后一班
				return null;
			}
			return EngineConstants.CLASS_DAY;
		}
		return null;
	}

    /**
     * 获取最后一班的 生产顺序值最大的 排程信息
     * @param baseScheduleList	待添加排产记录
     * @param scheduleList	已排产记录
     * @return
     */
    private Map<String, MaterialLastClassVo> mapLastClassMaxInfo(List<MaterialScheduleResultVo> baseScheduleList, List<MaterialScheduleResultVo> scheduleList, Map<String, LhflMachine> machineMap) {
    	// 合并安全库存排产记录与已排记录
    	List<MaterialScheduleResultVo> totalScheduleList = new ArrayList<>(baseScheduleList);
    	totalScheduleList.addAll(scheduleList);
        Map<String, MaterialLastClassVo> map = new HashMap<>();
        Map<String, List<MaterialScheduleResultVo>> machineScheduleMap = totalScheduleList.stream().collect(Collectors.groupingBy(r -> r.getMachineCode()));
        
        // 分别找出每个排程涉及机台的最后一个规格排产状况，没有排产的从第一班开始
        outter:
        for(Map.Entry<String,  List<MaterialScheduleResultVo>> entry : machineScheduleMap.entrySet()) {
        	String machineCode = entry.getKey(); // 机台编号
        	LhflMachine machine = machineMap.get(machineCode);
        	if (machine == null) {
        		continue;
        	}
            List<MaterialScheduleResultVo> list = entry.getValue();
            MaterialScheduleResultVo schedule = CollectionUtil.firstElement(list);
            Integer classShift = machine.getClassShift();
            
            Date expectFinishTime = null; // 上一个规格预计完成时间
            Integer produceOrder = null; // 上一个规格排产顺序
            String materialName = null; // 上一个规格物料名称
            
            // 判断各班机台是否启用
            boolean isMidEnable = Optional.ofNullable(machine).map(r -> ZltConstant.STATUS_ENABLE.equals(r.getMidStatus())).orElse(true);
            boolean isNightEnable = Optional.ofNullable(machine).map(r -> ZltConstant.STATUS_ENABLE.equals(r.getNightStatus())).orElse(true);
            boolean isDayEnable = Optional.ofNullable(machine).map(r -> ZltConstant.STATUS_ENABLE.equals(r.getDayStatus())).orElse(true);
            // 根据班制再对班次可用判断
            isMidEnable = isMidEnable && !Objects.equals(classShift, EngineConstants.CLASS_SHIFT_ONE); // 中班在长白班不可用
            isNightEnable = isNightEnable && !Objects.equals(classShift, EngineConstants.CLASS_SHIFT_ONE); // 夜班在长白班不可用
            isDayEnable = isDayEnable && !Objects.equals(classShift, EngineConstants.CLASS_SHIFT_TWO); // 白班在二班制不可用
            
            // 获取机台最后一个规格的排产记录
            MaterialScheduleResultVo lastSchedule = list.stream().filter(s -> s.getDayProduceOrder() != null)
            		.max(Comparator.comparing(MaterialScheduleResultVo::getDayProduceOrder)).orElse(null); // 先取中班的最晚的一笔
            String classType = EngineConstants.CLASS_DAY; // 最后一笔排产的班别
            Date classStartTime = schedule.getDayClassStartTime(); // 班次开始时间
            Date classEndTime = schedule.getDayClassEndTime(); // 班次结束时间
            inner: { // 查找排产记录内部代码块
	            if (lastSchedule != null && isDayEnable) {
	                expectFinishTime = lastSchedule.getDayExpectFinishTime();
	                produceOrder = lastSchedule.getDayProduceOrder();
	                materialName = lastSchedule.getMaterialName();
	                break inner; // 找到后跳出查找代码块
	            } else { // 取不到白班则取夜班最晚一笔
	            	lastSchedule = list.stream().filter(s -> s.getNightProduceOrder() != null)
	            			.max(Comparator.comparing(MaterialScheduleResultVo::getNightProduceOrder)).orElse(null);
	                classType = EngineConstants.CLASS_NIGHT;
	                classStartTime = schedule.getNightClassStartTime();
	                classEndTime = schedule.getNightClassEndTime();
	            }
	            
	            if (lastSchedule != null && isNightEnable) {
	                expectFinishTime = lastSchedule.getNightExpectFinishTime();
	                produceOrder = lastSchedule.getNightProduceOrder();
	                materialName = lastSchedule.getMaterialName();
	                break inner;
	            } else { // 取不到夜班则取中班最晚一笔
	            	lastSchedule = list.stream().filter(s -> s.getMidProduceOrder() != null)
	            			.max(Comparator.comparing(MaterialScheduleResultVo::getMidProduceOrder)).orElse(null);
	                classType = EngineConstants.CLASS_MID;
	                classStartTime = schedule.getMidClassStartTime();
	                classEndTime = schedule.getMidClassEndTime();
	            }
	            
	            if (lastSchedule != null && isMidEnable) {
	                expectFinishTime = lastSchedule.getMidExpectFinishTime();
	                produceOrder = lastSchedule.getMidProduceOrder();
	                materialName = lastSchedule.getMaterialName();
	            } else {
	            	continue outter;
	            }
            }
            
            MaterialLastClassVo lastClassVo = new MaterialLastClassVo(materialName, produceOrder, expectFinishTime);
            lastClassVo.setClassType(classType);
            lastClassVo.setClassStartTime(classStartTime);
            lastClassVo.setClassEndTime(classEndTime);
            lastClassVo.setClassShift(classShift);
            lastClassVo.setMachineMidEnable(isMidEnable);
            lastClassVo.setMachineNightEnable(isNightEnable);
            lastClassVo.setMachineDayEnable(isDayEnable);
            map.put(machineCode, lastClassVo);
        }
        return map;
    }

    /**
     * 获取中班的 生产顺序值最大的 排程信息
     * @param scheduleList
     * @return
     */
    private MaterialLastClassVo maxMidClassInfo(List<MaterialScheduleResultVo> scheduleList) {
        scheduleList = scheduleList.stream().sorted(Comparator.comparing(MaterialScheduleResultVo::getMidProduceOrder, Comparator.nullsFirst(Integer::compareTo)).reversed()).collect(Collectors.toList());
        MaterialScheduleResultVo schedule = scheduleList.get(0);
        int order = schedule.getMidProduceOrder() == null ? 0 : schedule.getMidProduceOrder();
        Date midExpectFinishTime = schedule.getMidExpectFinishTime() == null ? schedule.getMidClassStartTime() : schedule.getMidExpectFinishTime();
        MaterialLastClassVo lastClassVo = new MaterialLastClassVo(schedule.getMaterialName(), order, midExpectFinishTime);
        return lastClassVo;
    }

    /**
     * 获取夜班的 生产顺序值最大的 排程信息
     * @param scheduleList
     * @return
     */
    private MaterialLastClassVo maxNightClassInfo(List<MaterialScheduleResultVo> scheduleList) {
        scheduleList = scheduleList.stream().sorted(Comparator.comparing(MaterialScheduleResultVo::getNightProduceOrder, Comparator.nullsFirst(Integer::compareTo)).reversed()).collect(Collectors.toList());
        MaterialScheduleResultVo schedule = scheduleList.get(0);
        int order = schedule.getNightProduceOrder() == null ? 0 : schedule.getNightProduceOrder();
        Date midExpectFinishTime = schedule.getNightExpectFinishTime() == null ? schedule.getNightClassStartTime() : schedule.getNightExpectFinishTime();
        MaterialLastClassVo lastClassVo = new MaterialLastClassVo(schedule.getMaterialName(), order, midExpectFinishTime);
        return lastClassVo;
    }

    /**
     * 计算出物料对应的总需求量
     * @param scheduleList
     * @return
     */
    private Map<String, Double> mapMaterialTotalDemand(List<MaterialScheduleResultVo> scheduleList) {
        Map<String, Double> map = new HashMap<>();
        for(MaterialScheduleResultVo schedule : scheduleList) {
            Double totalDemandPlan = map.getOrDefault(schedule.getMaterialName(), 0D);
            totalDemandPlan = BigDecimalUtil.add(totalDemandPlan, getDouble(schedule.getDemandQty()));
            map.put(schedule.getMaterialName(), totalDemandPlan);
        }
        return map;
    }

    /**
     * 修改了中班生产顺序后，这个机台下的全部排程的计划完成时间都需要重新计算
     * @param scheduleList
     * @param materialIntervalTime
     */
    public void modifyMidProduceOrder(List<MaterialScheduleResultVo> scheduleList , int materialIntervalTime) {
        //按生产顺序排序
        scheduleList= scheduleList.stream().sorted(Comparator.comparing(MaterialScheduleResultVo::getMidProduceOrder, Comparator.nullsLast(Integer::compareTo))).collect(Collectors.toList());

        Date produceTime = null;
        String preMaterialName = "";  //上一个物料名称
        for(MaterialScheduleResultVo schedule : scheduleList) {
            Double midPlanQty = schedule.getMidPlanQty();  //中班计划量
            if(midPlanQty == null || midPlanQty == 0) {
                continue;
            }
            long singleCarTime = schedule.getSingleCarTime();  //单车称重消耗时间（毫秒）
            long produceTimeMillis = 0;

            produceTime = countStartTime(produceTime, schedule, preMaterialName, materialIntervalTime, EngineConstants.CLASS_MID);  //计算出预计开始时间
            schedule.setMidExpectStartTime(produceTime);

            produceTimeMillis = midPlanQty.longValue() * singleCarTime;  //全部称重完需要消耗的时间（毫秒）
            produceTime = addTimeMillis(produceTime, produceTimeMillis);  //计算出硫磺辅料称重完毕后的时间
            schedule.setMidExpectFinishTime(produceTime);  //设置计划完成时间
            preMaterialName = schedule.getMaterialName();
        }
    }

    /**
     * 修改了夜班生产顺序后，这个机台下的全部排程的计划完成时间都需要重新计算
     * @param scheduleList
     * @param materialIntervalTime
     */
    public void modifyNightProduceOrder(List<MaterialScheduleResultVo> scheduleList , int materialIntervalTime) {
        //按生产顺序排序
        scheduleList = scheduleList.stream().sorted(Comparator.comparing(MaterialScheduleResultVo::getNightProduceOrder, Comparator.nullsLast(Integer::compareTo))).collect(Collectors.toList());

        Date produceTime = null;
        String preMaterialName = "";  //上一个物料名称
        for(MaterialScheduleResultVo schedule : scheduleList) {
            Double nightPlanQty = schedule.getNightPlanQty();  //中班计划量
            if(nightPlanQty == null || nightPlanQty == 0) {
                continue;
            }
            long singleCarTime = schedule.getSingleCarTime();  //单车称重消耗时间（毫秒）
            long produceTimeMillis = 0;

            produceTime = countStartTime(produceTime, schedule, preMaterialName, materialIntervalTime, EngineConstants.CLASS_NIGHT);  //计算出预计开始时间
            schedule.setNightExpectStartTime(produceTime);

            produceTimeMillis = nightPlanQty.longValue() * singleCarTime;  //全部称重完需要消耗的时间（毫秒）
            produceTime = addTimeMillis(produceTime, produceTimeMillis);  //计算出硫磺辅料称重完毕后的时间
            schedule.setNightExpectFinishTime(produceTime);  //设置计划完成时间
            preMaterialName = schedule.getMaterialName();
        }
    }

    /**
     * 修改了白班生产顺序后，这个机台下的全部排程的计划完成时间都需要重新计算
     * @param scheduleList
     * @param materialIntervalTime
     */
    public void modifyDayProduceOrder(List<MaterialScheduleResultVo> scheduleList , int materialIntervalTime) {
        //按生产顺序排序
        scheduleList =scheduleList.stream().sorted(Comparator.comparing(MaterialScheduleResultVo::getDayProduceOrder, Comparator.nullsLast(Integer::compareTo))).collect(Collectors.toList());

        Date produceTime = null;
        String preMaterialName = "";  //上一个物料名称
        for(MaterialScheduleResultVo schedule : scheduleList) {
            Double dayPlanQty = schedule.getDayPlanQty();  //中班计划量
            if(dayPlanQty == null || dayPlanQty == 0) {
                continue;
            }
            long singleCarTime = schedule.getSingleCarTime();  //单车称重消耗时间（毫秒）
            long produceTimeMillis = 0;

            produceTime = countStartTime(produceTime, schedule, preMaterialName, materialIntervalTime, EngineConstants.CLASS_DAY);  //计算出预计开始时间
            schedule.setDayExpectStartTime(produceTime);

            produceTimeMillis = dayPlanQty.longValue() * singleCarTime;  //全部称重完需要消耗的时间（毫秒）
            produceTime = addTimeMillis(produceTime, produceTimeMillis);  //计算出硫磺辅料称重完毕后的时间
            schedule.setDayExpectFinishTime(produceTime);  //设置计划完成时间
            preMaterialName = schedule.getMaterialName();
        }
    }

    /**
     * 转机台后重新排序
     * @param scheduleList
     * @param materialIntervalTime
     */
    public void modifyMachine(List<MaterialScheduleResultVo> scheduleList , int materialIntervalTime) {
        //把排程记录按机台进行分组
        Map<String, List<MaterialScheduleResultVo>> scheduleMap = new HashMap<>();
        for(MaterialScheduleResultVo schedule : scheduleList) {
            List<MaterialScheduleResultVo> tempList = scheduleMap.getOrDefault(schedule.getMachineCode(),new ArrayList<>());
            tempList.add(schedule);
            scheduleMap.put(schedule.getMachineCode(), tempList);
        }
        scheduleList.clear();

        for(Map.Entry<String, List<MaterialScheduleResultVo>> entry : scheduleMap.entrySet()) {
            List<MaterialScheduleResultVo> scheduleGroupList = entry.getValue();
            this.modifyMidProduceOrder(scheduleGroupList, materialIntervalTime);
            this.modifyNightProduceOrder(scheduleGroupList, materialIntervalTime);
            this.modifyDayProduceOrder(scheduleGroupList, materialIntervalTime);
            scheduleList.addAll(scheduleList);
        }
    }

    /**
     * 转机台后默认把转机台的排产放到最后，并重新计算顺序和预计完成时间
     * @param schedule
     * @param maxSchedule
     * @param materialIntervalTime
     * @param oldClassShift  转机台前的机台的班制
     */
    public void retryMachine(MaterialScheduleResultVo schedule, MaterialScheduleResult maxSchedule, int materialIntervalTime, int oldClassShift) {
        transferClassShiftPlan(schedule, oldClassShift);  //转机台时，前后机台的班制不一样时，需要根据规则把计划量合并到新机台的班次中
        putScheduleLast(schedule, maxSchedule, materialIntervalTime);  //把各班的排产都放到机台最后
    }

    /**
     * 转机台时，前后机台的班制不一样时，需要根据规则把计划量合并到新机台的班次中
     * @param schedule
     * @param oldClassShift
     */
    public void transferClassShiftPlan(MaterialScheduleResultVo schedule, int oldClassShift) {
        Integer classShift = schedule.getClassShift();  //班制
        if(Objects.equals(classShift, EngineConstants.CLASS_SHIFT_ONE)) {
            //如果转入的机台是 长白班则把计划量都合并到 白班中去
            schedule.setDayPlanQty(BigDecimalUtil.add(getDouble(schedule.getMidPlanQty()), getDouble(schedule.getNightPlanQty()), getDouble(schedule.getDayPlanQty())));
            this.clearClassField(schedule, EngineConstants.CLASS_MID);
            this.clearClassField(schedule, EngineConstants.CLASS_NIGHT);
        } else if(Objects.equals(oldClassShift, EngineConstants.CLASS_SHIFT_ONE) && (Objects.equals(classShift, EngineConstants.CLASS_SHIFT_TWO) || Objects.equals(classShift, EngineConstants.CLASS_SHIFT_THREE))) {
            //转入前是长白班，转入后是两班制或者三班制，则把长白班的计划量都算到中班去
            schedule.setMidPlanQty(BigDecimalUtil.add(getDouble(schedule.getMidPlanQty()), getDouble(schedule.getNightPlanQty()), getDouble(schedule.getDayPlanQty())));
            this.clearClassField(schedule, EngineConstants.CLASS_NIGHT);
            this.clearClassField(schedule, EngineConstants.CLASS_DAY);
        } else if(Objects.equals(oldClassShift, EngineConstants.CLASS_SHIFT_THREE) && Objects.equals(classShift, EngineConstants.CLASS_SHIFT_TWO)) {
            //转入前是三班制，转入后是两班制，则把白班的计划量合并到夜班去
            schedule.setNightPlanQty(BigDecimalUtil.add(getDouble(schedule.getNightPlanQty()), getDouble(schedule.getDayPlanQty())));
            this.clearClassField(schedule, EngineConstants.CLASS_DAY);
        }
    }
    
    /**
     * 跨区批量接收记录，排到各个机台最后
     * @param spanReceiveList	接收列表
     * @param scheduleList	已排列表
     * @param materialIntervalTime	物料切换时间
     */
	public void batchSpanReceived(List<MaterialScheduleResultVo> spanReceiveList,
			List<MaterialScheduleResultVo> scheduleList, int materialIntervalTime) {
		Map<String, List<MaterialScheduleResultVo>> machineSpanMap = scheduleList.stream()
				.collect(Collectors.groupingBy(MaterialScheduleResultVo::getMachineCode));
		Map<String, List<MaterialScheduleResultVo>> materialSpanMap = scheduleList.stream()
				.collect(Collectors.groupingBy(MaterialScheduleResultVo::getMaterialName));
		for (MaterialScheduleResultVo spanSchedule : spanReceiveList) {
			String machineCode = spanSchedule.getMachineCode();
			String materialName = spanSchedule.getMaterialName();
			List<MaterialScheduleResultVo> spanScheduleList = machineSpanMap.get(machineCode);
			List<MaterialScheduleResultVo> spanMaterialScheduleList = materialSpanMap.get(materialName); // 同物料的排程数据
			if (CollectionUtils.isNotEmpty(spanScheduleList)) {
				// 按完成时间倒序排序，取出最晚的一笔（第一笔）
				spanScheduleList.sort((s1, s2) -> {
					Date time1 = this.getLastExpectFinishTime(s1);
					Date time2 = this.getLastExpectFinishTime(s2);
					return ObjectUtils.compare(time2, time1, false);
				});
				MaterialScheduleResultVo maxSchedule = CollectionUtil.firstElement(spanScheduleList); // 最晚的一笔排产记录
				this.spanReceivedClassEngine(spanSchedule, maxSchedule, spanMaterialScheduleList, materialIntervalTime);
				if (spanSchedule.getDemandQty() != null && spanSchedule.getDemandQty() > 0) {
					scheduleList.add(spanSchedule); // 处理完后，需要将排程数据添加到列表中，但是只有需求量大于0的才需要添加
				}
			}
		}
	}
	
	/**
	 * 取出最晚的预计完成时间
	 * @param schedule
	 * @return
	 */
	private Date getLastExpectFinishTime(MaterialScheduleResult schedule) {
		if (schedule.getDayExpectFinishTime() != null) {
			return schedule.getDayExpectFinishTime();
		} else if (schedule.getNightExpectFinishTime() != null) {
			return schedule.getNightExpectFinishTime();
		} else if (schedule.getMidExpectFinishTime() != null) {
			return schedule.getMidExpectFinishTime();
		} else {
			return null;
		}
	}

    /**
     * 跨区接收后，排到各个机台最后
     * @param schedule
     * @param maxSchedule
     * @param materialIntervalTime
     */
    public List<MaterialScheduleResultVo> spanReceivedClassEngine(MaterialScheduleResultVo schedule, MaterialScheduleResult maxSchedule, List<MaterialScheduleResultVo> oldScheduleList, int materialIntervalTime) {
        Integer classShift = schedule.getClassShift();  //班制
        if(classShift == EngineConstants.CLASS_SHIFT_ONE) {
            //如果转入的机台是长白班则把计划量都合并到 白班中去
            schedule.setDayPlanQty(BigDecimalUtil.add(getDouble(schedule.getMidPlanQty()), getDouble(schedule.getNightPlanQty()), getDouble(schedule.getDayPlanQty())));
            this.clearClassField(schedule, EngineConstants.CLASS_MID);
            this.clearClassField(schedule, EngineConstants.CLASS_NIGHT);
        } else if(classShift == EngineConstants.CLASS_SHIFT_TWO) {
            //如果机台是两班制，则把白班的计划量合并到夜班中
            schedule.setNightPlanQty(BigDecimalUtil.add(getDouble(schedule.getNightPlanQty()), getDouble(schedule.getDayPlanQty())));
            this.clearClassField(schedule, EngineConstants.CLASS_DAY);
        }
        // 把各班的跨区排产都放到机台最后
        return this.putSpanScheduleLast(schedule, maxSchedule, oldScheduleList, materialIntervalTime);
    }

    /**
     * 跨区接收将排程记录放到最后
     * @param schedule
     * @param maxSchedule
     * @param materialIntervalTime
     */
	private List<MaterialScheduleResultVo> putSpanScheduleLast(MaterialScheduleResultVo spanSchedule, MaterialScheduleResult maxSchedule, List<MaterialScheduleResultVo> oldScheduleList,
			int materialIntervalTime) {
		List<MaterialScheduleResultVo> modifyScheduleList = new ArrayList<>();
		if (spanSchedule == null || spanSchedule == null) {
			return modifyScheduleList;
		}
		LhflMachine machine = spanSchedule.getMachine();
		if (machine == null) {
			return modifyScheduleList;
		}
		List<MaterialScheduleResultVo> spanReceiveList = Arrays.asList(spanSchedule);
		MaterialScheduleResultVo scheduleVo = new MaterialScheduleResultVo();
		BeanUtils.copyProperties(maxSchedule, scheduleVo);
		scheduleVo.setMidClassStartTime(spanSchedule.getMidClassStartTime());
		scheduleVo.setNightClassStartTime(spanSchedule.getNightClassStartTime());
		scheduleVo.setDayClassStartTime(spanSchedule.getDayClassStartTime());
		scheduleVo.setMidClassEndTime(spanSchedule.getMidClassEndTime());
		scheduleVo.setNightClassEndTime(spanSchedule.getNightClassEndTime());
		scheduleVo.setDayClassEndTime(spanSchedule.getDayClassEndTime());
		List<MaterialScheduleResultVo> scheduleList = Arrays.asList(scheduleVo);
		Map<String, LhflMachine> machineMap = new HashMap<>();
		machineMap.put(machine.getMachineCode(), machine);

		Map<String, Double> materialTotalDemandMap = this.mapMaterialTotalDemand(scheduleList); // 计算出物料对应的总需求量
		Map<String, MaterialLastClassVo> lastClassMaxInfoMap = this.mapLastClassMaxInfo(spanReceiveList, scheduleList,
				machineMap); // 获取排程最后一班排序最大的排班信息
		String materialName = spanSchedule.getMaterialName();
		if (StringUtil.isEmpty(spanSchedule.getMachineCode())) {
			return modifyScheduleList; // 没有排上机台的也没有办法排接收量
		}

		Double receiveQty = spanSchedule.getDemandQty(); // 跨区接收量
		if (receiveQty == null || receiveQty == 0) {
			return modifyScheduleList; // 没有排上机台的也没有办法排接收量
		} else if (receiveQty > 0) { // 大于0，说明是增量，需要排上机台
			Double demandQty = materialTotalDemandMap.getOrDefault(materialName, 0D); // 总需求量
			Double stockQty = spanSchedule.getStockQty(); // 库存
			Double planQty = stockQty > demandQty ? receiveQty - (stockQty - demandQty) : receiveQty; // 实际应生产的跨区接收量
			if (planQty <= 0) {
				return modifyScheduleList;
			}
			this.resetPlanField(spanSchedule); // 重置排程计划相关字段
			spanSchedule.setDemandQty(planQty); // 将扣除库存后的计划量当作本次需要排程的计划量
			spanSchedule.setStockQty(stockQty);
			String remark = I18nUtil.getMessage("engine.material.span.recive.remark"); // 排产班次的备注信息
			this.insertScheduleAtLastClass(spanReceiveList, modifyScheduleList, lastClassMaxInfoMap,
					materialIntervalTime, remark); // 将记录插跨区接收记录插入到当前排产列表的最后一班
			spanSchedule.setDemandQty(receiveQty); // 需要将需求量还原
		} else { // 小于0，说明是减少量，需要从已排上扣减
			List<MaterialScheduleResultVo> materialScheduleList = oldScheduleList.stream()
					.filter(s -> Objects.equals(materialName, s.getMaterialName())) // 同一个物料的已排计划
					.sorted(Comparator.comparing(MaterialScheduleResultVo::getTotalPlanQty, Comparator.nullsLast(Double::compareTo))) // 按计划量从小到大排序
					.collect(Collectors.toList());
			Double surplusReceiveQty = BigDecimalUtil.valueOf(receiveQty).abs().doubleValue(); // 待扣减量
			// 优先从未发布计划扣减
			List<MaterialScheduleResultVo> notPublishScheduleList = materialScheduleList.stream()
					.filter(s -> ZltConstant.NO_RELEASE.equals(s.getReleaseStatus())).collect(Collectors.toList());
			for (MaterialScheduleResultVo schedule : notPublishScheduleList) {
				Double tempReceiveQty = this.subtractPlanQty(surplusReceiveQty, schedule);
				if (surplusReceiveQty.compareTo(tempReceiveQty) != 0) {
					modifyScheduleList.add(schedule); // 扣减前后有变化，需要添加到修改列表中
				}
				surplusReceiveQty = tempReceiveQty;
				if (surplusReceiveQty == 0) {
					return modifyScheduleList;
				}
			}
			// 如果没有扣减完，则再从完成量为0的扣减
			List<MaterialScheduleResultVo> notFinishScheduleList = materialScheduleList.stream()
					.filter(s -> !ZltConstant.NO_RELEASE.equals(s.getReleaseStatus())
							&& (s.getMidFinishQty() == null || s.getMidFinishQty() == 0)
							&& (s.getNightFinishQty() == null || s.getNightFinishQty() == 0)
							&& (s.getDayFinishQty() == null || s.getDayFinishQty() == 0))
					.collect(Collectors.toList());
			for (MaterialScheduleResultVo schedule : notFinishScheduleList) {
				Double tempReceiveQty = this.subtractPlanQty(surplusReceiveQty, schedule);
				if (surplusReceiveQty.compareTo(tempReceiveQty) != 0) {
					modifyScheduleList.add(schedule); // 扣减前后有变化，需要添加到修改列表中
				}
				surplusReceiveQty = tempReceiveQty;
				if (surplusReceiveQty == 0) {
					return modifyScheduleList;
				}
			}
		}
		return modifyScheduleList;
	}
	
	/**
	 * 扣减各班计划量
	 * @param subtractQty	扣减量
	 * @param schedule	待扣减计划
	 * @return
	 */
	private Double subtractPlanQty(Double subtractQty, MaterialScheduleResultVo schedule) {
		Double surplusSubtract = subtractQty; // 剩余扣减量
		// 按白班 > 夜班 > 中班的顺序扣减
		if (surplusSubtract != 0) {
			Double dayPlanQty = schedule.getDayPlanQty(); // 重算白班
			if (dayPlanQty != null && dayPlanQty > 0) {
				Double subPlanQty = surplusSubtract > dayPlanQty ? dayPlanQty : surplusSubtract;
				dayPlanQty = BigDecimalUtil.sub(dayPlanQty, subPlanQty);
				surplusSubtract = BigDecimalUtil.sub(surplusSubtract, subPlanQty);
				schedule.setDayPlanQty(dayPlanQty);
			}
		}
		if (surplusSubtract != 0) {
			Double nightPlanQty = schedule.getNightPlanQty(); // 重算夜班
			if (nightPlanQty != null && nightPlanQty > 0) {
				Double subPlanQty = surplusSubtract > nightPlanQty ? nightPlanQty : surplusSubtract;
				nightPlanQty = BigDecimalUtil.sub(nightPlanQty, subPlanQty);
				surplusSubtract = BigDecimalUtil.sub(surplusSubtract, subPlanQty);
				schedule.setNightPlanQty(nightPlanQty);
			}
		}
		if (surplusSubtract != 0) {
			Double midPlanQty = schedule.getMidPlanQty(); // 重算中班
			if (midPlanQty != null && midPlanQty > 0) {
				Double subPlanQty = surplusSubtract > midPlanQty ? midPlanQty : surplusSubtract;
				midPlanQty = BigDecimalUtil.sub(midPlanQty, subPlanQty);
				surplusSubtract = BigDecimalUtil.sub(surplusSubtract, subPlanQty);
				schedule.setMidPlanQty(midPlanQty);
			}
		}
		schedule.setTotalPlanQty(
				BigDecimalUtil.add(schedule.getMidPlanQty(), schedule.getNightPlanQty(), schedule.getDayPlanQty())); // 重算总计划量
		return surplusSubtract;
	}
	
	
	/**
	 * 合并跨区扣减需求，将同物料同机台的合并成一条。有正有负的情况，从正数的扣减对应的值
	 * @param spanScheduleList
	 * @return
	 */
	@Override
	public List<MaterialScheduleResultVo> mergeSubtractSpanSchedule(List<MaterialScheduleResultVo> spanScheduleList) {
		Map<String, List<MaterialScheduleResultVo>> spanScheduleMap = spanScheduleList.stream()
				.filter(s -> StringUtils.isNotEmpty(s.getMaterialName()) && StringUtils.isNotEmpty(s.getMachineCode())
						&& s.getDemandQty() != null && s.getDemandQty() != 0)
				.collect(Collectors.groupingBy(MaterialScheduleResultVo::getMaterialName));
		for (List<MaterialScheduleResultVo> scheduleList : spanScheduleMap.values()) {
			List<MaterialScheduleResultVo> addDemandList = scheduleList.stream().filter(s -> s.getDemandQty() > 0)
					.sorted(Comparator.comparing(MaterialScheduleResultVo::getDemandQty)) // 按需求量从小到大排序
					.collect(Collectors.toList()); // 新增需求的跨区请求
			List<MaterialScheduleResultVo> subDemandList = scheduleList.stream().filter(s -> s.getDemandQty() < 0)
					.collect(Collectors.toList()); // 扣减需求的跨区请求

			BigDecimal subDemandQty = subDemandList.stream().map(s -> BigDecimalUtil.valueOf(s.getDemandQty()))
					.reduce(BigDecimal.ZERO, BigDecimal::add); // 统计待扣减量
			if (subDemandQty.compareTo(BigDecimal.ZERO) != 0) { // 有扣减需求，则需要从新增需求中扣减
				BigDecimal surplusDemandQty = subDemandQty.abs(); // 剩余需扣减的需求量，默认等于扣减量的绝对值
				for (MaterialScheduleResultVo schedule : addDemandList) { // 遍历新增需求，从较小的需求开始扣减
					if (surplusDemandQty.compareTo(BigDecimal.ZERO) == 0) {
						break;
					}
					BigDecimal demandQty = BigDecimalUtil.valueOf(schedule.getDemandQty()); // 需求量
					BigDecimal newDemadQty = demandQty.subtract(BigDecimalUtil.least(surplusDemandQty, demandQty)); // 扣减
					surplusDemandQty = surplusDemandQty.subtract(BigDecimalUtil.least(surplusDemandQty, demandQty)); // 扣减剩余量
					schedule.setDemandQty(newDemadQty.doubleValue());
					schedule.setMidPlanQty(newDemadQty.doubleValue());
				}
				
				// 扣减需求合并成一条
				subDemandList.forEach(s -> {
					s.setDemandQty(0D);
					s.setMidPlanQty(0D);
				}); // 将扣减量全部清空
				MaterialScheduleResultVo subschedule = CollectionUtil.firstElement(subDemandList); // 将剩余扣减量全部赋值给第一个计划
				subschedule.setDemandQty(surplusDemandQty.negate().doubleValue());
				subschedule.setMidPlanQty(surplusDemandQty.negate().doubleValue());
			}
		}
		return spanScheduleList.stream().filter(s -> s.getDemandQty() != 0).collect(Collectors.toList()); // 需求量为0的全部过滤掉
	}

    /**
     * 把各班的排产都放到机台最后
     * @param schedule  要修改排程对象
     * @param maxSchedule  此机台目前排在最后的顺序和预计完成时间
     * @param materialIntervalTime
     */
    private void putScheduleLast(MaterialScheduleResultVo schedule, MaterialScheduleResult maxSchedule, int materialIntervalTime) {
        if(schedule.getMidPlanQty() != null && schedule.getMidPlanQty() > 0) {
            Double midPlanQty = schedule.getMidPlanQty();  //中班计划量
            long singleCarTime = schedule.getSingleCarTime();  //单车称重消耗时间（毫秒）
            long produceTimeMillis = 0;

            String preMaterialName = "";
            Date produceTime = maxSchedule.getMidExpectFinishTime();
            Integer order = (maxSchedule.getMidProduceOrder() == null ? 0 : maxSchedule.getMidProduceOrder());
            produceTime = countStartTime(produceTime, schedule, preMaterialName, materialIntervalTime, EngineConstants.CLASS_MID);  //计算出预计开始时间
            schedule.setMidExpectStartTime(produceTime);

            produceTimeMillis = midPlanQty.longValue() * singleCarTime;  //全部称重完需要消耗的时间（毫秒）
            produceTime = addTimeMillis(produceTime, produceTimeMillis);  //计算出硫磺辅料称重完毕后的时间
            schedule.setMidExpectFinishTime(produceTime);  //设置计划完成时间
            schedule.setMidProduceOrder(order + EngineConstants.ORDER_MULTIPLE);  //重新设置生产顺序

            maxSchedule.setMidExpectFinishTime(produceTime);  //设置计划完成时间
            maxSchedule.setMidProduceOrder(order + EngineConstants.ORDER_MULTIPLE);  //重新设置生产顺序
        }

        if(schedule.getNightPlanQty() != null && schedule.getNightPlanQty() > 0) {
            Double nightPlanQty = schedule.getNightPlanQty();  //夜班计划量
            long singleCarTime = schedule.getSingleCarTime();  //单车称重消耗时间（毫秒）
            long produceTimeMillis = 0;

            String preMaterialName = "";
            Date produceTime = maxSchedule.getNightExpectFinishTime();
            Integer order = (maxSchedule.getNightProduceOrder() == null ? 0 : maxSchedule.getNightProduceOrder());
            produceTime = countStartTime(produceTime, schedule, preMaterialName, materialIntervalTime, EngineConstants.CLASS_NIGHT);  //计算出预计开始时间
            schedule.setNightExpectStartTime(produceTime);

            produceTimeMillis = nightPlanQty.longValue() * singleCarTime;  //全部称重完需要消耗的时间（毫秒）
            produceTime = addTimeMillis(produceTime, produceTimeMillis);  //计算出硫磺辅料称重完毕后的时间
            schedule.setNightExpectFinishTime(produceTime);  //设置计划完成时间
            schedule.setNightProduceOrder(order + EngineConstants.ORDER_MULTIPLE);  //重新设置生产顺序

            maxSchedule.setNightExpectFinishTime(produceTime);  //设置计划完成时间
            maxSchedule.setNightProduceOrder(order + EngineConstants.ORDER_MULTIPLE);  //重新设置生产顺序
        }

        if(schedule.getDayPlanQty() != null && schedule.getDayPlanQty() > 0) {
            Double dayPlanQty = schedule.getDayPlanQty();  //白班计划量
            long singleCarTime = schedule.getSingleCarTime();  //单车称重消耗时间（毫秒）
            long produceTimeMillis = 0;

            String preMaterialName = "";
            Date produceTime = maxSchedule.getDayExpectFinishTime();
            Integer order = (maxSchedule.getDayProduceOrder() == null ? 0 : maxSchedule.getDayProduceOrder());
            produceTime = countStartTime(produceTime, schedule, preMaterialName, materialIntervalTime, EngineConstants.CLASS_DAY);  //计算出预计开始时间
            schedule.setDayExpectStartTime(produceTime);

            produceTimeMillis = dayPlanQty.longValue() * singleCarTime;  //全部称重完需要消耗的时间（毫秒）
            produceTime = addTimeMillis(produceTime, produceTimeMillis);  //计算出硫磺辅料称重完毕后的时间
            schedule.setDayExpectFinishTime(produceTime);  //设置计划完成时间
            schedule.setDayProduceOrder(order + EngineConstants.ORDER_MULTIPLE);  //重新设置生产顺序

            maxSchedule.setDayExpectFinishTime(produceTime);  //设置计划完成时间
            maxSchedule.setDayProduceOrder(order + EngineConstants.ORDER_MULTIPLE);  //重新设置生产顺序
        }
    }
	

    /**
     * 清空对应班的字段信息
     * @param schedule
     * @param classType
     */
    public void clearClassField(MaterialScheduleResult schedule, String classType) {
        if(EngineConstants.CLASS_MID.equals(classType)) {
            schedule.setMidPlanQty(0D);
            schedule.setMidProduceOrder(null);
            schedule.setMidExpectStartTime(null);
            schedule.setMidExpectFinishTime(null);
        } else if(EngineConstants.CLASS_NIGHT.equals(classType)) {
            schedule.setNightPlanQty(0D);
            schedule.setNightProduceOrder(null);
            schedule.setNightExpectStartTime(null);
            schedule.setNightExpectFinishTime(null);
        } else if(EngineConstants.CLASS_DAY.equals(classType)) {
            schedule.setDayPlanQty(0D);
            schedule.setDayProduceOrder(null);
            schedule.setDayExpectStartTime(null);
            schedule.setDayExpectFinishTime(null);
        }
    }

    /**
     * 计算出预计开始时间
     * @param produceTime   预计开始时间
     * @param schedule   排程对象
     * @param preMaterialName  上条排程查物料名称
     * @param materialIntervalTime 不同规格之间的间隔时间（单位：分）
     * @param classType 班别
     * @return
     */
    private Date countStartTime(Date produceTime, MaterialScheduleResultVo schedule, String preMaterialName, int materialIntervalTime, String classType) {
        //计算计划开始称重时间
        if (produceTime == null) {
            int classShift = schedule.getClassShift();
            //计划硫磺辅料开始称重时间
            if(EngineConstants.CLASS_MID.equals(classType)) {
                produceTime = schedule.getMidClassStartTime();
            } else if(EngineConstants.CLASS_NIGHT.equals(classType)) {
                produceTime = schedule.getNightClassStartTime();
            } else {
                produceTime = schedule.getDayClassStartTime();
            }
        } else if (StringUtils.isNotBlank(preMaterialName) && !preMaterialName.equals(schedule.getMaterialName())) {
            //上一次和这一次的物料不同，则需要在加一个【不同规格之间的间隔时间】
            produceTime = addTimeMillis(produceTime, materialIntervalTime * 60 * 1000);
        }
        return produceTime;
    }

    /**
     * 计算增加毫秒后的新时间
     * @param time  原来的时间
     * @param timeMillis  需要增加的时间戳
     * @return
     */
    private Date addTimeMillis(Date time, long timeMillis) {
        long resultTimeMillis = time.getTime() + timeMillis;
        return new Date(resultTimeMillis);
    }
}
