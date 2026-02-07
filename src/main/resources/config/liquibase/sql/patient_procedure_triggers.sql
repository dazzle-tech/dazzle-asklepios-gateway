CREATE OR REPLACE FUNCTION fn_patient_procedure_set_defaults()
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

  IF NEW.status IS NULL THEN
    NEW.status := 'REQUESTED';
END IF;

RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION fn_patient_procedure_audit_log()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
v_user text;
BEGIN
  v_user := current_setting('app.user', true);

  IF (TG_OP = 'INSERT') THEN
    INSERT INTO patient_procedure_log(
      procedure_id,
      action,
      created_by,
      created_date,
      last_modified_by,
      last_modified_date,
      payload
    )
    VALUES (
      NEW.id,
      'INSERT',
      COALESCE(NEW.created_by, v_user),
      now(),
      COALESCE(NEW.created_by, v_user),
      now(),
      to_jsonb(NEW)
    );
RETURN NEW;

ELSIF (TG_OP = 'UPDATE') THEN
    INSERT INTO patient_procedure_log(
      procedure_id,
      action,
      created_by,
      created_date,
      last_modified_by,
      last_modified_date,
      payload
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
      )
    );
RETURN NEW;

ELSIF (TG_OP = 'DELETE') THEN
    INSERT INTO patient_procedure_log(
      procedure_id,
      action,
      created_by,
      created_date,
      last_modified_by,
      last_modified_date,
      payload
    )
    VALUES (
      OLD.id,
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

DROP TRIGGER IF EXISTS trg_patient_procedure_set_defaults ON patient_procedure;
CREATE TRIGGER trg_patient_procedure_set_defaults
  BEFORE INSERT ON patient_procedure
  FOR EACH ROW
  EXECUTE FUNCTION fn_patient_procedure_set_defaults();

DROP TRIGGER IF EXISTS trg_patient_procedure_audit_log ON patient_procedure;
CREATE TRIGGER trg_patient_procedure_audit_log
  AFTER INSERT OR UPDATE OR DELETE ON patient_procedure
  FOR EACH ROW
  EXECUTE FUNCTION fn_patient_procedure_audit_log();
