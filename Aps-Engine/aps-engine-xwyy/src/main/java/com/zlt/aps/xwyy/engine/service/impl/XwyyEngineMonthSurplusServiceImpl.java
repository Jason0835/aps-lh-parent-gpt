package com.zlt.aps.xwyy.engine.service.impl;

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
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.xwyy.engine.mapper.XwyyEngineMonthSurplusMapper;
import com.zlt.aps.xwyy.engine.service.XwyyEngineMonthSurplusService;
import com.zlt.aps.xwyy.engine.vo.XwyyMonthSurplusVo;
import com.zlt.aps.xwyy.engine.vo.XwyyScheduleResultVo;

import lombok.extern.slf4j.Slf4j;

/**
 * 纤维压延断根据月度计划调整计划量服务
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-22 11:29:41
 * @Version 1.0
 */
@Service("xwyyEngineMonthSurplusService")
@Slf4j
public class XwyyEngineMonthSurplusServiceImpl implements XwyyEngineMonthSurplusService {
	/**
	 * 收尾提醒阈值默认值：2
	 */
	private final static Double DEFAULT_CLOSE_OUT_NUM = new Double("2");
	@Autowired
	private XwyyEngineMonthSurplusMapper xwyyEngineMonthSurplusMapper;
	@Resource
	private AutoScheduleLogService autoScheduleLogService;

	/**
	 * 计算月度计划量
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-22 11:34:10
	 * @param scheduleDate 排产日期
	 * @param scheduleList 纤维压延排程结果明细列表
	 * @param closeOutNum  收尾提醒阈值
	 */
	@Override
	public void calculateMonthSurplus(Date scheduleDate, List<XwyyScheduleResultVo> scheduleList, String closeOutNum) {
		// 抓取本月月度计划信息
		Map<String, XwyyMonthSurplusVo> monthSurplusMap = this.getMonthSurplusMap(scheduleDate);
		String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
		String logDetail = logSplit("本月月度计划信息：" + toJSONString(monthSurplusMap));
		autoScheduleLogService.insertXwyyScheduleLog(batchNo, "", "4.1、月度计划量基础数据日志", logDetail);

		for (XwyyScheduleResultVo resultVo : scheduleList) {
			// 大卷编号
			String bigRollCode = resultVo.getBigRollCode();
			// 取出对应帘布大卷的月度计划
			Optional<XwyyMonthSurplusVo> monthSurplusOptional = Optional.ofNullable(monthSurplusMap.get(bigRollCode));
			// 完成量
			Double monthFinishQty = monthSurplusOptional.map(XwyyMonthSurplusVo::getMonthFinishQty).orElse(0D);
			// 剩余量
			Double monthRemainQty = monthSurplusOptional.map(XwyyMonthSurplusVo::getMonthRemainQty).orElse(0D);
			// 判断如果已临近收尾，则需要控制不要超过完成量
			// 对剩余量做向上取整之后再做处理，modify by 20211230
			// 去掉该控制，半部件的排程结果不要受月计划量影响 modify by 20220105
//			this.reduceMonthRemain(resultVo, Math.ceil(monthRemainQty));

			// 设置收尾提示标识 和 生产状态字段
			this.setStatusAndCloseTip(resultVo, monthSurplusOptional.orElse(null), closeOutNum);
			// 插入日志
			this.insertCalculateLog(resultVo, monthFinishQty, monthRemainQty);
		}
	}

	/**
	 * 控制计划量不超过月度计划剩余量月度计划剩余量
	 * 
	 * @param resultVo       排产明细
	 * @param monthRemainQty 月度计划剩余量
	 */
	private void reduceMonthRemain(XwyyScheduleResultVo resultVo, Double monthRemainQty) {
		// 剩余量小于等于0，则直接清空当日的计划量
		if (monthRemainQty <= 0) {
			resultVo.setDayPlanQty(0D);
			resultVo.setNightPlanQty(0D);
			return;
		}
		// 当天该物料总计划量
		Double planQty = resultVo.getTotalPlan();
		// 从晚班开始扣减超出剩余量的计划量；如果不够扣，则晚班就不需要排产了
		if (monthRemainQty < planQty) {
			// 比较计划量与剩余量的差值 以及 晚班计划量，取较小值
			Double tempQty = Math.min(planQty - monthRemainQty, resultVo.getNightPlanQty());
			// 从晚班计划量中扣减
			resultVo.setNightPlanQty(resultVo.getNightPlanQty() - tempQty);
			// 总计划量同样扣减
			planQty -= tempQty;
		}
		// 如果晚班扣减后，总计划量仍然超过月度计划剩余量，则中班计划=月度剩余量
		if (monthRemainQty < planQty) {
			resultVo.setDayPlanQty(monthRemainQty);
			planQty = monthRemainQty;
		}
		// 设置计算后的总计划量
		resultVo.setTotalPlan(planQty);
	}

	/**
	 * 抓取排产日对应月份的月度计划生产信息
	 * 
	 * @param scheduleDate 排产日期
	 * @return key：物料编号，value：月度生产计划
	 */
	@Override
	public Map<String, XwyyMonthSurplusVo> getMonthSurplusMap(Date scheduleDate) {
		// 取出排程日期的年月
		String year = DateUtils.parseDateToStr("yyyy", scheduleDate);
		String month = DateUtils.parseDateToStr("MM", scheduleDate);
		List<XwyyMonthSurplusVo> monthSurplusList = xwyyEngineMonthSurplusMapper.listXwyyMonthPlanSurplus(year, month);
		Map<String, XwyyMonthSurplusVo> monthSurplusMap = monthSurplusList.stream()
				.collect(Collectors.toMap(XwyyMonthSurplusVo::getMaterialCode, Function.identity(), (v1, v2) -> v2));
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
	public void setStatusAndCloseTip(XwyyScheduleResultVo resultVo, XwyyMonthSurplusVo monthSurplusVo,
			String closeOutNum) {
		// 最小剩余量，转换类型
		Double closeOutNumQty = getDoubleOrDefault(closeOutNum, DEFAULT_CLOSE_OUT_NUM);
		// 设置提示与状态，默认不提示、状态未生产
		String productionStatus = EngineConstants.PRODUCTION_STATUS_NOT;
		String markCloseOutTip = EngineConstants.CLOSE_TIP_NOT;
		if (monthSurplusVo != null) {
			// 剩余量（个）
			Double monthRemainQty2 = monthSurplusVo.getMonthRemainQty2();
			// 完成量（个）
			Double monthFinishQty2 = monthSurplusVo.getMonthFinishQty2();
			if (monthFinishQty2 == 0D) {
				// 没有完成量，说明未生产
				productionStatus = EngineConstants.PRODUCTION_STATUS_NOT;
			} else if (monthFinishQty2 > 0D && monthRemainQty2 > 0) {
				// 完成量大于0，月度计划量也大于0，说明处于生产中
				productionStatus = EngineConstants.PRODUCTION_STATUS_ING;
			} else if (monthRemainQty2 <= 0) {
				// 剩余量小于等于0，说明已生产完成
				productionStatus = EngineConstants.PRODUCTION_STATUS_FINISH;
			}

			if (monthRemainQty2 <= closeOutNumQty) {
				// 帘布大卷月计划剩余量（个）小于等于2个，则说明需要收尾
				markCloseOutTip = EngineConstants.CLOSE_TIP_NEED;
			}
		} else {
			log.error("月计划汇总数据为空，物料编号为：{}", resultVo.getBigRollCode());
		}
		resultVo.setProductionStatus(productionStatus);
		resultVo.setMarkCloseOutTip(markCloseOutTip);
	}

	/**
	 * 新增计算日志
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-22 11:50:06
	 * @param resultVo       排产记录
	 * @param monthFinishQty 月度计划完成量
	 * @param monthRemainQty 月度计划剩余量
	 * @param standardSize   大卷标准长度
	 */
	private void insertCalculateLog(XwyyScheduleResultVo resultVo, Double monthFinishQty, Double monthRemainQty) {
		// 添加日志
		String logDetail = logSplit("根据月度生产计划处理生产状态：完成量为0，对应生产状态：未生产；完成量大于0，月度计划量也大于0，说明处于生产中；月度计划量小于等于0，说明已生产完成。",
				"月度计划剩余量：" + monthRemainQty + ",月度计划完成量：" + monthFinishQty, "最终的排程数据：" + toJSONString(resultVo));
		autoScheduleLogService.insertXwyyScheduleLog(resultVo.getBatchNo(), resultVo.getOrderNo(), "4.2、计划量取整卷运算",
				logDetail);
	}
}
