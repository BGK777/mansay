package com.hmdp.utils.systemUtil;

import java.util.concurrent.*;

/**
 * 线程池工具类，提供一个单例的线程池对象,
 * 通过使用IoDH，我们既可以实现延迟加载，又可以保证线程安全，不影响系统性能，不失为一种最好的Java语言单例模式实现方式
 */
public class ThreadPollUtil {
    //唯一实例
    public ThreadPoolExecutor poolExecutor;
    //私有化构造器，构造器初始化线程池对象
    private ThreadPollUtil(){
        //自定义线程池参数
        poolExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                // 等待队列为 有界的阻塞队列，满了会抛出异常
                new ArrayBlockingQueue<>(20),
                // 线程工厂 默认
                Executors.defaultThreadFactory(),
                // 拒绝策略 抛出 RejectedExecutionException来拒绝新任务的处理。
                new ThreadPoolExecutor.AbortPolicy());
    }

    //静态内部类，实例化工具类对象
    private static class PoolInner{
        //通过jvm虚拟机实现线程的安全，只会实例一次
        private final static ThreadPollUtil pool = new ThreadPollUtil();
    }

    //提供对外的唯一实例方法，调用getInstance时会加载静态内部类PoolInner，会初始化里面的成员变量pool，以第一次实例ThreadPollUtil；
    public static ThreadPollUtil getInstance(){
        return PoolInner.pool;
    }

}
