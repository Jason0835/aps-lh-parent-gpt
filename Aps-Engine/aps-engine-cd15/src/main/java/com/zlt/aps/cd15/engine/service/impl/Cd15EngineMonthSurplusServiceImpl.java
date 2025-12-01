package com.zlt.aps.cd15.engine.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineBigRollMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMonthSurplusMapper;
import com.zlt.aps.cd15.engine.service.Cd15EngineMonthSurplusService;
import com.zlt.aps.cd15.engine.vo.Cd15BigRollVo;
import com.zlt.aps.cd15.engine.vo.Cd15MonthSurplusVo;
import com.zlt.aps.cd15.engine.vo.Cd15ScheduleResultVo;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDoubleOrDefault;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;

/**
 * 15度裁断根据月度计划调整计划量服务
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-12 11:29:41
 * @Version 1.0
 */
@Service("cd15EngineMonthSurplusService")
@Slf4j
public class Cd15EngineMonthSurplusServiceImpl implements Cd15EngineMonthSurplusService {
	/**
	 * 收尾提醒阈值默认值：1
	 */
	private final static Double DEFAULT_CLOSE_OUT_NUM = new Double("1");
	@Autowired
	private Cd15EngineMonthSurplusMapper cd15EngineMonthSurplusMapper;
	@Autowired
	private Cd15EngineBigRollMapper cd15EngineBigRollMapper;
	@Resource
	private AutoScheduleLogService autoScheduleLogService;

	/**
	 * 计算月度计划量
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-12 11:34:10
	 * @param scheduleDate    排产日期
	 * @param scheduleList    15度裁断排程结果明细列表
	 * @param closeOutNum     收尾提醒阈值
	 * @param defaultLossRate 耗损率默认值
	 */
	@Override
	public void calculateMonthSurplus(Date scheduleDate, List<Cd15ScheduleResultVo> scheduleList, String closeOutNum,
			String defaultLossRate) {
		// 抓取本月月度计划信息
		Map<String, Cd15MonthSurplusVo> monthSurplusMap = this.getMonthSurplusMap(scheduleDate);
		// 抓取钢压大卷基础信息
		List<Cd15BigRollVo> bigRollList = cd15EngineBigRollMapper.listCd15BigRoll();
		Map<String, Cd15BigRollVo> bigRollMap = bigRollList.stream()
				.collect(Collectors.toMap(Cd15BigRollVo::getBigRollCode, Function.identity(), (v1, v2) -> v2));
		// 记录日志
		String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
		String logDetail = logSplit("本月月度计划信息：" + toJSONString(monthSurplusMap),
				"钢压大卷基础信息：" + toJSONString(bigRollMap));
		autoScheduleLogService.insertCd15ScheduleLog(batchNo, "", "4.1、月度计划量基础数据日志", logDetail);

		for (Cd15ScheduleResultVo resultVo : scheduleList) {
			// 物料信息
			String bigRollCode = resultVo.getBigRollCode();
			String steelStripCode = resultVo.getSteelStripCode1();

			// 获取物料对应的月计划量
			Optional<Cd15MonthSurplusVo> monthSurplusOptional = Optional
					.ofNullable(monthSurplusMap.get(steelStripCode));
			// 获取对应大卷的基础信息
			Optional<Cd15BigRollVo> bigRollOptional = Optional.ofNullable(bigRollMap.get(bigRollCode));
			// 月度计划完成量
			Double monthFinishQty = monthSurplusOptional.map(Cd15MonthSurplusVo::getMonthFinishQty).orElse(0D);
			// 月度计划剩余量
			Double monthRemainQty = monthSurplusOptional.map(Cd15MonthSurplusVo::getMonthRemainQty).orElse(0D);
			// 设置收尾提示标识 和 生产状态字段
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
	public Map<String, Cd15MonthSurplusVo> getMonthSurplusMap(Date scheduleDate) {
		// 取出排程日期的年月
		String year = DateUtils.parseDateToStr("yyyy", scheduleDate);
		String month = DateUtils.parseDateToStr("MM", scheduleDate);
		// 查询出指定年月的月度计划量
		List<Cd15MonthSurplusVo> monthSurplusList = cd15EngineMonthSurplusMapper.listCd15MonthPlanSurplus(year, month);
		Map<String, Cd15MonthSurplusVo> monthSurplusMap = monthSurplusList.stream()
				.collect(Collectors.toMap(Cd15MonthSurplusVo::getMaterialCode, Function.identity(), (v1, v2) -> v2));
		return monthSurplusMap;
	}

	/**
	 * 设置收尾提示标识 和 生产状态字段
	 * 
	 * @param resultVo       排产明细
	 * @param monthSurplusVo 月度计划信息
	 * @param closeOutNum    收尾提醒阈值
	 */
	@Override
	public void setStatusAndCloseTip(Cd15ScheduleResultVo resultVo, Cd15MonthSurplusVo monthSurplusVo,
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
			log.error("月计划汇总数据为空，物料编号为：{}", resultVo.getSteelStripCode1());
		}
		resultVo.setProductionStatus(productionStatus);
		resultVo.setMarkCloseOutTip(markCloseOutTip);
	}

	/**
	 * 新增计算日志
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-26 15:11:06
	 * @param resultVo       排产记录
	 * @param bigRollVo      大卷信息记录
	 * @param monthFinishQty 月度计划完成量
	 * @param monthRemainQty 月度计划剩余量
	 */
	private void insertCalculateLog(Cd15ScheduleResultVo resultVo, Cd15BigRollVo bigRollVo, Double monthFinishQty,
			Double monthRemainQty) {
		// 添加日志
		String logDetail = logSplit(
//				"计划量要取整卷大卷进行生产，但要校取整卷后是否超过月度计划剩余量，如果计划量超过月度计划剩余量，则不处理。",
//				"计算公式：钢压大卷整卷数=计划量 / 钢压大卷定长，结果向上取整；整卷排产计划量 =钢压大卷整卷数 * 钢压大卷定长。钢压大卷信息：" + toJSONString(bigRollVo),
				"完成量为0，对应生产状态：未生产；完成量大于0，月度计划量也大于0，说明处于生产中；月度计划量小于等于0，说明已生产完成。",
				"剩余量小宇等于“临近收尾阈值”，设置收尾提示。月度计划剩余量：" + monthRemainQty + ",月度计划完成量：" + monthFinishQty,
				"最终的排程数据：" + toJSONString(resultVo));
		autoScheduleLogService.insertCd15ScheduleLog(resultVo.getBatchNo(), resultVo.getOrderNo(), "4.2、计划量收尾提醒运算",
				logDetail);
	}

}
