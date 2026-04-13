package com.zlt.aps.mps.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.*;
import com.zlt.aps.common.engine.planmain.MdmMonthPlanAmountSumService;
import com.zlt.aps.common.engine.service.*;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.aps.mps.common.FinishClassEnum;
import com.zlt.aps.mps.domain.*;
import com.zlt.aps.mps.mapper.*;
import com.zlt.aps.mps.service.MesFinishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Gim
 */
@Service
public class MesFinishServiceImpl implements MesFinishService {
    @Resource
    private TCxClassShiftFinishQtyMapper finishQtyMapper;
    @Resource
    private TMesCxPartFinishQtyMapper partFinishQtyMapper;

    @Resource
    private MpsScheduleResultMapper cxScheduleResultMapper;
    @Resource
    private CxEngineAutoScheduleRecordMapper cxEngineAutoScheduleRecordMapper;

    @Autowired
    private MdmMonthPlanAmountSumService sumService;
    @Autowired
    private TCxMonthPlanSurplusService cxMonthPlanSurplusService;
    @Autowired
    private TCxEmbryoMonthPlanSurplusService embryoMonthPlanSurplusService;
    @Autowired
    private TBaseMonthPlanSurplusService monthPlanSurplusService;
    @Autowired
    private TTmMonthPlanSurplusService tmMonthPlanSurplusService;
    @Autowired
    private TTcMonthPlanSurplusService tcMonthPlanSurplusService;
    @Autowired
    private TNcMonthPlanSurplusService ncMonthPlanSurplusService;
    @Autowired
    private TTqMonthPlanSurplusService tqMonthPlanSurplusService;
    @Autowired
    private TGsqMonthPlanSurplusService gsqMonthPlanSurplusService;
    @Autowired
    private TGdcdMonthPlanSurplusService cd15MonthPlanSurplusService;
    @Autowired
    private TLbcdMonthPlanSurplusService cd90MonthPlanSurplusService;
    @Autowired
    private TXwyyMonthPlanSurplusService xwyyMonthPlanSurplusService;
    @Autowired
    private CxTCd90BigRollService cd90BigRollService;
    @Autowired
    private CxTCd90ParamsService cd90ParamsService;
    @Autowired
    private ParamsService paramsService;

    @Resource
    private TMesDayFinishQtyMapper dayFinishQtyMapper;
    
    @Resource
    private MesDayFinishTotalMapper mesDayFinishTotalMapper;

    /**
     * 成型排程完成量回报
     * @param dataVersion mes版本
     * @return
     */
    @Override
    public AjaxResult mergeCxFinish(String dataVersion) {
        List<TMesCxShiftFinishQty> mesList = finishQtyMapper.getMesCxFinishByDataVersion(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
        }
        List<TCxClassShiftFinishQty> list = new ArrayList<>();
        for (TMesCxShiftFinishQty mes : mesList) {
            TCxClassShiftFinishQty cx = new TCxClassShiftFinishQty();
            BeanUtils.copyProperties(mes, cx);
            this.setBaseSysValue(cx);
            list.add(cx);
        }
        // 成型是胎胚
        // 完成量回报
        // 根据工单号查询成型排程结果
//        String orderNo = mesList.get(0).getOrderNo();
//        CxScheduleResult result = cxScheduleResultMapper.selectOneByOrderNoAndDelFlag(orderNo);
//        if (result == null) {
//            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.result.schedule"));
//        }
        // 更新成型投产状态
        cxScheduleResultMapper.updateProductionStatusByOrderNoIn(CollectionUtil.propertiesToList(mesList, TMesCxShiftFinishQty::getOrderNo));
        // 根据批次号获取自动排程记录拿到生产排程版本
//        CxEngineAutoScheduleRecord record = cxEngineAutoScheduleRecordMapper.selectOneByCxBatch(result.getCxBatchNo());
//        if (record == null) {
//            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.cx.schedule"));
//        }
        // 2021.12.11去掉更新汇总表数据
//        String scheduleDate = DateUtil.formatMonth(record.getScheduleDate());
        // 合并
        finishQtyMapper.mergeCxFinishSql(list);
//        // 汇总数据到 embryoCode和class1FinishQty
//        List<TCxClassShiftFinishQty> totalList = finishQtyMapper.selectCxByScheduleDate(scheduleDate);
//
//        Map<String, TCxClassShiftFinishQty> cxMap = CollectionUtil.toMap(totalList, TCxClassShiftFinishQty::getEmbryoCode);
//        // 查询旧数据
//        List<TCxEmbryoMonthPlanSurplus> oldList = embryoMonthPlanSurplusService.getByEmbryoListAndApsVersion(new ArrayList<>(cxMap.keySet()), record.getMonthPlanApsVersion());
//        if (!CollectionUtil.isEmpty(oldList)) {
//            for (TCxEmbryoMonthPlanSurplus old : oldList) {
//                TCxClassShiftFinishQty cx = cxMap.get(old.getMaterialCode());
//                // 因为数据已经汇总到class1FinishQty，所以完成量取class1FinishQty
//                old.setMonthFinishQty(BigDecimal.valueOf(cx.getClass1FinishQty()));
//                old.setMonthRemainQty(getEmbryoRemainQty(old));
//                old.setUpdateTime(new Date());
//            }
//            embryoMonthPlanSurplusService.mergeSql(oldList);
//        }
        return  AjaxResult.success(I18nUtil.getMessage("mes.error.message.cx.finish"));
    }

    /**
     *  硫化排程完成量回报
     * @param dataVersion
     * @return
     */
    @Override
    public AjaxResult mergeLhFinish(String dataVersion) {

        return AjaxResult.success(I18nUtil.getMessage("mes.error.message.lh.finish"));
    }

    // 成型日完成量回报
    @Override
    public AjaxResult mergeCxDayFinish(String dataVersion) {
        List<TMesCxDayFinishQty> mesList = finishQtyMapper.getMesCxDayFinishByDataVersion(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
        }
        List<TCxDayFinishQty> list = new ArrayList<>();
        for (TMesCxDayFinishQty mes : mesList) {
            TCxDayFinishQty cx = new TCxDayFinishQty();
            BeanUtils.copyProperties(mes, cx);
            cx.setCreateDate(new Date());
            cx.setUpdateDate(new Date());
            list.add(cx);
        }
        // 合并
        finishQtyMapper.mergeCxDayFinishSql(list);
        Date finishDate = list.get(0).getFinishDate();
        String finishString = DateUtil.formatMonth(finishDate);
        List<TCxDayFinishQty> finishList = finishQtyMapper.selectCxDayByFinishDate(finishString);
        Map<String, TCxDayFinishQty> finishMap = CollectionUtil.toMap(finishList, TCxDayFinishQty::getEmbryoCode);
        // 查询旧数据
        String year = DateUtil.getYear(finishDate) + "";
        String month = DateUtil.getMonth(finishDate) + "";
        if (Integer.parseInt(month) < 10 && month.length() == 1) {
            month = "0" + month;
        }
        List<TCxEmbryoMonthPlanSurplus> embryoList = embryoMonthPlanSurplusService.getByEmbryoListAndYearAndMonth(new ArrayList<>(finishMap.keySet()), year, month);
        if (!CollectionUtil.isEmpty(embryoList)) {
            for (TCxEmbryoMonthPlanSurplus old : embryoList) {
                TCxDayFinishQty cx = finishMap.get(old.getMaterialCode());
                old.setMonthFinishQty(BigDecimal.valueOf(cx.getDayFinishQty()));
                old.setMonthRemainQty(getEmbryoRemainQty(old));
                old.setUpdateTime(new Date());
            }
            embryoMonthPlanSurplusService.mergeSql(embryoList);
        }
        return AjaxResult.success("成型日完成量回报成功");
    }

    // 硫化日完成量回报
    @Override
    public AjaxResult mergeLhDayFinish(String dataVersion) {
        List<LhDayFinishQty> mesList = finishQtyMapper.getMesLhDayFinishByDataVersion(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
        }
        List<LhDayFinishQty> list = new ArrayList<>();
        for (LhDayFinishQty mes : mesList) {
            LhDayFinishQty lh = new LhDayFinishQty();
            BeanUtils.copyProperties(mes, lh);
            lh.setCreateTime(new Date());
            lh.setUpdateTime(new Date());
            list.add(lh);
        }
        // 合并
        finishQtyMapper.mergeLhDayFinishSql(list);
        Date finishDate = list.get(0).getFinishDate();
        String finishString = DateUtil.formatMonth(finishDate);
        List<LhDayFinishQty> finishList = finishQtyMapper.selectLhDayByFinishDate(finishString);
        Map<String, LhDayFinishQty> finishMap = CollectionUtil.toMap(finishList, LhDayFinishQty::getMaterialCode);
        // 查询旧数据
        String year = DateUtil.getYear(finishDate) + "";
        String month = DateUtil.getMonth(finishDate) + "";
        if (Integer.parseInt(month) < 10 && month.length() == 1) {
            month = "0" + month;
        }
        List<TCxMonthPlanSurplus> oldList = cxMonthPlanSurplusService.getBySapCodeAndYearAndMonth(new ArrayList<>(finishMap.keySet()), year, month);
        if (!CollectionUtil.isEmpty(oldList)) {
            for (TCxMonthPlanSurplus old : oldList) {
                LhDayFinishQty lh = finishMap.get(old.getSapCode());
                old.setMonthFinishQty(lh.getDayFinishQty());
                old.setMonthRemainQty(getMonthRemainQty(old));
                old.setUpdateTime(new Date());
            }
            cxMonthPlanSurplusService.mergeSql(oldList);
        }
        return AjaxResult.success(I18nUtil.getMessage("硫化日完成量回报成功"));
    }

    /**
     * 获取月计划剩余量
     * @param monthPlanSurplus
     * @return
     */
    private static int getMonthRemainQty(TCxMonthPlanSurplus monthPlanSurplus) {
        int monthRemainQty = monthPlanSurplus.getMonthPlanQty() + monthPlanSurplus.getPlanModifyQty() + monthPlanSurplus.getSapBadQty() - monthPlanSurplus.getLastMonthStock() - monthPlanSurplus.getMonthFinishQty();

        return monthRemainQty;
    }

    /**
     * 获取胎胚剩余量
     * @param embryoMonthPlanSurplus
     * @return
     */
    private static BigDecimal getEmbryoRemainQty(TCxEmbryoMonthPlanSurplus embryoMonthPlanSurplus) {
        BigDecimal embryoRemainQty = embryoMonthPlanSurplus.getMonthPlanQty().add(embryoMonthPlanSurplus.getMonthPlanModifyQty()).add(embryoMonthPlanSurplus.getEmbryoBadQty()).subtract(embryoMonthPlanSurplus.getLastMonthStock()).subtract(embryoMonthPlanSurplus.getMonthFinishQty()).setScale(3, RoundingMode.UP);

        return embryoRemainQty;
    }

    /**
     *  成型8-12点的完成量
     * @param dataVersion
     * @return
     */
    @Override
    public AjaxResult mergeCxPartFinish(String dataVersion) {
        List<TMesCxPartFinishQty> mesList = partFinishQtyMapper.getAllByDataVersionAndIsDelete(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
        }
        List<TCxPartFinishQty> list = new ArrayList<>();
        HashMap<String, List<TMesCxPartFinishQty>> map = CollectionUtil.toMapList(mesList, obj -> GenerageMapKeyUtils.createMapKey(obj.getEmbryoCode(), obj.getBomDataVersion()));
        for (String key : map.keySet()) {
            List<TMesCxPartFinishQty> mesCxPartFinishQtyList = map.get(key);
            TCxPartFinishQty lh = new TCxPartFinishQty();
            lh.setStatDate(mesCxPartFinishQtyList.get(0).getStatDate());
            lh.setEmbryoCode(mesCxPartFinishQtyList.get(0).getEmbryoCode());
            lh.setBomDataVersion(mesCxPartFinishQtyList.get(0).getBomDataVersion());
            // 完成量
            Integer finish = 0;
            for (TMesCxPartFinishQty qty : mesCxPartFinishQtyList) {
                finish += qty.getFinishQty();
            }
            lh.setFinishQty(finish);
            this.setBaseSysValue(lh);
            list.add(lh);
        }
        partFinishQtyMapper.mergeCxPart(list);
        return AjaxResult.success(I18nUtil.getMessage("mes.error.message.cx8.finish"));
    }

    /**
     * 胎面完成量回报
     * 更新每日各班完成量以及更新排程状态
     */
    @Override
    public AjaxResult mergeTmFinish(String dataVersion) {
        List<TMesTmDayFinishQty> mesList = dayFinishQtyMapper.getTmByDataVersionAndIsDelete(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
        }
        List<TTmDayFinishQty> list = new ArrayList<>();
        Map<String, TMesTmDayFinishQty> mesMap = CollectionUtil.toMap(mesList, TMesTmDayFinishQty::getOrderNo);
        for (String code : mesMap.keySet()) {
            TMesTmDayFinishQty finishQty = mesMap.get(code);
            TTmDayFinishQty entity = new TTmDayFinishQty();
            entity.setTreadCode(finishQty.getTreadCode());
            this.setBaseSysValue(entity);
            entity.setOrderNo(finishQty.getOrderNo());
            entity.setScheduleDate(finishQty.getScheduleDate());
            entity.setDayFinishQty(finishQty.getDayFinishQty());
            entity.setNightFinishQty(finishQty.getNightFinishQty());
            list.add(entity);
        }

        // 完成量回报
        // 排产日
        Date scheduleDate = CollectionUtil.firstElement(mesList).getScheduleDate();
        // 月度计划版本号，直接从最新的月度计划抓取
        String monthPlanApsVersion = this.getMonthPlanApsVersion(scheduleDate);
        if (StringUtils.isEmpty(monthPlanApsVersion)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.monthPlan.version"));
		}
		// 构建月度计划状态列表
		List<MonthSurplusStatusVo> statusList = list.stream().map(m -> createStatusVo(m.getOrderNo(), m.getTreadCode()))
				.collect(Collectors.toList());
		// 收尾提醒阈值，从系统参数获取
		String closeOutNum = Optional.ofNullable(paramsService.getTmParam(EngineConstants.CLOSE_OUT_NUM)).orElse("0");

		// 合并
        dayFinishQtyMapper.mergeTmFinish(list);

        // 汇总数据到 code和dayFinishQty
        String formatScheduleDate = DateUtil.formatMonth(scheduleDate);
        List<TTmDayFinishQty> tmList = dayFinishQtyMapper.selectTmByScheduleDate(formatScheduleDate);
        Map<String, TTmDayFinishQty> tmMap = CollectionUtil.toMap(tmList, TTmDayFinishQty::getTreadCode);
        // 旧数据
        List<TTmMonthPlanSurplus> oldList = tmMonthPlanSurplusService.getByCodeList(monthPlanApsVersion, new ArrayList<>(tmMap.keySet()));
        if (!CollectionUtil.isEmpty(oldList)) {
            for (TTmMonthPlanSurplus old : oldList) {
                TTmDayFinishQty entity = tmMap.get(old.getMaterialCode());
                // 完成量=日班+夜班完成量
                buildFinish(old, entity.getDayFinishQty());
                old.setUpdateTime(new Date());
				// 更新状态列表的生产状态以及收尾提醒
				this.updateStatusAndTip(statusList, old, closeOutNum);
            }
            // 回写排程记录上的生产状态以及收尾提醒
            if (statusList.stream().anyMatch(s -> s.isModify())) {
                cxScheduleResultMapper.updateTmProductionStatusByOrderNo(statusList);
            }
        }
        return AjaxResult.success(I18nUtil.getMessage("mes.error.message.tm.finish"));
    }

    /**
     * 更新完成量+剩余量
     * @param k
     * @param finishBig
     * @param <K>
     */
    private static <K extends MonthPlanSurplusBaseEntity> void buildFinish(K k, BigDecimal finishBig) {
//        k.setMonthFinishQty(k.getMonthFinishQty().add(finishBig).setScale(3, RoundingMode.UP));
        k.setMonthFinishQty(finishBig.setScale(3, RoundingMode.UP));
        k.setMonthRemainQty(k.getMonthPlanQty().add(k.getMonthPlanModifyQty()).subtract(k.getMonthFinishQty()).setScale(3, RoundingMode.UP));
//        if (k.getMonthRemainQty().compareTo(BigDecimal.ZERO) < 1) {
//            k.setMonthRemainQty(BigDecimal.ZERO);
//        }
    }

	/**
	 * 胎侧完成量回报
     * 更新每日各班完成量以及更新排程状态
	 */
    @Override
    public AjaxResult mergeTcFinish(String dataVersion) {
        List<TMesTcDayFinishQty> mesList = dayFinishQtyMapper.getTcByDataVersionAndIsDelete(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
        }
        List<TTcDayFinishQty> list = new ArrayList<>();
        Map<String, TMesTcDayFinishQty> mesMap = CollectionUtil.toMap(mesList, obj -> obj.getOrderNo());
        for (String code : mesMap.keySet()) {
            TMesTcDayFinishQty finishQty = mesMap.get(code);
            TTcDayFinishQty entity = new TTcDayFinishQty();
            entity.setSidewallCode(finishQty.getSidewallCode());
            this.setBaseSysValue(entity);
            entity.setOrderNo(finishQty.getOrderNo());
            entity.setScheduleDate(finishQty.getScheduleDate());
            entity.setDayFinishQty(finishQty.getDayFinishQty());
            entity.setNightFinishQty(finishQty.getNightFinishQty());
            list.add(entity);
        }
        
        // 完成量回报
        // 排产日
        Date scheduleDate = CollectionUtil.firstElement(mesList).getScheduleDate();
        // 月度计划版本号，直接从最新的月度计划抓取
        String monthPlanApsVersion = this.getMonthPlanApsVersion(scheduleDate);
        if (StringUtils.isEmpty(monthPlanApsVersion)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.monthPlan.version"));
		}
		// 构建月度计划状态列表
		List<MonthSurplusStatusVo> statusList = list.stream()
				.map(m -> createStatusVo(m.getOrderNo(), m.getSidewallCode())).collect(Collectors.toList());
		// 收尾提醒阈值，从系统参数获取
		String closeOutNum = Optional.ofNullable(paramsService.getTcParam(EngineConstants.CLOSE_OUT_NUM)).orElse("0");
        // 合并
        dayFinishQtyMapper.mergeTcFinish(list);

        // 汇总数据到 code和dayFinishQty
        String formatScheduleDate = DateUtil.formatMonth(scheduleDate);
        List<TTcDayFinishQty> tcList = dayFinishQtyMapper.selectTcByScheduleDate(formatScheduleDate);

        Map<String, TTcDayFinishQty> map = CollectionUtil.toMap(tcList, TTcDayFinishQty::getSidewallCode);
        List<TTcMonthPlanSurplus> oldList = tcMonthPlanSurplusService.getByCodeList(monthPlanApsVersion, new ArrayList<>(map.keySet()));
        if (!CollectionUtil.isEmpty(oldList)) {
            for (TTcMonthPlanSurplus old : oldList) {
                TTcDayFinishQty entity = map.get(old.getMaterialCode());
                buildFinish(old, entity.getDayFinishQty());
                old.setUpdateTime(new Date());
				// 更新状态列表的生产状态以及收尾提醒
				this.updateStatusAndTip(statusList, old, closeOutNum);
            }
            // 回写排程记录上的生产状态以及收尾提醒
            if (statusList.stream().anyMatch(s -> s.isModify())) {
                cxScheduleResultMapper.updateTcProductionStatusByOrderNo(statusList);
            }
        }
        return AjaxResult.success(I18nUtil.getMessage("mes.error.message.tc.finish"));
    }

	/**
	 * 内衬完成量回报
     * 更新每日各班完成量以及更新排程状态
	 */
    @Override
    public AjaxResult mergeNcFinish(String dataVersion) {
        List<TMesNcDayFinishQty> mesList = dayFinishQtyMapper.getNcByDataVersionAndIsDelete(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
        }
        List<TNcDayFinishQty> list = new ArrayList<>();
        Map<String, TMesNcDayFinishQty> mesMap = CollectionUtil.toMap(mesList, obj -> obj.getOrderNo());
        for (String code : mesMap.keySet()) {
            TMesNcDayFinishQty finishQty = mesMap.get(code);
            TNcDayFinishQty entity = new TNcDayFinishQty();
            entity.setLiningCode(finishQty.getLiningCode());
            this.setBaseSysValue(entity);
            entity.setOrderNo(finishQty.getOrderNo());
            entity.setScheduleDate(finishQty.getScheduleDate());
            entity.setDayFinishQty(finishQty.getDayFinishQty());
            entity.setNightFinishQty(finishQty.getNightFinishQty());
            list.add(entity);
        }
        
        // 完成量回报
        // 排产日
        Date scheduleDate = CollectionUtil.firstElement(mesList).getScheduleDate();
        // 月度计划版本号，直接从最新的月度计划抓取
        String monthPlanApsVersion = this.getMonthPlanApsVersion(scheduleDate);
        if (StringUtils.isEmpty(monthPlanApsVersion)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.monthPlan.version"));
		}
		// 构建月度计划状态列表
		List<MonthSurplusStatusVo> statusList = list.stream()
				.map(m -> createStatusVo(m.getOrderNo(), m.getLiningCode())).collect(Collectors.toList());
		// 收尾提醒阈值，从系统参数获取
		String closeOutNum = Optional.ofNullable(paramsService.getNcParam(EngineConstants.CLOSE_OUT_NUM)).orElse("0");
        // 合并
        dayFinishQtyMapper.mergeNcFinish(list);

        // 汇总数据到 code和dayFinishQty
        String formatScheduleDate = DateUtil.formatMonth(scheduleDate);
        List<TNcDayFinishQty> ncList = dayFinishQtyMapper.selectNcByScheduleDate(formatScheduleDate);
        Map<String, TNcDayFinishQty> map = CollectionUtil.toMap(ncList, TNcDayFinishQty::getLiningCode);
        List<TNcMonthPlanSurplus> oldList = ncMonthPlanSurplusService.getByCodeList(monthPlanApsVersion, new ArrayList<>(map.keySet()));
        if (!CollectionUtil.isEmpty(oldList)) {
            for (TNcMonthPlanSurplus old : oldList) {
                TNcDayFinishQty entity = map.get(old.getMaterialCode());
                buildFinish(old, entity.getDayFinishQty());
                old.setUpdateTime(new Date());
				// 更新状态列表的生产状态以及收尾提醒
				this.updateStatusAndTip(statusList, old, closeOutNum);
            }
            // 回写排程记录上的生产状态以及收尾提醒
            if (statusList.stream().anyMatch(s -> s.isModify())) {
                cxScheduleResultMapper.updateNcProductionStatusByOrderNo(statusList);
            }
        }
        return AjaxResult.success(I18nUtil.getMessage("mes.error.message.nc.finish"));
    }

    /**
     * 15度裁断完成量回报
     * 更新每日各班完成量以及更新排程状态
     */
    @Override
    public AjaxResult mergeCd15Finish(String dataVersion) {
        List<TMesCd15DayFinishQty> mesList = dayFinishQtyMapper.getCd15ByDataVersionAndIsDelete(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
        }
        List<TCd15DayFinishQty> list = new ArrayList<>();
        Map<String, TMesCd15DayFinishQty> mesMap = CollectionUtil.toMap(mesList, TMesCd15DayFinishQty::getOrderNo);
        for (String code : mesMap.keySet()) {
            TMesCd15DayFinishQty finishQty = mesMap.get(code);
            TCd15DayFinishQty entity = new TCd15DayFinishQty();
            entity.setSteelStripCode(finishQty.getSteelStripCode());
            this.setBaseSysValue(entity);
            entity.setOrderNo(finishQty.getOrderNo());
            entity.setScheduleDate(finishQty.getScheduleDate());
            entity.setDayFinishQty(finishQty.getDayFinishQty());
            entity.setNightFinishQty(finishQty.getNightFinishQty());
            list.add(entity);
        }

        // 完成量回报
        // 排产日
        Date scheduleDate = CollectionUtil.firstElement(mesList).getScheduleDate();
        // 月度计划版本号，直接从最新的月度计划抓取
        String monthPlanApsVersion = this.getMonthPlanApsVersion(scheduleDate);
        if (StringUtils.isEmpty(monthPlanApsVersion)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.monthPlan.version"));
        }
		// 构建月度计划状态列表
		List<MonthSurplusStatusVo> statusList = list.stream()
				.map(m -> createStatusVo(m.getOrderNo(), m.getSteelStripCode())).collect(Collectors.toList());
		// 收尾提醒阈值，从系统参数获取
		String closeOutNum = Optional.ofNullable(paramsService.getCd15Param(EngineConstants.CLOSE_OUT_NUM)).orElse("0");
        // 合并
        dayFinishQtyMapper.mergeCd15Finish(list);
        // 汇总数据到 code和dayFinishQty
        String formatScheduleDate = DateUtil.formatMonth(scheduleDate);
        List<TCd15DayFinishQty> cd15List = dayFinishQtyMapper.selectCd15ByScheduleDate(formatScheduleDate);
        Map<String, TCd15DayFinishQty> map = CollectionUtil.toMap(cd15List, TCd15DayFinishQty::getSteelStripCode);
        List<TGdcdMonthPlanSurplus> oldList = cd15MonthPlanSurplusService.getByCodeList(monthPlanApsVersion, new ArrayList<>(map.keySet()));
        if (!CollectionUtil.isEmpty(oldList)) {
            for (TGdcdMonthPlanSurplus old : oldList) {
                TCd15DayFinishQty entity = map.get(old.getMaterialCode());
                buildFinish(old, entity.getDayFinishQty());
                old.setUpdateTime(new Date());
				// 更新状态列表的生产状态以及收尾提醒
				this.updateStatusAndTip(statusList, old, closeOutNum);
            }
            // 回写排程记录上的生产状态以及收尾提醒
            if (statusList.stream().anyMatch(s -> s.isModify())) {
                cxScheduleResultMapper.updateCd15ProductionStatusByOrderNo(statusList);
            }
        }
        return AjaxResult.success(I18nUtil.getMessage("mes.error.message.cd15.finish"));
    }

    /**
     * 90度裁断完成量回报
     * 更新每日各班完成量以及更新排程状态
     */
    @Override
    public AjaxResult mergeCd90Finish(String dataVersion) {
        List<TMesCd90DayFinishQty> mesList = dayFinishQtyMapper.getCd90ByDataVersionAndIsDelete(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
        }
        List<TCd90DayFinishQty> list = new ArrayList<>();
        Map<String, TMesCd90DayFinishQty> mesMap = CollectionUtil.toMap(mesList, TMesCd90DayFinishQty::getOrderNo);
        for (String code : mesMap.keySet()) {
            TMesCd90DayFinishQty finishQty = mesMap.get(code);
            TCd90DayFinishQty entity = new TCd90DayFinishQty();
            entity.setClothCode(finishQty.getClothCode());
            this.setBaseSysValue(entity);
            entity.setOrderNo(finishQty.getOrderNo());
            entity.setScheduleDate(finishQty.getScheduleDate());
            entity.setDayFinishQty(finishQty.getDayFinishQty());
            entity.setNightFinishQty(finishQty.getNightFinishQty());
            list.add(entity);
        }

        // 完成量回报
        // 排产日
        Date scheduleDate = CollectionUtil.firstElement(mesList).getScheduleDate();
        // 月度计划版本号，直接从最新的月度计划抓取
        String monthPlanApsVersion = this.getMonthPlanApsVersion(scheduleDate);
        if (StringUtils.isEmpty(monthPlanApsVersion)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.monthPlan.version"));
        }
		// 构建月度计划状态列表
		List<MonthSurplusStatusVo> statusList = list.stream().map(m -> createStatusVo(m.getOrderNo(), m.getClothCode()))
				.collect(Collectors.toList());
		// 收尾提醒阈值，从系统参数获取
		String closeOutNum = Optional.ofNullable(paramsService.getCd90Param(EngineConstants.CLOSE_OUT_NUM)).orElse("0");
        // 合并
        dayFinishQtyMapper.mergeCd90Finish(list);

        // 汇总数据到 code和dayFinishQty
        String formatScheduleDate = DateUtil.formatMonth(scheduleDate);
        List<TCd90DayFinishQty> cd90List = dayFinishQtyMapper.selectCd90ByScheduleDate(formatScheduleDate);
        Map<String, TCd90DayFinishQty> map = CollectionUtil.toMap(cd90List, TCd90DayFinishQty::getClothCode);
        List<TLbcdMonthPlanSurplus> oldList = cd90MonthPlanSurplusService.getByCodeList(monthPlanApsVersion, new ArrayList<>(map.keySet()));
        if (!CollectionUtil.isEmpty(oldList)) {
            for (TLbcdMonthPlanSurplus old : oldList) {
                TCd90DayFinishQty entity = map.get(old.getMaterialCode());
                buildFinish(old, entity.getDayFinishQty());
                old.setUpdateTime(new Date());
				// 更新状态列表的生产状态以及收尾提醒
				this.updateStatusAndTip(statusList, old, closeOutNum);
            }
            // 回写排程记录上的生产状态以及收尾提醒
            if (statusList.stream().anyMatch(s -> s.isModify())) {
                cxScheduleResultMapper.updateCd90ProductionStatusByOrderNo(statusList);
            }
        }
        return AjaxResult.success(I18nUtil.getMessage("mes.error.message.cd90.finish"));
    }

	/**
	 * 纤维压延完成量回报
     * 更新每日各班完成量以及更新排程状态
	 */
    @Override
    public AjaxResult mergeXwyyFinish(String dataVersion) {
        List<TMesXwyyDayFinishQty> mesList = dayFinishQtyMapper.getXwyyByDataVersionAndIsDelete(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
        }
        List<TXwyyDayFinishQty> list = new ArrayList<>();
        Map<String, TMesXwyyDayFinishQty> mesMap = CollectionUtil.toMap(mesList, TMesXwyyDayFinishQty::getOrderNo);
        for (String code : mesMap.keySet()) {
            TMesXwyyDayFinishQty finishQty = mesMap.get(code);
            TXwyyDayFinishQty entity = new TXwyyDayFinishQty();
            entity.setBigRollCode(finishQty.getBigRollCode());
            this.setBaseSysValue(entity);
            entity.setOrderNo(finishQty.getOrderNo());
            entity.setScheduleDate(finishQty.getScheduleDate());
            entity.setDayFinishQty(finishQty.getDayFinishQty());
            entity.setNightFinishQty(finishQty.getNightFinishQty());
            list.add(entity);
        }

        // 完成量回报
        // 排产日
        Date scheduleDate = CollectionUtil.firstElement(mesList).getScheduleDate();
        // 月度计划版本号，直接从最新的月度计划抓取
        String monthPlanApsVersion = this.getMonthPlanApsVersion(scheduleDate);
        if (StringUtils.isEmpty(monthPlanApsVersion)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.monthPlan.version"));
        }
		// 构建月度计划状态列表
		List<MonthSurplusStatusVo> statusList = list.stream()
				.map(m -> createStatusVo(m.getOrderNo(), m.getBigRollCode())).collect(Collectors.toList());
		// 收尾提醒阈值，从系统参数获取，如果没有配置则默认值为2
		String closeOutNum = Optional.ofNullable(paramsService.getXwyyParam(EngineConstants.CLOSE_OUT_NUM)).orElse("2");
        // 合并
        dayFinishQtyMapper.mergeXwyyFinish(list);

        // 汇总数据到 code和dayFinishQty
        List<TXwyyDayFinishQty> xwyyList = dayFinishQtyMapper.selectXwyyByScheduleDate(DateUtil.formatMonth(scheduleDate));
        Map<String, TXwyyDayFinishQty> map = CollectionUtil.toMap(xwyyList, TXwyyDayFinishQty::getBigRollCode);
        List<String> codeList = new ArrayList<>(map.keySet());
        List<TXwyyMonthPlanSurplus> oldList = xwyyMonthPlanSurplusService.getByCodeList(monthPlanApsVersion, codeList);
        List<CxTCd90BigRoll> bigRollList = cd90BigRollService.getByCrodSpecList(codeList);
        Map<String, CxTCd90BigRoll> bigRollMap = CollectionUtil.toMap(bigRollList, CxTCd90BigRoll::getBigRollCode);
        // 大卷标准长度默认信息
        CxTCd90Params cd90Params = cd90ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);
        if (!CollectionUtil.isEmpty(oldList)) {
            for (TXwyyMonthPlanSurplus old : oldList) {
                TXwyyDayFinishQty entity = map.get(old.getMaterialCode());
                buildFinish(old, entity.getDayFinishQty());
                old.setUpdateTime(new Date());
                CxTCd90BigRoll cd90BigRoll = bigRollMap.get(old.getMaterialCode());
                // 如果大卷信息维护表中没有数据，从默认表中取
                if (cd90BigRoll != null && cd90BigRoll.getActClothLength() != null) {
                    BigDecimal xwyyPlanNum = BigDecimal.ZERO;
                    BigDecimal xwyyModifyNum = BigDecimal.ZERO;
                    BigDecimal xwyyFinishNum = BigDecimal.ZERO;
                    if (!cd90BigRoll.getActClothLength().equals(BigDecimal.ZERO)) {
                        xwyyPlanNum = old.getMonthPlanQty().divide(cd90BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
                        xwyyModifyNum = old.getMonthPlanModifyQty().divide(cd90BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
                        xwyyFinishNum = entity.getDayFinishQty().divide(cd90BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
                    }
                    old.setMonthPlanQty2(xwyyPlanNum);
                    old.setMonthPlanModifyQty2(xwyyModifyNum);
                    old.setMonthFinishQty2(xwyyFinishNum);
                } else {
                    BigDecimal xwyyPlanNum = BigDecimal.ZERO;
                    BigDecimal xwyyModifyNum = BigDecimal.ZERO;
                    BigDecimal xwyyFinishNum = BigDecimal.ZERO;
                    if (cd90Params != null && StringUtils.isNotBlank(cd90Params.getParamValue()) && !Double.valueOf(cd90Params.getParamValue()).equals(0d)) {
                        xwyyPlanNum = old.getMonthPlanQty().divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 0, BigDecimal.ROUND_UP);
                        xwyyModifyNum = old.getMonthPlanModifyQty().divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 0, BigDecimal.ROUND_UP);
                        xwyyFinishNum = entity.getDayFinishQty().divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 0, BigDecimal.ROUND_UP);
                    }
                    old.setMonthPlanQty2(xwyyPlanNum);
                    old.setMonthPlanModifyQty2(xwyyModifyNum);
                    old.setMonthFinishQty2(xwyyFinishNum);
                }
                old.setMonthRemainQty2(old.getMonthPlanQty2().add(old.getMonthPlanModifyQty2()).subtract(old.getMonthFinishQty2()));

				// 物料编号
				String materialCode = old.getMaterialCode();
                // 完成量（个）
				BigDecimal monthFinishQty2 = old.getMonthFinishQty2();
				// 剩余量（个）
				BigDecimal monthRemainQty2 = old.getMonthRemainQty2();
				// 由于纤维压延是要小于等于，因此要加1
				closeOutNum = new BigDecimal(closeOutNum).add(BigDecimal.ONE).toString();
				// 更新状态列表的生产状态以及收尾提醒
				this.updateStatusAndTip(statusList, materialCode, monthFinishQty2, monthRemainQty2, closeOutNum);
            }
            // 回写排程记录上的生产状态以及收尾提醒
            if (statusList.stream().anyMatch(s -> s.isModify())) {
                cxScheduleResultMapper.updateXwyyProductionStatusByOrderNo(statusList);
            }
        }
        return AjaxResult.success(I18nUtil.getMessage("mes.error.message.xwyy.finish"));
    }


	/**
	 * 胎圈完成量回报
     * 更新每日各班完成量以及更新排程状态
	 */
    @Override
    public AjaxResult mergeTqFinish(String dataVersion) {
        List<TMesTqDayFinishQty> mesList = dayFinishQtyMapper.getTqByDataVersionAndIsDelete(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
        }
        List<TTqDayFinishQty> list = new ArrayList<>();
        Map<String, TMesTqDayFinishQty> mesMap = CollectionUtil.toMap(mesList, TMesTqDayFinishQty::getOrderNo);
        for (String code : mesMap.keySet()) {
            TMesTqDayFinishQty finishQty = mesMap.get(code);
            TTqDayFinishQty entity = new TTqDayFinishQty();
            entity.setBeadCode(finishQty.getBeadCode());
            this.setBaseSysValue(entity);
            entity.setOrderNo(finishQty.getOrderNo());
            entity.setScheduleDate(finishQty.getScheduleDate());
            entity.setDayFinishQty(finishQty.getDayFinishQty());
            entity.setNightFinishQty(finishQty.getNightFinishQty());
            entity.setMidFinishQty(finishQty.getMidFinishQty());
            list.add(entity);
        }

        // 完成量回报
        // 排产日
        Date scheduleDate = CollectionUtil.firstElement(mesList).getScheduleDate();
        // 月度计划版本号，直接从最新的月度计划抓取
        String monthPlanApsVersion = this.getMonthPlanApsVersion(scheduleDate);
        if (StringUtils.isEmpty(monthPlanApsVersion)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.monthPlan.version"));
        }
		// 构建月度计划状态列表
		List<MonthSurplusStatusVo> statusList = list.stream().map(m -> createStatusVo(m.getOrderNo(), m.getBeadCode()))
				.collect(Collectors.toList());
		// 收尾提醒阈值，从系统参数获取
		String closeOutNum = Optional.ofNullable(paramsService.getTqParam(EngineConstants.CLOSE_OUT_NUM)).orElse("0");
        // 合并
        dayFinishQtyMapper.mergeTqFinish(list);

        // 汇总数据到 code和dayFinishQty
        String formatScheduleDate = DateUtil.formatMonth(scheduleDate);
        List<TTqDayFinishQty> tqList = dayFinishQtyMapper.selectTqByScheduleDate(formatScheduleDate);
        Map<String, TTqDayFinishQty> map = CollectionUtil.toMap(tqList, TTqDayFinishQty::getBeadCode);
        List<TTqMonthPlanSurplus> oldList = tqMonthPlanSurplusService.getByCodeList(monthPlanApsVersion, new ArrayList<>(map.keySet()));
        if (!CollectionUtil.isEmpty(oldList)) {
            for (TTqMonthPlanSurplus old : oldList) {
                TTqDayFinishQty entity = map.get(old.getMaterialCode());
                buildFinish(old, entity.getDayFinishQty());
                old.setUpdateTime(new Date());
				// 更新状态列表的生产状态以及收尾提醒
				this.updateStatusAndTip(statusList, old, closeOutNum);
            }
            // 回写排程记录上的生产状态以及收尾提醒
            if (statusList.stream().anyMatch(s -> s.isModify())) {
                cxScheduleResultMapper.updateTqProductionStatusByOrderNo(statusList);
            }
        }
        return AjaxResult.success(I18nUtil.getMessage("mes.error.message.tq.finish"));
    }

    /**
     * 钢丝圈完成量回报
     * 更新每日各班完成量以及更新排程状态
     */
    @Override
    public AjaxResult mergeGsqFinish(String dataVersion) {
        List<TMesGsqDayFinishQty> mesList = dayFinishQtyMapper.getGsqByDataVersionAndIsDelete(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
        }
        List<TGsqDayFinishQty> list = new ArrayList<>();
        Map<String, TMesGsqDayFinishQty> mesMap = CollectionUtil.toMap(mesList, TMesGsqDayFinishQty::getOrderNo);
        for (String code : mesMap.keySet()) {
            TMesGsqDayFinishQty finishQty = mesMap.get(code);
            TGsqDayFinishQty entity = new TGsqDayFinishQty();
            entity.setSteelRingCode(finishQty.getSteelRingCode());
            this.setBaseSysValue(entity);
            entity.setOrderNo(finishQty.getOrderNo());
            entity.setScheduleDate(finishQty.getScheduleDate());
            entity.setDayFinishQty(finishQty.getDayFinishQty());
            entity.setNightFinishQty(finishQty.getNightFinishQty());
            entity.setMidFinishQty(finishQty.getMidFinishQty());
            list.add(entity);
        }

        // 完成量回报
        // 排产日
        Date scheduleDate = CollectionUtil.firstElement(mesList).getScheduleDate();
        // 月度计划版本号，直接从最新的月度计划抓取
        String monthPlanApsVersion = this.getMonthPlanApsVersion(scheduleDate);
        if (StringUtils.isEmpty(monthPlanApsVersion)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.monthPlan.version"));
        }
		// 构建月度计划状态列表
		List<MonthSurplusStatusVo> statusList = list.stream()
				.map(m -> createStatusVo(m.getOrderNo(), m.getSteelRingCode())).collect(Collectors.toList());
		// 收尾提醒阈值，从系统参数获取
		String closeOutNum = Optional.ofNullable(paramsService.getGsqParam(EngineConstants.CLOSE_OUT_NUM)).orElse("0");
        // 合并
        dayFinishQtyMapper.mergeGsqFinish(list);

        // 汇总数据到 code和dayFinishQty
        String formatScheduleDate = DateUtil.formatMonth(scheduleDate);
        List<TGsqDayFinishQty> gsqList = dayFinishQtyMapper.selectGsqByScheduleDate(formatScheduleDate);

        Map<String, TGsqDayFinishQty> map = CollectionUtil.toMap(gsqList, TGsqDayFinishQty::getSteelRingCode);
        List<TGsqMonthPlanSurplus> oldList = gsqMonthPlanSurplusService.getByCodeList(monthPlanApsVersion, new ArrayList<>(map.keySet()));
        if (!CollectionUtil.isEmpty(oldList)) {
            for (TGsqMonthPlanSurplus old : oldList) {
                TGsqDayFinishQty entity = map.get(old.getMaterialCode());
                buildFinish(old, entity.getDayFinishQty());
                old.setUpdateTime(new Date());
				// 更新状态列表的生产状态以及收尾提醒
				this.updateStatusAndTip(statusList, old, closeOutNum);
            }
            // 回写排程记录上的生产状态以及收尾提醒
            if (statusList.stream().anyMatch(s -> s.isModify())) {
                cxScheduleResultMapper.updateGsqProductionStatusByOrderNo(statusList);
            }
        }
        return AjaxResult.success(I18nUtil.getMessage("mes.error.message.gsq.finish"));
    }
    

	/**
	 * 更新状态列表的生产状态以及收尾提醒
	 * 
	 * @param statusList  状态列表
	 * @param monthPlan   月度计划明细
	 * @param closeOutNum 收尾提醒阈值
	 */
	private void updateStatusAndTip(List<MonthSurplusStatusVo> statusList, MonthPlanSurplusBaseEntity monthPlan,
			String closeOutNum) {
		// 物料编号
		String materialCode = monthPlan.getMaterialCode();
		// 完成量
		BigDecimal monthFinishQty = monthPlan.getMonthFinishQty();
		// 剩余量
		BigDecimal monthRemainQty = monthPlan.getMonthRemainQty();
		// 更新状态
		this.updateStatusAndTip(statusList, materialCode, monthFinishQty, monthRemainQty, closeOutNum);
	}
	

	/**
	 * 胎面日完成量回报，需要更新半部件月度计划汇总记录
	 */
	@Override
	public AjaxResult mergeTmDayFinish(String dataVersion) {
		List<TTmDayFinishQty> mesList = mesDayFinishTotalMapper.getTmDayFinishByDataVersion(dataVersion);
		if (CollectionUtil.isEmpty(mesList)) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
		}
		// 合并
		mesDayFinishTotalMapper.mergeTmDayFinish(dataVersion);
		// 排产日
		Date scheduleDate = CollectionUtil.firstElement(mesList).getScheduleDate();
		// 汇总数据到 code和dayFinishQty
		String formatScheduleDate = DateUtil.formatMonth(scheduleDate);
		List<TTmDayFinishQty> tmList = mesDayFinishTotalMapper.selectTmByScheduleDate(formatScheduleDate);

		// 月度计划版本号，直接从最新的月度计划抓取
		String monthPlanApsVersion = this.getMonthPlanApsVersion(scheduleDate);
		if (StringUtils.isEmpty(monthPlanApsVersion)) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.monthPlan.version"));
		}
		Map<String, TTmDayFinishQty> tmMap = CollectionUtil.toMap(tmList, TTmDayFinishQty::getTreadCode);
		// 取出半部件旧月度计划汇总数据
		List<TTmMonthPlanSurplus> oldList = tmMonthPlanSurplusService.getByCodeList(monthPlanApsVersion,
				new ArrayList<>(tmMap.keySet()));
		if (!CollectionUtil.isEmpty(oldList)) {
			for (TTmMonthPlanSurplus old : oldList) {
				TTmDayFinishQty entity = tmMap.get(old.getMaterialCode());
				// 完成量=日班完成量
				buildFinish(old, entity.getDayFinishQty());
				old.setUpdateTime(new Date());
			}
			// 更新半部件月度计划汇总表
			monthPlanSurplusService.mergeTm(oldList);
		}

		return AjaxResult.success(I18nUtil.getMessage("mes.error.message.tm.finish"));
	}

	/**
	 * 胎侧日完成量回报，需要更新半部件月度计划汇总记录
	 */
	@Override
	public AjaxResult mergeTcDayFinish(String dataVersion) {
		List<TTcDayFinishQty> mesList = mesDayFinishTotalMapper.getTcDayFinishByDataVersion(dataVersion);
		if (CollectionUtil.isEmpty(mesList)) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
		}
		// 合并
		mesDayFinishTotalMapper.mergeTcDayFinish(dataVersion);
		// 排产日
		Date scheduleDate = CollectionUtil.firstElement(mesList).getScheduleDate();
		// 汇总数据到 code和dayFinishQty
		String formatScheduleDate = DateUtil.formatMonth(scheduleDate);
		List<TTcDayFinishQty> tcList = mesDayFinishTotalMapper.selectTcByScheduleDate(formatScheduleDate);

		// 月度计划版本号，直接从最新的月度计划抓取
		String monthPlanApsVersion = this.getMonthPlanApsVersion(scheduleDate);
		if (StringUtils.isEmpty(monthPlanApsVersion)) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.monthPlan.version"));
		}
		Map<String, TTcDayFinishQty> tcMap = CollectionUtil.toMap(tcList, TTcDayFinishQty::getSidewallCode);
		// 取出半部件旧月度计划汇总数据
		List<TTcMonthPlanSurplus> oldList = tcMonthPlanSurplusService.getByCodeList(monthPlanApsVersion,
				new ArrayList<>(tcMap.keySet()));
		if (!CollectionUtil.isEmpty(oldList)) {
			for (TTcMonthPlanSurplus old : oldList) {
				TTcDayFinishQty entity = tcMap.get(old.getMaterialCode());
				// 完成量=日班完成量
				buildFinish(old, entity.getDayFinishQty());
				old.setUpdateTime(new Date());
			}
			// 更新半部件月度计划汇总表
			monthPlanSurplusService.mergeTc(oldList);
		}

		return AjaxResult.success(I18nUtil.getMessage("mes.error.message.tc.finish"));
	}

	/**
	 * 内衬日完成量回报，需要更新半部件月度计划汇总记录
	 */
	@Override
	public AjaxResult mergeNcDayFinish(String dataVersion) {
		List<TNcDayFinishQty> mesList = mesDayFinishTotalMapper.getNcDayFinishByDataVersion(dataVersion);
		if (CollectionUtil.isEmpty(mesList)) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
		}
		// 合并
		mesDayFinishTotalMapper.mergeNcDayFinish(dataVersion);
		// 排产日
		Date scheduleDate = CollectionUtil.firstElement(mesList).getScheduleDate();
		// 汇总数据到 code和dayFinishQty
		String formatScheduleDate = DateUtil.formatMonth(scheduleDate);
		List<TNcDayFinishQty> ncList = mesDayFinishTotalMapper.selectNcByScheduleDate(formatScheduleDate);

		// 月度计划版本号，直接从最新的月度计划抓取
		String monthPlanApsVersion = this.getMonthPlanApsVersion(scheduleDate);
		if (StringUtils.isEmpty(monthPlanApsVersion)) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.monthPlan.version"));
		}
		Map<String, TNcDayFinishQty> ncMap = CollectionUtil.toMap(ncList, TNcDayFinishQty::getLiningCode);
		// 取出半部件旧月度计划汇总数据
		List<TNcMonthPlanSurplus> oldList = ncMonthPlanSurplusService.getByCodeList(monthPlanApsVersion,
				new ArrayList<>(ncMap.keySet()));
		if (!CollectionUtil.isEmpty(oldList)) {
			for (TNcMonthPlanSurplus old : oldList) {
				TNcDayFinishQty entity = ncMap.get(old.getMaterialCode());
				// 完成量=日班完成量
				buildFinish(old, entity.getDayFinishQty());
				old.setUpdateTime(new Date());
			}
			// 更新半部件月度计划汇总表
			monthPlanSurplusService.mergeNc(oldList);
		}

		return AjaxResult.success(I18nUtil.getMessage("mes.error.message.nc.finish"));
	}

	/**
	 * 15度裁断日完成量回报 需要更新半部件月度计划汇总记录
	 */
	@Override
	public AjaxResult mergeCd15DayFinish(String dataVersion) {
		List<TCd15DayFinishQty> mesList = mesDayFinishTotalMapper.getCd15DayFinishByDataVersion(dataVersion);
		if (CollectionUtil.isEmpty(mesList)) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
		}
		// 合并
		mesDayFinishTotalMapper.mergeCd15DayFinish(dataVersion);
		// 排产日
		Date scheduleDate = CollectionUtil.firstElement(mesList).getScheduleDate();
		// 汇总数据到 code和dayFinishQty
		String formatScheduleDate = DateUtil.formatMonth(scheduleDate);
		List<TCd15DayFinishQty> cd15List = mesDayFinishTotalMapper.selectCd15ByScheduleDate(formatScheduleDate);

		// 月度计划版本号，直接从最新的月度计划抓取
		String monthPlanApsVersion = this.getMonthPlanApsVersion(scheduleDate);
		if (StringUtils.isEmpty(monthPlanApsVersion)) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.monthPlan.version"));
		}
		Map<String, TCd15DayFinishQty> cd15Map = CollectionUtil.toMap(cd15List, TCd15DayFinishQty::getSteelStripCode);
		// 取出半部件旧月度计划汇总数据
		List<TGdcdMonthPlanSurplus> oldList = cd15MonthPlanSurplusService.getByCodeList(monthPlanApsVersion,
				new ArrayList<>(cd15Map.keySet()));
		if (!CollectionUtil.isEmpty(oldList)) {
			for (TGdcdMonthPlanSurplus old : oldList) {
				TCd15DayFinishQty entity = cd15Map.get(old.getMaterialCode());
				// 完成量=日班完成量
				buildFinish(old, entity.getDayFinishQty());
				old.setUpdateTime(new Date());
			}
			// 更新半部件月度计划汇总表
			monthPlanSurplusService.mergeCd15(oldList);
		}

		return AjaxResult.success(I18nUtil.getMessage("mes.error.message.cd15.finish"));
	}

	/**
	 * 90度裁断日完成量回报，需要更新半部件月度计划汇总记录
	 */
	@Override
	public AjaxResult mergeCd90DayFinish(String dataVersion) {
		List<TCd90DayFinishQty> mesList = mesDayFinishTotalMapper.getCd90DayFinishByDataVersion(dataVersion);
		if (CollectionUtil.isEmpty(mesList)) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
		}
		// 合并
		mesDayFinishTotalMapper.mergeCd90DayFinish(dataVersion);
		// 排产日
		Date scheduleDate = CollectionUtil.firstElement(mesList).getScheduleDate();
		// 汇总数据到 code和dayFinishQty
		String formatScheduleDate = DateUtil.formatMonth(scheduleDate);
		List<TCd90DayFinishQty> cd90List = mesDayFinishTotalMapper.selectCd90ByScheduleDate(formatScheduleDate);

		// 月度计划版本号，直接从最新的月度计划抓取
		String monthPlanApsVersion = this.getMonthPlanApsVersion(scheduleDate);
		if (StringUtils.isEmpty(monthPlanApsVersion)) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.monthPlan.version"));
		}
		Map<String, TCd90DayFinishQty> cd90Map = CollectionUtil.toMap(cd90List, TCd90DayFinishQty::getClothCode);
		// 取出半部件旧月度计划汇总数据
		List<TLbcdMonthPlanSurplus> oldList = cd90MonthPlanSurplusService.getByCodeList(monthPlanApsVersion,
				new ArrayList<>(cd90Map.keySet()));
		if (!CollectionUtil.isEmpty(oldList)) {
			for (TLbcdMonthPlanSurplus old : oldList) {
				TCd90DayFinishQty entity = cd90Map.get(old.getMaterialCode());
				// 完成量=日班完成量
				buildFinish(old, entity.getDayFinishQty());
				old.setUpdateTime(new Date());
			}
			// 更新半部件月度计划汇总表
			monthPlanSurplusService.mergeCd90(oldList);
		}

		return AjaxResult.success(I18nUtil.getMessage("mes.error.message.cd90.finish"));
	}

	/**
	 * 纤维压延日完成量回报，需要更新半部件月度计划汇总记录
	 */
	@Override
	public AjaxResult mergeXwyyDayFinish(String dataVersion) {
		List<TXwyyDayFinishQty> mesList = mesDayFinishTotalMapper.getXwyyDayFinishByDataVersion(dataVersion);
		if (CollectionUtil.isEmpty(mesList)) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
		}
		// 合并
		mesDayFinishTotalMapper.mergeXwyyDayFinish(dataVersion);
		// 排产日
		Date scheduleDate = CollectionUtil.firstElement(mesList).getScheduleDate();
		// 汇总数据到 code和dayFinishQty
		String formatScheduleDate = DateUtil.formatMonth(scheduleDate);
		List<TXwyyDayFinishQty> xwyyList = mesDayFinishTotalMapper.selectXwyyByScheduleDate(formatScheduleDate);

		// 月度计划版本号，直接从最新的月度计划抓取
		String monthPlanApsVersion = this.getMonthPlanApsVersion(scheduleDate);
		if (StringUtils.isEmpty(monthPlanApsVersion)) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.monthPlan.version"));
		}
		Map<String, TXwyyDayFinishQty> xwyyMap = CollectionUtil.toMap(xwyyList, TXwyyDayFinishQty::getBigRollCode);
		// 取出半部件旧月度计划汇总数据
		List<TXwyyMonthPlanSurplus> oldList = xwyyMonthPlanSurplusService.getByCodeList(monthPlanApsVersion,
				new ArrayList<>(xwyyMap.keySet()));
		// 大卷信息
        List<CxTCd90BigRoll> bigRollList = cd90BigRollService.getByCrodSpecList(new ArrayList<>(xwyyMap.keySet()));
        Map<String, CxTCd90BigRoll> bigRollMap = CollectionUtil.toMap(bigRollList, CxTCd90BigRoll::getBigRollCode);
        // 大卷标准长度默认信息
        CxTCd90Params cd90Params = cd90ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);

        if (!CollectionUtil.isEmpty(oldList)) {
			for (TXwyyMonthPlanSurplus old : oldList) {
				TXwyyDayFinishQty entity = xwyyMap.get(old.getMaterialCode());
				// 完成量=日班完成量
				buildFinish(old, entity.getDayFinishQty());
				old.setUpdateTime(new Date());
                CxTCd90BigRoll cd90BigRoll = bigRollMap.get(old.getMaterialCode());
                // 如果大卷信息维护表中没有数据，从默认表中取
                if (cd90BigRoll != null && cd90BigRoll.getActClothLength() != null) {
                    BigDecimal xwyyPlanNum = BigDecimal.ZERO;
                    BigDecimal xwyyModifyNum = BigDecimal.ZERO;
                    BigDecimal xwyyFinishNum = BigDecimal.ZERO;
                    if (!cd90BigRoll.getActClothLength().equals(BigDecimal.ZERO)) {
                        xwyyPlanNum = old.getMonthPlanQty().divide(cd90BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
                        xwyyModifyNum = old.getMonthPlanModifyQty().divide(cd90BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
                        xwyyFinishNum = entity.getDayFinishQty().divide(cd90BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
                    }
                    old.setMonthPlanQty2(xwyyPlanNum);
                    old.setMonthPlanModifyQty2(xwyyModifyNum);
                    old.setMonthFinishQty2(xwyyFinishNum);
                } else {
                    BigDecimal xwyyPlanNum = BigDecimal.ZERO;
                    BigDecimal xwyyModifyNum = BigDecimal.ZERO;
                    BigDecimal xwyyFinishNum = BigDecimal.ZERO;
                    if (cd90Params != null && StringUtils.isNotBlank(cd90Params.getParamValue()) && !Double.valueOf(cd90Params.getParamValue()).equals(0d)) {
                        xwyyPlanNum = old.getMonthPlanQty().divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 0, BigDecimal.ROUND_UP);
                        xwyyModifyNum = old.getMonthPlanModifyQty().divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 0, BigDecimal.ROUND_UP);
                        xwyyFinishNum = entity.getDayFinishQty().divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 0, BigDecimal.ROUND_UP);
                    }
                    old.setMonthPlanQty2(xwyyPlanNum);
                    old.setMonthPlanModifyQty2(xwyyModifyNum);
                    old.setMonthFinishQty2(xwyyFinishNum);
                }
                old.setMonthRemainQty2(old.getMonthPlanQty2().add(old.getMonthPlanModifyQty2()).subtract(old.getMonthFinishQty2()));
            }
			// 更新半部件月度计划汇总表
			monthPlanSurplusService.mergeXwyy(oldList);
		}

		return AjaxResult.success(I18nUtil.getMessage("mes.error.message.xwyy.finish"));
	}

	/**
	 * 胎圈日完成量回报，需要更新半部件月度计划汇总记录
	 */
	@Override
	public AjaxResult mergeTqDayFinish(String dataVersion) {
		List<TTqDayFinishQty> mesList = mesDayFinishTotalMapper.getTqDayFinishByDataVersion(dataVersion);
		if (CollectionUtil.isEmpty(mesList)) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
		}
		// 合并
		mesDayFinishTotalMapper.mergeTqDayFinish(dataVersion);
		// 排产日
		Date scheduleDate = CollectionUtil.firstElement(mesList).getScheduleDate();
		// 汇总数据到 code和dayFinishQty
		String formatScheduleDate = DateUtil.formatMonth(scheduleDate);
		List<TTqDayFinishQty> tqList = mesDayFinishTotalMapper.selectTqByScheduleDate(formatScheduleDate);

		// 月度计划版本号，直接从最新的月度计划抓取
		String monthPlanApsVersion = this.getMonthPlanApsVersion(scheduleDate);
		if (StringUtils.isEmpty(monthPlanApsVersion)) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.monthPlan.version"));
		}
		Map<String, TTqDayFinishQty> tqMap = CollectionUtil.toMap(tqList, TTqDayFinishQty::getBeadCode);
		// 取出半部件旧月度计划汇总数据
		List<TTqMonthPlanSurplus> oldList = tqMonthPlanSurplusService.getByCodeList(monthPlanApsVersion,
				new ArrayList<>(tqMap.keySet()));
		if (!CollectionUtil.isEmpty(oldList)) {
			for (TTqMonthPlanSurplus old : oldList) {
				TTqDayFinishQty entity = tqMap.get(old.getMaterialCode());
				// 完成量=日班完成量
				buildFinish(old, entity.getDayFinishQty());
				old.setUpdateTime(new Date());
			}
			// 更新半部件月度计划汇总表
			monthPlanSurplusService.mergeTq(oldList);
		}

		return AjaxResult.success(I18nUtil.getMessage("mes.error.message.tq.finish"));
	}

	/**
	 * 钢丝圈日完成量回报，需要更新半部件月度计划汇总记录
	 */
	@Override
	public AjaxResult mergeGsqDayFinish(String dataVersion) {
		List<TGsqDayFinishQty> mesList = mesDayFinishTotalMapper.getGsqDayFinishByDataVersion(dataVersion);
		if (CollectionUtil.isEmpty(mesList)) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
		}
		// 合并
		mesDayFinishTotalMapper.mergeGsqDayFinish(dataVersion);
		// 排产日
		Date scheduleDate = CollectionUtil.firstElement(mesList).getScheduleDate();
		// 汇总数据到 code和dayFinishQty
		String formatScheduleDate = DateUtil.formatMonth(scheduleDate);
		List<TGsqDayFinishQty> gsqList = mesDayFinishTotalMapper.selectGsqByScheduleDate(formatScheduleDate);

		// 月度计划版本号，直接从最新的月度计划抓取
		String monthPlanApsVersion = this.getMonthPlanApsVersion(scheduleDate);
		if (StringUtils.isEmpty(monthPlanApsVersion)) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.monthPlan.version"));
		}
		Map<String, TGsqDayFinishQty> gsqMap = CollectionUtil.toMap(gsqList, TGsqDayFinishQty::getSteelRingCode);
		// 取出半部件旧月度计划汇总数据
		List<TGsqMonthPlanSurplus> oldList = gsqMonthPlanSurplusService.getByCodeList(monthPlanApsVersion,
				new ArrayList<>(gsqMap.keySet()));
		if (!CollectionUtil.isEmpty(oldList)) {
			for (TGsqMonthPlanSurplus old : oldList) {
				TGsqDayFinishQty entity = gsqMap.get(old.getMaterialCode());
				// 完成量=日班完成量
				buildFinish(old, entity.getDayFinishQty());
				old.setUpdateTime(new Date());
			}
			// 更新半部件月度计划汇总表
			monthPlanSurplusService.mergeGsq(oldList);
		}

		return AjaxResult.success(I18nUtil.getMessage("mes.error.message.gsq.finish"));
	}

	/**
	 * 各工序班次完成量同步
	 */
	@Override
	public AjaxResult mergeClassFinishQty(String dataVersion) {
		// 通过版本号获取需要同步的数据
		List<ClassFinishQty> mesList = mesDayFinishTotalMapper.listMesClassFinishQty(dataVersion);
		List<ClassFinishQty> resultList = new ArrayList<>();
		for (ClassFinishQty qty : mesList) {
			// 完成日期
			Date finishDate = qty.getFinishDate();
			// 完成量
			BigDecimal finishQty = qty.getFinishQty();
			// 物料号
			String materialCode = qty.getMaterialCode();
			// 工序号
			String procedureCode = qty.getProcedureCode();
			
			// 移除无效数据
			if (procedureCode == null || finishDate == null || finishQty == null || materialCode == null) {
				continue;
			}
			// 硫化的规格应该显示sap
			if (ApsConstant.PROCEDURE_CODE_LH.equals(procedureCode)) {
				if (qty.getSapCode() == null) {
					continue;
				}
				qty.setMaterialCode(qty.getSapCode());
			}
			// 将完成量放置到对应的栏位中
			if (FinishClassEnum.CLASS3.getCode().equals(qty.getQueryCode())) {
				qty.setClass3FinishQty(finishQty);
			} else if (FinishClassEnum.CLASS3_2.getCode().equals(qty.getQueryCode())) {
				// 如果是两班制的3班，则日期需要调整成下一天
				qty.setFinishDate(DateUtils.addDays(finishDate, 1));
				qty.setClass3FinishQty(finishQty);
			} else if (FinishClassEnum.CLASS1.getCode().equals(qty.getQueryCode())) {
				// 如果是1班，则日期需要调整成下一天
				qty.setFinishDate(DateUtils.addDays(finishDate, 1));
				qty.setClass1FinishQty(finishQty);
			} else if (FinishClassEnum.CLASS2.getCode().equals(qty.getQueryCode())) {
				qty.setClass2FinishQty(finishQty);
			}
			resultList.add(qty);
		}
		if (CollectionUtil.isEmpty(resultList)) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
		}
		mesDayFinishTotalMapper.mergeClassFinishQty(resultList);
		return AjaxResult.success();
	}

	/**
	 * 更新状态列表的生产状态以及收尾提醒
	 * 
	 * @param statusList     状态列表
	 * @param materialCode   物料编号
	 * @param monthFinishQty 月度完成量
	 * @param monthRemainQty 月度剩余量
	 * @param closeOutNum    收尾提醒阈值
	 */
	private void updateStatusAndTip(List<MonthSurplusStatusVo> statusList, String materialCode,
			BigDecimal monthFinishQty, BigDecimal monthRemainQty, String closeOutNum) {
		
		BigDecimal numCloseOutNum = new BigDecimal(closeOutNum);
		// 生产状态
		String productionStatus;
		if (monthFinishQty.compareTo(BigDecimal.ZERO) > 0 && monthRemainQty.compareTo(BigDecimal.ZERO) > 0) {
			// 完成量大于0，月度计划量也大于0，说明处于生产中
			productionStatus = EngineConstants.PRODUCTION_STATUS_ING;
		} else if (monthFinishQty.compareTo(BigDecimal.ZERO) > 0 && monthRemainQty.compareTo(BigDecimal.ZERO) <= 0) {
			// 完成量大于0，剩余量小于等于0，说明已生产完成
			productionStatus = EngineConstants.PRODUCTION_STATUS_FINISH;
		} else {
			// 其余情况均为未生产
			productionStatus = EngineConstants.PRODUCTION_STATUS_NOT;
		}
		// 收尾提示
		// 帘布大卷月计划剩余量（个）小于收尾提醒阈值，则说明需要收尾
		String markCloseOutTip = monthRemainQty.compareTo(numCloseOutNum) < 0 ? EngineConstants.CLOSE_TIP_NEED
				: EngineConstants.CLOSE_TIP_NOT;
		statusList.stream().filter(s -> s.getMaterialCode().equals(materialCode)).forEach(m -> {
			m.setMarkCloseOutTip(markCloseOutTip);
			m.setProductionStatus(productionStatus);
			m.setModify(true);
		});
	}

	/**
	 * 获取排产日最新的月度计划版本
	 * 
	 * @param scheduleDate 排产日
	 * @return 月度计划版本
	 */
	private String getMonthPlanApsVersion(Date scheduleDate) {
		if (scheduleDate == null) {
			return null;
		}
		// 取出排产日期的年月
		String year = DateUtils.parseDateToStr("yyyy", scheduleDate);
		String month = DateUtils.parseDateToStr("MM", scheduleDate);
		// 获取最新的月度计划版本
		return monthPlanSurplusService.selectMonthPlanApsVersion(year, month);
	}

	/**
	 * 构建月度计划状态值对象
	 * 
	 * @param orderNo      工单号
	 * @param materialCode 物料编码
	 * @return
	 */
	private MonthSurplusStatusVo createStatusVo(String orderNo, String materialCode) {
		MonthSurplusStatusVo statusVo = new MonthSurplusStatusVo();
		statusVo.setOrderNo(orderNo);
		statusVo.setMaterialCode(materialCode);
		return statusVo;
	}


    /**
     * 设置默认值
     * @param entity
     * @param <K>
     */
    private <K extends ApsBaseEntity> void  setBaseSysValue(K entity) {
        try {
            entity.setBaseVale(null);
        } catch (Exception e) {
            entity.setDelFlag("0");
            entity.setCreateBy("system");
            entity.setUpdateBy("system");
            entity.setCreateTime(new Date());
            entity.setUpdateTime(new Date());
        }
    }
}
