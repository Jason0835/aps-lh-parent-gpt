package com.zlt.mix.schedule.mapper;

import java.util.List;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.schedule.api.domain.entity.GlueDemandPlan;
import org.apache.ibatis.annotations.Param;
import com.zlt.mix.schedule.api.domain.entity.GlueDemandPlanInit;

/**
 * 分厂胶料需求计划（初始表）Mapper接口
 * 
 * @author Gim
 * @date 2022-04-05
 */
public interface GlueDemandPlanInitMapper extends BaseMapper<GlueDemandPlanInit> {

    /**
     * 查询分厂胶料需求计划（初始表）列表
     * 
     * @param glueDemandPlanInit 分厂胶料需求计划（初始表）
     * @return 分厂胶料需求计划（初始表）集合
     */
    List<GlueDemandPlanInit> selectGlueDemandPlanInitList(GlueDemandPlanInit glueDemandPlanInit);

    /**
     * 将分厂胶料需求初始化表数据备份到日志表
     * @param glueDemandPlanInit 参数：计划日期、分厂
     * @return 影响行数
     */
    public int backupToGlueDemandPlanInitLog(GlueDemandPlanInit glueDemandPlanInit);

    /**
     * 根据计划日期和分厂删除数据（物理删除）
     * @return 影响行数
     */
    public int deleteByPlanDateAndFactory(GlueDemandPlanInit glueDemandPlanInit);

    /**
     * 将集合数据批量插入初始表
     */
    public void batchInsertGlueDemandPlanInitInfo(List<GlueDemandPlanInit> importList);
}
