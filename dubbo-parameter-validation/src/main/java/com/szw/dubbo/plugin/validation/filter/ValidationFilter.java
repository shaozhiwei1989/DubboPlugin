package com.szw.dubbo.plugin.validation.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import com.szw.dubbo.plugin.common.result.ResponseCode;
import com.szw.dubbo.plugin.common.result.ServiceResponse;
import com.szw.dubbo.plugin.validation.annotation.ValidGroup;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.hibernate.validator.HibernateValidator;

import org.springframework.validation.annotation.Validated;


//@Activate(group = {CommonConstants.CONSUMER, CommonConstants.PROVIDER}, value = FilterConstants.VALIDATION_KEY)
@Activate(order = 1)
public class ValidationFilter implements Filter {

	private static final Validator validator;

	static {
		ValidatorFactory validatorFactory = Validation
				.byProvider(HibernateValidator.class)
				.configure()
				.buildValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		if (invocation == null) {
			return invoker.invoke(null);
		}

		Object[] args = invocation.getArguments();
		if (args == null || args.length == 0) {
			return invoker.invoke(invocation);
		}

		Map<Integer, Set<ConstraintViolation<Object>>> allValidatedMap = new LinkedHashMap<>();
		for (int i = 0; i < args.length; i++) {
			if (!checkIfNeedValid(args[i])) {
				continue;
			}

			List<Class<?>> validGroup = findValidGroup(invocation, i);
			Class<?>[] groups = validGroup.toArray(new Class<?>[] {});

			Set<ConstraintViolation<Object>> validated = validator.validate(args[i], groups);
			if (validated != null && !validated.isEmpty()) {
				allValidatedMap.put(i, validated);
			}
		}

		if (!allValidatedMap.isEmpty()) {
			String validMsgStr = formatValidMsg(allValidatedMap);
			if (invocation instanceof RpcInvocation ri) {
				if (ri.getReturnTypes()[0] == ServiceResponse.class) {
					return buildValidMsgResult(invocation, validMsgStr);
				}
			}
			throw new RpcException(validMsgStr);
		}

		return invoker.invoke(invocation);
	}

	private static List<Class<?>> findValidGroup(Invocation invocation, int index) {
		Class<?> parameterType = invocation.getParameterTypes()[index];
		ValidGroup validGroup = parameterType.getAnnotation(ValidGroup.class);

		List<Class<?>> list = new ArrayList<>();
		if (validGroup != null && validGroup.value() != null) {
			list.addAll(Arrays.asList(validGroup.value()));
		}
		return list;
	}

	private static boolean checkIfNeedValid(Object target) {
		Valid valid = target.getClass().getAnnotation(Valid.class);
		Validated validated = target.getClass().getAnnotation(Validated.class);
		return valid != null || validated != null;
	}

	private static String formatValidMsg(Map<Integer, Set<ConstraintViolation<Object>>> allValidatedMap) {
		StringJoiner joiner = new StringJoiner(", ");
		allValidatedMap.forEach((k, v) -> {
			StringJoiner inner = new StringJoiner(", ");
			v.forEach(violation -> inner.add(String.format("{%s:[%s]}", violation.getPropertyPath(), violation.getMessage())));
			joiner.add("参数" + (k + 1) + ":" + inner);
		});
		return joiner.toString();
	}

	private static Result buildValidMsgResult(Invocation invocation, String validMsgStr) {
		Result result = AsyncRpcResult.newDefaultAsyncResult(invocation);
		result.setAttachments(invocation.getAttachments());
		result.setObjectAttachments(invocation.getObjectAttachments());
		result.setValue(new ServiceResponse<>(ResponseCode.ERROR, validMsgStr));
		return result;
	}

}
