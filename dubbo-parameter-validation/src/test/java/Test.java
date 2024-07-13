import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.HibernateValidator;

public class Test {
	private static final Validator validator;

	static {
		ValidatorFactory validatorFactory = Validation
				.byProvider(HibernateValidator.class)
				.configure()
				.buildValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	public static void main(String[] args) {
		Map<Integer, Set<ConstraintViolation<Object>>> allValidatedMap = new LinkedHashMap<>();
		for (int i = 0; i < 5; i++) {
			A a = new A();
			a.setAge(0);
			Set<ConstraintViolation<Object>> validate = validator.validate(a);
			allValidatedMap.put(i, validate);
		}


//		validate.forEach(o->{
//			System.out.printf(o.getMessage() + "\n");
//			System.out.printf(o.getRootBean().getClass() + "\n");
//			System.out.printf(o.getPropertyPath() + "\n");
//		});

		String s = formatValidMsg(allValidatedMap);
		System.out.printf(s + "\n");
	}

	@Setter
	@Getter
	static class A {
		@NotNull(message = "msg 不能为空")
		private String msg;

		@Min(message = "age 不能小于1", value = 1)
		private Integer age;
	}


	private static String formatValidMsg(Map<Integer, Set<ConstraintViolation<Object>>> allValidatedMap) {
		StringJoiner joiner = new StringJoiner(", ");

		allValidatedMap.forEach((k, v) -> {
			StringJoiner inner = new StringJoiner(", ");
			v.forEach(violation -> {
				inner.add(String.format("{%s:[%s]}", violation.getPropertyPath(), violation.getMessage()));
			});

			joiner.add("参数" + (k + 1) + ":" + inner);
		});
        // haha
		return joiner.toString();
	}
}
