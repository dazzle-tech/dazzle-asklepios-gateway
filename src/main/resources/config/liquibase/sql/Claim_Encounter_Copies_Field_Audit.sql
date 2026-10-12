CREATE OR REPLACE FUNCTION public.trg_claim_encounter_copy_field_audit_fn()
RETURNS trigger AS $$
DECLARE
v_user text;
BEGIN
  v_user := current_setting('app.user', true);

  -- UPDATE
  IF TG_OP = 'UPDATE' THEN

    IF OLD.chief_complaint IS DISTINCT FROM NEW.chief_complaint THEN
      INSERT INTO public.claim_encounter_copy_field_audit (
        claim_encounter_copy_id,
        encounter_id,
        field_name,
        operation_type,
        old_value,
        new_value,
        log_date,
        log_by
      ) VALUES (
        NEW.id,
        NEW.encounter_id,
        'chiefComplaint',
        'UPDATE',
        OLD.chief_complaint,
        NEW.chief_complaint,
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by, v_user)
      );
END IF;

    IF OLD.history_of_present_illness IS DISTINCT FROM NEW.history_of_present_illness THEN
      INSERT INTO public.claim_encounter_copy_field_audit (
        claim_encounter_copy_id,
        encounter_id,
        field_name,
        operation_type,
        old_value,
        new_value,
        log_date,
        log_by
      ) VALUES (
        NEW.id,
        NEW.encounter_id,
        'historyOfPresentIllness',
        'UPDATE',
        OLD.history_of_present_illness,
        NEW.history_of_present_illness,
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by, v_user)
      );
END IF;

    IF OLD.physical_examination IS DISTINCT FROM NEW.physical_examination THEN
      INSERT INTO public.claim_encounter_copy_field_audit (
        claim_encounter_copy_id,
        encounter_id,
        field_name,
        operation_type,
        old_value,
        new_value,
        log_date,
        log_by
      ) VALUES (
        NEW.id,
        NEW.encounter_id,
        'physicalExamination',
        'UPDATE',
        OLD.physical_examination,
        NEW.physical_examination,
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by, v_user)
      );
END IF;

    IF OLD.assessment IS DISTINCT FROM NEW.assessment THEN
  INSERT INTO public.claim_encounter_copy_field_audit (
    claim_encounter_copy_id,
    encounter_id,
    field_name,
    operation_type,
    old_value,
    new_value,
    log_date,
    log_by
  ) VALUES (
    NEW.id,
    NEW.encounter_id,
    'assessment',
    'UPDATE',
    OLD.assessment,
    NEW.assessment,
    now(),
    COALESCE(NEW.last_modified_by, NEW.created_by, v_user)
  );
END IF;

    IF OLD.treatment_plan IS DISTINCT FROM NEW.treatment_plan THEN
      INSERT INTO public.claim_encounter_copy_field_audit (
        claim_encounter_copy_id,
        encounter_id,
        field_name,
        operation_type,
        old_value,
        new_value,
        log_date,
        log_by
      ) VALUES (
        NEW.id,
        NEW.encounter_id,
        'treatmentPlan',
        'UPDATE',
        OLD.treatment_plan,
        NEW.treatment_plan,
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by, v_user)
      );
END IF;

    IF OLD.pulse IS DISTINCT FROM NEW.pulse THEN
      INSERT INTO public.claim_encounter_copy_field_audit (
        claim_encounter_copy_id,
        encounter_id,
        field_name,
        operation_type,
        old_value,
        new_value,
        log_date,
        log_by
      ) VALUES (
        NEW.id,
        NEW.encounter_id,
        'pulse',
        'UPDATE',
        OLD.pulse::text,
        NEW.pulse::text,
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by, v_user)
      );
END IF;

    IF OLD.temperature IS DISTINCT FROM NEW.temperature THEN
      INSERT INTO public.claim_encounter_copy_field_audit (
        claim_encounter_copy_id,
        encounter_id,
        field_name,
        operation_type,
        old_value,
        new_value,
        log_date,
        log_by
      ) VALUES (
        NEW.id,
        NEW.encounter_id,
        'temperature',
        'UPDATE',
        OLD.temperature::text,
        NEW.temperature::text,
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by, v_user)
      );
END IF;

    IF OLD.respiratory_rate IS DISTINCT FROM NEW.respiratory_rate THEN
      INSERT INTO public.claim_encounter_copy_field_audit (
        claim_encounter_copy_id,
        encounter_id,
        field_name,
        operation_type,
        old_value,
        new_value,
        log_date,
        log_by
      ) VALUES (
        NEW.id,
        NEW.encounter_id,
        'respiratoryRate',
        'UPDATE',
        OLD.respiratory_rate::text,
        NEW.respiratory_rate::text,
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by, v_user)
      );
END IF;

    IF OLD.oxygen_saturation IS DISTINCT FROM NEW.oxygen_saturation THEN
      INSERT INTO public.claim_encounter_copy_field_audit (
        claim_encounter_copy_id,
        encounter_id,
        field_name,
        operation_type,
        old_value,
        new_value,
        log_date,
        log_by
      ) VALUES (
        NEW.id,
        NEW.encounter_id,
        'oxygenSaturation',
        'UPDATE',
        OLD.oxygen_saturation::text,
        NEW.oxygen_saturation::text,
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by, v_user)
      );
END IF;

    IF OLD.blood_pressure_systolic IS DISTINCT FROM NEW.blood_pressure_systolic THEN
      INSERT INTO public.claim_encounter_copy_field_audit (
        claim_encounter_copy_id,
        encounter_id,
        field_name,
        operation_type,
        old_value,
        new_value,
        log_date,
        log_by
      ) VALUES (
        NEW.id,
        NEW.encounter_id,
        'bloodPressureSystolic',
        'UPDATE',
        OLD.blood_pressure_systolic::text,
        NEW.blood_pressure_systolic::text,
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by, v_user)
      );
END IF;

    IF OLD.blood_pressure_diastolic IS DISTINCT FROM NEW.blood_pressure_diastolic THEN
      INSERT INTO public.claim_encounter_copy_field_audit (
        claim_encounter_copy_id,
        encounter_id,
        field_name,
        operation_type,
        old_value,
        new_value,
        log_date,
        log_by
      ) VALUES (
        NEW.id,
        NEW.encounter_id,
        'bloodPressureDiastolic',
        'UPDATE',
        OLD.blood_pressure_diastolic::text,
        NEW.blood_pressure_diastolic::text,
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by, v_user)
      );
END IF;

    IF OLD.height IS DISTINCT FROM NEW.height THEN
      INSERT INTO public.claim_encounter_copy_field_audit (
        claim_encounter_copy_id,
        encounter_id,
        field_name,
        operation_type,
        old_value,
        new_value,
        log_date,
        log_by
      ) VALUES (
        NEW.id,
        NEW.encounter_id,
        'height',
        'UPDATE',
        OLD.height::text,
        NEW.height::text,
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by, v_user)
      );
END IF;

    IF OLD.weight IS DISTINCT FROM NEW.weight THEN
      INSERT INTO public.claim_encounter_copy_field_audit (
        claim_encounter_copy_id,
        encounter_id,
        field_name,
        operation_type,
        old_value,
        new_value,
        log_date,
        log_by
      ) VALUES (
        NEW.id,
        NEW.encounter_id,
        'weight',
        'UPDATE',
        OLD.weight::text,
        NEW.weight::text,
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by, v_user)
      );
END IF;

RETURN NEW;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_claim_encounter_copy_field_audit
  ON public.claim_encounter_copies;

CREATE TRIGGER trg_claim_encounter_copy_field_audit
  AFTER UPDATE
  ON public.claim_encounter_copies
  FOR EACH ROW
  EXECUTE FUNCTION public.trg_claim_encounter_copy_field_audit_fn();
