package com.zlt.mix.setting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.setting.api.domain.entity.LhflScheduleParams;
import com.zlt.mix.setting.mapper.LhflScheduleParamsMapper;
import com.zlt.mix.setting.service.LhflScheduleParamsService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 排程参数（硫磺辅料排程设置）Service业务层处理
 *
 * @author Liam
 * @date 2022-04-06
 */
@Service
public class LhflScheduleParamsServiceImpl extends ServiceImpl<LhflScheduleParamsMapper, LhflScheduleParams> implements LhflScheduleParamsService {
    @Resource
    private LhflScheduleParamsMapper lhflScheduleParamsMapper;

    /**
     * 查询排程参数（硫磺辅料排程设置）列表
     *
     * @param lhflScheduleParams 排程参数（硫磺辅料排程设置）
     * @return 排程参数（硫磺辅料排程设置）
     */
    @Override
    public List<LhflScheduleParams> selectLhflScheduleParamsList(LhflScheduleParams lhflScheduleParams) {
        return lhflScheduleParamsMapper.selectLhflScheduleParamsList(lhflScheduleParams);
    }

    /**
     * 保存排程参数（硫磺辅料排程设置）信息（id为空则新增，id不为空则修改）
     *
     * @param lhflScheduleParams
     */
    @Override
    public AjaxResult saveLhflScheduleParams(LhflScheduleParams lhflScheduleParams) {

        //基本参数校验
        if (!ObjectUtils.allNotNull(lhflScheduleParams.getId(), lhflScheduleParams.getParamCode(), lhflScheduleParams.getParamName(), lhflScheduleParams.getParamValue())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.invalidParameter"));
        }

        //校验正则表达式
        boolean regularResult = true;
        String regularExpression = lhflScheduleParams.getRegularExpression();
        if (StringUtils.isNotEmpty(regularExpression)) {
            Pattern pattern = Pattern.compile(regularExpression);
            regularResult = pattern.matcher(lhflScheduleParams.getParamValue()).matches();
        }

        if (regularResult) {
            lhflScheduleParams.setBaseValue(lhflScheduleParams.getId());
            baseMapper.updateById(lhflScheduleParams);
        }
        return regularResult ? AjaxResult.success() : AjaxResult.error(lhflScheduleParams.getErrorTips());
    }

    /**
     * 校验排程参数信息唯一性（同一ID的数据也算作重复）
     */
    public String checkScheduleParamsUnique(LhflScheduleParams lhflScheduleParams) {
        if (lhflScheduleParams == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        QueryWrapper<LhflScheduleParams> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DEL_FLAG", ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq("MIX_AREA", lhflScheduleParams.getMixArea());
        queryWrapper.eq("PARAM_CODE", lhflScheduleParams.getParamCode());


        List<LhflScheduleParams> list = baseMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    @Override
    public AjaxResult copyLhflScheduleParams(LhflScheduleParams lhflScheduleParams) {
        //基本参数校验
        if (!ObjectUtils.allNotNull(lhflScheduleParams.getId(), lhflScheduleParams.getParamCode(), lhflScheduleParams.getParamName(), lhflScheduleParams.getParamValue())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.invalidParameter"));
        }

        //校验是否存在
        if (ZltConstant.NOT_UNIQUE.equals(checkScheduleParamsUnique(lhflScheduleParams))) {
            return AjaxResult.error(I18nUtil.getMessage("setting.lhflScheduleParams.database.unique"));
        }

        //校验正则表达式
        boolean regularResult = true;
        String regularExpression = lhflScheduleParams.getRegularExpression();
        if (StringUtils.isNotEmpty(regularExpression)) {
            Pattern pattern = Pattern.compile(regularExpression);
            regularResult = pattern.matcher(lhflScheduleParams.getParamValue()).matches();
        }

        if (regularResult) {
            lhflScheduleParams.setBaseValue(null);
            lhflScheduleParams.setUpdateBy(null);
            lhflScheduleParams.setUpdateTime(null);
            baseMapper.insert(lhflScheduleParams);
        }
        return regularResult ? AjaxResult.success() : AjaxResult.error(lhflScheduleParams.getErrorTips());
    }
}
