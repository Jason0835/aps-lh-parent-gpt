package com.zlt.aps.tm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tm.api.domain.dto.TmLossSettingDto;
import com.zlt.aps.tm.entity.TmLossSetting;

import java.util.List;


/**
 * 胎面损耗率设定Service接口
 *
 * @author chen
 * @date 2021-07-12
 */
public interface TmLossSettingService extends IService<TmLossSetting> {
    /**
     * 查询胎面损耗率设定
     *
     * @param id 胎面损耗率设定ID
     * @return 胎面损耗率设定
     */
    public TmLossSettingDto selectTmLossSettingById(Long id);

    /**
     * 查询胎面损耗率设定列表
     *
     * @param tmLossSetting 胎面损耗率设定
     * @return 胎面损耗率设定集合
     */
    public List<TmLossSettingDto> selectTmLossSettingList(TmLossSetting tmLossSetting);

    /**
     * 新增胎面损耗率设定
     *
     * @param tmLossSetting 胎面损耗率设定
     * @return 结果
     */
    public int insertTmLossSetting(TmLossSetting tmLossSetting);

    /**
     * 修改胎面损耗率设定
     *
     * @param tmLossSetting 胎面损耗率设定
     * @return 结果
     */
    public int updateTmLossSetting(TmLossSetting tmLossSetting);

    /**
     * 批量删除胎面损耗率设定
     *
     * @param ids 需要删除的胎面损耗率设定ID
     * @return 结果
     */
    public int deleteTmLossSettingByIds(Long[] ids);

    /**
     * 删除胎面损耗率设定信息
     *
     * @param id 胎面损耗率设定ID
     * @return 结果
     */
    public int deleteTmLossSettingById(Long id);

    /**
     * 校验胎面损耗率设定唯一性
     */
    public String checkTmLossSettingUnique(TmLossSetting tmLossSetting);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<TmLossSettingDto> list, boolean updateSupport, Long importLogId);

    void deleteAll();
}
