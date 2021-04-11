package com.company;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class AsyncMatcher {
    public static CompletableFuture<Boolean> matches(String text, Pattern pattern) {
        return CompletableFuture
                .supplyAsync(() -> pattern.matcher(text).matches());
    }
}
