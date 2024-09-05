package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.dto.CxLossSettingDto;
import com.zlt.aps.cx.entity.CxLossSetting;

import java.util.List;

/**
 * 成型损耗率设定Service接口
 *
 * @author chen
 * @date 2021-07-19
 */
public interface CxLossSettingService extends IService<CxLossSetting> {
    /**
     * 查询成型损耗率设定
     *
     * @param id 成型损耗率设定ID
     * @return 成型损耗率设定
     */
    public CxLossSettingDto selectCxLossSettingById(Long id);

    /**
     * 查询成型损耗率设定列表
     *
     * @param cxLossSetting 成型损耗率设定
     * @return 成型损耗率设定集合
     */
    public List<CxLossSettingDto> selectCxLossSettingList(CxLossSetting cxLossSetting);

    /**
     * 新增成型损耗率设定
     *
     * @param cxLossSetting 成型损耗率设定
     * @return 结果
     */
    public int insertCxLossSetting(CxLossSetting cxLossSetting);

    /**
     * 修改成型损耗率设定
     *
     * @param cxLossSetting 成型损耗率设定
     * @return 结果
     */
    public int updateCxLossSetting(CxLossSetting cxLossSetting);

    /**
     * 批量删除成型损耗率设定
     *
     * @param ids 需要删除的成型损耗率设定ID
     * @return 结果
     */
    public int deleteCxLossSettingByIds(Long[] ids);

    /**
     * 删除成型损耗率设定信息
     *
     * @param id 成型损耗率设定ID
     * @return 结果
     */
    public int deleteCxLossSettingById(Long id);

    /**
     * 校验成型损耗率设定唯一性
     */
    public String checkCxLossSettingUnique(CxLossSetting cxLossSetting);

    /**
     * 导入数据
     */
    AjaxResult importData(List<CxLossSettingDto> list, boolean updateSupport, Long importLogId);
}
