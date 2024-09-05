package com.zlt.aps.tc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tc.api.domain.dto.TcLossSettingDto;
import com.zlt.aps.tc.entity.TcLossSetting;

import java.util.List;


/**
 * 胎侧损耗率设定Service接口
 *
 * @author chen
 * @date 2021-07-13
 */
public interface TcLossSettingService extends IService<TcLossSetting> {
    /**
     * 查询胎侧损耗率设定
     *
     * @param id 胎侧损耗率设定ID
     * @return 胎侧损耗率设定
     */
    public TcLossSettingDto selectTcLossSettingById(Long id);

    /**
     * 查询胎侧损耗率设定列表
     *
     * @param tcLossSetting 胎侧损耗率设定
     * @return 胎侧损耗率设定集合
     */
    public List<TcLossSettingDto> selectTcLossSettingList(TcLossSetting tcLossSetting);

    /**
     * 新增胎侧损耗率设定
     *
     * @param tcLossSetting 胎侧损耗率设定
     * @return 结果
     */
    public int insertTcLossSetting(TcLossSetting tcLossSetting);

    /**
     * 修改胎侧损耗率设定
     *
     * @param tcLossSetting 胎侧损耗率设定
     * @return 结果
     */
    public int updateTcLossSetting(TcLossSetting tcLossSetting);

    /**
     * 批量删除胎侧损耗率设定
     *
     * @param ids 需要删除的胎侧损耗率设定ID
     * @return 结果
     */
    public int deleteTcLossSettingByIds(Long[] ids);

    /**
     * 删除胎侧损耗率设定信息
     *
     * @param id 胎侧损耗率设定ID
     * @return 结果
     */
    public int deleteTcLossSettingById(Long id);

    /**
     * 校验胎侧损耗率设定唯一性
     */
    public String checkTcLossSettingUnique(TcLossSetting tcLossSetting);

    /**
     * 导入数据
     */
    public AjaxResult importData(List<TcLossSettingDto> list, boolean updateSupport, Long importLogId);

    void deleteAll();
}
