package com.zlt.aps.xwyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.xwyy.api.domain.entity.XwyyAssistRequirement;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 纤维压延外厂需求Mapper接口
 *
 * @author chen
 * @date 2022-03-14
 */
public interface XwyyAssistRequirementMapper extends BaseMapper<XwyyAssistRequirement> {
    /**
     * 查询纤维压延外厂需求
     *
     * @param id 纤维压延外厂需求ID
     * @return 纤维压延外厂需求
     */
    public XwyyAssistRequirement selectXwyyAssistRequirementById(Long id);

    /**
     * 查询纤维压延外厂需求列表
     *
     * @param xwyyAssistRequirement 纤维压延外厂需求
     * @return 纤维压延外厂需求集合
     */
    public List<XwyyAssistRequirement> selectXwyyAssistRequirementList(XwyyAssistRequirement xwyyAssistRequirement);

    /**
     * 新增纤维压延外厂需求
     *
     * @param xwyyAssistRequirement 纤维压延外厂需求
     * @return 结果
     */
    public int insertXwyyAssistRequirement(XwyyAssistRequirement xwyyAssistRequirement);

    /**
     * 修改纤维压延外厂需求
     *
     * @param xwyyAssistRequirement 纤维压延外厂需求
     * @return 结果
     */
    public int updateXwyyAssistRequirement(XwyyAssistRequirement xwyyAssistRequirement);

    /**
     * 删除纤维压延外厂需求
     *
     * @param id 纤维压延外厂需求ID
     * @return 结果
     */
    public int deleteXwyyAssistRequirementById(Long id);

    /**
     * 批量删除纤维压延外厂需求
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteXwyyAssistRequirementByIds(Long[] ids);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<XwyyAssistRequirement> list);

    /**
     * 校验记录是否唯一
     * @param xwyyAssistRequirement 要校验的记录
     * @return 查询到的记录数
     */
    public int checkUnique(XwyyAssistRequirement xwyyAssistRequirement);

    /**
     * 删除排程日期的所有数据
     * @param scheduleDate 要删除的日期
     * @return 删除的条数
     */
    public int deleteAll(@Param("scheduleDate") Date scheduleDate);
}
