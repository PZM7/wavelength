package com.wavelength.social;

public final class ConnectionMapper {
    private ConnectionMapper() {}

    public static ConnectionResponse response(Connection connection) {
        return new ConnectionResponse(
                connection.getId(),
                connection.getRequesterId(),
                connection.getReceiverId(),
                connection.getStatus(),
                connection.getCreatedAt(),
                connection.getUpdatedAt());
    }
}
