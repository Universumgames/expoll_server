-- Rename old tables
RENAME TABLE authenticator TO authenticator_old;
RENAME TABLE challenge TO challenge_old;
RENAME TABLE vote TO vote_old;
RENAME TABLE user_polls_poll TO user_polls_poll_old;
RENAME TABLE session TO session_old;
RENAME TABLE poll_user_note TO poll_user_note_old;
/* RENAME TABLE poll_option_string TO poll_option_string_old;
RENAME TABLE poll_option_date_time TO poll_option_date_time_old;
RENAME TABLE poll_option_date TO poll_option_date_old; */
RENAME TABLE poll TO poll_old;
RENAME TABLE user TO user_old;


-- create new empty tables
create table user as select * from user_old where 0=1;
create table poll as select * from poll_old where 0=1;
create table vote as select * from vote_old where 0=1;
create table user_polls_poll as select * from user_polls_poll_old where 0=1;
create table session as select * from session_old where 0=1;
create table poll_user_note as select * from poll_user_note_old where 0=1;
/* create table poll_option_string as select * from poll_option_string_old where 0=1;
create table poll_option_date_time as select * from poll_option_date_time_old where 0=1;
create table poll_option_date as select * from poll_option_date_old where 0=1; */
create table challenge as select * from challenge_old where 0=1;
create table authenticator as select * from authenticator_old where 0=1;

-- create temp uid table
-- fcc61aab-18a2-49b8-bd48-2ee386b4ee86
create table if not exists uid_pair (
    uid int(11) primary key,
    uid_new varchar(36)
);

-- Tranform table columns
alter table user modify column id varchar(36) not null primary key;
alter table poll modify column adminId varchar(36) not null;
alter table vote modify column userId varchar(36);
alter table user_polls_poll modify column userId varchar(36);
alter table session modify column userId varchar(36);
alter table poll_user_note modify column userId varchar(36);
alter table challenge modify column userId varchar(36);
alter table authenticator modify column userId varchar(36);
alter table session modify column userId varchar(36);

-- modify user id#
DELIMITER //

/* DECLARE @old_id int(11);
DECLARE @new_id varchar(36); */

-- user loop
FOR user in (SELECT * FROM user_old)
DO
    SET @old_id := user.id;
    SET @new_id := UUID();
    -- insert pair for later use
    INSERT INTO uid_pair (uid, uid_new) VALUES (@old_id, @new_id);
    -- insert user with new id
    INSERT INTO user(id, username, firstName, lastName, mail, active, admin) VALUES (@new_id, user.username, user.firstName, user.lastName, user.mail, user.active, user.admin);
END FOR;

-- update poll adminId
FOR poll in (SELECT * FROM poll_old)
DO
    SET @old_id = poll.adminId;
    SET @new_id = (SELECT uid_new FROM uid_pair WHERE uid = @old_id);
    -- insert poll with new id
    INSERT INTO poll(id, name, created, updated, description, type, maxPerUserVoteCount, adminId, allowsMaybe, allowsEditing) VALUES (poll.id, poll.name, poll.created, poll.updated, poll.description, poll.type, poll.maxPerUserVoteCount, @new_id, poll.allowsMaybe, poll.allowsEditing);
END FOR;

-- update vote userId
FOR vote IN (SELECT * FROM vote_old)
DO
    SET @old_id := vote.userId;
    SET @new_id := (SELECT uid_new FROM uid_pair WHERE uid = @old_id);
    -- insert vote with new id
    INSERT INTO vote(id, optionID, userId, pollId, votedFor) VALUES (vote.id, vote.optionID, @new_id, vote.pollId, vote.votedFor);
END FOR;

-- update user_polls_poll userId
FOR user_polls_poll IN (SELECT * FROM user_polls_poll_old)
DO
    SET @old_id := user_polls_poll.userId;
    SET @new_id := (SELECT uid_new FROM uid_pair WHERE uid = @old_id);
    -- insert user_polls_poll with new id
    INSERT INTO user_polls_poll(userId, pollId) VALUES (@new_id, user_polls_poll.pollId);
END FOR;

-- update session userId
FOR session IN (SELECT * FROM session_old)
DO
    SET @old_id = session.userId;
    SET @new_id = (SELECT uid_new FROM uid_pair WHERE uid = @old_id);
    -- insert session with new id
    INSERT INTO session(loginKey, expiration, userId) VALUES (session.loginKey, session.expiration, @new_id);
END FOR;

-- update poll_user_note userId
FOR poll_user_note IN (SELECT * FROM poll_user_note_old)
DO
    SET @old_id := poll_user_note.userId;
    SET @new_id := (SELECT uid_new FROM uid_pair WHERE uid = @old_id);
    -- insert poll_user_note with new id
    INSERT INTO poll_user_note(id, note, userId, pollId) VALUES (poll_user_note.id, poll_user_note.note, @new_id, poll_user_note.pollId);
END FOR;

-- update challenge userId
FOR challenge IN (SELECT * FROM challenge_old)
DO
    SET @old_id = challenge.userId;
    SET @new_id = (SELECT uid_new FROM uid_pair WHERE uid = @old_id);
    -- insert challenge with new id
    INSERT INTO challenge(id, challenge, userId) VALUES (challenge.id, challenge.challenge, @new_id);
END FOR;

-- update authenticator userId
FOR authenticator IN (SELECT * FROM authenticator_old)
DO
    SET @old_id := authenticator.userId;
    SET @new_id := (SELECT uid_new FROM uid_pair WHERE uid = @old_id);
    -- insert authenticator with new id
    INSERT INTO authenticator(credentialID, credentialPublicKey, counter, transports, name, initiatorPlatform, created, userId) VALUES (authenticator.credentialID, authenticator.credentialPublicKey, authenticator.counter, authenticator.transports, authenticator.name, authenticator.initiatorPlatform, authenticator.created, @new_id);
END FOR;



-- drop old tables
SET FOREIGN_KEY_CHECKS=0;
drop table if exists uid_pair;
drop table authenticator_old;
drop table if exists challenge_old;
drop table if exists vote_old;
drop table if exists user_polls_poll_old;
drop table if exists session_old;
drop table if exists poll_user_note_old;
drop table if exists poll_option_string_old;
drop table if exists poll_option_date_time_old;
drop table if exists poll_option_date_old;
drop table if exists poll_old;
drop table if exists user_old;
SET FOREIGN_KEY_CHECKS=1;

//
DELIMITER ;
