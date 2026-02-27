-- Unique constraints
ALTER TABLE users ADD CONSTRAINT uq_users_username UNIQUE (username);
ALTER TABLE users ADD CONSTRAINT uq_users_email    UNIQUE (email);
ALTER TABLE rooms ADD CONSTRAINT uq_rooms_code     UNIQUE (code);
ALTER TABLE invites ADD CONSTRAINT uq_invites_token_hash UNIQUE (token_hash);

-- Partial unique indexes for participants (NULLs are distinct, so we use partial indexes)
CREATE UNIQUE INDEX uq_participant_room_user  ON participants(room_id, user_id)  WHERE user_id  IS NOT NULL;
CREATE UNIQUE INDEX uq_participant_room_guest ON participants(room_id, guest_id) WHERE guest_id IS NOT NULL;

-- Unique vote per round per participant
ALTER TABLE votes ADD CONSTRAINT uq_vote_round_participant UNIQUE (round_id, participant_id);

-- Performance indexes
CREATE INDEX idx_participants_room_id  ON participants(room_id);
CREATE INDEX idx_participants_user_id  ON participants(user_id)  WHERE user_id  IS NOT NULL;
CREATE INDEX idx_participants_guest_id ON participants(guest_id) WHERE guest_id IS NOT NULL;
CREATE INDEX idx_stories_room_id       ON stories(room_id);
CREATE INDEX idx_stories_room_order    ON stories(room_id, order_index);
CREATE INDEX idx_rounds_room_id        ON rounds(room_id);
CREATE INDEX idx_rounds_story_id       ON rounds(story_id);
CREATE INDEX idx_votes_round_id        ON votes(round_id);
CREATE INDEX idx_invites_room_id       ON invites(room_id);
