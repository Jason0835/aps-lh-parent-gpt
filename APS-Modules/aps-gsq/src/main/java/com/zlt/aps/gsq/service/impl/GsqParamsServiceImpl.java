package com.zlt.aps.gsq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.gsq.entity.GsqParams;
import com.zlt.aps.gsq.mapper.GsqParamsMapper;
import com.zlt.aps.gsq.service.GsqParamsService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * 钢丝圈排程参数配置 Service业务层处理
 *
 * @author zlt
 * @date 2021-05-25
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class GsqParamsServiceImpl extends AbstractDocService<GsqParams> implements GsqParamsService {

    @Resource
    private GsqParamsMapper gsqParamsMapper;

    @Override
    protected String getDocTypeCode() {
        return "GSQ0801";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("GSQ0801");
        return sysDocType;
    }

    @Override
    public String checkUnique(GsqParams query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.gsq.params.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段：工厂编号 + 参数编码
        return new ArrayList<>(Arrays.asList("factoryCode", "paramCode"));
    }

    /**
     * 根据参数编码和工厂编号查询参数
     *
     * @param paramCode   参数编码
     * @param factoryCode 工厂编号
     * @return 参数实体
     */
    @Override
    public GsqParams selectOneByParamCode(String paramCode, String factoryCode) {
        if (StringUtils.isBlank(paramCode)) {
            return null;
        }
        LambdaQueryWrapper<GsqParams> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(GsqParams::getParamCode, paramCode);
        if (StringUtils.isNotBlank(factoryCode)) {
            wrapper.eq(GsqParams::getFactoryCode, factoryCode);
        }
        return gsqParamsMapper.selectOne(wrapper);
    }

    /**
     * 查询指定工厂的所有参数，返回参数编码到参数值的映射
     *
     * @param factoryCode 工厂编号
     * @return 参数编码-参数值映射
     */
    @Override
    public Map<String, String> listGsqParams(String factoryCode) {
        Map<String, String> params = new HashMap<>();
        QueryWrapper<GsqParams> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        List<GsqParams> paramsList = gsqParamsMapper.selectList(queryWrapper);
        if (PubUtil.isNotEmpty(paramsList)) {
            for (GsqParams param : paramsList) {
                params.put(param.getParamCode(), param.getParamValue());
            }
        }
        return params;
    }
}
