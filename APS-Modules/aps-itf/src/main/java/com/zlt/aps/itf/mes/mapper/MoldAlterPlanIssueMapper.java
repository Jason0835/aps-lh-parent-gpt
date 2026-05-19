package com.zlt.aps.itf.mes.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.mes.vo.MoldAlterPlanIssue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模具交替计划下发Mapper接口
 * 写入APS中间表MOLD_ALTER_PLAN（建在jy_aps_mid主库），通知MES来获取模具交替计划数据
 *
 * @author APS Team
 */
@DS(DataSource.MASTER)
@Mapper
public interface MoldAlterPlanIssueMapper {

    /**
     * 批量新增模具交替计划到APS中间表MOLD_ALTER_PLAN
     *
     * @param moldAlterPlanList 模具交替计划列表
     * @return 插入数量
     */
    int insertMoldAlterPlanList(@Param("list") List<MoldAlterPlanIssue> moldAlterPlanList);

    /**
     * 按工单号和分厂编码删除中间表中的旧数据，避免脏数据残留导致MES消费异常
     *
     * @param orderNos 工单号列表
     * @param factoryCode 分厂编码
     * @return 删除数量
     */
    int deleteByOrderNosAndFactoryCode(@Param("orderNos") List<String> orderNos, @Param("factoryCode") String factoryCode);
}
