CREATE OR REPLACE FUNCTION public.trg_claim_encounter_diagnosis_log_fn()
RETURNS trigger AS $$
BEGIN

  IF TG_OP = 'INSERT' THEN

    INSERT INTO public.claim_encounter_diagnosis_logs (
      claim_encounter_diagnosis_id,
      operation_type,
      log_date,
      log_by,
      diagnosis_id,
      type,
      suspected,
      major
    )
    VALUES (
      NEW.id,
      'INSERT',
      now(),
      COALESCE(NEW.last_modified_by, NEW.created_by),
      NEW.diagnosis_id,
      NEW.type,
      NEW.suspected,
      NEW.major
    );

RETURN NEW;
END IF;

  IF TG_OP = 'UPDATE' THEN

    IF ROW(OLD.*) IS DISTINCT FROM ROW(NEW.*) THEN

      INSERT INTO public.claim_encounter_diagnosis_logs (
        claim_encounter_diagnosis_id,
        operation_type,
        log_date,
        log_by,
        diagnosis_id,
        type,
        suspected,
        major
      )
      VALUES (
        NEW.id,
        'UPDATE',
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by),
        NEW.diagnosis_id,
        NEW.type,
        NEW.suspected,
        NEW.major
      );

END IF;

RETURN NEW;
END IF;

RETURN NEW;
END;
$$
LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_claim_encounter_diagnosis_log
ON public.claim_encounter_diagnoses;

CREATE TRIGGER trg_claim_encounter_diagnosis_log
  AFTER INSERT OR UPDATE
                    ON public.claim_encounter_diagnoses
                    FOR EACH ROW
                    EXECUTE FUNCTION public.trg_claim_encounter_diagnosis_log_fn();
