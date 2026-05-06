package com.zlt.aps.job.task;

import java.util.Date;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.job.mapper.SysJobMapper;
import com.zlt.aps.job.common.CXFinishQueryCodeEnum;
import com.zlt.aps.job.service.IRequestMesService;

/**
 * 向MES主动发送请求同步数据
 * 
 * @author ruoyi
 */
@Slf4j
@Component("reqMesTask")
public class RequestMesTask {
	@Autowired(required = false)
	private IRequestMesService iRequestMesService;

	@Autowired
	private SysJobMapper sysJobMapper;

	/**
	 * 检查aps-mps服务是否可用
	 * aps-mps服务未部署时，Feign客户端不会注入，返回false
	 * @return true-可用 false-不可用
	 */
	private boolean isMpsAvailable() {
		if (iRequestMesService == null) {
			log.warn("aps-mps服务未启动，跳过MES同步请求");
			return false;
		}
		return true;
	}

	/**
	 * 胎胚月结库存同步任务
	 */
	public void cxSyncMonthStock() {
		String time = DateUtils.getDate();
		cxSyncMonthStock(time);
	}

	/**
	 * 胎胚月结库存同步任务
	 * 
	 * @param queryDate 查询日期，格式：yyyy-MM-dd
	 */
	public void cxSyncMonthStock(String queryDate) {
		if (!isMpsAvailable()) return;
		iRequestMesService.cxSyncMonthStock(queryDate);
	}

	/**
	 * 胎胚不良数同步任务
	 */
	public void cxTireBadNum() {
		String time = DateUtils.getDate();
		cxTireBadNum(time);
	}

	/**
	 * 胎胚不良数同步任务
	 * 
	 * @param queryDate 查询日期，格式：yyyy-MM-dd
	 */
	public void cxTireBadNum(String queryDate) {
		if (!isMpsAvailable()) return;
		iRequestMesService.cxTireBadNum(queryDate);
	}

	/**
	 * 成型8-12点的完成量（产量）同步任务
	 */
	public void cxFinish() {
		String time = DateUtils.getDate();
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
		if (!isMpsAvailable()) return;
		iRequestMesService.sendCxFinish(startDate, endDate);
	}

	/**
	 * 半部件代号与SAP物料品号对应关系同步任务
	 */
	public void syncSapMaterial() {
		if (!isMpsAvailable()) return;
		iRequestMesService.syncSapMaterial();
	}

	/**
	 * 硫化每日库存同步
	 */
	public void lhSyncStock() {
		String startTime = DateUtils.dateTime(DateUtils.addDays(DateUtils.getNowDate(), -1)) + " 08:00:00";
		String endTime = DateUtils.getDate() + " 08:00:00";
		lhSyncStock(startTime, endTime);
	}

	/**
	 * 硫化每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd hh24:mi:ss
	 * @param endTime   结束时间 yyyy-MM-dd hh24:mi:ss
	 */
	public void lhSyncStock(String startTime, String endTime) {
		if (!isMpsAvailable()) return;
		iRequestMesService.lhSyncStock(startTime, endTime);
	}

	/**
	 * 成型每日库存同步
	 */
	public void cxSyncStock() {
		String time = DateUtils.getDate();
		cxSyncStock(time);
	}

	/**
	 * 成型每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd
	 * @param endTime   结束时间 yyyy-MM-dd
	 */
	public void cxSyncStock(String queryDate) {
		if (!isMpsAvailable()) return;
		iRequestMesService.cxSyncStock(queryDate);
	}

	/**
	 * 胎面每日库存同步
	 */
	public void tmSyncStock() {
		String time = DateUtils.getDate();
		tmSyncStock(time, time);
	}

	/**
	 * 胎面每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd
	 * @param endTime   结束时间 yyyy-MM-dd
	 */
	public void tmSyncStock(String startTime, String endTime) {
		if (!isMpsAvailable()) return;
		iRequestMesService.tmSyncStock(startTime, endTime);
	}

	/**
	 * 胎侧每日库存同步
	 */
	public void tcSyncStock() {
		String time = DateUtils.getDate();
		tcSyncStock(time, time);
	}

	/**
	 * 胎侧每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd
	 * @param endTime   结束时间 yyyy-MM-dd
	 */
	public void tcSyncStock(String startTime, String endTime) {
		if (!isMpsAvailable()) return;
		iRequestMesService.tcSyncStock(startTime, endTime);
	}

	/**
	 * 内衬每日库存同步
	 */
	public void ncSyncStock() {
		String time = DateUtils.getDate();
		ncSyncStock(time, time);
	}

	/**
	 * 内衬每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd
	 * @param endTime   结束时间 yyyy-MM-dd
	 */
	public void ncSyncStock(String startTime, String endTime) {
		if (!isMpsAvailable()) return;
		iRequestMesService.ncSyncStock(startTime, endTime);
	}

	/**
	 * 胎圈每日库存同步
	 */
	public void tqSyncStock() {
		String time = DateUtils.getDate();
		tqSyncStock(time, time);
	}

	/**
	 * 胎圈每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd
	 * @param endTime   结束时间 yyyy-MM-dd
	 */
	public void tqSyncStock(String startTime, String endTime) {
		if (!isMpsAvailable()) return;
		iRequestMesService.tqSyncStock(startTime, endTime);
	}

	/**
	 * 钢丝圈每日库存同步
	 */
	public void gsqSyncStock() {
		String time = DateUtils.getDate();
		gsqSyncStock(time, time);
	}

	/**
	 * 钢丝圈每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd
	 * @param endTime   结束时间 yyyy-MM-dd
	 */
	public void gsqSyncStock(String startTime, String endTime) {
		if (!isMpsAvailable()) return;
		iRequestMesService.gsqSyncStock(startTime, endTime);
	}

	/**
	 * 15度裁断每日库存同步
	 */
	public void cd15SyncStock() {
		String time = DateUtils.getDate();
		cd15SyncStock(time, time);
	}

	/**
	 * 15度裁断每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd
	 * @param endTime   结束时间 yyyy-MM-dd
	 */
	public void cd15SyncStock(String startTime, String endTime) {
		if (!isMpsAvailable()) return;
		iRequestMesService.cd15SyncStock(startTime, endTime);
	}

	/**
	 * 90度裁断每日库存同步
	 */
	public void cd90SyncStock() {
		String time = DateUtils.getDate();
		cd90SyncStock(time, time);
	}

	/**
	 * 90度裁断每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd
	 * @param endTime   结束时间 yyyy-MM-dd
	 */
	public void cd90SyncStock(String startTime, String endTime) {
		if (!isMpsAvailable()) return;
		iRequestMesService.cd90SyncStock(startTime, endTime);
	}

	/**
	 * 钢带压延每日库存同步
	 */
	public void gdyySyncStock() {
		String time = DateUtils.getDate();
		gdyySyncStock(time, time);
	}

	/**
	 * 钢带压延每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd
	 * @param endTime   结束时间 yyyy-MM-dd
	 */
	public void gdyySyncStock(String startTime, String endTime) {
		if (!isMpsAvailable()) return;
		iRequestMesService.gdyySyncStock(startTime, endTime);
	}

	/**
	 * 纤维压延每日库存同步
	 */
	public void xwyySyncStock() {
		String time = DateUtils.getDate();
		xwyySyncStock(time, time);
	}

	/**
	 * 纤维压延每日库存同步
	 * 
	 * @param startTime 开始时间 yyyy-MM-dd
	 * @param endTime   结束时间 yyyy-MM-dd
	 */
	public void xwyySyncStock(String startTime, String endTime) {
		if (!isMpsAvailable()) return;
		iRequestMesService.xwyySyncStock(startTime, endTime);
	}

	/**
	 * 成型日完成量同步（16点执行）
	 */
	public void cxDayFinish() {
		if (!isMpsAvailable()) return;
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		String today = DateUtils.getDate();
		String startDate = yestoday + " 16:00:00";
		String endDate = today + " 15:59:59";
		iRequestMesService.cxDayFinish(startDate, endDate);
	}

	/**
	 * 成型8点成量同步
	 */
	public void cx8AMFinish() {
		if (!isMpsAvailable()) return;
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		String today = DateUtils.getDate();
		String startDate = yestoday + " 16:00:00";
		String endDate = today + " 08:00:00";
		iRequestMesService.cxDayFinish(startDate, endDate);
	}

	/**
	 * 硫化日完成量同步（16点执行）
	 */
	public void lhDayFinish() {
		if (!isMpsAvailable()) return;
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		String today = DateUtils.getDate();
		String startDate = yestoday + " 16:00:00";
		String endDate = today + " 15:59:59";
		iRequestMesService.lhDayFinish(startDate, endDate);
	}

	/**
	 * 硫化8点完成量同步
	 */
	public void lh8AMFinish() {
		if (!isMpsAvailable()) return;
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		String today = DateUtils.getDate();
		String startDate = yestoday + " 16:00:00";
		String endDate = today + " 08:00:00";
		iRequestMesService.lhDayFinish(startDate, endDate);
	}

	/**
	 * 胎面日完成量同步（12点执行）
	 */
	public void tmDayFinish() {
		if (!isMpsAvailable()) return;
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 11:59:59";
		iRequestMesService.tmDayFinish(startDate, endDate);
	}

	/**
	 * 胎面8点完成量同步
	 */
	public void tm8AMFinish() {
		if (!isMpsAvailable()) return;
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 08:00:00";
		iRequestMesService.tmDayFinish(startDate, endDate);
	}

	/**
	 * 胎侧日完成量同步（12点执行）
	 */
	public void tcDayFinish() {
		if (!isMpsAvailable()) return;
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 11:59:59";
		iRequestMesService.tcDayFinish(startDate, endDate);
	}

	/**
	 * 胎侧8点完成量同步
	 */
	public void tc8AMFinish() {
		if (!isMpsAvailable()) return;
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 08:00:00";
		iRequestMesService.tcDayFinish(startDate, endDate);
	}

	/**
	 * 胎圈日完成量同步（12点执行）
	 */
	public void tqDayFinish() {
		if (!isMpsAvailable()) return;
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 11:59:59";
		iRequestMesService.tqDayFinish(startDate, endDate);
	}

	/**
	 * 胎圈8点完成量同步
	 */
	public void tq8AMFinish() {
		if (!isMpsAvailable()) return;
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 08:00:00";
		iRequestMesService.tqDayFinish(startDate, endDate);
	}

	/**
	 * 钢丝圈日完成量同步（12点执行）
	 */
	public void gsqDayFinish() {
		if (!isMpsAvailable()) return;
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 11:59:59";
		iRequestMesService.gsqDayFinish(startDate, endDate);
	}

	/**
	 * 钢丝圈8点完成量同步
	 */
	public void gsq8AMFinish() {
		if (!isMpsAvailable()) return;
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 08:00:00";
		iRequestMesService.gsqDayFinish(startDate, endDate);
	}

	/**
	 * 内衬日完成量同步（12点执行）
	 */
	public void ncDayFinish() {
		if (!isMpsAvailable()) return;
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 11:59:59";
		iRequestMesService.ncDayFinish(startDate, endDate);
	}

	/**
	 * 内衬8点完成量同步
	 */
	public void nc8AMFinish() {
		if (!isMpsAvailable()) return;
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 08:00:00";
		iRequestMesService.ncDayFinish(startDate, endDate);
	}

	/**
	 * 15度裁断日完成量同步（12点执行）
	 */
	public void cd15DayFinish() {
		if (!isMpsAvailable()) return;
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 11:59:59";
		iRequestMesService.cd15DayFinish(startDate, endDate);
	}

	/**
	 * 15度裁断8点完成量同步
	 */
	public void cd158AMFinish() {
		if (!isMpsAvailable()) return;
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 08:00:00";
		iRequestMesService.cd15DayFinish(startDate, endDate);
	}

	/**
	 * 90度裁断日完成量同步（12点执行）
	 */
	public void cd90DayFinish() {
		if (!isMpsAvailable()) return;
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 11:59:59";
		iRequestMesService.cd90DayFinish(startDate, endDate);
	}

	/**
	 * 90度裁断8点完成量同步
	 */
	public void cd908AMFinish() {
		if (!isMpsAvailable()) return;
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 08:00:00";
		iRequestMesService.cd90DayFinish(startDate, endDate);
	}

	/**
	 * 纤维压延日完成量同步（12点执行）
	 */
	public void xwyyDayFinish() {
		if (!isMpsAvailable()) return;
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 11:59:59";
		iRequestMesService.xwyyDayFinish(startDate, endDate);
	}

	/**
	 * 纤维压延8点完成量同步
	 */
	public void xwyy8AMFinish() {
		if (!isMpsAvailable()) return;
		String yestoday = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(new Date(), -1));
		String today = DateUtils.getDate();
		String startDate = yestoday + " 12:00:00";
		String endDate = today + " 08:00:00";
		iRequestMesService.xwyyDayFinish(startDate, endDate);
	}

	/**
	 * 成型机台当前生产规格接口
	 */
	public void cxProductionSpec() {
		if (!isMpsAvailable()) return;
		iRequestMesService.cxProductionSpec();
	}

	/**
	 * 成型中班完成量接口
	 */
	public void cxMoonFinish() {
		if (!isMpsAvailable()) return;
		String time = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(DateUtils.getNowDate(), -1));
		String startDate = time + " 16:00:00";
		String endDate = time + " 23:59:59";
		iRequestMesService.cxMidNightFinish(startDate, endDate, CXFinishQueryCodeEnum.CLASS1.getCode());
	}

	/**
	 * 成型夜班完成量接口
	 */
	public void cxNightFinish() {
		if (!isMpsAvailable()) return;
		String time = DateUtils.getDate();
		String startDate = time + " 00:00:00";
		String endDate = time + " 08:00:00";
		iRequestMesService.cxMidNightFinish(startDate, endDate, CXFinishQueryCodeEnum.CLASS2.getCode());
	}

	/**
	 * 硫化机台当前生产规格接口
	 */
	public void lhInProductionSpec() {
		if (!isMpsAvailable()) return;
		iRequestMesService.lhInProductionSpec();
	}
	
	/**
	 * 3班完成量，两班制
	 */
	public void class3FinishQtyTwo(String procedureCode) {
		if (!isMpsAvailable()) return;
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
		if (!isMpsAvailable()) return;
		String time = DateUtils.getDate();
		String startDate = time + " 08:00:00";
		String endDate = time + " 15:59:59";
		iRequestMesService.classFinishQty(procedureCode, startDate, endDate, CXFinishQueryCodeEnum.CLASS3.getCode());
	}
	
	/**
	 * 1班完成量，只有三班制有
	 * @param procedureCode	工序编号
	 */
	public void class1FinishQty(String procedureCode) {
		if (!isMpsAvailable()) return;
		Date date = DateUtils.addDays(DateUtils.getNowDate(), -1);
		String time = DateUtils.dateTime(date);
		String startDate = time + " 16:00:00";
		String endDate = time + " 23:59:59";
		iRequestMesService.classFinishQty(procedureCode, startDate, endDate, CXFinishQueryCodeEnum.CLASS1.getCode());
	}
	
	/**
	 * 2班完成量，两班制
	 * @param procedureCode	工序编号
	 */
	public void class2FinishQtyTwo(String procedureCode) {
		if (!isMpsAvailable()) return;
		String time = DateUtils.getDate();
		String startDate = time + " 00:00:00";
		String endDate = time + " 11:59:59";
		iRequestMesService.classFinishQty(procedureCode, startDate, endDate, CXFinishQueryCodeEnum.CLASS2.getCode());
	}
	
	/**
	 * 2班完成量，三班制
	 * @param procedureCode	工序编号
	 */
	public void class2FinishQtyThree(String procedureCode) {
		if (!isMpsAvailable()) return;
		String time = DateUtils.getDate();
		String startDate = time + " 00:00:00";
		String endDate = time + " 07:59:59";
		iRequestMesService.classFinishQty(procedureCode, startDate, endDate, CXFinishQueryCodeEnum.CLASS2.getCode());
	}

	/**
	 * 同步15度裁断线边库库存
	 */
	public void cd15LineSideStock() {
		if (!isMpsAvailable()) return;
		iRequestMesService.syncCd15LineSideStock();
	}

	/**
	 * 同步90度裁断线边库库存
	 */
	public void cd90LineSideStock() {
		if (!isMpsAvailable()) return;
		iRequestMesService.syncCd90LineSideStock();
	}

    /**
     * 触发指定kettle同步任务
     * @param syncKey 同步键
     */
    public void kettleSync(String syncKey) {
        if (!isMpsAvailable()) return;
        iRequestMesService.kettleSync(syncKey);
    }
    
    /**
     * 执行排程的同步
     * @param scheduleKey 排程键
     */
    public void runApsSyncData(String scheduleKey) {
        if (!isMpsAvailable()) return;
        iRequestMesService.runSyncData(scheduleKey);
    }
}
