package com.zlt.aps.common.engine.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.MdmMonthProdPlan;
import com.zlt.aps.common.engine.mapper.MdmMonthProdPlanMapper;
import com.zlt.aps.common.engine.service.MdmMonthProdPlanService;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 主计划月度生产计划Service业务层处理
 * 
 * @author Joran.zhang
 * @date 2021-06-24
 */
@Service
public class MdmMonthProdPlanServiceImpl implements MdmMonthProdPlanService
{
    @Resource
    private MdmMonthProdPlanMapper mdmMonthProdPlanMapper;
    /**
     * 批量处理数
     */
    private static final int BATCH_NUM = 100;

    /**
     * 查询主计划月度生产计划
     * 
     * @param id 主计划月度生产计划ID
     * @return 主计划月度生产计划
     */
    @Override
    public MdmMonthProdPlan selectMdmMonthProdPlanById(Long id)
    {
        return mdmMonthProdPlanMapper.selectMdmMonthProdPlanById(id);
    }

    /**
     * 查询主计划月度生产计划列表
     * 
     * @param mdmMonthProdPlan 主计划月度生产计划
     * @return 主计划月度生产计划
     */
    @Override
    public List<MdmMonthProdPlan> selectMdmMonthProdPlanList(MdmMonthProdPlan mdmMonthProdPlan)
    {
        return mdmMonthProdPlanMapper.selectMdmMonthProdPlanList(mdmMonthProdPlan);
    }

    @Override
    public List<MdmMonthProdPlan> getByParams(MdmMonthProdPlan mdmMonthProdPlan) {
        return mdmMonthProdPlanMapper.getByParams(mdmMonthProdPlan);
    }

    @Override
    public List<MdmMonthProdPlan> getByApsVersion(String apsVersion) {
        return mdmMonthProdPlanMapper.selectAllByMonthPlanApsVersion(apsVersion);
    }

    @Override
    public List<MdmMonthProdPlan> getByApsVersionOld(String apsVersion) {
        return mdmMonthProdPlanMapper.selectAllByMonthPlanApsVersionOld(apsVersion);
    }

    @Override
    public int update(MdmMonthProdPlan entity) {
        return mdmMonthProdPlanMapper.updateByPrimaryKey(entity);
    }

    @Override
    public void deleteByApsVersion(String apsVersion) {
        mdmMonthProdPlanMapper.deleteByMonthPlanApsVersion(apsVersion);
    }

    /**
     * 根据生产排程版本汇总月度计划量
     * @param monthPlanApsVersion
     * @return
     */
    @Override
    public List<MdmMonthProdPlan> selectMonthTotalPlanQtyByApsVersion(String monthPlanApsVersion) {
        return mdmMonthProdPlanMapper.selectMonthTotalPlanQtyByApsVersion(monthPlanApsVersion);
    }

    @Override
    public void insertBatch(List<MdmMonthProdPlan> prodList) {
        // 分批插入，100一次
        List<MdmMonthProdPlan> batchList = new ArrayList<>();
        int total = prodList.size();
        int index = 0;
        for (MdmMonthProdPlan prodPlan : prodList) {
            batchList.add(prodPlan);
            index++;
            if (batchList.size() < BATCH_NUM && index <total) {
                continue;
            }
            mdmMonthProdPlanMapper.insertBatch(batchList);
            batchList.clear();
        }
    }

    /**
     * 根据年月获取下个月初稿的数据集合
     * key sap+胎胚代码
     * @param year 年
     * @param month 月
     * @return
     */
    @Override
    public Map<String, MdmMonthProdPlan> nextMonthPlanDraft(String year, String month,String isFinalized,String monthPlanApsVersion) {
        Map<String, MdmMonthProdPlan> resultMap=new HashMap<>();
        List<MdmMonthProdPlan> nextMonthPlanDraftList=this.mdmMonthProdPlanMapper.selectMonthTotalPlanQtyByNextMonthDraft(year,month,isFinalized,monthPlanApsVersion);
        if(StringUtils.isNotEmpty(nextMonthPlanDraftList)){
            for(MdmMonthProdPlan mdmMonthProdPlan:nextMonthPlanDraftList){
                String mapKey= GenerageMapKeyUtils.createMapKey(mdmMonthProdPlan.getMaterialCode(),mdmMonthProdPlan.getEmbryoCode(),mdmMonthProdPlan.getBomDataVersion());
                if(!resultMap.containsKey(mapKey)){
                    resultMap.put(mapKey,mdmMonthProdPlan);
                }
            }
        }
        return resultMap;
    }

    /**
     * 根据胎胚分组
     * @param monthPlanApsVersion
     * @return
     */
    @Override
    public Map<String, List<MdmMonthProdPlan>> selectMonthPlanListBymonthPlanApsVersion(String monthPlanApsVersion) {
        Map<String, List<MdmMonthProdPlan>> resultMap=new HashMap<>();
        if(StringUtils.isNotEmpty(monthPlanApsVersion)){
            List<MdmMonthProdPlan> monthProdPlanList=this.mdmMonthProdPlanMapper.selectMonthTotalPlanQtyByNextMonthDraft("","", EngineConstants.IS_FINALIZED_YES,monthPlanApsVersion);
            if(StringUtils.isNotEmpty(monthProdPlanList)){
                resultMap=monthProdPlanList.stream().collect(Collectors.groupingBy(mdmMonthProdPlan -> GenerageMapKeyUtils.createMapKey(mdmMonthProdPlan.getMaterialCode(),mdmMonthProdPlan.getEmbryoCode())));
            }
        }
        return resultMap;
    }


}
