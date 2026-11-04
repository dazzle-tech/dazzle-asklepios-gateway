CREATE OR REPLACE FUNCTION public.trg_patient_encounter_assessment_field_audit_fn()
RETURNS trigger AS $$
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

  -- INSERT
  IF TG_OP = 'INSERT' THEN

    -- Example:
    IF NEW.assessment IS NOT NULL THEN
      INSERT INTO public.patient_encounter_assessment_field_audit (
        patient_encounter_assessment_id,
        field_name,
        operation_type,
        old_value,
        new_value,
        log_date,
        log_by,
        reopen_session_id
      ) VALUES (
        NEW.id,
        'assessment',
        'INSERT',
        NULL,
        NEW.assessment,
        now(),
        COALESCE(NEW.created_by, v_user),
        v_reopen_session_id
      );
END IF;

RETURN NEW;
END IF;


  -- UPDATE
  IF TG_OP = 'UPDATE' THEN

    IF OLD.assessment IS DISTINCT FROM NEW.assessment THEN
      INSERT INTO public.patient_encounter_assessment_field_audit (
        patient_encounter_assessment_id,
        field_name,
        operation_type,
        old_value,
        new_value,
        log_date,
        log_by,
        reopen_session_id
      ) VALUES (
        NEW.id,
        'assessment',
        'UPDATE',
        OLD.assessment,
        NEW.assessment,
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by, v_user),
        v_reopen_session_id
      );
END IF;

RETURN NEW;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;


DROP TRIGGER IF EXISTS trg_patient_encounter_assessment_field_audit
  ON public.encounter_assessments;


CREATE TRIGGER trg_patient_encounter_assessment_field_audit
  AFTER INSERT OR UPDATE
                    ON public.encounter_assessments
                    FOR EACH ROW
                    EXECUTE FUNCTION public.trg_patient_encounter_assessment_field_audit_fn();
