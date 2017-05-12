package com.github.rmannibucau.oauth2;

import org.apache.catalina.UserDatabase;
import org.tomitribe.crest.api.Command;
import org.tomitribe.crest.api.Option;

import java.net.URISyntaxException;

@Command("user")
public class UsersCommands {
    @Command
    public static void create(@Option("username") final String user,
                              @Option("password") final String pwd,
                              final UserDatabase database) throws URISyntaxException {
        database.createUser(user, pwd, user);
    }
}
