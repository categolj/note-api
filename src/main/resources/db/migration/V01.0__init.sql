CREATE TABLE IF NOT EXISTS note
(
    note_id    VARCHAR(36) NOT NULL,
    entry_id   INT8        NOT NULL UNIQUE,
    note_url   VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (note_id)
);

CREATE TABLE IF NOT EXISTS reader
(
    reader_id    VARCHAR(36)  NOT NULL,
    email        VARCHAR(255) NOT NULL UNIQUE,
    reader_state VARCHAR(255) NOT NULL    DEFAULT 'DISABLED',
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (reader_id)
);

CREATE TABLE IF NOT EXISTS reader_password
(
    reader_id       VARCHAR(36)              NOT NULL,
    hashed_password VARCHAR(255)             NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (reader_id),
    CONSTRAINT reader_password_fk_on_reader FOREIGN KEY (reader_id) REFERENCES reader (reader_id) ON DELETE CASCADE
);

-- subscription
CREATE TABLE IF NOT EXISTS note_reader
(
    note_id    VARCHAR(36)              NOT NULL,
    reader_id  VARCHAR(36)              NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (note_id, reader_id),
    CONSTRAINT note_reader_fk_on_note FOREIGN KEY (note_id) REFERENCES note (note_id) ON DELETE CASCADE,
    CONSTRAINT note_reader_fk_on_reader FOREIGN KEY (reader_id) REFERENCES reader (reader_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS activation_link
(
    activation_id VARCHAR(36) NOT NULL,
    reader_id     VARCHAR(36) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (activation_id),
    CONSTRAINT activation_link_fk_on_reader FOREIGN KEY (reader_id) REFERENCES reader (reader_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS password_reset
(
    reset_id   VARCHAR(36)              NOT NULL,
    reader_id  VARCHAR(36)              NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (reset_id),
    CONSTRAINT password_reset_fk_on_reader FOREIGN KEY (reader_id) REFERENCES reader (reader_id) ON DELETE CASCADE
);