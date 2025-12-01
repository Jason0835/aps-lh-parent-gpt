package com.zlt.mix.schedule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.schedule.api.domain.dto.GlueDemandPlanExportDictDto;
import com.zlt.mix.schedule.api.domain.entity.GlueDemandPlan;
import com.zlt.mix.schedule.api.domain.entity.GlueDemandPlanInit;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 分厂胶料需求计划Service接口
 *
 * @author chen
 * @date 2022-04-18
 */
public interface GlueDemandPlanService extends IService<GlueDemandPlan> {
    /**
     * 查询分厂胶料需求计划列表
     *
     * @param glueDemandPlan 分厂胶料需求计划
     * @return 分厂胶料需求计划集合
     */
    List<GlueDemandPlan> selectGlueDemandPlanList(GlueDemandPlan glueDemandPlan);

    /**
     * 保存分厂胶料需求计划信息（id为空则新增，id不为空则修改）
     *
     * @param glueDemandPlan
     */
    void saveGlueDemandPlan(GlueDemandPlan glueDemandPlan);

    /**
     * 批量删除分厂胶料需求计划
     *
     * @param ids 需要删除的分厂胶料需求计划ID
     * @return 结果
     */
    int deleteGlueDemandPlanByIds(Long[] ids);

    /**
     * 校验分厂胶料需求计划唯一性
     */
    String checkGlueDemandPlanUnique(GlueDemandPlan glueDemandPlan);

    /**
     * 导入分厂胶料需求计划数据
     */
    @Transactional(rollbackFor = Exception.class)
    AjaxResult importData(List<GlueDemandPlanInit> list, Long importLogId, Boolean isSkip);

    /**
     * 根据系统中设置好的 胶料号与密炼区的匹配关系，重新匹配密炼区为空的 分厂胶料需求计划
     * @param planDate 计划日期
     */
    void rematch(Date planDate);

    /**
     * 分厂计划根据密炼区拆分
     * @param list 拆分后的记录
     * @param id 拆分前的记录id
     */
    void splitPlan(List<GlueDemandPlan> list, Long id);

    /**
     * 根据模板导出分厂胶料需求计划
     *
     * @param glueDemandPlan 查询参数
     * @return 文件字节
     */
    byte[] export(GlueDemandPlanExportDictDto glueDemandPlan);

    /**
     * 检测对应日期和分厂的数据是否存在
     *
     * @param glueDemandPlan 日期和分厂
     * @return 是否唯一的常量值
     */
    String checkPlanDateAndFactoryExist(GlueDemandPlan glueDemandPlan);
}
