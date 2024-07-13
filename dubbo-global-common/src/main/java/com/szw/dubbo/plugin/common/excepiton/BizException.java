package com.szw.dubbo.plugin.common.excepiton;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BizException extends RuntimeException {

	private int code;

	private String desc;


	public BizException(int code, String desc) {
		super(desc);
		this.code = code;
		this.desc = desc;
	}

	public BizException(Throwable cause, int code, String desc) {
		super(desc, cause);
		this.code = code;
		this.desc = desc;
	}


	public BizException(Throwable cause, boolean enableSuppression, boolean writableStackTrace, int code, String desc) {
		super(desc, cause, enableSuppression, writableStackTrace);
		this.code = code;
		this.desc = desc;
	}

}
