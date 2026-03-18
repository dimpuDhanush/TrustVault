import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class UiAsync {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(
            Math.max(2, Math.min(6, Runtime.getRuntime().availableProcessors())),
            new TrustVaultThreadFactory());

    private UiAsync() {
    }

    public static <T> void run(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        }, EXECUTOR).whenComplete((result, throwable) -> Platform.runLater(() -> {
            if (throwable == null) {
                onSuccess.accept(result);
                return;
            }

            Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                    ? throwable.getCause()
                    : throwable;
            AppLogger.get(UiAsync.class).log(Level.SEVERE, "Background task failed.", cause);
            onFailure.accept(cause);
        }));
    }

    public static void shutdown() {
        EXECUTOR.shutdown();
    }

    private static class TrustVaultThreadFactory implements ThreadFactory {
        private int sequence;

        @Override
        public synchronized Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "trustvault-worker-" + (++sequence));
            thread.setDaemon(true);
            return thread;
        }
    }
}
