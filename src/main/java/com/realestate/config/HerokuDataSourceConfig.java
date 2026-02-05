package com.realestate.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Configures DataSource from Heroku DATABASE_URL when present.
 * Converts postgres:// to JDBC format and sets username/password.
 */
@Configuration
@Profile("heroku")
public class HerokuDataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource() throws URISyntaxException, UnsupportedEncodingException {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalStateException("DATABASE_URL environment variable is not set. Required for heroku profile.");
        }
        // Parse postgres://user:password@host:port/dbname (password may contain : or @)
        String clean = databaseUrl.replaceFirst("^postgres://", "http://");
        URI uri = new URI(clean);
        String userInfo = uri.getUserInfo();
        String username = null;
        String password = null;
        if (userInfo != null) {
            int colon = userInfo.indexOf(':');
            username = colon >= 0 ? userInfo.substring(0, colon) : userInfo;
            password = colon >= 0 ? userInfo.substring(colon + 1) : null;
            username = URLDecoder.decode(username, StandardCharsets.UTF_8);
            if (password != null) password = URLDecoder.decode(password, StandardCharsets.UTF_8);
        }
        String path = uri.getPath();
        String dbName = (path != null && path.length() > 1) ? path.substring(1) : "postgres";
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + "/" + dbName;

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setMaximumPoolSize(10);
        return ds;
    }
}
