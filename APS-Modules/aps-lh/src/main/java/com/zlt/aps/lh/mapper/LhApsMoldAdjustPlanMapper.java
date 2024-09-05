package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhApsMoldAdjustPlan;
import com.zlt.aps.lh.vo.MoldPlanPublishRecordVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 硫化工序模具变动单APSMapper接口
 * 
 * @author Joran.zhang
 * @date 2022-06-07
 */
public interface LhApsMoldAdjustPlanMapper 
{
    /**
     * 查询硫化工序模具变动单APS
     * 
     * @param id 硫化工序模具变动单APSID
     * @return 硫化工序模具变动单APS
     */
    public LhApsMoldAdjustPlan selectLhApsMoldAdjustPlanById(Long id);

    /**
     * 查询硫化工序模具变动单APS列表
     * 
     * @param lhApsMoldAdjustPlan 硫化工序模具变动单APS
     * @return 硫化工序模具变动单APS集合
     */
    public List<LhApsMoldAdjustPlan> selectLhApsMoldAdjustPlanList(LhApsMoldAdjustPlan lhApsMoldAdjustPlan);

    /**
     * 新增硫化工序模具变动单APS
     * 
     * @param lhApsMoldAdjustPlan 硫化工序模具变动单APS
     * @return 结果
     */
    public int insertLhApsMoldAdjustPlan(LhApsMoldAdjustPlan lhApsMoldAdjustPlan);

    /**
     * 修改硫化工序模具变动单APS
     * 
     * @param lhApsMoldAdjustPlan 硫化工序模具变动单APS
     * @return 结果
     */
    public int updateLhApsMoldAdjustPlan(LhApsMoldAdjustPlan lhApsMoldAdjustPlan);

    /**
     * 删除硫化工序模具变动单APS
     * 
     * @param id 硫化工序模具变动单APSID
     * @return 结果
     */
    public int deleteLhApsMoldAdjustPlanById(Long id);

    /**
     * 批量删除硫化工序模具变动单APS
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteLhApsMoldAdjustPlanByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<LhApsMoldAdjustPlan> list);

    /**
     * 根据下达日期进行数据删除
     * @param planDate
     * @return
     */
    int  deleteLhApsMoldAdjustPlanByPlanDate(@Param("planDate") String planDate);

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param ids id
     * @return 查询到的记录数
     */
    public int isReleasingOrTimeoutByIds(Long[] ids);

    /**
     * 把换模计划数据发布到中间库
     * @param dataVersion 接口发布版本号
     * @param ids  排程发布的ids
     * @param factoryCode 厂别
     * @param companyCode 分公司编号
     */
    public void deployMoldPlanToMes(@Param("dataVersion") String dataVersion, @Param("ids") long[] ids,
                                    @Param("factoryCode") String factoryCode, @Param("companyCode") String companyCode);

    /**
     * 保存发布日志
     *
     * @param moldPlanPublishRecordVo 要保存的发布日志
     * @return 结果
     */
    public int insertPublishRecord(MoldPlanPublishRecordVo moldPlanPublishRecordVo);

    public int batchUpdate(@Param("array") long[] ids, @Param("status") String status);

    /**
     * 根据数据版本更新发布日志状态
     *
     * @param dataVersion 数据版本
     * @param status      状态
     */
    public int updatePublishRecordVersion(@Param("dataVersion") String dataVersion, @Param("status") String status);

    /**
     *  检测下发日期是否存在发布成功的记录
     * @param planDate yyyy-MM-dd
     * @return
     */
    public int isPublishSuccessValidate( @Param("planDate") String planDate);

    /**
     * 根据id查询未发布记录的条数
     * @param ids id
     * @return 未发布的记录条数
     */
    public int isPublishByIds(Long[] ids);

    /**
     * 根据ids更改执行状态
     *
     * @param lhApsMoldAdjustPlan ids、要更改的状态
     * @return 结果
     */
    int changeExecute(LhApsMoldAdjustPlan lhApsMoldAdjustPlan);
}
