package team.kitemc.verifymc.infrastructure;

import team.kitemc.verifymc.infrastructure.annotation.Component;
import team.kitemc.verifymc.infrastructure.annotation.Inject;
import team.kitemc.verifymc.infrastructure.annotation.Service;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 轻量级依赖注入容器
 * 支持构造函数注入、单例/原型作用域、循环依赖检测
 * 线程安全实现
 */
public class DIContainer {
    private static final Logger LOGGER = Logger.getLogger("VerifyMC-DI");
    private final ConcurrentHashMap<String, Object> singletonInstances = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BeanDefinition> beanDefinitions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, String> typeToNameMap = new ConcurrentHashMap<>();
    private final ThreadLocal<Set<String>> creatingBeans = ThreadLocal.withInitial(HashSet::new);
    private volatile boolean initialized = false;

    /**
     * Bean定义类
     * 存储Bean的元数据信息
     */
    public static class BeanDefinition {
        private final String name;
        private final Class<?> type;
        private final Scope scope;
        private final Constructor<?> constructor;
        private final Class<?>[] dependencies;

        public BeanDefinition(String name, Class<?> type, Scope scope, Constructor<?> constructor, Class<?>[] dependencies) {
            this.name = name;
            this.type = type;
            this.scope = scope;
            this.constructor = constructor;
            this.dependencies = dependencies;
        }

        public String getName() { return name; }
        public Class<?> getType() { return type; }
        public Scope getScope() { return scope; }
        public Constructor<?> getConstructor() { return constructor; }
        public Class<?>[] getDependencies() { return dependencies; }
    }

    /**
     * Bean作用域枚举
     */
    public enum Scope {
        SINGLETON,   // 单例：整个应用只有一个实例
        PROTOTYPE    // 原型：每次获取都创建新实例
    }

    /**
     * 注册单例Bean（使用类型作为名称）
     * @param type Bean类型
     */
    public <T> void registerSingleton(Class<T> type) {
        registerSingleton(type.getSimpleName(), type);
    }

    /**
     * 注册单例Bean
     * @param name Bean名称
     * @param type Bean类型
     */
    public <T> void registerSingleton(String name, Class<T> type) {
        registerBean(name, type, Scope.SINGLETON);
    }

    /**
     * 注册原型Bean（使用类型作为名称）
     * @param type Bean类型
     */
    public <T> void registerPrototype(Class<T> type) {
        registerPrototype(type.getSimpleName(), type);
    }

    /**
     * 注册原型Bean
     * @param name Bean名称
     * @param type Bean类型
     */
    public <T> void registerPrototype(String name, Class<T> type) {
        registerBean(name, type, Scope.PROTOTYPE);
    }

    /**
     * 注册Bean实例（直接提供实例）
     * @param name Bean名称
     * @param instance Bean实例
     */
    public <T> void registerInstance(String name, T instance) {
        if (instance == null) {
            throw new IllegalArgumentException("实例不能为空");
        }
        String beanName = name != null ? name : instance.getClass().getSimpleName();
        singletonInstances.put(beanName, instance);
        typeToNameMap.put(instance.getClass(), beanName);
        LOGGER.info("已注册Bean实例: " + beanName);
    }

    /**
     * 注册Bean实例（使用类型作为名称）
     * @param instance Bean实例
     */
    public <T> void registerInstance(T instance) {
        if (instance == null) {
            throw new IllegalArgumentException("实例不能为空");
        }
        registerInstance(instance.getClass().getSimpleName(), instance);
    }

    /**
     * 注册Bean
     * @param name Bean名称
     * @param type Bean类型
     * @param scope 作用域
     */
    public <T> void registerBean(String name, Class<T> type, Scope scope) {
        if (name == null || name.isEmpty()) {
            name = type.getSimpleName();
        }
        Constructor<?> constructor = findInjectConstructor(type);
        Class<?>[] dependencies = constructor.getParameterTypes();
        BeanDefinition definition = new BeanDefinition(name, type, scope, constructor, dependencies);
        beanDefinitions.put(name, definition);
        typeToNameMap.put(type, name);
        LOGGER.info("已注册Bean定义: " + name + " (作用域: " + scope + ", 依赖: " + Arrays.toString(dependencies) + ")");
    }

    /**
     * 自动扫描并注册带有@Component或@Service注解的类
     * @param packagePrefix 包前缀
     * @param classes 要注册的类列表
     */
    public void scanAndRegister(String packagePrefix, List<Class<?>> classes) {
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Service.class)) {
                Service service = clazz.getAnnotation(Service.class);
                String name = service.value().isEmpty() ? clazz.getSimpleName() : service.value();
                registerBean(name, clazz, Scope.SINGLETON);
            } else if (clazz.isAnnotationPresent(Component.class)) {
                Component component = clazz.getAnnotation(Component.class);
                String name = component.value().isEmpty() ? clazz.getSimpleName() : component.value();
                registerBean(name, clazz, Scope.SINGLETON);
            }
        }
    }

    /**
     * 查找带有@Inject注解的构造函数
     * 如果没有标记的构造函数，则使用无参构造函数或唯一的构造函数
     */
    private Constructor<?> findInjectConstructor(Class<?> type) {
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        Constructor<?> injectConstructor = null;
        Constructor<?> noArgConstructor = null;
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(Inject.class)) {
                injectConstructor = constructor;
                break;
            }
            if (constructor.getParameterCount() == 0) {
                noArgConstructor = constructor;
            }
        }
        if (injectConstructor != null) {
            injectConstructor.setAccessible(true);
            return injectConstructor;
        }
        if (noArgConstructor != null) {
            noArgConstructor.setAccessible(true);
            return noArgConstructor;
        }
        if (constructors.length == 1) {
            constructors[0].setAccessible(true);
            return constructors[0];
        }
        throw new IllegalStateException("找不到合适的构造函数: " + type.getName() + 
            "。请使用@Inject注解标记构造函数，或提供无参构造函数");
    }

    /**
     * 获取Bean实例（按类型）
     * @param type Bean类型
     * @return Bean实例
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        String name = typeToNameMap.get(type);
        if (name == null) {
            throw new IllegalStateException("未找到类型为 " + type.getName() + " 的Bean定义");
        }
        return (T) getBean(name);
    }

    /**
     * 获取Bean实例（按名称）
     * @param name Bean名称
     * @return Bean实例
     */
    public Object getBean(String name) {
        Object instance = singletonInstances.get(name);
        if (instance != null) {
            return instance;
        }
        BeanDefinition definition = beanDefinitions.get(name);
        if (definition == null) {
            throw new IllegalStateException("未找到名称为 " + name + " 的Bean定义");
        }
        return createBean(definition);
    }

    /**
     * 创建Bean实例
     */
    private Object createBean(BeanDefinition definition) {
        String name = definition.getName();
        Set<String> currentCreating = creatingBeans.get();
        if (currentCreating.contains(name)) {
            throw new IllegalStateException("检测到循环依赖: " + buildCircularDependencyMessage(name, currentCreating));
        }
        currentCreating.add(name);
        try {
            Object instance = doCreateBean(definition);
            if (definition.getScope() == Scope.SINGLETON) {
                singletonInstances.put(name, instance);
            }
            return instance;
        } finally {
            currentCreating.remove(name);
        }
    }

    /**
     * 实际创建Bean实例
     */
    private Object doCreateBean(BeanDefinition definition) {
        Constructor<?> constructor = definition.getConstructor();
        Class<?>[] dependencies = definition.getDependencies();
        Object[] args = new Object[dependencies.length];
        for (int i = 0; i < dependencies.length; i++) {
            args[i] = resolveDependency(dependencies[i]);
        }
        try {
            LOGGER.fine("创建Bean实例: " + definition.getName());
            return constructor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException("创建Bean失败: " + definition.getName(), e);
        }
    }

    /**
     * 解析依赖
     */
    private Object resolveDependency(Class<?> type) {
        String beanName = typeToNameMap.get(type);
        if (beanName != null) {
            return getBean(beanName);
        }
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
            if (type.isAssignableFrom(entry.getValue().getType())) {
                return getBean(entry.getKey());
            }
        }
        throw new IllegalStateException("无法解析依赖: " + type.getName());
    }

    /**
     * 构建循环依赖错误信息
     */
    private String buildCircularDependencyMessage(String beanName, Set<String> creating) {
        StringBuilder sb = new StringBuilder();
        sb.append(beanName);
        for (String name : creating) {
            sb.append(" -> ").append(name);
        }
        sb.append(" -> ").append(beanName);
        return sb.toString();
    }

    /**
     * 检查是否包含指定名称的Bean
     * @param name Bean名称
     * @return 是否包含
     */
    public boolean containsBean(String name) {
        return beanDefinitions.containsKey(name) || singletonInstances.containsKey(name);
    }

    /**
     * 检查是否包含指定类型的Bean
     * @param type Bean类型
     * @return 是否包含
     */
    public boolean containsBean(Class<?> type) {
        return typeToNameMap.containsKey(type);
    }

    /**
     * 获取Bean的作用域
     * @param name Bean名称
     * @return 作用域
     */
    public Scope getScope(String name) {
        BeanDefinition definition = beanDefinitions.get(name);
        return definition != null ? definition.getScope() : null;
    }

    /**
     * 获取所有Bean名称
     * @return Bean名称集合
     */
    public Set<String> getBeanNames() {
        Set<String> names = new HashSet<>();
        names.addAll(beanDefinitions.keySet());
        names.addAll(singletonInstances.keySet());
        return names;
    }

    /**
     * 获取单例Bean数量
     * @return 单例数量
     */
    public int getSingletonCount() {
        return singletonInstances.size();
    }

    /**
     * 获取Bean定义数量
     * @return 定义数量
     */
    public int getBeanDefinitionCount() {
        return beanDefinitions.size();
    }

    /**
     * 初始化所有单例Bean
     */
    public void initializeSingletons() {
        if (initialized) {
            LOGGER.warning("容器已经初始化");
            return;
        }
        LOGGER.info("开始初始化所有单例Bean...");
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
            BeanDefinition definition = entry.getValue();
            if (definition.getScope() == Scope.SINGLETON && !singletonInstances.containsKey(entry.getKey())) {
                try {
                    getBean(entry.getKey());
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "初始化单例Bean失败: " + entry.getKey(), e);
                    throw e;
                }
            }
        }
        initialized = true;
        LOGGER.info("所有单例Bean初始化完成，共 " + singletonInstances.size() + " 个实例");
    }

    /**
     * 清空容器
     */
    public void clear() {
        singletonInstances.clear();
        beanDefinitions.clear();
        typeToNameMap.clear();
        initialized = false;
        LOGGER.info("容器已清空");
    }

    /**
     * 检查容器是否已初始化
     * @return 是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
}
