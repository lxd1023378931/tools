package com.uzak.thread;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.concurrent.*;

/**
 * @Auther: liangxiudou
 * @Date: 2020/3/22 13:59
 * @Description:
 */
public class CountDownActuate {

    private static final ExecutorService EXECUTOR_SERVICE =
            new ThreadPoolExecutor(0, 32, 10, TimeUnit.SECONDS, new SynchronousQueue<>(),
                    Executors.defaultThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());

    private ExecutorService executorService;

    public CountDownActuate() {
        executorService = EXECUTOR_SERVICE;
    }

    public CountDownActuate(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Runnable线程任务
     */
    private List<TkR> tkRS = Lists.newArrayList();

    /**
     * Callable线程任务
     */
    private List<FutureTask> tkCS = Lists.newArrayList();

    public void addTkR(TkR tkR) {
        tkRS.add(tkR);
    }

    public <V> Future<V> addTkC(TkC<V> tkC) {
        FutureTask<V> futureTask = new FutureTask<>(tkC::apply);
        tkCS.add(futureTask);
        return futureTask;
    }

    public void start() throws InterruptedException {
        start(0, null);
    }

    public void start(long timeout, TimeUnit unit) throws InterruptedException {
        if (tkCS.isEmpty() && tkRS.isEmpty()) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(tkRS.size() + tkCS.size());
        tkRS.forEach(tkR -> execute(tkR, countDownLatch));
        tkCS.forEach(tkC -> execute(tkC::run, countDownLatch));
        if (timeout > 0 && unit != null) {
            countDownLatch.await(timeout, unit);
        } else {
            countDownLatch.await();
        }
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }


    private <V> Future<V> submit(TkC<V> task, CountDownLatch countDownLatch) {
        return getExecutorService().submit(() -> task.and(countDownLatch).apply());
    }

    private void execute(TkR task, CountDownLatch countDownLatch) {
        getExecutorService().execute(() -> task.and(countDownLatch).apply());
    }

    @FunctionalInterface
    public interface TkC<R> {
        R apply();

        default TkC<R> and(CountDownLatch countDownLatch) {
            return () -> {
                R r = apply();
                countDownLatch.countDown();
                return r;
            };
        }
    }

    @FunctionalInterface
    public interface TkR {
        void apply();

        default TkR and(CountDownLatch countDownLatch) {
            return () -> {
                apply();
                countDownLatch.countDown();
            };
        }
    }
}
