package com.ruoyi.job.task;

import java.util.Date;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.job.common.CXFinishQueryCodeEnum;
import com.ruoyi.job.service.IRequestMesService;

/**
 * 向MES主动发送请求同步数据
 * 
 * @author ruoyi
 */
@Component("reqMesTask")
public class RequestMesTask {
	@Resource
	private IRequestMesService iRequestMesService;

	/**
	 * 胎胚月结库存同步任务
	 */
	public void cxSyncMonthStock() {
		String time = DateUtils.getDate(); // 获得当前日期
		cxSyncMonthStock(time);
	}

	/**
	 * 胎胚月结库存同步任务
	 * 
	 * @param queryDate 查询日期，格式：yyyy-MM-dd
	 */
	public void cxSyncMonthStock(String queryDate) {
		iRequestMesService.cxSyncMonthStock(queryDate);
	}

	/**
	 * 胎胚不良数同步任务
	 */
	public void cxTireBadNum() {
		String time = DateUtils.getDate(); // 获得当前日期
		cxTireBadNum(time);
	}

	/**
	 * 胎胚不良数同步任务
	 * 
	 * @param queryDate 查询日期，格式：yyyy-MM-dd
	 */
	public void cxTireBadNum(String queryDate) {
		iRequestMesService.cxTireBadNum(queryDate);
	}

	/**
	 * 成型8-12点的完成量（产量）同步任务
	 */
	public void cxFinish() {
		String time = DateUtils.getDate(); // 获得当前日期
		String startDate = time + " 08:00:00";
		String endDate = time + " 11:00:00";
		this.cxFinish(startDate, endDate);
	}

	/**
	 * 成型8-12点的完成量（产量）同步任务
	 * 
	 * @param startDate 开始时间 yyyy-MM-dd HH:mm:ss
	 * @param endDate   结束时间 yyyy-MM-dd HH:mm:ss
	 */
	public void cxFinish(String startDate, String endDate) {
		iRequestMesService.sendCxFinish(startDate, endDate);
	}

	/**
	 * 半部件代号与SAP物料品号对应关系同步任务
	 */
	public void syncSapMaterial() {
		iRequestMesService.syncSapMaterial();
	}

	/**
	 * 硫化每日库存同步
	 */
	public void lhSyncStock() {
		// 需要取上一天8点到当天8点的
		String startTime = DateUtils.dateTime(DateUtils.addDays(DateUtils.getNowDate(), -1)) + " 08:00:00";
		String endTime = DateUtils.getDate() + " 08:00:00"; // 获得服务器当时时间
		lhSyncStock(startTime, endTime);
	}

	/**
	 * 硫化每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd hh24:mi:ss
	 * @param endTime   结束时间 yyyy-MM-dd hh24:mi:ss
	 */
	public void lhSyncStock(String startTime, String endTime) {
		iRequestMesService.lhSyncStock(startTime, endTime);
	}

	/**
	 * 成型每日库存同步
	 */
	public void cxSyncStock() {
		String time = DateUtils.getDate(); // 获得服务器当时时间
		cxSyncStock(time); // 成型每日库存同步（异步）
	}

	/**
	 * 成型每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd
	 * @param endTime   结束时间 yyyy-MM-dd
	 */
	public void cxSyncStock(String queryDate) {
		iRequestMesService.cxSyncStock(queryDate);
	}

	/**
	 * 胎面每日库存同步
	 */
	public void tmSyncStock() {
		String time = DateUtils.getDate(); // 获得服务器当时时间
		tmSyncStock(time, time);
	}

	/**
	 * 胎面每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd
	 * @param endTime   结束时间 yyyy-MM-dd
	 */
	public void tmSyncStock(String startTime, String endTime) {
		iRequestMesService.tmSyncStock(startTime, endTime);
	}

	/**
	 * 胎侧每日库存同步
	 */
	public void tcSyncStock() {
		String time = DateUtils.getDate(); // 获得服务器当时时间
		tcSyncStock(time, time);
	}

	/**
	 * 胎侧每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd
	 * @param endTime   结束时间 yyyy-MM-dd
	 */
	public void tcSyncStock(String startTime, String endTime) {
		iRequestMesService.tcSyncStock(startTime, endTime);
	}

	/**
	 * 内衬每日库存同步
	 */
	public void ncSyncStock() {
		String time = DateUtils.getDate(); // 获得服务器当时时间
		ncSyncStock(time, time);
	}

	/**
	 * 内衬每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd
	 * @param endTime   结束时间 yyyy-MM-dd
	 */
	public void ncSyncStock(String startTime, String endTime) {
		iRequestMesService.ncSyncStock(startTime, endTime);
	}

	/**
	 * 胎圈每日库存同步
	 */
	public void tqSyncStock() {
		String time = DateUtils.getDate(); // 获得服务器当时时间
		tqSyncStock(time, time);
	}

	/**
	 * 胎圈每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd
	 * @param endTime   结束时间 yyyy-MM-dd
	 */
	public void tqSyncStock(String startTime, String endTime) {
		iRequestMesService.tqSyncStock(startTime, endTime);
	}

	/**
	 * 钢丝圈每日库存同步
	 */
	public void gsqSyncStock() {
		String time = DateUtils.getDate(); // 获得服务器当时时间
		gsqSyncStock(time, time);
	}

	/**
	 * 钢丝圈每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd
	 * @param endTime   结束时间 yyyy-MM-dd
	 */
	public void gsqSyncStock(String startTime, String endTime) {
		iRequestMesService.gsqSyncStock(startTime, endTime);
	}

	/**
	 * 15度裁断每日库存同步
	 */
	public void cd15SyncStock() {
		String time = DateUtils.getDate(); // 获得服务器当时时间
		cd15SyncStock(time, time);
	}

	/**
	 * 15度裁断每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd
	 * @param endTime   结束时间 yyyy-MM-dd
	 */
	public void cd15SyncStock(String startTime, String endTime) {
		iRequestMesService.cd15SyncStock(startTime, endTime);
	}

	/**
	 * 90度裁断每日库存同步
	 */
	public void cd90SyncStock() {
		String time = DateUtils.getDate(); // 获得服务器当时时间
		cd90SyncStock(time, time);
	}

	/**
	 * 90度裁断每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd
	 * @param endTime   结束时间 yyyy-MM-dd
	 */
	public void cd90SyncStock(String startTime, String endTime) {
		iRequestMesService.cd90SyncStock(startTime, endTime);
	}

	/**
	 * 钢带压延每日库存同步
	 */
	public void gdyySyncStock() {
		String time = DateUtils.getDate(); // 获得服务器当时时间
		gdyySyncStock(time, time);
	}

	/**
	 * 钢带压延每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd
	 * @param endTime   结束时间 yyyy-MM-dd
	 */
	public void gdyySyncStock(String startTime, String endTime) {
		iRequestMesService.gdyySyncStock(startTime, endTime);
	}

	/**
	 * 纤维压延每日库存同步
	 */
	public void xwyySyncStock() {
		String time = DateUtils.getDate(); // 获得服务器当时时间
		xwyySyncStock(time, time);
	}

	/**
	 * 纤维压延每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd
	 * @param endTime   结束时间 yyyy-MM-dd
	 */
	public void xwyySyncStock(String startTime, String endTime) {
		iRequestMesService.xwyySyncStock(startTime, endTime);
	}

	/**
	 * 成型日完成量同步（16点执行）
	 * 
	 */
	public void cxDayFinish() {
		// 获得上一天日期
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		 // 获得当前日期
		String today = DateUtils.getDate();
		String startDate = yestoday + " 16:00:00";
		String endDate = today + " 15:59:59";
		iRequestMesService.cxDayFinish(startDate, endDate);
	}

	/**
	 * 成型8点成量同步
	 * 
	 */
	public void cx8AMFinish() {
		// 获得上一天日期
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		 // 获得当前日期
		String today = DateUtils.getDate();
		String startDate = yestoday + " 16:00:00";
		String endDate = today + " 08:00:00";
		iRequestMesService.cxDayFinish(startDate, endDate);
	}

	/**
	 * 硫化日完成量同步（16点执行）
	 * 
	 */
	public void lhDayFinish() {
		// 获得上一天日期
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		 // 获得当前日期
		String today = DateUtils.getDate();
		String startDate = yestoday + " 16:00:00";
		String endDate = today + " 15:59:59";
		iRequestMesService.lhDayFinish(startDate, endDate);
	}

	/**
	 * 硫化8点完成量同步
	 * 
	 */
	public void lh8AMFinish() {
		// 获得上一天日期
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		 // 获得当前日期
		String today = DateUtils.getDate();
		String startDate = yestoday + " 16:00:00";
		String endDate = today + " 08:00:00";
		iRequestMesService.lhDayFinish(startDate, endDate);
	}

	/**
	 * 胎面日完成量同步（12点执行）
	 * 
	 */
	public void tmDayFinish() {
		// 获得上一天日期
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		 // 获得当前日期
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 11:59:59";
		iRequestMesService.tmDayFinish(startDate, endDate);
	}

	/**
	 * 胎面8点完成量同步
	 * 
	 */
	public void tm8AMFinish() {
		// 获得上一天日期
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		 // 获得当前日期
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 08:00:00";
		iRequestMesService.tmDayFinish(startDate, endDate);
	}

	/**
	 * 胎侧日完成量同步（12点执行）
	 * 
	 */
	public void tcDayFinish() {
		// 获得上一天日期
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		 // 获得当前日期
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 11:59:59";
		iRequestMesService.tcDayFinish(startDate, endDate);
	}

	/**
	 * 胎侧8点完成量同步
	 * 
	 */
	public void tc8AMFinish() {
		// 获得上一天日期
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		 // 获得当前日期
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 08:00:00";
		iRequestMesService.tcDayFinish(startDate, endDate);
	}

	/**
	 * 胎圈日完成量同步（12点执行）
	 * 
	 */
	public void tqDayFinish() {
		// 获得上一天日期
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		 // 获得当前日期
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 11:59:59";
		iRequestMesService.tqDayFinish(startDate, endDate);
	}

	/**
	 * 胎圈8点完成量同步
	 * 
	 */
	public void tq8AMFinish() {
		// 获得上一天日期
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		 // 获得当前日期
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 08:00:00";
		iRequestMesService.tqDayFinish(startDate, endDate);
	}

	/**
	 * 钢丝圈日完成量同步（12点执行）
	 * 
	 */
	public void gsqDayFinish() {
		// 获得上一天日期
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		 // 获得当前日期
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 11:59:59";
		iRequestMesService.gsqDayFinish(startDate, endDate);
	}

	/**
	 * 钢丝圈8点完成量同步
	 * 
	 */
	public void gsq8AMFinish() {
		// 获得上一天日期
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		 // 获得当前日期
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 08:00:00";
		iRequestMesService.gsqDayFinish(startDate, endDate);
	}

	/**
	 * 内衬日完成量同步（12点执行）
	 * 
	 */
	public void ncDayFinish() {
		// 获得上一天日期
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		 // 获得当前日期
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 11:59:59";
		iRequestMesService.ncDayFinish(startDate, endDate);
	}

	/**
	 * 内衬8点完成量同步
	 * 
	 */
	public void nc8AMFinish() {
		// 获得上一天日期
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		 // 获得当前日期
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 08:00:00";
		iRequestMesService.ncDayFinish(startDate, endDate);
	}

	/**
	 * 15度裁断日完成量同步（12点执行）
	 * 
	 */
	public void cd15DayFinish() {
		// 获得上一天日期
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		 // 获得当前日期
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 11:59:59";
		iRequestMesService.cd15DayFinish(startDate, endDate);
	}

	/**
	 * 15度裁断8点完成量同步
	 * 
	 */
	public void cd158AMFinish() {
		// 获得上一天日期
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		 // 获得当前日期
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 08:00:00";
		iRequestMesService.cd15DayFinish(startDate, endDate);
	}

	/**
	 * 90度裁断日完成量同步（12点执行）
	 * 
	 */
	public void cd90DayFinish() {
		// 获得上一天日期
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		 // 获得当前日期
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 11:59:59";
		iRequestMesService.cd90DayFinish(startDate, endDate);
	}

	/**
	 * 90度裁断8点完成量同步
	 * 
	 */
	public void cd908AMFinish() {
		// 获得上一天日期
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		 // 获得当前日期
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 08:00:00";
		iRequestMesService.cd90DayFinish(startDate, endDate);
	}

	/**
	 * 纤维压延日完成量同步（12点执行）
	 * 
	 */
	public void xwyyDayFinish() {
		// 获得上一天日期
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		 // 获得当前日期
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 11:59:59";
		iRequestMesService.xwyyDayFinish(startDate, endDate);
	}

	/**
	 * 纤维压延8点完成量同步
	 * 
	 */
	public void xwyy8AMFinish() {
		// 获得上一天日期
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		 // 获得当前日期
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 08:00:00";
		iRequestMesService.xwyyDayFinish(startDate, endDate);
	}

	/**
	 * 成型机台当前生产规格接口
	 */
	public void cxProductionSpec() {
		iRequestMesService.cxProductionSpec();
	}

	/**
	 * 成型中班完成量接口
	 * 
	 */
	public void cxMoonFinish() {
		String time = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(DateUtils.getNowDate(), -1)); // 获得上一天日期
		String startDate = time + " 16:00:00";
		String endDate = time + " 23:59:59";
		iRequestMesService.cxMidNightFinish(startDate, endDate, CXFinishQueryCodeEnum.CLASS1.getCode());
	}

	/**
	 * 成型夜班完成量接口
	 * 
	 */
	public void cxNightFinish() {
		String time = DateUtils.getDate(); // 获得当前日期
		String startDate = time + " 00:00:00";
		String endDate = time + " 08:00:00";
		iRequestMesService.cxMidNightFinish(startDate, endDate, CXFinishQueryCodeEnum.CLASS2.getCode());
	}

	/**
	 * 硫化机台当前生产规格接口
	 */
	public void lhInProductionSpec() {
		iRequestMesService.lhInProductionSpec();
	}
	
	/**
	 * 3班完成量，两班制
	 * 
	 */
	public void class3FinishQtyTwo(String procedureCode) {
		// 两班制的3班是上一天的12点到0点，接口是过了0点才执行，因此需要获得上一天日期
		Date date = DateUtils.addDays(DateUtils.getNowDate(), -1);
		String time = DateUtils.dateTime(date);
		
		String startDate = time + " 12:00:00";
		String endDate = time + " 23:59:59";
		iRequestMesService.classFinishQty(procedureCode, startDate, endDate, CXFinishQueryCodeEnum.CLASS3_2.getCode());
	}
	
	/**
	 * 3班完成量，三班制
	 * @param procedureCode	工序编号
	 */
	public void class3FinishQtyThree(String procedureCode) {
		String time = DateUtils.getDate(); // 获得当前日期
		String startDate = time + " 08:00:00";
		String endDate = time + " 15:59:59";
		iRequestMesService.classFinishQty(procedureCode, startDate, endDate, CXFinishQueryCodeEnum.CLASS3.getCode());
	}
	
	/**
	 * 1班完成量，只有三班制有
	 * @param procedureCode	工序编号
	 * 
	 */
	public void class1FinishQty(String procedureCode) {
		// 三班制的1班是上一天的16点到0点，接口是过了0点才执行，因此需要获得上一天日期
		Date date = DateUtils.addDays(DateUtils.getNowDate(), -1);
		String time = DateUtils.dateTime(date);
		String startDate = time + " 16:00:00";
		String endDate = time + " 23:59:59";
		iRequestMesService.classFinishQty(procedureCode, startDate, endDate, CXFinishQueryCodeEnum.CLASS1.getCode());
	}
	
	/**
	 * 2班完成量，两班制
	 * @param procedureCode	工序编号
	 * 
	 */
	public void class2FinishQtyTwo(String procedureCode) {
		String time = DateUtils.getDate(); // 获得当前日期
		String startDate = time + " 00:00:00";
		String endDate = time + " 11:59:59";
		iRequestMesService.classFinishQty(procedureCode, startDate, endDate, CXFinishQueryCodeEnum.CLASS2.getCode());
	}
	
	/**
	 * 2班完成量，三班制
	 * @param procedureCode	工序编号
	 * 
	 */
	public void class2FinishQtyThree(String procedureCode) {
		String time = DateUtils.getDate(); // 获得当前日期
		String startDate = time + " 00:00:00";
		String endDate = time + " 07:59:59";
		iRequestMesService.classFinishQty(procedureCode, startDate, endDate, CXFinishQueryCodeEnum.CLASS2.getCode());
	}

	/**
	 * 同步15度裁断线边库库存
	 * 
	 */
	public void cd15LineSideStock() {
		iRequestMesService.syncCd15LineSideStock();
	}

	/**
	 * 同步90度裁断线边库库存
	 * 
	 */
	public void cd90LineSideStock() {
		iRequestMesService.syncCd90LineSideStock();
	}
}
