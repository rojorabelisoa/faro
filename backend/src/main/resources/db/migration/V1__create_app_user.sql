CREATE TABLE app_user (
    id            UUID PRIMARY KEY,
    subject       VARCHAR(255) NOT NULL UNIQUE,
    email         VARCHAR(320),
    display_name  VARCHAR(255),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE NOT NULL
);
