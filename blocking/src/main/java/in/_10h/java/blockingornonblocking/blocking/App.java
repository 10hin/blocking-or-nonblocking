package in._10h.java.blockingornonblocking.blocking;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Hello world!
 *
 */
public class App {
    private static final int POOL_SIZE = 4;
    private static final int THREAD_POOL_SHUTDOWN_AWAIT_TIME_SECONDS = 60;
    private static final String ANSWER = "Number of this is your Answer!";
    private static final Pattern LINE_SEPARATOR_REGEXP = Pattern.compile("\\r?\\n");
    private static final Pattern QUERY_SEPARATOR_REGEXP = Pattern.compile("&");
    public static void main(final String[] args) throws Throwable {
        Optional<ExecutorService> threadPoolHolder = Optional.empty();
        try {
            threadPoolHolder = Optional.of(new ThreadPoolExecutor(0, POOL_SIZE, 60L, TimeUnit.SECONDS, new LinkedTransferQueue<>()));
            final var threadPool = threadPoolHolder.get();
            final var inputPath = parseArguments(args);

            final List<Integer> expectedAnswers = new ArrayList<>();
            final List<Future<Integer>> results = new ArrayList<>();
            try (final var inputs = Files.lines(inputPath)) {
                final ThreadLocal<HttpClient> clients = ThreadLocal.withInitial(() -> HttpClient.newHttpClient());
                inputs.forEach((inputLine) -> {
                    final var uri = URI.create(inputLine);
                    // ブロッキングAPIで計算
                    var result = threadPool.submit(() -> {

                        final var req = HttpRequest.newBuilder(uri)
                                .version(HttpClient.Version.HTTP_2)
                                .GET()
                                .build();

                        final var resp = clients.get().send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                        var respLines = LINE_SPLITTING_REGEXP.split(resp.body());

                        for (int lineNumber = 1; lineNumber < respLines.length; lineNumber++) {
                            final var respLine = respLines[lineNumber];
                            if (respLine.equals(ANSWER)) {
                                return lineNumber;
                            }
                        }

                        throw new InternalError("answer line not found in response");

                    });
                    results.add(result);

                    // 答え合わせ用
                    final var QUERY_SEPARATOR_REGEXP.split(uri.getQuery());
                });

            }



        } finally {
            threadPoolHolder.ifPresent(App::shutdownThreadPool);
        }

    }

    private static Path parseArguments(final String[] args) {
        return Paths.get(args[0]);
    }

    private static void shutdownThreadPool(final ExecutorService threadPool) {
        Objects.requireNonNull(threadPool, "threadPool must not be null");

        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(THREAD_POOL_SHUTDOWN_AWAIT_TIME_SECONDS / 2, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
                if (!threadPool.awaitTermination(THREAD_POOL_SHUTDOWN_AWAIT_TIME_SECONDS / 2, TimeUnit.SECONDS)) {
                    System.err.println("Failed to shutdown thread pool");
                }
            }
        } catch (InterruptedException ex) {
            System.err.println("Interrupted while awaiting thread pool shutdown");
            // 割り込みステータスを保留する
            Thread.currentThread().interrupt();
        }

    }

}
