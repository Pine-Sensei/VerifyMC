package team.kitemc.verifymc.infrastructure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 服务层组件注解
 * 继承自Component，用于标记服务类
 * 服务类通常包含业务逻辑
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface Service {
    /**
     * 服务名称，可选
     * 如果不指定，将使用类名作为服务名称
     */
    String value() default "";
}
