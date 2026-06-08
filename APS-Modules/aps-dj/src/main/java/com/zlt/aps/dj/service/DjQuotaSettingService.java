package com.zlt.aps.dj.service;

import java.util.List;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.dj.api.domain.entity.DjQuotaSetting;


/**
 * 垫胶定额设定Service接口
 *
 * @author zlt
 * @date 2021-06-29
 */
public interface DjQuotaSettingService {
    /**
     * 查询垫胶定额设定
     *
     * @param id 垫胶定额设定ID
     * @return 垫胶定额设定
     */
    public DjQuotaSetting selectNcQuotaSettingById(Long id);

    /**
     * 查询垫胶定额设定列表
     *
     * @param ncQuotaSetting 垫胶定额设定
     * @return 垫胶定额设定集合
     */
    public List<DjQuotaSetting> selectNcQuotaSettingList(DjQuotaSetting ncQuotaSetting);

    /**
     * 新增垫胶定额设定
     *
     * @param ncQuotaSetting 垫胶定额设定
     * @return 结果
     */
    public int insertNcQuotaSetting(DjQuotaSetting ncQuotaSetting);

    /**
     * 修改垫胶定额设定
     *
     * @param ncQuotaSetting 垫胶定额设定
     * @return 结果
     */
    public int updateNcQuotaSetting(DjQuotaSetting ncQuotaSetting);

    /**
     * 批量删除垫胶定额设定
     *
     * @param ids 需要删除的垫胶定额设定ID
     * @return 结果
     */
    public int deleteNcQuotaSettingByIds(Long[] ids);

    /**
     * 删除垫胶定额设定信息
     *
     * @param id 垫胶定额设定ID
     * @return 结果
     */
    public int deleteNcQuotaSettingById(Long id);

    /**
     * 校验垫胶定额设定唯一性
     */
    public String checkNcQuotaSettingUnique(DjQuotaSetting ncQuotaSetting);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<DjQuotaSetting> list, boolean updateSupport, Long importLogId);
}
