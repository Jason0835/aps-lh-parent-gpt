package com.zlt.aps.dj.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.dj.api.domain.dto.DjMouthPlateDto;
import com.zlt.aps.dj.api.domain.entity.DjMouthPlate;

/**
 * <p>
 * 垫胶口型板信息维护 Mapper 接口
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-05-27
 */
public interface DjMouthPlateMapper extends BaseMapper<DjMouthPlate> {

    /**
     * 查询口型板信息及对应的机台名称
     *
     * @param mouthPlate 口型板信息
     * @return 查询到的口型板信息
     */
    public List<DjMouthPlateDto> selectMouthPlateWithMachineInfo(DjMouthPlate mouthPlate);

    /**
     * 校验记录唯一性
     *
     * @param mouthPlate 要校验的记录
     * @return 查询到相同的记录条数
     */
    public int checkUnique(DjMouthPlate mouthPlate);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<DjMouthPlateDto> list);
}
