package com.zlt.aps.dj.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.dj.api.domain.dto.DjCurlRollDto;
import com.zlt.aps.dj.api.domain.entity.DjCurlRoll;

/**
 * <p>
 * 垫胶卷曲信息表 服务类
 * </p>
 *
 * @author zlt
 * @since 2023-09-07
 */
public interface DjCurlRollService extends IService<DjCurlRoll> {

    /**
     * 根据条件查询垫胶卷曲信息列表
     *
     * @return
     */
    List<DjCurlRoll> listCurlRoll(DjCurlRoll dto);

    /**
     * 保存垫胶卷曲信息信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveCurlRoll(DjCurlRoll entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    void deleteCurlRoll(Long[] ids);

    /**
     * 根据code判断垫胶卷曲代号是否已经存在
     */
    String checkCurlRollCodeUnique(DjCurlRoll dto);

    /**
     * 导入数据
     */
    AjaxResult importData(List<DjCurlRollDto> list, boolean updateSupport, Long importLogId);

    /**
     * 根据code查询卷曲长度
     *
     * @param curlRoll 查询条件
     * @return 结果
     */
    AjaxResult selectCurlLengthByCode(DjCurlRoll curlRoll);
}
