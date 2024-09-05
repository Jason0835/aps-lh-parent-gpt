package com.zlt.aps.cd15.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.dto.Cd15BigRollDto;
import com.zlt.aps.cd15.entity.Cd15BigRoll;

import java.util.List;

/**
 * <p>
 * 15度裁断钢压大卷信息表 服务类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface Cd15BigRollService extends IService<Cd15BigRoll> {

    /**
     * 根据条件查询钢压大卷信息列表
     *
     * @return
     */
    List<Cd15BigRollDto> listBigRoll(Cd15BigRollDto dto);

    /**
     * 保存钢压大卷信息信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveBigRoll(Cd15BigRoll entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    void deleteBigRoll(Long[] ids);

    /**
     * 根据code判断钢压大卷代号是否已经存在
     */
    String checkBigRollCodeUnique(Cd15BigRollDto dto);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<Cd15BigRollDto> list, boolean updateSupport, Long importLogId);
}
