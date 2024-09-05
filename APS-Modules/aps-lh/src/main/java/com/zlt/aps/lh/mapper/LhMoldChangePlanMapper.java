package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlan;
import com.zlt.aps.lh.vo.MoldPlanPublishRecordVo;

import java.util.List;

import org.apache.ibatis.annotations.Param;

/**
 * 模具变动单Mapper接口
 *
 * @author zlt
 * @date 2021-06-17
 */
public interface LhMoldChangePlanMapper {
    /**
     * 查询模具变动单
     *
     * @param id 模具变动单ID
     * @return 模具变动单
     */
    public LhMoldChangePlan selectLhMoldChangePlanById(Long id);

    /**
     * 查询模具变动单列表
     *
     * @param lhMoldChangePlan 模具变动单
     * @return 模具变动单集合
     */
    public List<LhMoldChangePlan> selectLhMoldChangePlanList(LhMoldChangePlan lhMoldChangePlan);

    /**
     * 新增模具变动单
     *
     * @param lhMoldChangePlan 模具变动单
     * @return 结果
     */
    public int insertLhMoldChangePlan(LhMoldChangePlan lhMoldChangePlan);

    /**
     * 修改模具变动单
     *
     * @param lhMoldChangePlan 模具变动单
     * @return 结果
     */
    public int updateLhMoldChangePlan(LhMoldChangePlan lhMoldChangePlan);

    /**
     * 删除模具变动单
     *
     * @param id 模具变动单ID
     * @return 结果
     */
    public int deleteLhMoldChangePlanById(Long id);

    /**
     * 批量删除模具变动单
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteLhMoldChangePlanByIds(Long[] ids);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<LhMoldChangePlan> list);

    /**
     * 把模具变动单发布到中间库
     * @param dataVersion 接口发布版本号
     * @param ids  排程发布的ids
     */
    void deployMoldChangePlanToMes(@Param("dataVersion") String dataVersion, @Param("ids") long[] ids);

    /**
     * 发布模具变动单，更新发布状态
     * @param ids 待发布的模具变动单id
     */
    void updateRelease(@Param("ids") long[] ids);
    
    /**
     * 新增发布记录
     * @param record
     */
    void insertPublishRecord(@Param("record")MoldPlanPublishRecordVo record);
}
