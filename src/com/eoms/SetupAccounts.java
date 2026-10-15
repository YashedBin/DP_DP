package com.eoms;

import com.eoms.config.DatabaseConnection;
import com.eoms.repository.TeacherRepository;
import com.eoms.repository.StudentRepository;
import com.eoms.service.PasswordHashes;
import java.io.Console;

/** Explicit, one-time local administrator setup; never creates accounts on app startup. */
public final class SetupAccounts {
    public static void main(String[] args) throws Exception {
        Console console = System.console();
        if (console == null) throw new IllegalStateException("Run from an interactive console");
        String username = null;
        long studentId = -1;
        if (args.length == 0 || (args.length == 2 && "--teacher".equals(args[0]))) {
            username = args.length == 0 ? console.readLine("New teacher username: ").trim() : args[1];
            if (username.isBlank()) throw new IllegalArgumentException("Username required");
        } else if (args.length == 2 && "--student".equals(args[0])) {
            studentId = Long.parseLong(args[1]);
        } else {
            throw new IllegalArgumentException("Usage: SetupAccounts [--teacher USERNAME | --student ID]");
        }
        char[] password = console.readPassword("New password (at least 8 characters): ");
        try {
            String hash = PasswordHashes.hash(new String(password));
            DatabaseConnection database = new DatabaseConnection();
            if (studentId > 0) new StudentRepository(database).setPassword(studentId, hash);
            else if (args.length == 2) new TeacherRepository(database).setPassword(username, hash);
            else new TeacherRepository(database).bootstrap(username, hash);
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
        System.out.println("Account password configured. Verify identity before sharing credentials.");
    }
}
