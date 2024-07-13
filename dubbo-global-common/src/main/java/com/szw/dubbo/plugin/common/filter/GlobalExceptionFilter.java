package com.szw.dubbo.plugin.common.filter;

import com.szw.dubbo.plugin.common.excepiton.BizException;
import com.szw.dubbo.plugin.common.result.ResponseCode;
import com.szw.dubbo.plugin.common.result.ServiceResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;


@Activate(order = 2)
@Slf4j(topic = "running")
public class GlobalExceptionFilter implements Filter {


	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		Result result = invoker.invoke(invocation);
		if (result.hasException()) {
			Throwable throwable = result.getException();
			String methodName = invocation.getMethodName();

			log.error(methodName, throwable);

			if (invocation instanceof RpcInvocation ri) {
				if (ri.getReturnTypes()[0] == ServiceResponse.class) {
					if (throwable instanceof BizException e) {
						return buildNewResult(invocation, e);
					}
					else if (throwable instanceof Exception e) {
						return buildNewResult(invocation, wrapException(e));
					}
				}
			}
		}
		return result;
	}

	private static Result buildNewResult(Invocation invocation, BizException e) {
		Result err = AsyncRpcResult.newDefaultAsyncResult(invocation);
		err.setException(null);
		err.setAttachments(invocation.getAttachments());
		err.setObjectAttachments(invocation.getObjectAttachments());
		err.setValue(new ServiceResponse<>(e.getCode(), e.getDesc()));
		return err;
	}

	private static BizException wrapException(Exception e) {
		return new BizException(ResponseCode.ERROR, "#接口异常#");
	}

}
