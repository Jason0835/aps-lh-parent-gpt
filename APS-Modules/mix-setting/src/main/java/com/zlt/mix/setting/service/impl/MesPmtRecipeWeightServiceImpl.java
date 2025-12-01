package com.zlt.mix.setting.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.setting.mapper.MesPmtRecipeWeightMapper;
import com.zlt.mix.setting.api.domain.entity.MesPmtRecipeWeight;
import com.zlt.mix.setting.service.MesPmtRecipeWeightService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 配方称量明细Service业务层处理
 *
 * @author chen
 * @date 2022-06-01
 */
@Service
public class MesPmtRecipeWeightServiceImpl extends ServiceImpl<MesPmtRecipeWeightMapper, MesPmtRecipeWeight> implements MesPmtRecipeWeightService {
    @Resource
    private MesPmtRecipeWeightMapper mesPmtRecipeWeightMapper;

    /**
     * 查询配方称量明细列表
     *
     * @param mesPmtRecipeWeight 配方称量明细
     * @return 配方称量明细
     */
    @Override
    public List<MesPmtRecipeWeight> selectMesPmtRecipeWeightList(MesPmtRecipeWeight mesPmtRecipeWeight) {
        return mesPmtRecipeWeightMapper.selectMesPmtRecipeWeightList(mesPmtRecipeWeight);
    }
}
