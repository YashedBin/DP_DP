package com.eoms.patterns.observer;

import java.sql.Connection;
import java.sql.SQLException;

public interface ExamEventListener {
    void onEvent(ExamEvent event, Connection transaction) throws SQLException;
}
