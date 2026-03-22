-- config/liquibase/sql/Patient_Warnings_Log.sql

CREATE OR REPLACE FUNCTION public.trg_patient_warnings_log_fn()
RETURNS trigger AS $$
BEGIN
  -- INSERT: log full row snapshot.
  IF TG_OP = 'INSERT' THEN
    INSERT INTO public.patient_warnings_log (
      warning_id,
      operation_type,
      log_date,
      log_by,
      patient_id,
      encounter_id,
      warning_type,
      warning,
      severity,
      onset_date_undefined,
      onset_date,
      by_patient,
      source_of_information,
      note,
      action_taken,
      status,
      resolved_by,
      resolved_date,
      cancelled_by,
      cancelled_date,
      cancellation_reason,
      created_by,
      created_date,
      last_modified_by,
      last_modified_date
    ) VALUES (
      NEW.id,
      'INSERT',
      now(),
      COALESCE(NEW.last_modified_by, NEW.created_by),
      NEW.patient_id,
      NEW.encounter_id,
      NEW.warning_type,
      NEW.warning,
      NEW.severity,
      NEW.onset_date_undefined,
      NEW.onset_date,
      NEW.by_patient,
      NEW.source_of_information,
      NEW.note,
      NEW.action_taken,
      NEW.status,
      NEW.resolved_by,
      NEW.resolved_date,
      NEW.cancelled_by,
      NEW.cancelled_date,
      NEW.cancellation_reason,
      NEW.created_by,
      NEW.created_date,
      NEW.last_modified_by,
      NEW.last_modified_date
    );

    RETURN NEW;
  END IF;

  -- UPDATE: log full row snapshot only when any field changed.
  IF TG_OP = 'UPDATE' THEN
    IF ROW(OLD.*) IS DISTINCT FROM ROW(NEW.*) THEN
      INSERT INTO public.patient_warnings_log (
        warning_id,
        operation_type,
        log_date,
        log_by,
        patient_id,
        encounter_id,
        warning_type,
        warning,
        severity,
        onset_date_undefined,
        onset_date,
        by_patient,
        source_of_information,
        note,
        action_taken,
        status,
        resolved_by,
        resolved_date,
        cancelled_by,
        cancelled_date,
        cancellation_reason,
        created_by,
        created_date,
        last_modified_by,
        last_modified_date
      ) VALUES (
        NEW.id,
        'UPDATE',
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by),
        NEW.patient_id,
        NEW.encounter_id,
        NEW.warning_type,
        NEW.warning,
        NEW.severity,
        NEW.onset_date_undefined,
        NEW.onset_date,
        NEW.by_patient,
        NEW.source_of_information,
        NEW.note,
        NEW.action_taken,
        NEW.status,
        NEW.resolved_by,
        NEW.resolved_date,
        NEW.cancelled_by,
        NEW.cancelled_date,
        NEW.cancellation_reason,
        NEW.created_by,
        NEW.created_date,
        NEW.last_modified_by,
        NEW.last_modified_date
      );
    END IF;

    RETURN NEW;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_patient_warnings_log ON public.patient_warnings;

CREATE TRIGGER trg_patient_warnings_log
  AFTER INSERT OR UPDATE
  ON public.patient_warnings
  FOR EACH ROW
  EXECUTE FUNCTION public.trg_patient_warnings_log_fn();
