package com.zlt.aps.mps.service.impl;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.aps.common.engine.domain.TCxEmbryoMonthPlanSurplus;
import com.zlt.aps.common.engine.domain.TCxMonthPlanSurplus;
import com.zlt.aps.common.engine.domain.TLhMonthStock;
import com.zlt.aps.common.engine.domain.TSapEmbryoBadNumber;
import com.zlt.aps.common.engine.service.MonthPlanService;
import com.zlt.aps.common.engine.service.TCxEmbryoMonthPlanSurplusService;
import com.zlt.aps.common.engine.service.TCxMonthPlanSurplusService;
import com.zlt.aps.common.engine.service.TLhMonthStockService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.mps.domain.*;
import com.zlt.aps.mps.mapper.CxMonthStockMapper;
import com.zlt.aps.mps.mapper.CxStockMapper;
import com.zlt.aps.mps.mapper.TMesCxStockMapper;
import com.zlt.aps.mps.mapper.TSapStockMapper;
import com.zlt.aps.mps.service.MesCxService;
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
public class MesCxServiceImpl implements MesCxService {

    @Resource
    private TMesCxStockMapper mesStockMapper;
    @Resource
    private TSapStockMapper stockMapper;
    @Resource
    private CxStockMapper cxStockMapper;
    @Resource
    private CxMonthStockMapper cxMonthStockMapper;
    @Autowired
    private TLhMonthStockService lhMonthStockService;
    @Autowired
    private TCxMonthPlanSurplusService wtService;
    @Autowired
    private TCxEmbryoMonthPlanSurplusService tpService;
    @Autowired
    private MonthPlanService monthPlanService;

    @Override
    public AjaxResult mergeCxStock(String dataVersion) {
        // 取MES同步dataVersion数据
        List<TMesCxStock> list = mesStockMapper.getCxStockByDataVersion(dataVersion);
        if (CollectionUtil.isEmpty(list)) {
            return AjaxResult.error("数据为空");
        }
        List<CxStock> cxList = new ArrayList<>();
        for (TMesCxStock mes : list) {
            CxStock cx = new CxStock();
            cx.setStockDate(mes.getStockDate());
            cx.setEmbryoCode(mes.getEmbryoCode());
            cx.setBomDataVersion(mes.getBomDataVersion());
            // 库存量=库存+硫化库存(弃用)
//            Integer stockNum = mes.getStockNum() + mes.getLhStock();
            // 库存量 = 可用库存
            cx.setStockNum(mes.getAvailableStock().longValue());
            cx.setUnavailableStock(mes.getUnavailableStock().longValue());
            cx.setOverTimeStock(mes.getOverTimeStock().longValue());
            this.setBaseSysValue(cx);
            cxList.add(cx);
        }
        cxStockMapper.mergeSql(cxList);
        return AjaxResult.success();
    }

    @Override
    public AjaxResult mergeCxMonthStock(String dataVersion) {
        List<TMesCxMonthStock> list = mesStockMapper.getCxMonthStockByDataVersion(dataVersion);
        if (CollectionUtil.isEmpty(list)) {
            return AjaxResult.error("数据为空");
        }
        // 2021.12.09 胎胚月结不计入外胎，不重算外胎
//        List<TLhMonthStock> lhList = new ArrayList<>();
        List<CxMonthStock> cxList = new ArrayList<>();
//        HashMap<String, List<TMesCxMonthStock>> lhMap = new HashMap<>();
        HashMap<String, List<TMesCxMonthStock>> cxMap = new HashMap<>();
        for (TMesCxMonthStock stock : list) {
//            List<TMesCxMonthStock> lh = lhMap.get(stock.getSapCode());
            List<TMesCxMonthStock> cx = cxMap.get(stock.getEmbryoCode() + "+" + stock.getBomDataVersion());
//            if (lh == null) {
//                lh = new ArrayList<>();
//                lhMap.put(stock.getSapCode(), lh);
//            }
            if (cx == null) {
                cx = new ArrayList<>();
                cxMap.put(stock.getEmbryoCode() + "+" + stock.getBomDataVersion(), cx);
            }
//            lh.add(stock);
            cx.add(stock);
        }
        String apsVersion = "";

//        // 外胎
//        for (String sapCode : lhMap.keySet()) {
//            // 总库存
//            Integer totalNum = 0;
//            // 总超期库存
////            Integer overTimeTotal = 0;
//            TLhMonthStock lhMonthStock = new TLhMonthStock();
//            List<TMesCxMonthStock> lh = lhMap.get(sapCode);
//            TMesCxMonthStock mes = lh.get(0);
//            lhMonthStock.setStockMonth(mes.getStockMonth());
//            lhMonthStock.setSapCode(mes.getSapCode());
//            lhMonthStock.setRemark(mes.getRemark());
//            for (TMesCxMonthStock stock : lh) {
//                // 库存=库存量+硫化库存
//                totalNum = totalNum + stock.getAvailableStock();
////                // 超期库存累计
////                overTimeTotal = overTimeTotal + stock.getOverTimeStock();
//            }
//            lhMonthStock.setStockNum(totalNum);
////            lhMonthStock.setOverTimeStock(overTimeTotal);
//            lhList.add(lhMonthStock);
//        }

        // 胎胚
        for (String code : cxMap.keySet()) {
            // 总库存
            Integer totalNum = 0;
            // 总超期库存
            Integer overTimeTotal = 0;
            CxMonthStock cxMonthStock = new CxMonthStock();
            List<TMesCxMonthStock> tp = cxMap.get(code);
            TMesCxMonthStock mes = tp.get(0);
            cxMonthStock.setStockMonth(mes.getStockMonth());
            cxMonthStock.setEmbryoCode(mes.getEmbryoCode());
            cxMonthStock.setBomDataVersion(mes.getBomDataVersion());
            cxMonthStock.setRemark(mes.getRemark());
            for (TMesCxMonthStock stock : tp) {
                // 库存=库存量+硫化库存
                totalNum = totalNum + stock.getAvailableStock();
                // 超期库存累计
                overTimeTotal = overTimeTotal + stock.getOverTimeStock();
            }
            cxMonthStock.setStockNum(totalNum.toString());
            cxMonthStock.setOverTimeStock(overTimeTotal.toString());
            cxList.add(cxMonthStock);
        }
//        lhMonthStockService.mergeSql(lhList);
        cxMonthStockMapper.mergeSql(cxList);
        // 外胎汇总
//        Map<String, TLhMonthStock> lhStockMap = CollectionUtil.toMap(lhList, TLhMonthStock::getSapCode);
//        List<String> sapCodeList = new ArrayList<>(lhStockMap.keySet());
        // 更新下个月数据
        Date stockMonth = list.get(0).getStockMonth();
        Date after1Month = DateUtil.getAfter1Month(stockMonth);
        String afterYear = Integer.toString(DateUtil.getYear(after1Month));
        String afterMonth = Integer.toString(DateUtil.getMonth(after1Month));
        if (Integer.parseInt(afterMonth) < 10 && afterMonth.length() == 1) {
            afterMonth = "0" + afterMonth;
        }
//        List<TCxMonthPlanSurplus> wtList = wtService.getBySapCodeAndYearAndMonth(sapCodeList, afterYear, afterMonth);
//        // 如果有旧数据，库存量要更新
//        if (!CollectionUtil.isEmpty(wtList)) {
//            for (TCxMonthPlanSurplus wt : wtList) {
//                TLhMonthStock lhMonthStock = lhStockMap.get(wt.getSapCode());
//                wt.setLastMonthStock(lhMonthStock.getStockNum());
//                wt.setMonthRemainQty(getMonthRemainQty(wt));
//            }
//            apsVersion = wtList.get(0).getMonthPlanApsVersion();
//        }
        // 胎胚汇总
        Map<String, CxMonthStock> cxStockMap = CollectionUtil.toMap(cxList, CxMonthStock::getEmbryoCode);
        List<String> embryoCode = new ArrayList<>(cxStockMap.keySet());
        List<TCxEmbryoMonthPlanSurplus> tpList = tpService.getByEmbryoListAndYearAndMonth(embryoCode, afterYear, afterMonth);
        // 如果有旧数据，库存量要更新
        if (!CollectionUtil.isEmpty(tpList)) {
            for (TCxEmbryoMonthPlanSurplus tp : tpList) {
                CxMonthStock cxMonthStock = cxStockMap.get(tp.getMaterialCode());
                tp.setLastMonthStock(BigDecimal.valueOf(Double.parseDouble(cxMonthStock.getStockNum())));
                tp.setMonthRemainQty(getEmbryoRemainQty(tp));
            }
            apsVersion = tpList.get(0).getMonthPlanApsVersion();
        }
        // 合并
//        wtService.mergeSql(wtList);
        tpService.mergeSql(tpList);
        if (StringUtils.isNotBlank(apsVersion)) {
            // 重算
            monthPlanService.recalculateByApsVersion(apsVersion);
        }
        return AjaxResult.success();
    }

    private static int getMonthRemainQty(TCxMonthPlanSurplus monthPlanSurplus) {
        int monthRemainQty = monthPlanSurplus.getMonthPlanQty() + monthPlanSurplus.getPlanModifyQty() + monthPlanSurplus.getSapBadQty() - monthPlanSurplus.getLastMonthStock() - monthPlanSurplus.getMonthFinishQty();

        return monthRemainQty;
    }

    private static BigDecimal getEmbryoRemainQty(TCxEmbryoMonthPlanSurplus embryoMonthPlanSurplus) {
        BigDecimal embryoRemainQty = embryoMonthPlanSurplus.getMonthPlanQty().add(embryoMonthPlanSurplus.getMonthPlanModifyQty()).add(embryoMonthPlanSurplus.getEmbryoBadQty()).subtract(embryoMonthPlanSurplus.getLastMonthStock()).subtract(embryoMonthPlanSurplus.getMonthFinishQty()).setScale(3, RoundingMode.UP);

        return embryoRemainQty;
    }

    @Override
    public AjaxResult mergeCxSapStock(String dataVersion) {
        // 获取MES成品库存数据
        List<TMesSapStock> list = mesStockMapper.getCxSapStockByDataVersion(dataVersion);
        if (CollectionUtil.isEmpty(list)) {
            return AjaxResult.error("数据为空");
        }
        List<TSapStock> stockList = new ArrayList<>();
        for (TMesSapStock mes : list) {
            TSapStock stock = new TSapStock();
            stock.setStockDate(mes.getStockDate());
            stock.setSapCode(mes.getSapCode());
            // 总库存=库存+被领用
            stock.setStockNum((mes.getStockNum() == null ? 0 : mes.getStockNum()) + (mes.getPickedQty() == null ? 0 : mes.getPickedQty()));
            this.setBaseSysValue(stock);
            stockList.add(stock);
        }
        // 合并成品库存
        stockMapper.mergeSql(stockList);
        return AjaxResult.success();
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
