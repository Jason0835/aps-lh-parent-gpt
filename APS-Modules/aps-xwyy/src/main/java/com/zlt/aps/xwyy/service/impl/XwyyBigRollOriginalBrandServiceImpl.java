package com.zlt.aps.xwyy.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollOriginalBrand;
import com.zlt.aps.xwyy.mapper.XwyyBigRollOriginalBrandMapper;
import com.zlt.aps.xwyy.service.XwyyBigRollOriginalBrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 帘布大卷原线品牌Service业务层处理
 *
 * @author chen
 * @date 2022-05-11
 */
@Service
public class XwyyBigRollOriginalBrandServiceImpl extends ServiceImpl<XwyyBigRollOriginalBrandMapper, XwyyBigRollOriginalBrand> implements XwyyBigRollOriginalBrandService {
    @Autowired
    private XwyyBigRollOriginalBrandMapper xwyyBigRollOriginalBrandMapper;

    /**
     * 查询帘布大卷原线品牌
     *
     * @param id 帘布大卷原线品牌ID
     * @return 帘布大卷原线品牌
     */
    @Override
    public XwyyBigRollOriginalBrand selectXwyyBigRollOriginalBrandById(Long id) {
        return xwyyBigRollOriginalBrandMapper.selectXwyyBigRollOriginalBrandById(id);
    }

    /**
     * 查询帘布大卷原线品牌列表
     *
     * @param xwyyBigRollOriginalBrand 帘布大卷原线品牌
     * @return 帘布大卷原线品牌
     */
    @Override
    public List<XwyyBigRollOriginalBrand> selectXwyyBigRollOriginalBrandList(XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand) {
        return xwyyBigRollOriginalBrandMapper.selectXwyyBigRollOriginalBrandList(xwyyBigRollOriginalBrand);
    }

    /**
     * 新增帘布大卷原线品牌
     *
     * @param xwyyBigRollOriginalBrand 帘布大卷原线品牌
     * @return 结果
     */
    @Override
    public int insertXwyyBigRollOriginalBrand(XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand) {
        if (UserConstants.NOT_UNIQUE.equals(checkXwyyBigRollOriginalBrandUnique(xwyyBigRollOriginalBrand))) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.message.bigRollOriginalBrand.datebaseUnique"));
        }
        xwyyBigRollOriginalBrand.setBaseVale(null);
        return xwyyBigRollOriginalBrandMapper.insertXwyyBigRollOriginalBrand(xwyyBigRollOriginalBrand);
    }

    /**
     * 修改帘布大卷原线品牌
     *
     * @param xwyyBigRollOriginalBrand 帘布大卷原线品牌
     * @return 结果
     */
    @Override
    public int updateXwyyBigRollOriginalBrand(XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand) {
        if (UserConstants.NOT_UNIQUE.equals(checkXwyyBigRollOriginalBrandUnique(xwyyBigRollOriginalBrand))) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.message.bigRollOriginalBrand.datebaseUnique"));
        }
        xwyyBigRollOriginalBrand.setBaseVale(xwyyBigRollOriginalBrand.getId());
        return xwyyBigRollOriginalBrandMapper.updateXwyyBigRollOriginalBrand(xwyyBigRollOriginalBrand);
    }

    /**
     * 批量删除帘布大卷原线品牌
     *
     * @param ids 需要删除的帘布大卷原线品牌ID
     * @return 结果
     */
    @Override
    public int deleteXwyyBigRollOriginalBrandByIds(Long[] ids) {
        return xwyyBigRollOriginalBrandMapper.deleteXwyyBigRollOriginalBrandByIds(ids);
    }

    /**
     * 删除帘布大卷原线品牌信息
     *
     * @param id 帘布大卷原线品牌ID
     * @return 结果
     */
    @Override
    public int deleteXwyyBigRollOriginalBrandById(Long id) {
        return xwyyBigRollOriginalBrandMapper.deleteXwyyBigRollOriginalBrandById(id);
    }

    /**
     * 校验帘布大卷原线品牌唯一性
     */
    @Override
    public String checkXwyyBigRollOriginalBrandUnique(XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand) {
        if (xwyyBigRollOriginalBrand == null) {
            return UserConstants.NOT_UNIQUE;
        }
        LambdaQueryWrapper<XwyyBigRollOriginalBrand> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XwyyBigRollOriginalBrand::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(XwyyBigRollOriginalBrand::getBigRollCode, xwyyBigRollOriginalBrand.getBigRollCode());
        wrapper.eq(XwyyBigRollOriginalBrand::getBrand, xwyyBigRollOriginalBrand.getBrand());
        if (xwyyBigRollOriginalBrand.getId() != null) {
            wrapper.ne(XwyyBigRollOriginalBrand::getId, xwyyBigRollOriginalBrand.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        if (CollectionUtils.isNotEmpty(xwyyBigRollOriginalBrandMapper.selectList(wrapper))) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入帘布大卷原线品牌数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<XwyyBigRollOriginalBrand> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<XwyyBigRollOriginalBrand> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(item -> item.getBigRollCode() + item.getBrand(), Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            XwyyBigRollOriginalBrand bigRollOriginalBrand = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(bigRollOriginalBrand.getBigRollCode() + bigRollOriginalBrand.getBrand());
            if (hasValue > 1) {
                failureNum++;
                bigRollOriginalBrand.setId(-999L);
                addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.message.bigRollOriginalBrand.excelUnique"), importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, bigRollOriginalBrand);
            if (CollectionUtils.isNotEmpty(validated)) {
                bigRollOriginalBrand.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                bigRollOriginalBrand.setBaseVale(null);
                importList.add(bigRollOriginalBrand);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                xwyyBigRollOriginalBrandMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    XwyyBigRollOriginalBrand bigRollOriginalBrand = list.get(i);
                    // 错误记录跳过
                    if (bigRollOriginalBrand.getId() != null && bigRollOriginalBrand.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkXwyyBigRollOriginalBrandUnique(bigRollOriginalBrand);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertXwyyBigRollOriginalBrand(bigRollOriginalBrand);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.data.message.bigRollOriginalBrand.datebaseUnique"), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
