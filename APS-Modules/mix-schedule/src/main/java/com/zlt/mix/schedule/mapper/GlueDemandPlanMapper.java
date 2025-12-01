package com.zlt.mix.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.schedule.api.domain.entity.GlueDemandPlan;
import com.zlt.mix.setting.api.domain.entity.FactoryGlueAreaRelation;

import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 分厂胶料需求计划Mapper接口
 * 
 * @author chen
 * @date 2022-04-18
 */
public interface GlueDemandPlanMapper extends BaseMapper<GlueDemandPlan> {

    /**
     * 查询分厂胶料需求计划列表
     * 
     * @param glueDemandPlan 分厂胶料需求计划
     * @return 分厂胶料需求计划集合
     */
    List<GlueDemandPlan> selectGlueDemandPlanList(GlueDemandPlan glueDemandPlan);

    /**
     * 批量删除分厂胶料需求计划
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteGlueDemandPlanByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     * @param list 导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listGlueDemandPlanNotUnique(@Param("importList") List<GlueDemandPlan> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     * @param list
     */
    void batchInsertGlueDemandPlanInfo(@Param("list") List<GlueDemandPlan> list);

    /**
     * 从初始表(根据计划日期、分厂过滤数据)中获取数据批量新增到需求表中
     */
    void batchInsertFromInit(GlueDemandPlan glueDemandPlan);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    void mergeSql(List<GlueDemandPlan> list);

    /**
     * 将分厂胶料需求表数据备份到日志表
     * @param glueDemandPlan 参数：计划日期、分厂
     * @return 影响行数
     */
    public int backupToGlueDemandPlanLog(GlueDemandPlan glueDemandPlan);

    /**
     * 根据计划日期和分厂删除数据（物理删除）
     * @return 影响行数
     */
    public int deleteByPlanDateAndFactory(GlueDemandPlan glueDemandPlan);

    /**
     * 校验重新匹配是否有缺少配置关系的情况
     * @return
     */
    Integer checkRematchNotExistsMixArea(@Param("planDate") Date planDate);
    
    /**
     * 根据系统中设置好的 胶料号与密炼区的匹配关系，重新匹配密炼区为空的 分厂胶料需求计划
     * @param planDate 计划日期
     */
    void rematch(@Param("planDate") Date planDate, @Param("factory") String factory);

    /**
     * 获取终炼胶名的Set集合
     * @return 终炼胶名的Set集合
     */
    Set<String> listMaterialNameSet();
    
    /**
     * 取出分厂胶料密炼区的对应关系
     * @param factory 分厂
     * @return
     */
    List<FactoryGlueAreaRelation> listFactoryGlueAreaRelation(@Param("factory") String factory);
}
