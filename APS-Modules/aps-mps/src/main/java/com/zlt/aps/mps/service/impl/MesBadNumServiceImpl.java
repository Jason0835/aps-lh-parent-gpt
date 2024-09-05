package com.zlt.aps.mps.service.impl;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.aps.common.engine.domain.TCxEmbryoMonthPlanSurplus;
import com.zlt.aps.common.engine.domain.TCxMonthPlanSurplus;
import com.zlt.aps.common.engine.domain.TSapEmbryoBadNumber;
import com.zlt.aps.common.engine.service.MonthPlanService;
import com.zlt.aps.common.engine.service.TCxEmbryoMonthPlanSurplusService;
import com.zlt.aps.common.engine.service.TCxMonthPlanSurplusService;
import com.zlt.aps.common.engine.service.TSapEmbryoBadNumberService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.mps.domain.TMesSapEmbryoBadNumber;
import com.zlt.aps.mps.mapper.TMesSapEmbryoBadNumberMapper;
import com.zlt.aps.mps.service.MesBadNumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * @author Gim
 */
@Service
public class MesBadNumServiceImpl implements MesBadNumService {

    @Resource
    private TMesSapEmbryoBadNumberMapper badNumberMapper;

    @Autowired
    private TSapEmbryoBadNumberService badNumberService;
    @Autowired
    private TCxMonthPlanSurplusService wtService;
    @Autowired
    private TCxEmbryoMonthPlanSurplusService tpService;
    @Autowired
    private MonthPlanService monthPlanService;

    @Override
    public AjaxResult mergeBadNum(String dataVersion) {
        List<TMesSapEmbryoBadNumber> mesList = badNumberMapper.selectAllByDataVersionAndIsDelete(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error(I18nUtil.getMessage("mdm.error.message.embryo.bad.empty"));
        }
        List<TSapEmbryoBadNumber> list = new ArrayList<>();
//        List<String> sapBadCodeList = new ArrayList<>();
        List<String> embryoBadCodeList = new ArrayList<>();
        for (TMesSapEmbryoBadNumber mes : mesList) {
            TSapEmbryoBadNumber entity = new TSapEmbryoBadNumber();
            entity.setBadDate(mes.getBadDate());
            entity.setEmbryoCode(mes.getEmbryoCode());
            entity.setSapCode(mes.getSapCode());
            entity.setBadNum(mes.getBadNum());
            entity.setBomDataVersion(mes.getBomDataVersion());
            this.setBaseSysValue(entity);
            list.add(entity);
//            sapBadCodeList.add(entity.getSapCode());
            embryoBadCodeList.add(entity.getEmbryoCode());
        }
        badNumberService.mergeSql(list);
        Date badDate = list.get(0).getBadDate();
        String badMonth = DateUtil.formatMonth(badDate);
//        List<TSapEmbryoBadNumber> wtBadList = badNumberService.getSapByParams(badMonth, sapBadCodeList);
        List<TSapEmbryoBadNumber> tpBadList = badNumberService.getEmbryoByParams(badMonth, embryoBadCodeList);
        // 不良数需要重算外胎、胎胚、半部件
        // 2021.12.9 外胎不需要不良
        // 更新本月数据
        String year = Integer.toString(DateUtil.getYear(badDate));
        String month = Integer.toString(DateUtil.getMonth(badDate));
        if (Integer.parseInt(month) < 10 && month.length() == 1) {
            month = "0" + month;
        }
        String apsVersion = "";
        // 外胎
//        HashMap<String, List<TSapEmbryoBadNumber>> wtMap = CollectionUtil.toMapList(wtBadList, TSapEmbryoBadNumber::getSapCode);
//        List<String> sapCodeList = new ArrayList<>(wtMap.keySet());
//        List<TCxMonthPlanSurplus> wtList = wtService.getBySapCodeAndYearAndMonth(sapCodeList, year, month);
//        if (!CollectionUtil.isEmpty(wtList)) {
//            for (TCxMonthPlanSurplus wt : wtList) {
//                List<TSapEmbryoBadNumber> badList = wtMap.get(wt.getSapCode());
//                Integer badNum = 0;
//                for (TSapEmbryoBadNumber bad : badList) {
//                    badNum += bad.getBadNum();
//                }
//                wt.setSapBadQty(badNum);
//                wt.setMonthRemainQty(getMonthRemainQty(wt));
//            }
//            apsVersion = wtList.get(0).getMonthPlanApsVersion();
//        }

        // 胎胚
        HashMap<String, List<TSapEmbryoBadNumber>> tpMap = CollectionUtil.toMapList(tpBadList, TSapEmbryoBadNumber::getEmbryoCode);
        List<String> embryoCodeList = new ArrayList<>(tpMap.keySet());
        List<TCxEmbryoMonthPlanSurplus> tpList = tpService.getByEmbryoListAndYearAndMonth(embryoCodeList, year, month);
        if (!CollectionUtil.isEmpty(tpList)) {
            for (TCxEmbryoMonthPlanSurplus tp : tpList) {
                List<TSapEmbryoBadNumber> badList = tpMap.get(tp.getMaterialCode());
                BigDecimal badNum = BigDecimal.ZERO;
                for (TSapEmbryoBadNumber bad : badList) {
                    badNum = badNum.add(BigDecimal.valueOf(bad.getBadNum()));
                }
                tp.setEmbryoBadQty(badNum);
                tp.setMonthRemainQty(getEmbryoRemainQty(tp));
            }
            apsVersion = tpList.get(0).getMonthPlanApsVersion();
        }
//        wtService.mergeSql(wtList);
        tpService.mergeSql(tpList);
        if (StringUtils.isNotBlank(apsVersion)) {
            // 重算半部件
            monthPlanService.recalculateByApsVersion(apsVersion);
        }
        return AjaxResult.success(I18nUtil.getMessage("mes.error.message.bad.num"));
    }

    private static int getMonthRemainQty(TCxMonthPlanSurplus monthPlanSurplus) {
        int monthRemainQty = monthPlanSurplus.getMonthPlanQty() + monthPlanSurplus.getPlanModifyQty() + monthPlanSurplus.getSapBadQty() - monthPlanSurplus.getLastMonthStock() - monthPlanSurplus.getMonthFinishQty();

        return monthRemainQty;
    }

    private static BigDecimal getEmbryoRemainQty(TCxEmbryoMonthPlanSurplus embryoMonthPlanSurplus) {
        BigDecimal embryoRemainQty = embryoMonthPlanSurplus.getMonthPlanQty().add(embryoMonthPlanSurplus.getMonthPlanModifyQty()).add(embryoMonthPlanSurplus.getEmbryoBadQty()).subtract(embryoMonthPlanSurplus.getLastMonthStock()).subtract(embryoMonthPlanSurplus.getMonthFinishQty()).setScale(3, RoundingMode.UP);

        return embryoRemainQty;
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
