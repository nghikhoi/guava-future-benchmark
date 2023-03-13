package me.nghikhoi.benchmark;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static com.google.common.util.concurrent.Futures.getDone;

public class LazyTransformFuture<F, T> extends AbstractFuture<T> implements Runnable {

    private final ListenableFuture<F> inputFuture;
    private final Function<F, T> transform;

    public LazyTransformFuture(ListenableFuture<F> future, Function<F, T> transform) {
        this.inputFuture = future;
        this.transform = transform;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return super.get();
    }

    @Override
    public void run() {
        ListenableFuture<? extends F> localInputFuture = inputFuture;
        Function<F, T> localFunction = transform;
        if (isCancelled() | localInputFuture == null | localFunction == null) {
            return;
        }

        if (localInputFuture.isCancelled()) {
            @SuppressWarnings("unchecked")
            boolean unused =
                    setFuture((ListenableFuture<T>) localInputFuture); // Respects cancellation cause setting
            return;
        }

        /*
         * Any of the setException() calls below can fail if the output Future is cancelled between now
         * and then. This means that we're silently swallowing an exception -- maybe even an Error. But
         * this is no worse than what FutureTask does in that situation. Additionally, because the
         * Future was cancelled, its listeners have been run, so its consumers will not hang.
         *
         * Contrast this to the situation we have if setResult() throws, a situation described below.
         */
        F sourceResult;
        try {
            sourceResult = getDone(localInputFuture);
        } catch (CancellationException e) {
            // TODO(user): verify future behavior - unify logic with getFutureValue in AbstractFuture. This
            // code should be unreachable with correctly implemented Futures.
            // Cancel this future and return.
            // At this point, inputFuture is cancelled and outputFuture doesn't exist, so the value of
            // mayInterruptIfRunning is irrelevant.
            cancel(false);
            return;
        } catch (ExecutionException e) {
            // Set the cause of the exception as this future's exception.
            setException(e.getCause());
            return;
        } catch (RuntimeException e) {
            // Bug in inputFuture.get(). Propagate to the output Future so that its consumers don't hang.
            setException(e);
            return;
        } catch (Error e) {
            /*
             * StackOverflowError, OutOfMemoryError (e.g., from allocating ExecutionException), or
             * something. Try to treat it like a RuntimeException. If we overflow the stack again, the
             * resulting Error will propagate upward up to the root call to set().
             */
            setException(e);
            return;
        }

        T transformResult;
        try {
            transformResult = transform.apply(sourceResult);
        } catch (Throwable t) {
            // This exception is irrelevant in this thread, but useful for the client.
            setException(t);
            return;
        } finally {

        }
        set(transformResult);
    }
}
