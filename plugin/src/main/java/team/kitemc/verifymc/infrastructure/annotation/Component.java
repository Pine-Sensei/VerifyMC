package team.kitemc.verifymc.infrastructure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 组件标记注解
 * 用于标记可被依赖注入容器管理的组件类
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {
    /**
     * 组件名称，可选
     * 如果不指定，将使用类名作为组件名称
     */
    String value() default "";
}
