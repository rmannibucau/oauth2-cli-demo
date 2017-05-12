package com.github.rmannibucau.oauth2;

import org.tomitribe.crest.api.Command;

@Command("exit")
public class ExitCommand {
    @Command
    public static void normal() {
        throw new ExitException();
    }

    public static class ExitException extends RuntimeException {
    }
}
