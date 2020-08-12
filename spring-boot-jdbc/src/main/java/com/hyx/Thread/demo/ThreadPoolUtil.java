package com.hyx.Thread.demo;
import com.alibaba.fastjson.JSON;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.impl.client.FutureRequestExecutionMetrics;
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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Description:
 * @Author: haoyuexun
 * @Date: 2020/7/24 12:15
 */

@Component("threadPoolUtil")
public class ThreadPoolUtil {
    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolUtil.class);

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
//
    @Autowired
    private ThreadPoolTaskExecutor compareThreadPoolTaskExecutor;

    public void executeTask(Runnable task) {
        threadPoolTaskExecutor.submit(task);
//        try {
//            System.out.println("返回的结果  pass=" + future.get().getPassCount());
//            System.out.println("返回的结果 fail=" + future.get().getFailCount());
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }

        //try {
        //注意task.get()会阻塞，直到返回数据为止，所以一般这样用法很少用
        //resp = task.get();
        //} catch (InterruptedException e) {
        //e.printStackTrace();
        //} catch (ExecutionException e) {
        //e.printStackTrace();
        //}

    }


    public void executeTasks(List<Runnable> tasks) {
        long start = System.currentTimeMillis();
        logger.error("---任务开始：当前时间:{}" , DateUtil.getDateToString(start));
        logger.error("----->task={}", JSON.toJSONString(tasks));
        List<Future<?>> result = new ArrayList<>();
        for(Runnable task: tasks) {
            Future<?> taskResult = threadPoolTaskExecutor.submit(task);
            result.add(taskResult);
        }

        validTaskEnd(result, start);

    }


    public void executeCompareTask(List<Runnable> tasks) {
        long start = System.currentTimeMillis();
        logger.error("---任务开始：当前时间:{}" , DateUtil.getDateToString(start));
        List<Future<?>> result = new ArrayList<>();
        for(Runnable task: tasks) {
//            logger.error("----->task={}", JSON.toJSONString(task));
            Future<?> taskResult = compareThreadPoolTaskExecutor.submit(task);
            result.add(taskResult);
        }
        validTaskEnd(result, start);

    }


    private void validTaskEnd(List<Future<?>> result, long start) {
        while (true) {
            boolean isAllDone = true;
            for (Future<?> taskResult : result) {
                isAllDone &= ( taskResult.isDone() || taskResult.isCancelled() );
            }
            if (isAllDone) {
                // 任务都执行完毕，跳出循环
                long end = System.currentTimeMillis();
                long useSecond = (end-start)/1000;
                logger.error("---任务结束，当前时间{}", DateUtil.getDateToString(end));
                logger.error("--任务用时:{}秒", useSecond);
                break;
            }
            try {

//                logger.info("waiting and sleep 1000 ...");
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (Exception e) {
                System.out.println(e.toString());
                break;
            }
        }
    }




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

//        @Override
//        protected void addInterceptors(InterceptorRegistry registry) {
//            super.addInterceptors(registry);
//            // 将 ApiInterceptor 拦截器类添加进去
//            registry.addInterceptor(new ApiInterceptor());
//        }

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


//        @Bean(name="compareThreadPoolTaskExecutor")
//        public ThreadPoolTaskExecutor compareThreadPoolTaskExecutor(){
//            ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
//            pool.setKeepAliveSeconds(keepAliveSeconds);
//            // 核心线程池数
//            pool.setCorePoolSize(corePoolSize);
//            // 最大线程
//            pool.setMaxPoolSize(maxPoolSize);
//            // 队列容量
//            pool.setQueueCapacity(queueCapacity);
//            // 队列满，线程被拒绝执行策略
//            pool.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
//            return pool;
//        }


    }
}
