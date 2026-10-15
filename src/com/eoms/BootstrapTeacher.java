package com.eoms;

import com.eoms.config.DatabaseConnection;
import com.eoms.repository.TeacherRepository;
import com.eoms.service.PasswordHashes;
import java.io.Console;

/** Explicit, one-time local administrator setup; never creates accounts on app startup. */
public final class BootstrapTeacher {
    public static void main(String[] args) throws Exception {
        Console console = System.console();
        if (console == null) throw new IllegalStateException("Run from an interactive console");
        String username = console.readLine("Teacher username: ").trim();
        if (username.isBlank()) throw new IllegalArgumentException("Username required");
        char[] password = console.readPassword("Teacher password (at least 8 characters): ");
        try {
            new TeacherRepository(new DatabaseConnection()).bootstrap(username,
                    PasswordHashes.hash(new String(password)));
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
        System.out.println("Teacher account created.");
    }
}
