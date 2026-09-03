CREATE OR REPLACE FUNCTION public.trg_patient_encounter_field_audit_fn()
RETURNS trigger AS $$
DECLARE
v_user text;
BEGIN
  v_user := current_setting('app.user', true);

  -- INSERT
  IF TG_OP = 'INSERT' THEN

    IF NEW.chief_complaint IS NOT NULL THEN
      INSERT INTO public.patient_encounter_field_audit (
        patient_encounter_id,
        field_name,
        operation_type,
        old_value,
        new_value,
        log_date,
        log_by
      ) VALUES (
        NEW.id,
        'chiefComplaint',
        'INSERT',
        NULL,
        NEW.chief_complaint,
        now(),
        COALESCE(NEW.created_by, v_user)
      );
END IF;

    IF NEW.history_of_present_illness IS NOT NULL THEN
      INSERT INTO public.patient_encounter_field_audit (
        patient_encounter_id,
        field_name,
        operation_type,
        old_value,
        new_value,
        log_date,
        log_by
      ) VALUES (
        NEW.id,
        'historyOfPresentIllness',
        'INSERT',
        NULL,
        NEW.history_of_present_illness,
        now(),
        COALESCE(NEW.created_by, v_user)
      );
END IF;

    IF NEW.physical_examination_summery IS NOT NULL THEN
      INSERT INTO public.patient_encounter_field_audit (
        patient_encounter_id,
        field_name,
        operation_type,
        old_value,
        new_value,
        log_date,
        log_by
      ) VALUES (
        NEW.id,
        'physicalExaminationSummery',
        'INSERT',
        NULL,
        NEW.physical_examination_summery,
        now(),
        COALESCE(NEW.created_by, v_user)
      );
END IF;

RETURN NEW;
END IF;

  -- UPDATE
  IF TG_OP = 'UPDATE' THEN

    IF OLD.chief_complaint IS DISTINCT FROM NEW.chief_complaint THEN
      INSERT INTO public.patient_encounter_field_audit (
        patient_encounter_id,
        field_name,
        operation_type,
        old_value,
        new_value,
        log_date,
        log_by
      ) VALUES (
        NEW.id,
        'chiefComplaint',
        'UPDATE',
        OLD.chief_complaint,
        NEW.chief_complaint,
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by, v_user)
      );
END IF;

    IF OLD.history_of_present_illness IS DISTINCT FROM NEW.history_of_present_illness THEN
      INSERT INTO public.patient_encounter_field_audit (
        patient_encounter_id,
        field_name,
        operation_type,
        old_value,
        new_value,
        log_date,
        log_by
      ) VALUES (
        NEW.id,
        'historyOfPresentIllness',
        'UPDATE',
        OLD.history_of_present_illness,
        NEW.history_of_present_illness,
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by, v_user)
      );
END IF;

    IF OLD.physical_examination_summery IS DISTINCT FROM NEW.physical_examination_summery THEN
      INSERT INTO public.patient_encounter_field_audit (
        patient_encounter_id,
        field_name,
        operation_type,
        old_value,
        new_value,
        log_date,
        log_by
      ) VALUES (
        NEW.id,
        'physicalExaminationSummery',
        'UPDATE',
        OLD.physical_examination_summery,
        NEW.physical_examination_summery,
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by, v_user)
      );
END IF;

RETURN NEW;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_patient_encounter_field_audit
  ON public.patient_encounters;

CREATE TRIGGER trg_patient_encounter_field_audit
  AFTER INSERT OR UPDATE
                    ON public.patient_encounters
                    FOR EACH ROW
                    EXECUTE FUNCTION public.trg_patient_encounter_field_audit_fn();
