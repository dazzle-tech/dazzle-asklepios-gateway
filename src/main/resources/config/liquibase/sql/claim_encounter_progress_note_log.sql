CREATE OR REPLACE FUNCTION public.trg_claim_encounter_progress_note_log_fn()
RETURNS trigger AS $$
BEGIN

  IF TG_OP = 'INSERT' THEN

    INSERT INTO public.claim_encounter_progress_note_logs (
      claim_encounter_progress_note_id,
      operation_type,
      log_date,
      log_by,
      note_text,
      cancelled_by,
      cancelled_date,
      cancellation_reason,
      created_by,
      created_date
    )
    VALUES (
      NEW.id,
      'INSERT',
      now(),
      COALESCE(NEW.last_modified_by, NEW.created_by),
      NEW.note_text,
      NEW.cancelled_by,
      NEW.cancelled_date,
      NEW.cancellation_reason,
      NEW.created_by,
      NEW.created_date
    );

RETURN NEW;
END IF;

  IF TG_OP = 'UPDATE' THEN

    IF ROW(OLD.*) IS DISTINCT FROM ROW(NEW.*) THEN

      INSERT INTO public.claim_encounter_progress_note_logs (
        claim_encounter_progress_note_id,
        operation_type,
        log_date,
        log_by,
        note_text,
        cancelled_by,
        cancelled_date,
        cancellation_reason,
        created_by,
        created_date
      )
      VALUES (
        NEW.id,
        'UPDATE',
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by),
        NEW.note_text,
        NEW.cancelled_by,
        NEW.cancelled_date,
        NEW.cancellation_reason,
        NEW.created_by,
        NEW.created_date
      );

END IF;

RETURN NEW;
END IF;

RETURN NEW;
END;
$$
LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_claim_encounter_progress_note_log
ON public.claim_encounter_progress_notes;

CREATE TRIGGER trg_claim_encounter_progress_note_log
  AFTER INSERT OR UPDATE
                    ON public.claim_encounter_progress_notes
                    FOR EACH ROW
                    EXECUTE FUNCTION public.trg_claim_encounter_progress_note_log_fn();
