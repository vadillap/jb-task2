package com.company;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class MatcherManager {
    private final Pattern pattern;
    private final long timeout;

    // билдим паттерн заранее, чтобы избежать повторяющихся операций
    // если регулярка неверная, то исключении также вылетит здесь
    public MatcherManager(String regex, long timeout) {
        pattern = Pattern.compile(regex);
        this.timeout = timeout;
    }

    public boolean matchStrings(List<String> strings) {
        List<CompletableFuture<Boolean>> taskFutures = new ArrayList<>();

        // поставим на выполнение все задачи с указанным таймаутом
        for (String s : strings) {
            taskFutures.add(
                    AsyncMatcher.matches(s, pattern)
                            .orTimeout(timeout, TimeUnit.SECONDS)
                            .exceptionally(ex -> {
                                if (ex != null) {
                                    System.out.println("Task suspended");
                                }
                                return false;
                            })

            );
        }

        // соберем все таски в одну
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                taskFutures.toArray(new CompletableFuture[0])
        );

        // после выполнения достанем результат
        CompletableFuture<List<Boolean>> allTaskFuture = allFutures
                .thenApply(v ->
                        taskFutures.stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList())
                );

        // убедимся, что все строки удовлетворяют шаблону
        CompletableFuture<Boolean> resultFuture = allTaskFuture
                .thenApply(tasks -> tasks.stream().allMatch(Boolean::valueOf));

        // извлекаем результат из future
        try {
            return resultFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
    }
}
