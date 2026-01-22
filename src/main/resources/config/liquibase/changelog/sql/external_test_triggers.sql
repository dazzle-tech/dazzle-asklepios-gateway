-- config/liquibase/changelog/sql/external_test_triggers.sql

CREATE OR REPLACE FUNCTION fn_external_test_set_defaults()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
v_user text;
BEGIN
  v_user := current_setting('app.user', true);

  IF NEW.created_date IS NULL THEN
    NEW.created_date := now();
END IF;

  IF NEW.created_by IS NULL THEN
    NEW.created_by := v_user;
END IF;

RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION fn_external_test_audit_log()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
v_user text;
BEGIN
  v_user := current_setting('app.user', true);

  IF (TG_OP = 'INSERT') THEN
    INSERT INTO external_test_log(
      external_test_id,
      test_id,
      action,
      created_by,
      created_date,
      last_modified_by,
      last_modified_date,
      payload
    )
    VALUES (
      NEW.id,
      NEW.test_id,
      'INSERT',
      COALESCE(NEW.created_by, v_user),
      now(),
      COALESCE(NEW.created_by, v_user),
      now(),
      to_jsonb(NEW)
    );
RETURN NEW;

ELSIF (TG_OP = 'DELETE') THEN
    INSERT INTO external_test_log(
      external_test_id,
      test_id,
      action,
      created_by,
      created_date,
      last_modified_by,
      last_modified_date,
      payload
    )
    VALUES (
      OLD.id,
      OLD.test_id,
      'DELETE',
      COALESCE(v_user, OLD.created_by),
      now(),
      v_user,
      now(),
      to_jsonb(OLD)
    );
RETURN OLD;
END IF;

RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS trg_external_test_set_defaults ON external_test;
CREATE TRIGGER trg_external_test_set_defaults
  BEFORE INSERT ON external_test
  FOR EACH ROW
  EXECUTE FUNCTION fn_external_test_set_defaults();

DROP TRIGGER IF EXISTS trg_external_test_audit_log ON external_test;
CREATE TRIGGER trg_external_test_audit_log
  AFTER INSERT OR DELETE ON external_test
FOR EACH ROW
EXECUTE FUNCTION fn_external_test_audit_log();
