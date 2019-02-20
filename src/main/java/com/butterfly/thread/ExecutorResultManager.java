package com.butterfly.thread;

import java.util.concurrent.*;

/**
 * Created by adminnistrator on 2019/2/20.
 * 使用ExecutorCompletionService 管理线程池处理任务的返回结果
 */
public class ExecutorResultManager {
    public static void main( String[] args )
    {
//        baseThread();
        executorThread();
    }

    /**
     *  使用并发容器将callable.call() 的返回Future存储起来。
     *  然后使用一个消费者线程去遍历这个并发容器，
     *  调用Future.isDone()去判断各个任务是否处理完毕。
     *  然后再处理响应的业务。
     */
    public static void baseThread(){
        // 队列
        final BlockingQueue<Future<String>> futures = new LinkedBlockingQueue<>();

        // 生产者
        new Thread() {
            @Override
            public void run() {
                ExecutorService pool = Executors.newCachedThreadPool();

                for (int i=0; i< 10; i++) {
                    final int index = i;
                    Future<String> submit = pool.submit(new Callable<String>() {
                        @Override
                        public String call() throws Exception {
                            return "task done" + index;
                        }
                    });
                    try {
                        futures.put(submit);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();



        // 消费者
        new Thread() {
            @Override
            public void run() {
                while(true) {
                    for (Future<String> future : futures) {
                        if(future.isDone()) {
                            // 处理业务
                            // .............
                            try {
                                System.out.println("优秀");
                                System.out.println(future.get());

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }

                        };
                    }
                }
            }
        }.start();
    }

    /**
     * 使用jdk 自带线程池结果管理器：ExecutorCompletionService
     * 它将BlockingQueue 和Executor 封装起来
     */
    public static void executorThread(){
        final ExecutorCompletionService<String> service = new ExecutorCompletionService<String>(
                Executors.newCachedThreadPool());

        // 生产者
        new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    final int index = i;
                    service.submit(new Callable<String>() {
                        @Override
                        public String call() throws Exception {
                            return "task done" + index;
                        }
                    });
                }
            }
        }.start();


        // 消费者
        new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Future<String> take = service.take();
                        // do some thing........
                        try {
                            System.out.println("优秀");
                            System.out.println(take.get());

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
