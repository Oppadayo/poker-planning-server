CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users (optional account)
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Rooms / Sessions
CREATE TABLE rooms (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(100) NOT NULL,
    code             VARCHAR(10)  NOT NULL,
    creator_user_id  UUID         REFERENCES users(id) ON DELETE SET NULL,
    creator_guest_id VARCHAR(36),
    deck_type        VARCHAR(30)  NOT NULL DEFAULT 'FIBONACCI',
    allow_observers  BOOLEAN      NOT NULL DEFAULT TRUE,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    current_story_id UUID,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Participants
CREATE TABLE participants (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id      UUID         NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id      UUID         REFERENCES users(id) ON DELETE CASCADE,
    guest_id     VARCHAR(36),
    role         VARCHAR(20)  NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    online       BOOLEAN      NOT NULL DEFAULT FALSE,
    joined_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT participants_user_or_guest CHECK (
        (user_id IS NOT NULL AND guest_id IS NULL) OR
        (user_id IS NULL AND guest_id IS NOT NULL)
    )
);

-- Stories
CREATE TABLE stories (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id        UUID         NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    title          VARCHAR(200) NOT NULL,
    description    TEXT,
    external_ref   VARCHAR(500),
    order_index    INT          NOT NULL DEFAULT 0,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    final_estimate VARCHAR(20),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Rounds
CREATE TABLE rounds (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id       UUID        NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    story_id      UUID        NOT NULL REFERENCES stories(id) ON DELETE CASCADE,
    status        VARCHAR(20) NOT NULL DEFAULT 'VOTING',
    started_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revealed_at   TIMESTAMPTZ,
    finalized_at  TIMESTAMPTZ
);

-- Votes
CREATE TABLE votes (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    round_id       UUID        NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
    participant_id UUID        NOT NULL REFERENCES participants(id) ON DELETE CASCADE,
    value          VARCHAR(20) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Invites
CREATE TABLE invites (
    id                     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash             VARCHAR(64) NOT NULL,
    room_id                UUID        NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    role                   VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANT',
    expires_at             TIMESTAMPTZ,
    max_uses               INT,
    uses                   INT         NOT NULL DEFAULT 0,
    revoked_at             TIMESTAMPTZ,
    creator_participant_id UUID        NOT NULL REFERENCES participants(id) ON DELETE CASCADE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
