package com.uzak.thread;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Supplier;

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

    public <V> CustomFuture<V> addTkC(TkC<V> tkC) {
        FutureTask<V> futureTask = new FutureTask<>(tkC::apply);
        tkCS.add(futureTask);
        return new CustomFuture<>(futureTask);
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
        Assert.notNull(executorService, "ExecutorService is null!");
        return executorService;
    }

    private void execute(TkR task, CountDownLatch countDownLatch) {
        getExecutorService().execute(() -> task.and(countDownLatch).apply());
    }

    @AllArgsConstructor
    public static class CustomFuture<V> {
        private Future<V> future;

        public V get(Supplier<V> orElse) {
            try {
                return Optional.ofNullable(future.get()).orElseGet(orElse);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            return orElse.get();
        }
    }

    @FunctionalInterface
    public interface TkC<R> {
        R apply();

        default TkC<R> and(CountDownLatch countDownLatch) {
            return () -> {
                try {
                    R r = apply();
                    return r;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
                return null;
            };
        }
    }

    @FunctionalInterface
    public interface TkR {
        void apply();

        default TkR and(CountDownLatch countDownLatch) {
            return () -> {
                try {
                    apply();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            };
        }
    }
}
