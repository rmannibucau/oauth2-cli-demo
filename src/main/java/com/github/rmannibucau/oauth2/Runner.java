package com.github.rmannibucau.oauth2;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.UserDatabase;
import org.apache.catalina.realm.MessageDigestCredentialHandler;
import org.apache.catalina.realm.UserDatabaseRealm;
import org.apache.catalina.users.MemoryUserDatabase;
import org.apache.meecrowave.Meecrowave;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.tomitribe.crest.Main;
import org.tomitribe.crest.environments.Environment;
import org.tomitribe.crest.environments.SystemEnvironment;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Runner {
    public static void main(final String[] args) throws IOException {
        final UserDatabase users = new MemoryUserDatabase();
        try (final Meecrowave meecrowave = new Meecrowave(new Meecrowave.Builder()
                .realm(new UserDatabaseRealm() {
                    {
                        database = users;
                    }

                    @Override
                    protected void startInternal() throws LifecycleException {
                        if (getCredentialHandler() == null) {
                            setCredentialHandler(new MessageDigestCredentialHandler());
                        }
                        setState(LifecycleState.STARTING);
                    }
                })
                .property("oauth2-client-force", "true"))) {
            // start the server
            meecrowave.bake();

            // start the cli
            final Main cli = new Main(ClientCommands.class, ExitCommand.ExitException.class);
            Terminal terminal = TerminalBuilder.builder().build();
            final LineReader lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();
            Environment.ENVIRONMENT_THREAD_LOCAL.set(new SystemEnvironment() {
                private final PrintStream out = new PrintStream(terminal.output());

                @Override
                public PrintStream getOutput() {
                    return out;
                }

                @Override
                public InputStream getInput() {
                    return terminal.input();
                }

                @Override
                public <T> T findService(final Class<T> type) {
                    if (UserDatabase.class == type) {
                        return type.cast(users);
                    }
                    final BeanManager bm = CDI.current().getBeanManager();
                    return type.cast(bm.getReference(bm.resolve(bm.getBeans(type)), type, bm.createCreationalContext(null)));
                }
            });
            String line;
            while ((line = lineReader.readLine("> ")) != null) {
                try {
                    final Collection<String> params = parse(line);
                    terminal.output().write(String.valueOf(cli.exec(params.toArray(new String[params.size()]))).getBytes(StandardCharsets.UTF_8));
                } catch (final ExitCommand.ExitException e) {
                    break;
                } catch (final Exception e) {
                    e.printStackTrace(); // show the error
                }
            }
        }
    }

    private static Collection<String> parse(String input) { // copied from https://github.com/ncsa/security-lib/blob/master/ncsa-security-common/ncsa-security-util/src/main/java/edu/uiuc/ncsa/security/util/cli/CommandLineTokenizer.java
        boolean isQuotePending = false;
        String cl2 = input.trim();
        List<String> outV = new ArrayList<>();
        char[] c = cl2.toCharArray();
        StringBuffer currentArg = new StringBuffer();
        for (final char current : c) {
            if (isQuotePending) {
                // look for the next quote
                if (current == '"') {
                    // close off the current arg, start on the next.
                    outV.add(currentArg.toString());
                    currentArg = new StringBuffer();
                    isQuotePending = false;
                } else {
                    currentArg.append(current);
                }
            } else {
                // so no quote is pending.
                if (current == ' ' || current == '"') {
                    // we have no quote and have a blank, so tokenize on that
                    outV.add(currentArg.toString());
                    currentArg = new StringBuffer();
                    isQuotePending = (current == '"');
                } else {
                    currentArg.append(current);
                }
            }
        } //end i - for
        outV.add(currentArg.toString());
        if (outV.size() > 0) {
            String temp = outV.get(0);
            temp = temp.toLowerCase();
            outV.set(0, temp);
            // Now we loop through everything else and get rid of extra blank
            // or empty tokens. These can occur when the user enters multiple
            // blanks between commands.
            for (int j = 0; j < outV.size(); j++) {
                temp = (String) outV.get(j);
                if (temp == null || temp.length() == 0) {
                    // remove it, adjust indices.
                    outV.remove(j);
                    j = j - 1;
                }
            } //end other j - for
        }
        isQuotePending = false; // just to be sure.
        return outV;
    }
}
