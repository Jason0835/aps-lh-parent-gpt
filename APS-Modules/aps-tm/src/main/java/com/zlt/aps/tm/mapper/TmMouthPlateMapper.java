package com.zlt.aps.tm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tm.api.domain.dto.TmMouthPlateDto;
import com.zlt.aps.tm.entity.TmMouthPlate;

import java.util.List;

/**
 * <p>
 * 胎面口型板信息维护 Mapper 接口
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-05-27
 */
public interface TmMouthPlateMapper extends BaseMapper<TmMouthPlate> {

    /**
     * 查询口型板信息及对应的机台名称
     *
     * @param tmMouthPlate 口型板信息
     * @return 查询到的口型板信息
     */
    public List<TmMouthPlateDto> selectMouthPlateWithMachineInfo(TmMouthPlate tmMouthPlate);

    /**
     * 校验记录唯一性
     *
     * @param mouthPlate 要校验的记录
     * @return 查询到相同的记录条数
     */
    public int checkUnique(TmMouthPlate mouthPlate);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<TmMouthPlateDto> list);

    /**
     * 根据机台id和编号查询记录
     *
     * @param mouthPlate 要查询的记录
     * @return 结果
     */
    public TmMouthPlateDto selectByCodeAndMachineId(TmMouthPlateDto mouthPlate);

    /**
     * 批量插入记录
     *
     * @param list 要插入的记录
     */
    void insertList(List<TmMouthPlateDto> list);

    void deleteAll();
}
