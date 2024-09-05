package com.zlt.aps.nc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.nc.api.domain.dto.NcLossSettingDto;
import com.zlt.aps.nc.entity.NcLossSetting;

import java.util.List;


/**
 * 内衬损耗率设定Service接口
 *
 * @author chen
 * @date 2021-07-13
 */
public interface NcLossSettingService extends IService<NcLossSetting> {
    /**
     * 查询内衬损耗率设定
     *
     * @param id 内衬损耗率设定ID
     * @return 内衬损耗率设定
     */
    public NcLossSettingDto selectNcLossSettingById(Long id);

    /**
     * 查询内衬损耗率设定列表
     *
     * @param ncLossSetting 内衬损耗率设定
     * @return 内衬损耗率设定集合
     */
    public List<NcLossSettingDto> selectNcLossSettingList(NcLossSetting ncLossSetting);

    /**
     * 新增内衬损耗率设定
     *
     * @param ncLossSetting 内衬损耗率设定
     * @return 结果
     */
    public int insertNcLossSetting(NcLossSetting ncLossSetting);

    /**
     * 修改内衬损耗率设定
     *
     * @param ncLossSetting 内衬损耗率设定
     * @return 结果
     */
    public int updateNcLossSetting(NcLossSetting ncLossSetting);

    /**
     * 批量删除内衬损耗率设定
     *
     * @param ids 需要删除的内衬损耗率设定ID
     * @return 结果
     */
    public int deleteNcLossSettingByIds(Long[] ids);

    /**
     * 删除内衬损耗率设定信息
     *
     * @param id 内衬损耗率设定ID
     * @return 结果
     */
    public int deleteNcLossSettingById(Long id);

    /**
     * 校验内衬损耗率设定唯一性
     */
    public String checkNcLossSettingUnique(NcLossSetting ncLossSetting);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<NcLossSettingDto> list, boolean updateSupport, Long importLogId);

    void deleteAll();
}
