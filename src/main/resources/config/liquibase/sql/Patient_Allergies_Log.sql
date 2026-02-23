-- config/liquibase/sql/Patient_Allergies_Log.sql

CREATE OR REPLACE FUNCTION public.trg_patient_allergies_log_fn()
RETURNS trigger AS $$
BEGIN
  -- INSERT: log full row snapshot.
  IF TG_OP = 'INSERT' THEN
    INSERT INTO public.patient_allergies_log (
      allergy_id,
      operation_type,
      log_date,
      log_by,
      patient_id,
      encounter_id,
      allergen_type,
      allergen_id,
      severity,
      medication_class_id,
      criticality,
      certainty,
      treatment_strategy,
      onset,
      onset_date_undefined,
      onset_date,
      type_of_propensity,
      by_patient,
      source_of_information,
      allergic_reactions,
      note,
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
      NEW.allergen_type,
      NEW.allergen_id,
      NEW.severity,
      NEW.medication_class_id,
      NEW.criticality,
      NEW.certainty,
      NEW.treatment_strategy,
      NEW.onset,
      NEW.onset_date_undefined,
      NEW.onset_date,
      NEW.type_of_propensity,
      NEW.by_patient,
      NEW.source_of_information,
      NEW.allergic_reactions,
      NEW.note,
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
      INSERT INTO public.patient_allergies_log (
        allergy_id,
        operation_type,
        log_date,
        log_by,
        patient_id,
        encounter_id,
        allergen_type,
        allergen_id,
        severity,
        medication_class_id,
        criticality,
        certainty,
        treatment_strategy,
        onset,
        onset_date_undefined,
        onset_date,
        type_of_propensity,
        by_patient,
        source_of_information,
        allergic_reactions,
        note,
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
        NEW.allergen_type,
        NEW.allergen_id,
        NEW.severity,
        NEW.medication_class_id,
        NEW.criticality,
        NEW.certainty,
        NEW.treatment_strategy,
        NEW.onset,
        NEW.onset_date_undefined,
        NEW.onset_date,
        NEW.type_of_propensity,
        NEW.by_patient,
        NEW.source_of_information,
        NEW.allergic_reactions,
        NEW.note,
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

DROP TRIGGER IF EXISTS trg_patient_allergies_log ON public.patient_allergies;

CREATE TRIGGER trg_patient_allergies_log
  AFTER INSERT OR UPDATE
  ON public.patient_allergies
  FOR EACH ROW
  EXECUTE FUNCTION public.trg_patient_allergies_log_fn();

