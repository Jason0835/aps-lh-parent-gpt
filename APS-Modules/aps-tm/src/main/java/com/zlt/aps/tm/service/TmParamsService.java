package com.zlt.aps.tm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tm.api.domain.dto.TmParamsDto;
import com.zlt.aps.tm.entity.TmParams;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 胎面参数信息Service接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface TmParamsService extends IService<TmParams> {
    /**
     * 查询胎面参数信息
     *
     * @param id 胎面参数信息ID
     * @return 胎面参数信息
     */
    public TmParams selectParamsById(Long id);

    /**
     * 查询胎面参数信息列表
     *
     * @param tmParams 胎面参数信息
     * @return 胎面参数信息集合
     */
    public List<TmParamsDto> selectParamsList(TmParams tmParams);

    /**
     * 修改胎面参数信息
     *
     * @param tmParams 胎面参数信息
     * @return 结果
     */
    @Transactional
    public AjaxResult updateParams(TmParams tmParams);

    /**
     * 校验胎面参数代码唯一
     *
     * @param tmParams 胎面参数信息
     * @return 是否唯一
     */
    public String checkParamsCodeUnique(TmParams tmParams);
}
