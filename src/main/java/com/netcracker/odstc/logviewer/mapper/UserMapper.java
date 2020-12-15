package com.netcracker.odstc.logviewer.mapper;

import com.netcracker.odstc.logviewer.models.User;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserMapper implements RowMapper<User> {

    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User(rs.getBigDecimal("object_id").toBigInteger());
        return user;
    }
}