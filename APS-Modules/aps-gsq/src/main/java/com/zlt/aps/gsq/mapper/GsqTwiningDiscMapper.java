package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDisc;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 钢丝圈缠绕盘 Mapper 接口
 *
 * @author zlt
 * @date 2026-07-08
 */
public interface GsqTwiningDiscMapper extends BaseMapper<GsqTwiningDisc> {

    /**
     * 查询钢丝圈缠绕盘列表
     *
     * @param entity 查询条件
     * @return 列表
     */
    List<GsqTwiningDisc> listTwiningDisc(GsqTwiningDisc entity);

    /**
     * 校验缠绕盘编码是否已存在
     *
     * @param entity 实体
     * @return 已存在数量（0表示唯一，>0表示不唯一）
     */
    int checkUnique(GsqTwiningDisc entity);

    /**
     * 批量合并保存（存在则更新，否则新增），用于导入场景
     *
     * @param list 待保存数据集合
     */
    void mergeSql(List<GsqTwiningDisc> list);

    /**
     * 按钢丝圈编号批量查询施工信息表中的钢丝圈信息（编号->名称映射，用于导入校验与名称反显）
     *
     * @param codes 钢丝圈编号集合
     * @return 钢丝圈信息列表（key：BEAD_CODE 钢丝圈编号、BEAD_NAME 钢丝圈名称）
     */
    List<Map<String, Object>> listSteelRingInfoByCodes(@Param("codes") List<String> codes);

    /**
     * 查询施工信息表全部钢丝圈选项（编码+名称，去重），供页面下拉选择使用
     *
     * @return 钢丝圈选项列表（key：BEAD_CODE 钢丝圈编号、BEAD_NAME 钢丝圈名称）
     */
    List<Map<String, Object>> listSteelRingOptions();

    /**
     * MES缠绕盘清单同步专用批量插入（XML显式列，绕过MetaObjectHandler，CREATE_BY='MES'）
     *
     * @param list 待插入的缠绕盘清单（均为APS中不存在的新增编码）
     */
    void batchInsertMesDisc(List<GsqTwiningDisc> list);
}
