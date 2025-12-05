package com.zlt.aps.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.zlt.aps.constants.CxEngineConstants;
import com.zlt.aps.cx.mapper.entity.CxParamsEntityMapper;
import com.zlt.aps.cx.mapper.entity.CxProductConstructionInfoMapper;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxParams;
import com.zlt.aps.cxlh.cx.api.domain.vo.CxProductConstructionInfoDto;
import com.zlt.aps.cxlh.cx.api.domain.vo.LhAlgorithmScheduleResultDto;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.maindata.mapper.CxEmbryoMonthPlanSurplusEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.service.IMdmProductConstructionService;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.entity.CxEmbryoMonthPlanSurplus;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.vo.MdmProductConstructionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 提供缓存相关方法
 *
 * @author Nick
 */
@Component("CommonQueryCacheService")
@Slf4j
public class CommonQueryCacheService {

    /**
     * 分批查询硫化规格施工
     */
    private static final int BATCH_QUERY_SIZE = 900;


    @Resource
    private MdmMaterialInfoEntityMapper mdmMaterialInfoEntityMapper;
    @Resource
    private CxEmbryoMonthPlanSurplusEntityMapper cxEmbryoMonthPlanSurplusEntityMapper;
    @Resource
    private CxProductConstructionInfoMapper cxProductConstructionInfoMapper;
    @Resource
    private IMdmProductConstructionService mdmProductConstructionService;
    @Resource
    private CxParamsEntityMapper cxParamsEntityMapper;

    /**
     * 依据硫化计划的物料编号查询对应硫化物料信息
     *
     * @param lhScheduleResultList 硫化任务列表
     * @return 硫化物料信息列表
     */
    public List<MdmMaterialInfo> querySulfurSpecInfo(List<LhScheduleResult> lhScheduleResultList) {
        if (lhScheduleResultList.isEmpty()) {
            return new ArrayList<>();
        }

        // 1.依据物料施工代码汇总一个列表
        List<String> specCodeList = lhScheduleResultList.stream()
                .map(LhScheduleResult::getSpecCode)
                .collect(Collectors.toList());
        List<MdmMaterialInfo> productInfoArrayList = new ArrayList<>();

        // 2.如果列表长度大于900，分批查询
        if (!specCodeList.isEmpty()) {
            List<List<String>> splitList = ScmListUtils.getSplitList(specCodeList, BATCH_QUERY_SIZE);
            for (List<String> specCodeSplitItemList : splitList) {
                List<MdmMaterialInfo> cxSpecCodeSplitItemListInfoList = mdmMaterialInfoEntityMapper.selectByUniqueKeyList(specCodeSplitItemList);
                productInfoArrayList.addAll(cxSpecCodeSplitItemListInfoList);
            }
        }

        // 3.返回硫化物料施工列表
        return productInfoArrayList;
    }


    /**
     * 查询外胎施工信息
     * @param lhScheduleResultList 硫化对象
     * @return List<MdmProductConstruction> 外胎施工信息列表
     */
    public List<MdmProductConstructionVO> queryProductConstructionInfo(List<LhScheduleResult> lhScheduleResultList) {
        if (lhScheduleResultList.isEmpty()) {
            return new ArrayList<>();
        }

        // 1.依据物料施工代码汇总一个列表
        List<String> specCodeList = lhScheduleResultList.stream()
                .map(item -> item.getProductCode()+"_"+item.getSpecCode())
                .collect(Collectors.toList());

        List<MdmProductConstructionVO> cxMdmProductConstruction = mdmProductConstructionService.queryByFactoryCodeAndSpecCodes(SecurityUtils.getUserCurrentFactory(), new HashSet<>(specCodeList));

        // 3.返回硫化物料施工列表
        return new ArrayList<>(cxMdmProductConstruction);
    }


    /**
     * 依据日期获取对应月份的成型胎胚月度剩余量表
     *
     * @param scheduleDate 日期
     */
    public List<CxEmbryoMonthPlanSurplus> getMonthRemainQtyList(Date scheduleDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(scheduleDate);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        QueryWrapper<CxEmbryoMonthPlanSurplus> cxEmbryoMonthPlanSurplusQueryWrapper = new QueryWrapper<>();
        cxEmbryoMonthPlanSurplusQueryWrapper.eq("YEAR", year);
        cxEmbryoMonthPlanSurplusQueryWrapper.eq("MONTH", month);
        List<CxEmbryoMonthPlanSurplus> cxEmbryoMonthPlanSurplusList = cxEmbryoMonthPlanSurplusEntityMapper.selectList(cxEmbryoMonthPlanSurplusQueryWrapper);
        if (cxEmbryoMonthPlanSurplusList.isEmpty()) {
            return new ArrayList<>();
        }
        return cxEmbryoMonthPlanSurplusList;
    }


    /**
     * 依据任务列表获取对应胎胚施工信息
     *
     * @param lhAlgorithmScheduleResults 任务列表
     * @return List<CxProductConstructionInfoDto> 施工列表
     */
    public List<CxProductConstructionInfoDto> queryEmbryoCodeInfo(List<LhAlgorithmScheduleResultDto> lhAlgorithmScheduleResults) {
        List<String> embryoList = lhAlgorithmScheduleResults.stream()
                .map(item -> item.getLhScheduleResult().getEmbryoCode())
                .collect(Collectors.toList());
        List<CxProductConstructionInfoDto> constructionInfoDtoList = new ArrayList<>();

        //判断embryoList是否大于900 如果大于进行分割
        if (!embryoList.isEmpty()) {
            List<List<String>> splitList = ScmListUtils.getSplitList(embryoList, 900);
            for (List<String> embryoSplitItemList : splitList) {
                List<CxProductConstructionInfoDto> cxProductConstructionInfoList = cxProductConstructionInfoMapper.selectProcedureConstructionList(CxEngineConstants.CONSTRUCTION_TYPE, embryoSplitItemList);
                constructionInfoDtoList.addAll(cxProductConstructionInfoList);
            }
        }
        if (constructionInfoDtoList.isEmpty()) {
            return new ArrayList<>();
        }
        return constructionInfoDtoList;
    }


    /**
     * 查询所有的成型参数列表
     */
    public List<CxParams> queryCxParams() {
        QueryWrapper<CxParams> cxParamsQueryWrapper = new QueryWrapper<>();
        List<CxParams> cxParamsList = cxParamsEntityMapper.selectList(cxParamsQueryWrapper);
        if (cxParamsList.isEmpty()) {
            return new ArrayList<>();
        }
        return cxParamsList;
    }


}
