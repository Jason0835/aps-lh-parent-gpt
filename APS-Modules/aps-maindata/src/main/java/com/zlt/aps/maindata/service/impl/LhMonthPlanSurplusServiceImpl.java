package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.enums.LocationTypeEnum;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.maindata.enums.SystemBaseEnums;
import com.zlt.aps.maindata.mapper.LhMonthPlanSurplusDetailMapper;
import com.zlt.aps.maindata.mapper.LhMonthPlanSurplusEntityMapper;
import com.zlt.aps.maindata.service.ILhMonthPlanSurplusService;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.entity.LhMonthPlanSurplus;
import com.zlt.aps.monthplan.api.domain.entity.LhMonthPlanSurplusDetail;
import com.zlt.aps.monthplan.api.domain.vo.LhMonthPlanSurplusDetailVo;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhMonthPlanSurplusServiceImpl.java
 * 描    述：LhMonthPlanSurplusServiceImpl月度计划外胎汇总业务层处理
 *@author zlt
 *@date 2025-02-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhMonthPlanSurplusServiceImpl extends AbstractDocService<LhMonthPlanSurplus>  implements ILhMonthPlanSurplusService {

    @Resource
    private LhMonthPlanSurplusEntityMapper lhMonthPlanSurplusEntityMapper;
    @Resource
    private LhMonthPlanSurplusDetailMapper lhMonthPlanSurplusDetailMapper;

    @Override
    protected String getDocTypeCode() {
        return "0108";
    }



    /**
     * 根据分厂编码和规格代码查询
     * @param factoryCode
     * @param specCodes
     * @return
     */
    @Override
    public List<LhMonthPlanSurplus> queryByFactoryAndSpecCodes(String factoryCode, Set<String> specCodes,Integer year,Integer month) {
        // 将 Set 转换为 List，便于切分批次
        List<String> codeList = new ArrayList<>(specCodes);
        //定义最终返回的List
        List<LhMonthPlanSurplus> finalList  = new ArrayList<>();
        //判断集合的长度是多少 如果超过900条则进行切分查询
        if (codeList.size() > SystemBaseEnums.SPLIT_LENGTH.getCode()) {
            List<List<String>> splitList = ScmListUtils.getSplitList(codeList, SystemBaseEnums.SPLIT_LENGTH.getCode());
            //将多次查询的结果汇总到finalList中
            for (List<String> splitItemList : splitList) {
                List<LhMonthPlanSurplus> queryList = lhMonthPlanSurplusEntityMapper.queryByFactoryAndSpecCodes(factoryCode, splitItemList,year,month);
                finalList.addAll(queryList);
            }
        }else{
            finalList = lhMonthPlanSurplusEntityMapper.queryByFactoryAndSpecCodes(factoryCode, codeList,year,month);
        }
        return finalList;
    }

    @Override
    public AjaxResult updateMonthPlanSurplus(int year, int month) {
        // 查询对应年、月的月度外胎汇总记录
        List<LhMonthPlanSurplus> surplusList = lhMonthPlanSurplusEntityMapper.selectList(Wrappers.lambdaQuery(LhMonthPlanSurplus.class)
                .eq(LhMonthPlanSurplus::getYear, year)
                .eq(LhMonthPlanSurplus::getMonth, month)
        );

        reAssignmentFinishQty(surplusList);

        return AjaxResult.success();
    }

    /**
     * 重算分配明细完成量
     *
     * @param surplusList 月度计划外胎汇总对象列表
     */
    @Override
    public void reAssignmentFinishQty(List<LhMonthPlanSurplus> surplusList) {
        if (CollectionUtils.isEmpty(surplusList)) {
            return;
        }

        // 根据年月、分厂、物料、规格查询对应月度外胎汇总明细记录，根据id排序，尽可能保证分配的完成量和之前一致（月计划调整会导致记录的数量和条数发生变化）
        List<LhMonthPlanSurplusDetail> surplusDetailList = lhMonthPlanSurplusDetailMapper.selectParamList(surplusList);
        // 按照库位分组
        List<LhMonthPlanSurplusDetail> outPlanList = new ArrayList<>();
        List<LhMonthPlanSurplusDetail> oePlanList = new ArrayList<>();
        List<LhMonthPlanSurplusDetail> inPlanList = new ArrayList<>();
        for (LhMonthPlanSurplusDetail item : surplusDetailList) {
            // 总完成量先清空为0
            item.setCompleteQty(0L);
            if (LocationTypeEnum.FOREIGN_LOCATION.getValue().equals(item.getLocationType())) {
                outPlanList.add(item);
            } else if (LocationTypeEnum.OE_LOCATION.getValue().equals(item.getLocationType())) {
                oePlanList.add(item);
            } else {
                inPlanList.add(item);
            }
        }

        Map<String, Long> surplusMap = surplusList.stream()
                .filter(v -> v.getMonthFinishQty() != null && v.getMonthFinishQty() > 0)
                .collect(Collectors.groupingBy(v -> GenerageMapKeyUtils.createMapKey(v.getYear(), v.getMonth(), v.getFactoryCode(), v.getProductCode(), v.getSpecCode()), Collectors.summingLong(LhMonthPlanSurplus::getMonthFinishQty)));
        // 对应分配记录的总量
        Function<LhMonthPlanSurplusDetail, String> keyFunc = v -> GenerageMapKeyUtils.createMapKey(v.getYear(), v.getMonth(), v.getFactoryCode(), v.getProductCode(), v.getSpecCode());

        // 优先分配给OE，再给外销，最后是内销
        reAssignmentDetailFinish(oePlanList, keyFunc, surplusMap);
        reAssignmentDetailFinish(outPlanList, keyFunc, surplusMap);
        reAssignmentDetailFinish(inPlanList, keyFunc, surplusMap);

        this.baseDao.updateBatch(surplusDetailList);
    }

    /**
     * 查询月度外胎汇总
     */
    @Override
    public List<LhMonthPlanSurplusDetailVo> selectDetailList(LhMonthPlanSurplusDetail queryVO) {
        List<LhMonthPlanSurplusDetailVo> list = lhMonthPlanSurplusDetailMapper.selectDetailList(queryVO);
        for (LhMonthPlanSurplusDetailVo item : list) {
            item.setWbsElement("");
            long planQty = item.getPlanQty() == null ? 0 : item.getPlanQty();
            long finishQty = item.getFinishQty() == null ? 0 : item.getFinishQty();
            long surplusQty = planQty - finishQty;
            item.setPlanQty(planQty);
            item.setFinishQty(finishQty);
            item.setSurplusQty(surplusQty < 0 ? 0 : surplusQty);
        }
        return list;
    }

    /**
     * 分配月度外胎汇总明细完成量
     *
     * @param detailList 月度外胎汇总明细
     * @param keyFunc    月度外胎汇总明细对应月度外胎汇总记录
     * @param surplusMap 月度外胎汇总的Map
     */
    private void reAssignmentDetailFinish(List<LhMonthPlanSurplusDetail> detailList, Function<LhMonthPlanSurplusDetail, String> keyFunc, Map<String, Long> surplusMap) {
        for (LhMonthPlanSurplusDetail itemDetail : detailList) {
            if (itemDetail.getProductionQty() == null || itemDetail.getProductionQty() <= 0) {
                continue;
            }
            String key = keyFunc.apply(itemDetail);
            Long monthFinishQty = surplusMap.get(key);
            if (monthFinishQty == null || monthFinishQty <= 0) {
                continue;
            }

            // 更新对应月度外胎汇总明细的完成量、扣减对应月度外胎汇总的完成量
            long productionQty = itemDetail.getProductionQty();
            long completeQty;
            if (monthFinishQty >= productionQty) {
                completeQty = productionQty;
                monthFinishQty -= productionQty;
            } else {
                completeQty = monthFinishQty;
                monthFinishQty = 0L;
            }
            itemDetail.setCompleteQty(completeQty);
            surplusMap.put(key, monthFinishQty);
        }
    }
}
