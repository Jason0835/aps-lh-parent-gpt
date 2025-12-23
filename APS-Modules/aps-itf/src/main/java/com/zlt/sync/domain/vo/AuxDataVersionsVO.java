package com.zlt.sync.domain.vo;

import com.zlt.sync.domain.AuxDataVersions;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuxDataVersionsVO extends AuxDataVersions {
	private static final long serialVersionUID = 4716819258606079223L;
	private String yyyyMMdd;
}
