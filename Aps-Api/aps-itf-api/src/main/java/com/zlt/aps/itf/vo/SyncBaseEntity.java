package com.zlt.aps.itf.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
public class SyncBaseEntity implements Serializable {
	private static final long serialVersionUID = -802951407857239574L;

	private String dataVersion;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")    // 入参格式
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private Date createDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")    // 入参格式
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private Date updateDate;
    private Integer isDelete;

}
