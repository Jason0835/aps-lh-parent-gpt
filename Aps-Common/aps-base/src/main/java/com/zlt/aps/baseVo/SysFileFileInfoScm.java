package com.zlt.aps.baseVo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.zlt.file.api.domain.SysFileFileInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;


@ApiModel(value = "文件原始信息对象", description = "文件原始信息对象 ")
@Data
public class SysFileFileInfoScm extends SysFileFileInfo {

    private static final long serialVersionUID = 1L;
    /**
     * 上传次数
     */
    private Integer uploadNum;

    /**
     * bizId集合
     */
    private List<String> bizIds;

    /** 创建人人 */
    @ApiModelProperty(value = "创建人", notes = "虚字段从createBy转义", name = "createByName")
    @TableField(exist = false)
    private String createByName;


}