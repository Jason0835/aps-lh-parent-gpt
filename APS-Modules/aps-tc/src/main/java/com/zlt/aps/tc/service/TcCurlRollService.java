package com.zlt.aps.tc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tc.api.domain.dto.TcCurlRollDto;
import com.zlt.aps.tc.api.domain.entity.TcCurlRoll;

import java.util.List;

/**
 * <p>
 * 胎侧卷曲信息表 服务类
 * </p>
 *
 * @author zlt
 * @since 2023-09-07
 */
public interface TcCurlRollService extends IService<TcCurlRoll> {

    /**
     * 根据条件查询胎侧卷曲信息列表
     *
     * @return
     */
    List<TcCurlRoll> listCurlRoll(TcCurlRoll dto);

    /**
     * 保存胎侧卷曲信息信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveCurlRoll(TcCurlRoll entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    void deleteCurlRoll(Long[] ids);

    /**
     * 根据code判断胎侧卷曲代号是否已经存在
     */
    String checkCurlRollCodeUnique(TcCurlRoll dto);

    /**
     * 导入数据
     */
    AjaxResult importData(List<TcCurlRollDto> list, boolean updateSupport, Long importLogId);

    /**
     * 根据code查询卷曲长度
     *
     * @param curlRoll 查询条件
     * @return 结果
     */
    AjaxResult selectCurlLengthByCode(TcCurlRoll curlRoll);
}
