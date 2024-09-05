package com.zlt.aps.nc.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.nc.api.domain.entity.NcQuotaSetting;

import java.util.List;


/**
 * 内衬定额设定Service接口
 *
 * @author zlt
 * @date 2021-06-29
 */
public interface NcQuotaSettingService {
    /**
     * 查询内衬定额设定
     *
     * @param id 内衬定额设定ID
     * @return 内衬定额设定
     */
    public NcQuotaSetting selectNcQuotaSettingById(Long id);

    /**
     * 查询内衬定额设定列表
     *
     * @param ncQuotaSetting 内衬定额设定
     * @return 内衬定额设定集合
     */
    public List<NcQuotaSetting> selectNcQuotaSettingList(NcQuotaSetting ncQuotaSetting);

    /**
     * 新增内衬定额设定
     *
     * @param ncQuotaSetting 内衬定额设定
     * @return 结果
     */
    public int insertNcQuotaSetting(NcQuotaSetting ncQuotaSetting);

    /**
     * 修改内衬定额设定
     *
     * @param ncQuotaSetting 内衬定额设定
     * @return 结果
     */
    public int updateNcQuotaSetting(NcQuotaSetting ncQuotaSetting);

    /**
     * 批量删除内衬定额设定
     *
     * @param ids 需要删除的内衬定额设定ID
     * @return 结果
     */
    public int deleteNcQuotaSettingByIds(Long[] ids);

    /**
     * 删除内衬定额设定信息
     *
     * @param id 内衬定额设定ID
     * @return 结果
     */
    public int deleteNcQuotaSettingById(Long id);

    /**
     * 校验内衬定额设定唯一性
     */
    public String checkNcQuotaSettingUnique(NcQuotaSetting ncQuotaSetting);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<NcQuotaSetting> list, boolean updateSupport, Long importLogId);
}
