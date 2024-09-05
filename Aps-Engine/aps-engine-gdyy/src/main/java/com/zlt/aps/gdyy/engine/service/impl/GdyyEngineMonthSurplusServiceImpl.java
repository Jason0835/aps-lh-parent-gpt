package com.zlt.aps.gdyy.engine.service.impl;

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
import com.zlt.aps.gdyy.engine.mapper.GdyyEngineMonthSurplusMapper;
import com.zlt.aps.gdyy.engine.service.GdyyEngineMonthSurplusService;
import com.zlt.aps.gdyy.engine.vo.GdyyMonthSurplusVo;
import com.zlt.aps.gdyy.engine.vo.GdyyScheduleResultVo;

import lombok.extern.slf4j.Slf4j;

/**
 * 钢带压延断根据月度计划调整计划量服务
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-19 11:29:41
 * @Version 1.0
 */
@Service("gdyyEngineMonthSurplusService")
@Slf4j
public class GdyyEngineMonthSurplusServiceImpl implements GdyyEngineMonthSurplusService {
	/**
	 * 收尾提醒阈值默认值：2
	 */
	private final static Double DEFAULT_CLOSE_OUT_NUM = new Double("2");
	@Autowired
	private GdyyEngineMonthSurplusMapper gdyyEngineMonthSurplusMapper;
	@Resource
	private AutoScheduleLogService autoScheduleLogService;

	/**
	 * 计算月度计划量
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-19 11:34:10
	 * @param scheduleDate 排产日期
	 * @param scheduleList 钢带压延排程结果明细列表
	 * @param closeOutNum  收尾提醒阈值
	 */
	@Override
	public void calculateMonthSurplus(Date scheduleDate, List<GdyyScheduleResultVo> scheduleList, String closeOutNum) {
		// 抓取本月月度计划信息
		Map<String, GdyyMonthSurplusVo> monthSurplusMap = this.getMonthSurplusMap(scheduleDate);
		String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
		autoScheduleLogService.insertGdyyScheduleLog(batchNo, "", "3.1、月度计划量基础数据日志",
				"本月月度计划信息：" + toJSONString(monthSurplusMap));
		// 本次检查剩余量是否已达到收尾标准
		for (GdyyScheduleResultVo resultVo : scheduleList) {
			// 大卷编号
			String bigRollCode = resultVo.getBigRollCode();
			// 取出对应钢压大卷的月度计划
			Optional<GdyyMonthSurplusVo> monthSurplusOptional = Optional.ofNullable(monthSurplusMap.get(bigRollCode));
			Double monthFinishQty = monthSurplusOptional.map(GdyyMonthSurplusVo::getMonthFinishQty2).orElse(0D);
			// 整卷剩余量
			Double monthRemainQty2 = monthSurplusOptional.map(GdyyMonthSurplusVo::getMonthRemainQty2).orElse(0D);
			// 详细剩余量
			Double monthRemainQty = monthSurplusOptional.map(GdyyMonthSurplusVo::getMonthRemainQty).orElse(0D);
			// 判断如果已临近收尾，则需要控制不要超过完成量
			// 对剩余量做向上取整之后再做处理，modify by 20211230
			// 去掉该控制，半部件的排程结果不要受月计划量影响 modify by 20220105
//			this.reduceMonthRemain(resultVo, Math.ceil(monthRemainQty));

			// 设置收尾提示标识 和 生产状态字段
			this.setStatusAndCloseTip(resultVo, monthSurplusOptional.orElse(null), closeOutNum);
			// 插入日志
			this.insertCalculateLog(resultVo, monthFinishQty, monthRemainQty2);
		}
	}

	/**
	 * 控制计划量不超过月度计划剩余量月度计划剩余量
	 * 
	 * @param resultVo       排产明细
	 * @param monthRemainQty 月度计划剩余量
	 */
	private void reduceMonthRemain(GdyyScheduleResultVo resultVo, Double monthRemainQty) {
		// 剩余量小于等于0，则直接清空当日的计划量
		if (monthRemainQty <= 0) {
			resultVo.setClass1Plan(0D);
			resultVo.setClass2Plan(0D);
			resultVo.setClass3Plan(0D);
			return;
		}
		// 当天该物料总计划量
		Double planQty = resultVo.getClass1Plan() + resultVo.getClass2Plan() + resultVo.getClass3Plan();
		// 从3班开始扣减
		if (monthRemainQty < planQty) {
			// 比较计划量与剩余量的差值 以及 三班计划量，取较小值
			Double tempQty = Math.min(planQty - monthRemainQty, resultVo.getClass3Plan());
			// 从三班计划量中扣减
			resultVo.setClass3Plan(resultVo.getClass3Plan() - tempQty);
			// 总计划量同样扣减
			planQty -= tempQty;
		}
		// 1班扣减后总计划量仍然超过月度计划剩余量，则在2班扣减计划量
		if (monthRemainQty < planQty) {
			Double tempQty = Math.min(planQty - monthRemainQty, resultVo.getClass2Plan());
			resultVo.setClass2Plan(resultVo.getClass2Plan() - tempQty);
			planQty -= tempQty;
		}
		// 2班扣减后总计划量仍然超过月度计划剩余量，则1班计划量=月度计划剩余量
		if (monthRemainQty < planQty) {
			resultVo.setClass1Plan(monthRemainQty);
		}
	}

	/**
	 * 抓取排产日对应月份的月度计划生产信息
	 * 
	 * @param scheduleDate 排产日期
	 * @return key：物料编号，value：月度生产计划
	 */
	@Override
	public Map<String, GdyyMonthSurplusVo> getMonthSurplusMap(Date scheduleDate) {
		// 取出排程日期的年月
		String year = DateUtils.parseDateToStr("yyyy", scheduleDate);
		String month = DateUtils.parseDateToStr("MM", scheduleDate);
		List<GdyyMonthSurplusVo> monthSurplusList = gdyyEngineMonthSurplusMapper.listGdyyMonthPlanSurplus(year, month);
		Map<String, GdyyMonthSurplusVo> monthSurplusMap = monthSurplusList.stream()
				.collect(Collectors.toMap(GdyyMonthSurplusVo::getMaterialCode, Function.identity(), (v1, v2) -> v2));
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
	public void setStatusAndCloseTip(GdyyScheduleResultVo resultVo, GdyyMonthSurplusVo monthSurplusVo,
			String closeOutNum) {
		// 最小剩余量，转换类型
		Double closeOutNumQty = getDoubleOrDefault(closeOutNum, DEFAULT_CLOSE_OUT_NUM);
		// 设置提示与状态，默认不提示、状态未生产
		String productionStatus = EngineConstants.PRODUCTION_STATUS_NOT;
		String markCloseOutTip = EngineConstants.CLOSE_TIP_NOT;
		if (monthSurplusVo != null) {
			// 剩余量（米）
			Double monthRemainQty = monthSurplusVo.getMonthRemainQty();
			// 剩余量（个）
			Double monthRemainQty2 = monthSurplusVo.getMonthRemainQty2();
			// 完成量（个）
			Double monthFinishQty2 = monthSurplusVo.getMonthFinishQty2();
			if (monthFinishQty2 == 0) {
				// 没有完成量，说明未生产
				productionStatus = EngineConstants.PRODUCTION_STATUS_NOT;
			} else if (monthFinishQty2 > 0 && monthRemainQty2 > 0) {
				// 完成量大于0，剩余量也大于0，说明处于生产中
				productionStatus = EngineConstants.PRODUCTION_STATUS_ING;
			} else if (monthRemainQty2 <= 0) {
				// 剩余量小于等于0，说明已生产完成
				productionStatus = EngineConstants.PRODUCTION_STATUS_FINISH;
			}

			if (monthRemainQty2 < closeOutNumQty || monthRemainQty < resultVo.getDayUsed()) {
				// 钢压大卷月计划剩余量（个）小于2个 或者 剩余量（米）小于 日用参考，则说明需要收尾
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
	 * @Date 2021-7-19 13:11:06
	 * @param resultVo       排产记录
	 * @param monthFinishQty 月度计划完成量
	 * @param monthRemainQty 月度计划剩余量
	 */
	private void insertCalculateLog(GdyyScheduleResultVo resultVo, Double monthFinishQty, Double monthRemainQty) {
		// 添加日志
		String logDetail = logSplit("根据月度生产计划处理生产状态", "完成量为0，对应生产状态：未生产；完成量大于0，月度计划量也大于0，说明处于生产中；月度计划量小于等于0，说明已生产完成。",
				"月度计划剩余量：" + monthRemainQty + ",月度计划完成量：" + monthFinishQty, "最终的排程数据：" + toJSONString(resultVo));
		autoScheduleLogService.insertGdyyScheduleLog(resultVo.getBatchNo(), resultVo.getOrderNo(), "3.2、计划量取整卷运算",
				logDetail);
	}
}
