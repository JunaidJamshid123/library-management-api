package com.library.library_management.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Component
public class DatabaseConnectionLogger {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionLogger.class);

    private final DataSource dataSource;

    public DatabaseConnectionLogger(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void verifyDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            log.info("==========================================================");
            log.info("  DATABASE CONNECTED SUCCESSFULLY");
            log.info("  URL      : {}", meta.getURL());
            log.info("  Username : {}", meta.getUserName());
            log.info("  Product  : {} {}", meta.getDatabaseProductName(), meta.getDatabaseProductVersion());
            log.info("  Driver   : {} {}", meta.getDriverName(), meta.getDriverVersion());
            log.info("==========================================================");
        } catch (Exception e) {
            log.error("==========================================================");
            log.error("  DATABASE CONNECTION FAILED");
            log.error("  Reason: {}", e.getMessage());
            log.error("==========================================================", e);
        }
    }
}
