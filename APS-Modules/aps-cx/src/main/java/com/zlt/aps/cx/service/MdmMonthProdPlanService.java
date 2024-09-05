package com.zlt.aps.cx.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.CxMdmMonthProdPlan1;
import com.zlt.aps.cx.api.domain.entity.CxMdmMonthProdPlan2;
import com.zlt.aps.cx.api.domain.entity.Gante;
import com.zlt.aps.cx.api.domain.entity.MdmMonthProdPlan;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 主计划月度生产计划Service接口
 *
 * @author zlt
 * @date 2021-09-15
 */
public interface MdmMonthProdPlanService {
    /**
     * 查询主计划月度生产计划
     *
     * @param id 主计划月度生产计划ID
     * @return 主计划月度生产计划
     */
    public MdmMonthProdPlan selectMdmMonthProdPlanById(Long id);

    /**
     * 查询主计划月度生产计划列表
     *
     * @param mdmMonthProdPlan 主计划月度生产计划
     * @return 主计划月度生产计划集合
     */
    public List<MdmMonthProdPlan> selectMdmMonthProdPlanList(MdmMonthProdPlan mdmMonthProdPlan);

    /**
     * 新增主计划月度生产计划
     *
     * @param mdmMonthProdPlan 主计划月度生产计划
     * @return 结果
     */
    @Transactional
    public AjaxResult insertMdmMonthProdPlan(MdmMonthProdPlan mdmMonthProdPlan);

    /**
     * 修改主计划月度生产计划
     *
     * @param mdmMonthProdPlan 主计划月度生产计划
     * @return 结果
     */
    @Transactional
    public int updateMdmMonthProdPlan(MdmMonthProdPlan mdmMonthProdPlan);

    public int updateExpectedExcessArrears(MdmMonthProdPlan mdmMonthProdPlan);

    /**
     * 批量删除主计划月度生产计划
     *
     * @param ids 需要删除的主计划月度生产计划ID
     * @return 结果
     */
    @Transactional
    public int deleteMdmMonthProdPlanByIds(Long[] ids);

    /**
     * 删除主计划月度生产计划信息
     *
     * @param id 主计划月度生产计划ID
     * @return 结果
     */
    @Transactional
    public int deleteMdmMonthProdPlanById(Long id);

    /**
     * 校验主计划月度生产计划唯一性
     */
    public String checkMdmMonthProdPlanUnique(MdmMonthProdPlan mdmMonthProdPlan);

    /**
     * 导入主计划月度生产计划数据
     */
    public AjaxResult importData(byte[] data, String mainPlanMonth, boolean updateSupport, Long importLogId, boolean isFinamized,Map<String, String> dictMap) throws Exception;

    /**
     * 预计超欠产导出
     */
    public List<CxMdmMonthProdPlan1> expectedExport(MdmMonthProdPlan mdmMonthProdPlan);

    /**
     * 超欠产导出
     */
    public List<CxMdmMonthProdPlan2> overProdExport(MdmMonthProdPlan mdmMonthProdPlan);

    /**
     * 下发主计划
     */
    public AjaxResult issuePlan(MdmMonthProdPlan mdmMonthProdPlan, Map<String, String> map);


    /**
     * 查询月计划甘特图数据
     */
    public List<Gante> getMonthPlanGanteData(Gante gante);

    /**
     * 查询月计划柱状图数据
     */
    public Map<String,List<Integer>> dailyChart(String scheduleDate);
}
