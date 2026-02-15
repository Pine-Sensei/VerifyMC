package team.kitemc.verifymc.infrastructure.lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import team.kitemc.verifymc.infrastructure.lifecycle.LifecycleState;

/**
 * 生命周期管理器
 * 负责管理所有实现Lifecycle接口的组件的生命周期
 * 确保组件按照正确的顺序初始化、启动和停止
 */
public class LifecycleManager {
    private static final Logger LOGGER = Logger.getLogger("VerifyMC-Lifecycle");
    private final ConcurrentHashMap<String, Lifecycle> components = new ConcurrentHashMap<>();
    private final List<String> startupOrder = new ArrayList<>();
    private volatile boolean isRunning = false;

    /**
     * 注册生命周期组件
     * @param component 组件实例
     */
    public void register(Lifecycle component) {
        String name = component.getClass().getSimpleName();
        components.put(name, component);
        startupOrder.add(name);
        LOGGER.info("已注册生命周期组件: " + name);
    }

    /**
     * 注册生命周期组件（指定名称）
     * @param name 组件名称
     * @param component 组件实例
     */
    public void register(String name, Lifecycle component) {
        if (name == null || name.isEmpty()) {
            name = component.getClass().getSimpleName();
        }
        components.put(name, component);
        startupOrder.add(name);
        LOGGER.info("已注册生命周期组件: " + name);
    }

    /**
     * 注销生命周期组件
     * @param name 组件名称
     */
    public void unregister(String name) {
        Lifecycle component = components.remove(name);
        if (component != null) {
            startupOrder.remove(name);
            try {
                if (component.getState() == LifecycleState.STARTED) {
                    component.stop();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "停止组件时发生异常: " + name, e);
            }
            LOGGER.info("已注销生命周期组件: " + name);
        }
    }

    /**
     * 初始化所有组件
     */
    public void initializeAll() {
        LOGGER.info("开始初始化所有生命周期组件...");
        for (String name : startupOrder) {
            Lifecycle component = components.get(name);
            if (component != null && component.getState() == LifecycleState.NEW) {
                try {
                    component.initialize();
                    LOGGER.info("组件初始化完成: " + name);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "组件初始化失败: " + name, e);
                    component.setState(LifecycleState.FAILED);
                }
            }
        }
        LOGGER.info("所有组件初始化完成");
    }

    /**
     * 启动所有组件
     */
    public void startAll() {
        if (isRunning) {
            LOGGER.warning("生命周期管理器已在运行中");
            return;
        }
        LOGGER.info("开始启动所有生命周期组件...");
        for (String name : startupOrder) {
            Lifecycle component = components.get(name);
            if (component != null) {
                LifecycleState state = component.getState();
                if (state == LifecycleState.INITIALIZED || state == LifecycleState.NEW) {
                    try {
                        if (state == LifecycleState.NEW) {
                            component.initialize();
                        }
                        component.start();
                        LOGGER.info("组件启动完成: " + name);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "组件启动失败: " + name, e);
                        component.setState(LifecycleState.FAILED);
                    }
                }
            }
        }
        isRunning = true;
        LOGGER.info("所有组件启动完成");
    }

    /**
     * 停止所有组件
     * 按照启动的逆序停止
     */
    public void stopAll() {
        if (!isRunning) {
            LOGGER.warning("生命周期管理器未在运行");
            return;
        }
        LOGGER.info("开始停止所有生命周期组件...");
        List<String> reverseOrder = new ArrayList<>(startupOrder);
        java.util.Collections.reverse(reverseOrder);
        for (String name : reverseOrder) {
            Lifecycle component = components.get(name);
            if (component != null && component.getState() == LifecycleState.STARTED) {
                try {
                    component.stop();
                    LOGGER.info("组件停止完成: " + name);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "组件停止时发生异常: " + name, e);
                    component.setState(LifecycleState.FAILED);
                }
            }
        }
        isRunning = false;
        LOGGER.info("所有组件停止完成");
    }

    /**
     * 获取组件
     * @param name 组件名称
     * @return 组件实例，不存在则返回null
     */
    public Lifecycle getComponent(String name) {
        return components.get(name);
    }

    /**
     * 获取所有组件名称
     * @return 组件名称列表
     */
    public List<String> getComponentNames() {
        return new ArrayList<>(components.keySet());
    }

    /**
     * 获取组件数量
     * @return 组件数量
     */
    public int getComponentCount() {
        return components.size();
    }

    /**
     * 检查是否正在运行
     * @return 是否正在运行
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * 获取处于特定状态的组件列表
     * @param state 目标状态
     * @return 处于该状态的组件名称列表
     */
    public List<String> getComponentsByState(LifecycleState state) {
        List<String> result = new ArrayList<>();
        for (ConcurrentHashMap.Entry<String, Lifecycle> entry : components.entrySet()) {
            if (entry.getValue().getState() == state) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
}
