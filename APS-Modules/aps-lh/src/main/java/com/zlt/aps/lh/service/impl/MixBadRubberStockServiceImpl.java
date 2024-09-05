package com.zlt.aps.lh.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.lh.mapper.MixBadRubberStockMapper;
import com.zlt.aps.lh.api.domain.entity.MixBadRubberStock;
import com.zlt.aps.lh.service.MixBadRubberStockService;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * 不合格胶库存Service业务层处理
 * 
 * @author zlt
 * @date 2021-11-08
 */
@Service
public class MixBadRubberStockServiceImpl implements MixBadRubberStockService
{
    @Autowired
    private MixBadRubberStockMapper mixBadRubberStockMapper;

    /**
     * 查询不合格胶库存
     * 
     * @param id 不合格胶库存ID
     * @return 不合格胶库存
     */
    @Override
    public MixBadRubberStock selectMixBadRubberStockById(Long id)
    {
        return mixBadRubberStockMapper.selectMixBadRubberStockById(id);
    }

    /**
     * 查询不合格胶库存列表
     * 
     * @param mixBadRubberStock 不合格胶库存
     * @return 不合格胶库存
     */
    @Override
    public List<MixBadRubberStock> selectMixBadRubberStockList(MixBadRubberStock mixBadRubberStock)
    {
        return mixBadRubberStockMapper.selectMixBadRubberStockList(mixBadRubberStock);
    }

    /**
     * 新增不合格胶库存
     * 
     * @param mixBadRubberStock 不合格胶库存
     * @return 结果
     */
    @Override
    public int insertMixBadRubberStock(MixBadRubberStock mixBadRubberStock)
    {
        mixBadRubberStock.setBaseVale(null);
        return mixBadRubberStockMapper.insertMixBadRubberStock(mixBadRubberStock);
    }

    /**
     * 修改不合格胶库存
     * 
     * @param mixBadRubberStock 不合格胶库存
     * @return 结果
     */
    @Override
    public int updateMixBadRubberStock(MixBadRubberStock mixBadRubberStock)
    {
        mixBadRubberStock.setBaseVale(mixBadRubberStock.getId());
        return mixBadRubberStockMapper.updateMixBadRubberStock(mixBadRubberStock);
    }

    /**
     * 批量删除不合格胶库存
     * 
     * @param ids 需要删除的不合格胶库存ID
     * @return 结果
     */
    @Override
    public int deleteMixBadRubberStockByIds(Long[] ids)
    {
        return mixBadRubberStockMapper.deleteMixBadRubberStockByIds(ids);
    }

    /**
     * 删除不合格胶库存信息
     * 
     * @param id 不合格胶库存ID
     * @return 结果
     */
    @Override
    public int deleteMixBadRubberStockById(Long id)
    {
        return mixBadRubberStockMapper.deleteMixBadRubberStockById(id);
    }

    /**
     * 校验不合格胶库存唯一性
     */
    @Override
    public String checkMixBadRubberStockUnique(MixBadRubberStock mixBadRubberStock) {
        if (mixBadRubberStock == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MixBadRubberStock> list = mixBadRubberStockMapper.selectMixBadRubberStockList(mixBadRubberStock);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入不合格胶库存数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MixBadRubberStock> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MixBadRubberStock> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MixBadRubberStock mixBadRubberStock = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, mixBadRubberStock);
            if (CollectionUtils.isNotEmpty(validated)) {
                mixBadRubberStock.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mixBadRubberStock.setBaseVale(null);
                importList.add(mixBadRubberStock);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    mixBadRubberStockMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MixBadRubberStock mixBadRubberStock = list.get(i);
                    // 错误记录跳过
                    if (mixBadRubberStock.getId() != null && mixBadRubberStock.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMixBadRubberStockUnique(mixBadRubberStock);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMixBadRubberStock(mixBadRubberStock);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage(""), importErrorLogs);
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
