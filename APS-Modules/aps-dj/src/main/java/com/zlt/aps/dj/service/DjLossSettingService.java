package com.zlt.aps.dj.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.dj.api.domain.dto.DjLossSettingDto;
import com.zlt.aps.dj.api.domain.entity.DjLossSetting;


/**
 * 垫胶损耗率设定Service接口
 *
 * @author chen
 * @date 2021-07-13
 */
public interface DjLossSettingService extends IService<DjLossSetting> {
    /**
     * 查询垫胶损耗率设定
     *
     * @param id 垫胶损耗率设定ID
     * @return 垫胶损耗率设定
     */
    public DjLossSettingDto selectNcLossSettingById(Long id);

    /**
     * 查询垫胶损耗率设定列表
     *
     * @param ncLossSetting 垫胶损耗率设定
     * @return 垫胶损耗率设定集合
     */
    public List<DjLossSettingDto> selectNcLossSettingList(DjLossSetting ncLossSetting);

    /**
     * 新增垫胶损耗率设定
     *
     * @param ncLossSetting 垫胶损耗率设定
     * @return 结果
     */
    public int insertNcLossSetting(DjLossSetting ncLossSetting);

    /**
     * 修改垫胶损耗率设定
     *
     * @param ncLossSetting 垫胶损耗率设定
     * @return 结果
     */
    public int updateNcLossSetting(DjLossSetting ncLossSetting);

    /**
     * 批量删除垫胶损耗率设定
     *
     * @param ids 需要删除的垫胶损耗率设定ID
     * @return 结果
     */
    public int deleteNcLossSettingByIds(Long[] ids);

    /**
     * 删除垫胶损耗率设定信息
     *
     * @param id 垫胶损耗率设定ID
     * @return 结果
     */
    public int deleteNcLossSettingById(Long id);

    /**
     * 校验垫胶损耗率设定唯一性
     */
    public String checkNcLossSettingUnique(DjLossSetting ncLossSetting);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<DjLossSettingDto> list, boolean updateSupport, Long importLogId);

    void deleteAll();
}
