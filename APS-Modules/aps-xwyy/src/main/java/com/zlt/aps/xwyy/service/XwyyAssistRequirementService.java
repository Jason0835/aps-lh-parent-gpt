package com.zlt.aps.xwyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.xwyy.api.domain.entity.XwyyAssistRequirement;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 纤维压延外厂需求Service接口
 *
 * @author chen
 * @date 2022-03-14
 */
public interface XwyyAssistRequirementService extends IService<XwyyAssistRequirement> {
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
    @Transactional
    public int insertXwyyAssistRequirement(XwyyAssistRequirement xwyyAssistRequirement);

    /**
     * 修改纤维压延外厂需求
     *
     * @param xwyyAssistRequirement 纤维压延外厂需求
     * @return 结果
     */
    @Transactional
    public int updateXwyyAssistRequirement(XwyyAssistRequirement xwyyAssistRequirement);

    /**
     * 批量删除纤维压延外厂需求
     *
     * @param ids 需要删除的纤维压延外厂需求ID
     * @return 结果
     */
    @Transactional
    public int deleteXwyyAssistRequirementByIds(Long[] ids);

    /**
     * 删除纤维压延外厂需求信息
     *
     * @param id 纤维压延外厂需求ID
     * @return 结果
     */
    @Transactional
    public int deleteXwyyAssistRequirementById(Long id);

    /**
     * 校验纤维压延外厂需求唯一性
     */
    public String checkXwyyAssistRequirementUnique(XwyyAssistRequirement xwyyAssistRequirement);

    /**
     * 导入纤维压延外厂需求数据
     */
    @Transactional
    public AjaxResult importData(List<XwyyAssistRequirement> list, Long importLogId, Date scheduleDate);
}
