package com.zlt.aps.nc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.nc.api.domain.dto.NcParamsDto;
import com.zlt.aps.nc.entity.NcParams;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 内衬参数信息Service接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface NcParamsService extends IService<NcParams> {
    /**
     * 查询内衬参数信息
     *
     * @param id 内衬参数信息ID
     * @return 内衬参数信息
     */
    public NcParams selectParamsById(Long id);

    /**
     * 查询内衬参数信息列表
     *
     * @param params 内衬参数信息     * @return 内衬参数信息集合
     */
    public List<NcParamsDto> selectParamsList(NcParams params);

    /**
     * 修改内衬参数信息
     *
     * @param params 内衬参数信息
     * @return 结果
     */
    @Transactional
    public AjaxResult updateParams(NcParams params);

    /**
     * 校验内衬参数代码唯一
     *
     * @param params 内衬参数信息
     * @return 是否唯一
     */
    public String checkParamsCodeUnique(NcParams params);
}
