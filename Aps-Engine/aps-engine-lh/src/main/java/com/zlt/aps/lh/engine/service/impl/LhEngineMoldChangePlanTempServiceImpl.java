package com.zlt.aps.lh.engine.service.impl;

import com.zlt.aps.lh.engine.domain.LhEngineMoldChangePlan;
import com.zlt.aps.lh.engine.mapper.LhEngineMoldChangePlanTempMapper;
import com.zlt.aps.lh.engine.service.LhEngineMoldChangePlanTempService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
  *  模具变动单生成临时记录
  * @ClassName LhEngineMoldChangePlanTempServiceImpl
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/9/3 16:26
  * @Version 1.0
**/
@Service("lhEngineMoldChangePlanTempService")
public class LhEngineMoldChangePlanTempServiceImpl implements LhEngineMoldChangePlanTempService {

    @Autowired
    private LhEngineMoldChangePlanTempMapper lhEngineMoldChangePlanTempMapper;

    @Override
    public List<LhEngineMoldChangePlan> selectLhEngineMoldChangePlanList(LhEngineMoldChangePlan lhEngineMoldChangePlan) {
        return lhEngineMoldChangePlanTempMapper.selectLhEngineMoldChangePlanList(lhEngineMoldChangePlan);
    }

    @Override
    public int batchCreateMoldChangePlan(List<LhEngineMoldChangePlan> lhEngineMoldChangePlanList) {
        return lhEngineMoldChangePlanTempMapper.batchCreateMoldChangePlan(lhEngineMoldChangePlanList);
    }

    @Override
    public int deleteLhEngineMoldChangePlanByParams(String sourceCxOrder, List<String> list, List<Long> idList, Date scheduleDate) {
        return lhEngineMoldChangePlanTempMapper.deleteLhEngineMoldChangePlanByParams(sourceCxOrder,list,idList,scheduleDate);
    }
}
