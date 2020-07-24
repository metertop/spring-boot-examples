package com.hyx.Thread.demo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * @Description:
 * @Author: haoyuexun
 * @Date: 2020/7/24 12:15
 */

@Component
public class ThreadPoolUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ThreadPoolUtil.class);

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public void executeTask(Runnable task){
        threadPoolTaskExecutor.submit(task);
        //try {
        //注意task.get()会阻塞，直到返回数据为止，所以一般这样用法很少用
        //resp = task.get();
        //} catch (InterruptedException e) {
        //e.printStackTrace();
        //} catch (ExecutionException e) {
        //e.printStackTrace();
        //}
    }

    /**
     * @Description:
     * @Author: haoyuexun
     * @Date: 2020/7/24 12:17
     */
    @SpringBootConfiguration
    public static class WebConfig extends WebMvcConfigurationSupport {
        @Value("${server.port}")
        public String port;

        @Value("${threadpool.core-pool-size}")
        private int corePoolSize;

        @Value("${threadpool.max-pool-size}")
        private int maxPoolSize;

        @Value("${threadpool.queue-capacity}")
        private int queueCapacity;

        @Value("${threadpool.keep-alive-seconds}")
        private int keepAliveSeconds;

        @Override
        protected void addInterceptors(InterceptorRegistry registry) {
            super.addInterceptors(registry);
            // 将 ApiInterceptor 拦截器类添加进去
            registry.addInterceptor(new ApiInterceptor());
        }

        @Bean(name="threadPoolTaskExecutor")
        public ThreadPoolTaskExecutor threadPoolTaskExecutor(){
            ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
            pool.setKeepAliveSeconds(keepAliveSeconds);
            // 核心线程池数
            pool.setCorePoolSize(corePoolSize);
            // 最大线程
            pool.setMaxPoolSize(maxPoolSize);
            // 队列容量
            pool.setQueueCapacity(queueCapacity);
            // 队列满，线程被拒绝执行策略
            pool.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
            return pool;
        }
    }
}
