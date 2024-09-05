package com.zlt.aps.tq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.dto.TqLossSettingDto;
import com.zlt.aps.tq.entity.TqLossSetting;

import java.util.List;


/**
 * 胎圈损耗率设定Service接口
 *
 * @author chen
 * @date 2021-07-13
 */
public interface TqLossSettingService extends IService<TqLossSetting> {
    /**
     * 查询胎圈损耗率设定
     *
     * @param id 胎圈损耗率设定ID
     * @return 胎圈损耗率设定
     */
    public TqLossSettingDto selectTqLossSettingById(Long id);

    /**
     * 查询胎圈损耗率设定列表
     *
     * @param tqLossSetting 胎圈损耗率设定
     * @return 胎圈损耗率设定集合
     */
    public List<TqLossSettingDto> selectTqLossSettingList(TqLossSetting tqLossSetting);

    /**
     * 新增胎圈损耗率设定
     *
     * @param tqLossSetting 胎圈损耗率设定
     * @return 结果
     */
    public int insertTqLossSetting(TqLossSetting tqLossSetting);

    /**
     * 修改胎圈损耗率设定
     *
     * @param tqLossSetting 胎圈损耗率设定
     * @return 结果
     */
    public int updateTqLossSetting(TqLossSetting tqLossSetting);

    /**
     * 批量删除胎圈损耗率设定
     *
     * @param ids 需要删除的胎圈损耗率设定ID
     * @return 结果
     */
    public int deleteTqLossSettingByIds(Long[] ids);

    /**
     * 删除胎圈损耗率设定信息
     *
     * @param id 胎圈损耗率设定ID
     * @return 结果
     */
    public int deleteTqLossSettingById(Long id);

    /**
     * 校验胎圈损耗率设定唯一性
     */
    public String checkTqLossSettingUnique(TqLossSetting tqLossSetting);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<TqLossSettingDto> list, boolean updateSupport, Long importLogId);

    void deleteAll();
}
