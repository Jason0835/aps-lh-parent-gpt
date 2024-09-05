package com.zlt.aps.gdyy.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.gdyy.api.domain.dto.GdyyMattersAttentionDto;
import com.zlt.aps.gdyy.entity.GdyyMattersAttention;
import com.zlt.aps.gdyy.mapper.GdyyMattersAttentionMapper;
import com.zlt.aps.gdyy.service.GdyyMattersAttentionService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * <p>
 * 帘布大卷注意事项信息表 服务实现类
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-07
 */
@Service
public class GdyyMattersAttentionServiceImpl extends ServiceImpl<GdyyMattersAttentionMapper, GdyyMattersAttention> implements GdyyMattersAttentionService {

    @Resource
    private GdyyMattersAttentionMapper gdyyMattersAttentionMapper;

    /**
     * 根据条件大卷注意事项列表
     *
     * @return
     */
    public List<GdyyMattersAttentionDto> listGdyyMattersAttention(GdyyMattersAttentionDto dto) {
        return gdyyMattersAttentionMapper.listGwyyMattersAttention(dto);
    }

    /**
     * 保存大卷注意事项信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveGdyyMattersAttention(GdyyMattersAttention entity) {
        entity.setBaseVale(entity.getId());  //根据id是否为空给创建时间，创建人，更新时间，更新人赋值
        this.saveOrUpdate(entity);
    }

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    public void deleteGdyyMattersAttention(Long[] ids) {
        for (int i = 0; i < ids.length; i++) {
            GdyyMattersAttention entity = new GdyyMattersAttention();
            entity.setId(ids[i]);
            entity.setDelFlag(ApsConstant.DEL_FLAG_DEL);
            entity.setUpdateTime(new Date());
            this.updateById(entity);
        }
    }

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<GdyyMattersAttention> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        //做校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<GdyyMattersAttention> importList = new ArrayList<>();
		// 按业务主键分组
		Map<String, Long> groupMap = list.stream()
				.collect(Collectors.groupingBy(GdyyMattersAttention::getBigRollCode, Collectors.counting()));
        for (int i = 0; i < list.size(); i++) {
            GdyyMattersAttention entity = list.get(i);
			// excel内业务主键唯一校验
			Long hasValue = groupMap.get(entity.getBigRollCode());
			if (hasValue > 1) {
				entity.setId(-999L);
				String columnName = I18nUtil.getMessage("ui.common.column.gy.bigRollCode");
				addImportErrorLog(importLogId, i + 2,
						String.format(I18nUtil.getMessage("ui.data.column.all.conflictRecord"), columnName),
						importErrorLogs);
                failureNum++;
				continue;
			}

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, entity);
            if (CollectionUtils.isNotEmpty(validated)) {
				entity.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{

                //时间校验
                if (entity.getStartTime()!=null && entity.getEndTime()!=null) {
                    if(entity.getStartTime().after(entity.getEndTime())){
                        failureNum++;
                        entity.setId(-999L);
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.beginDateCanNotGreatThanEndDate"), importErrorLogs);
                        continue;
                    }
                }
                entity.setBaseVale(null);
                importList.add(entity);
            }
        }
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用merge即可
                if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                    successNum = importList.size();
                    gdyyMattersAttentionMapper.mergeSql(importList);
                } else {
                    //查询数据库已存在对象
                    for (int i = 0; i < list.size(); i++) {
                        GdyyMattersAttention excelItem = list.get(i);
                        //过滤错误的记录
                        if (excelItem.getId() != null && excelItem.getId() == -999L) {
                            continue;
                        }
                        // 唯一性校验
                        GdyyMattersAttentionDto query = new GdyyMattersAttentionDto();
                        query.setBigRollCode(excelItem.getBigRollCode());
                        List<GdyyMattersAttentionDto> unic = gdyyMattersAttentionMapper.listGwyyMattersAttention(query);
                        if (CollectionUtils.isEmpty(unic)) {
                            //不存在插入
                            successNum++;
                            gdyyMattersAttentionMapper.insert(excelItem);
                        } else {
                            // 存在，插入错误详细日志
                            failureNum++;
                            addImportErrorLog(importLogId, i + 2,
                                    I18nUtil.getMessage("ui.gdyyMattersAttention.message.unique"), importErrorLogs);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // 执行sql失败，插入导入失败记录
                successNum = 0;
                failureNum = list.size();
                importErrorLogs.clear();
                addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            }
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
