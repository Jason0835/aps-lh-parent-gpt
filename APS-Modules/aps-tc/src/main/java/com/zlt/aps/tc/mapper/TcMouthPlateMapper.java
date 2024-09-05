package com.zlt.aps.tc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tc.api.domain.dto.TcMouthPlateDto;
import com.zlt.aps.tc.entity.TcMouthPlate;

import java.util.List;

/**
 * <p>
 * 胎侧口型板信息维护 Mapper 接口
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-02
 */
public interface TcMouthPlateMapper extends BaseMapper<TcMouthPlate> {


    /**
     * 查询口型板信息及对应的机台名称
     *
     * @param mouthPlate 口型板信息
     * @return 查询到的口型板信息
     */
    public List<TcMouthPlateDto> selectMouthPlateWithMachineInfo(TcMouthPlate mouthPlate);

    /**
     * 校验记录唯一性
     *
     * @param mouthPlate 要校验的记录
     * @return 查询到相同的记录条数
     */
    public int checkUnique(TcMouthPlate mouthPlate);

    /**
     * 合并操作，存在则更新，否则新增
     */
    public void mergeSql(List<TcMouthPlateDto> list);

    void deleteAll();
}
