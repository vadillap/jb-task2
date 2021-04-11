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

    public MatcherManager(String regex, long timeout) {
        pattern = Pattern.compile(regex);
        this.timeout = timeout;
    }

    public boolean matchStrings(List<String> strings) {
        List<CompletableFuture<Boolean>> taskFutures = new ArrayList<>();

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

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                taskFutures.toArray(new CompletableFuture[0])
        );

        CompletableFuture<List<Boolean>> allTaskFuture = allFutures
                .thenApply(v ->
                        taskFutures.stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList())
                );

        CompletableFuture<Boolean> resultFuture = allTaskFuture
                .thenApply(tasks -> tasks.stream().allMatch(Boolean::valueOf));

        try {
            return resultFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
    }
}
