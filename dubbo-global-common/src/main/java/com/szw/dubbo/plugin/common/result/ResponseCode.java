package com.szw.dubbo.plugin.common.result;

public interface ResponseCode {
	int SUCCESS = 100000;
	int ERROR = 999999;

	int getCode();

	String getDesc();

}
