CREATE OR REPLACE FUNCTION fn_vital_signs_audit_log()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
v_user text;
v_reopen_session_raw text;
v_reopen_session_id bigint;
BEGIN
  v_user := current_setting('app.user', true);
  v_reopen_session_raw := current_setting('app.reopen_session_id', true);

  IF v_reopen_session_raw IS NULL OR btrim(v_reopen_session_raw) = '' THEN
    v_reopen_session_id := NULL;
  ELSIF v_reopen_session_raw ~ '^[0-9]+$' THEN
    v_reopen_session_id := v_reopen_session_raw::bigint;
  ELSE
    v_reopen_session_id := NULL;
  END IF;

  IF (TG_OP = 'INSERT') THEN
    INSERT INTO vital_signs_log(
      vital_signs_id,
      action,
      created_by,
      created_date,
      last_modified_by,
      last_modified_date,
      payload,
      reopen_session_id
    )
    VALUES (
      NEW.id,
      'INSERT',
      COALESCE(NEW.created_by, v_user),
      now(),
      COALESCE(NEW.created_by, v_user),
      now(),
      to_jsonb(NEW),
      v_reopen_session_id
    );
RETURN NEW;

ELSIF (TG_OP = 'UPDATE') THEN
    INSERT INTO vital_signs_log(
      vital_signs_id,
      action,
      created_by,
      created_date,
      last_modified_by,
      last_modified_date,
      payload,
      reopen_session_id
    )
    VALUES (
      NEW.id,
      'UPDATE',
      COALESCE(OLD.created_by, v_user),
      now(),
      COALESCE(NEW.last_modified_by, v_user),
      now(),
      jsonb_build_object(
        'old', to_jsonb(OLD),
        'new', to_jsonb(NEW)
      ),
      v_reopen_session_id
    );
RETURN NEW;

ELSIF (TG_OP = 'DELETE') THEN
    INSERT INTO vital_signs_log(
      vital_signs_id,
      action,
      created_by,
      created_date,
      last_modified_by,
      last_modified_date,
      payload,
      reopen_session_id
    )
    VALUES (
      OLD.id,
      'DELETE',
      COALESCE(v_user, OLD.created_by),
      now(),
      v_user,
      now(),
      to_jsonb(OLD),
      v_reopen_session_id
    );
RETURN OLD;
END IF;

RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS trg_vital_signs_audit_log ON vital_signs;
CREATE TRIGGER trg_vital_signs_audit_log
  AFTER INSERT OR UPDATE OR DELETE ON vital_signs
  FOR EACH ROW
  EXECUTE FUNCTION fn_vital_signs_audit_log();
