package com.zlt.aps.cd90.engine.service.impl;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDoubleOrDefault;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineBigRollMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineMonthSurplusMapper;
import com.zlt.aps.cd90.engine.service.Cd90EngineMonthSurplusService;
import com.zlt.aps.cd90.engine.vo.Cd90BigRollVo;
import com.zlt.aps.cd90.engine.vo.Cd90MonthSurplusVo;
import com.zlt.aps.cd90.engine.vo.Cd90ScheduleResultVo;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 90度裁断根据月度计划调整计划量服务
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-14 11:29:41
 * @Version 1.0
 */
@Service("cd90EngineMonthSurplusService")
@Slf4j
public class Cd90EngineMonthSurplusServiceImpl implements Cd90EngineMonthSurplusService {
	/**
	 * 收尾提醒阈值默认值：1
	 */
	private final static Double DEFAULT_CLOSE_OUT_NUM = new Double("1");
	@Autowired
	private Cd90EngineMonthSurplusMapper cd90EngineMonthSurplusMapper;
	@Autowired
	private Cd90EngineBigRollMapper cd90EngineBigRollMapper;
	@Resource
	private AutoScheduleLogService autoScheduleLogService;

	/**
	 * 计算月度计划量
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 11:34:10
	 * @param scheduleDate    排产日期
	 * @param scheduleList    90度裁断排程结果明细列表
	 * @param closeOutNum     收尾提醒阈值
	 * @param defaultLossRate 耗损率默认值
	 */
	@Override
	public void calculateMonthSurplus(Date scheduleDate, List<Cd90ScheduleResultVo> scheduleList, String closeOutNum,
			String defaultLossRate) {
		// 抓取本月月度计划信息
		Map<String, Cd90MonthSurplusVo> monthSurplusMap = this.getMonthSurplusMap(scheduleDate);
		// 抓取帘布大卷基础信息
		List<Cd90BigRollVo> bigRollList = cd90EngineBigRollMapper.listCd90BigRoll();
		Map<String, Cd90BigRollVo> bigRollMap = bigRollList.stream()
				.collect(Collectors.toMap(Cd90BigRollVo::getBigRollCode, Function.identity(), (v1, v2) -> v2));
		// 记录日志
		String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
		String logDetail = logSplit("本月月度计划信息：" + toJSONString(monthSurplusMap),
				"帘布大卷基础信息：" + toJSONString(bigRollMap));
		autoScheduleLogService.insertCd90ScheduleLog(batchNo, "", "4.1、月度计划量基础数据日志", logDetail);

		for (Cd90ScheduleResultVo resultVo : scheduleList) {
			// 物料信息
			String bigRollCode = resultVo.getBigRollCode();
			String clothCode = resultVo.getClothCode();

			// 获取物料对应的月计划量
			Optional<Cd90MonthSurplusVo> monthSurplusOptional = Optional.ofNullable(monthSurplusMap.get(clothCode));
			// 获取对应大卷的基础信息
			Optional<Cd90BigRollVo> bigRollOptional = Optional.ofNullable(bigRollMap.get(bigRollCode));
			// 月度计划完成量
			Double monthFinishQty = monthSurplusOptional.map(Cd90MonthSurplusVo::getMonthFinishQty).orElse(0D);
			// 月度计划剩余量
			Double monthRemainQty = monthSurplusOptional.map(Cd90MonthSurplusVo::getMonthRemainQty).orElse(0D);
			this.setStatusAndCloseTip(resultVo, monthSurplusOptional.orElse(null), closeOutNum);
			// 新增计算日志
			this.insertCalculateLog(resultVo, bigRollOptional.orElse(null), monthFinishQty, monthRemainQty);
		}
	}

	/**
	 * 抓取排产日对应月份的月度计划生产信息
	 * 
	 * @param scheduleDate 排产日期
	 * @return key：物料编号，value：月度生产计划
	 */
	@Override
	public Map<String, Cd90MonthSurplusVo> getMonthSurplusMap(Date scheduleDate) {
		// 取出排程日期的年月
		String year = DateUtils.parseDateToStr("yyyy", scheduleDate);
		String month = DateUtils.parseDateToStr("MM", scheduleDate);
		List<Cd90MonthSurplusVo> monthSurplusList = cd90EngineMonthSurplusMapper.listCd90MonthPlanSurplus(year, month);
		Map<String, Cd90MonthSurplusVo> monthSurplusMap = monthSurplusList.stream()
				.collect(Collectors.toMap(Cd90MonthSurplusVo::getMaterialCode, Function.identity(), (v1, v2) -> v2));
		return monthSurplusMap;
	}

	/**
	 * 设置收尾提示标识 和 生产状态字段
	 * 
	 * @param resultVo       排产明细
	 * @param monthSurplusVo 月度计划
	 * @param closeOutNum    收尾提醒阈值
	 */
	@Override
	public void setStatusAndCloseTip(Cd90ScheduleResultVo resultVo, Cd90MonthSurplusVo monthSurplusVo,
			String closeOutNum) {
		// 收尾提醒阈值，转换类型
		Double closeOutNumQty = getDoubleOrDefault(closeOutNum, DEFAULT_CLOSE_OUT_NUM);
		// 生产状态
		String productionStatus = EngineConstants.PRODUCTION_STATUS_NOT;
		// 收尾提示
		String markCloseOutTip = EngineConstants.CLOSE_TIP_NOT;
		if (monthSurplusVo != null) {
			// 完成量
			Double monthFinishQty = monthSurplusVo.getMonthFinishQty();
			// 剩余量
			Double monthRemainQty = monthSurplusVo.getMonthRemainQty();
			if (monthFinishQty == 0D) {
				// 没有完成量，说明未生产
				productionStatus = EngineConstants.PRODUCTION_STATUS_NOT;
			} else if (monthFinishQty > 0D && monthRemainQty > 0) {
				// 完成量大于0，剩余量也大于0，说明处于生产中
				productionStatus = EngineConstants.PRODUCTION_STATUS_ING;
			} else if (monthRemainQty <= 0) {
				// 月度计划量小于等于0，说明已生产完成
				productionStatus = EngineConstants.PRODUCTION_STATUS_FINISH;
			}
			// 收尾提示。如果月度剩余量小于收尾提醒阈值，则需要打上收尾标志
			if (monthRemainQty < closeOutNumQty) {
				markCloseOutTip = EngineConstants.CLOSE_TIP_NEED;
			}
		} else {
			log.error("月计划汇总数据为空，物料编号为：{}", resultVo.getClothCode());
		}
		resultVo.setProductionStatus(productionStatus);
		resultVo.setMarkCloseOutTip(markCloseOutTip);
	}

	/**
	 * 新增计算日志
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-27 11:10:00
	 * @param resultVo       排产记录
	 * @param bigRollVo      大卷信息记录
	 * @param monthFinishQty 月度计划完成量
	 * @param monthRemainQty 月度计划剩余量
	 */
	private void insertCalculateLog(Cd90ScheduleResultVo resultVo, Cd90BigRollVo bigRollVo, Double monthFinishQty,
			Double monthRemainQty) {
		// 添加日志
		String logDetail = logSplit(
//				"计划量要取整卷大卷进行生产，但要校取整卷后是否超过月度计划剩余量，如果计划量超过月度计划剩余量，则以月度计划剩余量为准。",
//				"计算公式：钢压大卷整卷数=计划量 / 钢压大卷定长，结果向上取整；整卷排产计划量 =钢压大卷整卷数 * 钢压大卷定长。钢压大卷信息：" + toJSONString(bigRollVo),
				"完成量为0，对应生产状态：未生产；完成量大于0，月度计划量也大于0，说明处于生产中；月度计划量小于等于0，说明已生产完成。",
				"月度计划剩余量：" + monthRemainQty + ",月度计划完成量：" + monthFinishQty, "最终的排程数据：" + toJSONString(resultVo));
		autoScheduleLogService.insertCd90ScheduleLog(resultVo.getBatchNo(), resultVo.getOrderNo(), "4.2、计划量收尾提醒运算",
				logDetail);
	}

}
