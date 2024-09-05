package com.zlt.aps.tc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tc.api.domain.dto.TcParamsDto;
import com.zlt.aps.tc.entity.TcParams;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 胎侧参数信息Service接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface TcParamsService extends IService<TcParams> {
    /**
     * 查询胎侧参数信息
     *
     * @param id 胎侧参数信息ID
     * @return 胎侧参数信息
     */
    public TcParams selectParamsById(Long id);

    /**
     * 查询胎侧参数信息列表
     *
     * @param params 胎侧参数信息
     * @return 胎侧参数信息集合
     */
    public List<TcParamsDto> selectParamsList(TcParams params);

    /**
     * 修改胎侧参数信息
     *
     * @param params 胎侧参数信息
     * @return 结果
     */
    @Transactional
    public AjaxResult updateParams(TcParams params);

    /**
     * 校验胎侧参数代码唯一
     *
     * @param params 胎侧参数信息
     * @return 是否唯一
     */
    public String checkParamsCodeUnique(TcParams params);
}
