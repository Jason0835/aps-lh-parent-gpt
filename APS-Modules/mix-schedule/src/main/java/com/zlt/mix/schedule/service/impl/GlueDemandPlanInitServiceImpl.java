package com.zlt.mix.schedule.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.schedule.api.domain.entity.GlueDemandPlanInit;
import com.zlt.mix.schedule.mapper.GlueDemandPlanInitMapper;
import com.zlt.mix.schedule.service.GlueDemandPlanInitService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 分厂胶料需求计划（初始表）Service业务层处理
 *
 * @author Gim
 * @date 2022-04-05
 */
@Service
public class GlueDemandPlanInitServiceImpl extends ServiceImpl<GlueDemandPlanInitMapper, GlueDemandPlanInit> implements GlueDemandPlanInitService {
    @Resource
    private GlueDemandPlanInitMapper glueDemandPlanInitMapper;

    /**
     * 查询分厂胶料需求计划（初始表）列表
     *
     * @param glueDemandPlanInit 分厂胶料需求计划（初始表）
     * @return 分厂胶料需求计划（初始表）
     */
    @Override
    public List<GlueDemandPlanInit> selectGlueDemandPlanInitList(GlueDemandPlanInit glueDemandPlanInit) {
       /* if (StringUtils.isNotEmpty(glueDemandPlanInit.getEndTime())) {
            glueDemandPlanInit.setEndTime(glueDemandPlanInit.getEndTime() + " 23:59:59");
        }*/
        return glueDemandPlanInitMapper.selectGlueDemandPlanInitList(glueDemandPlanInit);
    }

}
