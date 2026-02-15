package team.kitemc.verifymc.infrastructure.lifecycle;

/**
 * 生命周期接口
 * 定义组件的生命周期方法
 * 实现此接口的组件将由LifecycleManager统一管理
 */
public interface Lifecycle {
    /**
     * 初始化组件
     * 在组件注册后、启动前调用
     * 用于执行一次性初始化操作
     * @throws Exception 初始化过程中可能抛出的异常
     */
    void initialize() throws Exception;

    /**
     * 启动组件
     * 在初始化完成后调用
     * 用于启动服务、连接资源等
     * @throws Exception 启动过程中可能抛出的异常
     */
    void start() throws Exception;

    /**
     * 停止组件
     * 在插件禁用时调用
     * 用于释放资源、关闭连接等
     * @throws Exception 停止过程中可能抛出的异常
     */
    void stop() throws Exception;

    /**
     * 获取组件名称
     * @return 组件名称
     */
    String getName();

    /**
     * 获取组件状态
     * @return 当前状态
     */
    LifecycleState getState();

    /**
     * 设置组件状态
     * @param state 新状态
     */
    void setState(LifecycleState state);

    /**
     * 组件生命周期状态枚举
     */
    enum LifecycleState {
        NEW,           // 新创建，未初始化
        INITIALIZED,   // 已初始化
        STARTED,       // 已启动
        STOPPED,       // 已停止
        FAILED         // 失败
    }
}
