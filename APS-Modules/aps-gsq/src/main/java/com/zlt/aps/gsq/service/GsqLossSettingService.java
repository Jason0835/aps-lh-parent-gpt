package com.zlt.aps.gsq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.dto.GsqLossSettingDto;
import com.zlt.aps.gsq.entity.GsqLossSetting;

import java.util.List;


/**
 * 钢丝圈损耗率设定Service接口
 *
 * @author chen
 * @date 2021-07-13
 */
public interface GsqLossSettingService extends IService<GsqLossSetting> {
    /**
     * 查询钢丝圈损耗率设定
     *
     * @param id 钢丝圈损耗率设定ID
     * @return 钢丝圈损耗率设定
     */
    public GsqLossSettingDto selectGsqLossSettingById(Long id);

    /**
     * 查询钢丝圈损耗率设定列表
     *
     * @param gsqLossSetting 钢丝圈损耗率设定
     * @return 钢丝圈损耗率设定集合
     */
    public List<GsqLossSettingDto> selectGsqLossSettingList(GsqLossSetting gsqLossSetting);

    /**
     * 新增钢丝圈损耗率设定
     *
     * @param gsqLossSetting 钢丝圈损耗率设定
     * @return 结果
     */
    public int insertGsqLossSetting(GsqLossSetting gsqLossSetting);

    /**
     * 修改钢丝圈损耗率设定
     *
     * @param gsqLossSetting 钢丝圈损耗率设定
     * @return 结果
     */
    public int updateGsqLossSetting(GsqLossSetting gsqLossSetting);

    /**
     * 批量删除钢丝圈损耗率设定
     *
     * @param ids 需要删除的钢丝圈损耗率设定ID
     * @return 结果
     */
    public int deleteGsqLossSettingByIds(Long[] ids);

    /**
     * 删除钢丝圈损耗率设定信息
     *
     * @param id 钢丝圈损耗率设定ID
     * @return 结果
     */
    public int deleteGsqLossSettingById(Long id);

    /**
     * 校验钢丝圈损耗率设定唯一性
     */
    public String checkGsqLossSettingUnique(GsqLossSetting gsqLossSetting);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GsqLossSettingDto> list, boolean updateSupport, Long importLogId);

    void deleteAll();
}
