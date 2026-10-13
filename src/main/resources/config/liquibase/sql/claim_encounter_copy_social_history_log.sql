CREATE OR REPLACE FUNCTION public.trg_claim_encounter_copy_social_history_log_fn()
RETURNS trigger AS $$
BEGIN

  IF TG_OP = 'INSERT' THEN

    INSERT INTO public.claim_encounter_copy_social_history_logs (
      claim_encounter_copy_social_history_id,
      operation_type,
      log_date,
      log_by,
      is_current_smoker,
      smoke_start_date,
      cigarette_amount,
      cigarette_type,
      is_previous_smoker,
      smoke_quit_date,
      exposure_to_second_hand_smoke,
      alcohol_consumption,
      type_of_alcohol,
      alcohol_since_when,
      substance_use,
      route,
      frequency,
      physical_limitation,
      diagnosed_eating_disorders,
      patient_is_free,
      free_text,
      status,
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
      NEW.is_current_smoker,
      NEW.smoke_start_date,
      NEW.cigarette_amount,
      NEW.cigarette_type,
      NEW.is_previous_smoker,
      NEW.smoke_quit_date,
      NEW.exposure_to_second_hand_smoke,
      NEW.alcohol_consumption,
      NEW.type_of_alcohol,
      NEW.alcohol_since_when,
      NEW.substance_use,
      NEW.route,
      NEW.frequency,
      NEW.physical_limitation,
      NEW.diagnosed_eating_disorders,
      NEW.patient_is_free,
      NEW.free_text,
      NEW.status,
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

      INSERT INTO public.claim_encounter_copy_social_history_logs (
        claim_encounter_copy_social_history_id,
        operation_type,
        log_date,
        log_by,
        is_current_smoker,
        smoke_start_date,
        cigarette_amount,
        cigarette_type,
        is_previous_smoker,
        smoke_quit_date,
        exposure_to_second_hand_smoke,
        alcohol_consumption,
        type_of_alcohol,
        alcohol_since_when,
        substance_use,
        route,
        frequency,
        physical_limitation,
        diagnosed_eating_disorders,
        patient_is_free,
        free_text,
        status,
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
        NEW.is_current_smoker,
        NEW.smoke_start_date,
        NEW.cigarette_amount,
        NEW.cigarette_type,
        NEW.is_previous_smoker,
        NEW.smoke_quit_date,
        NEW.exposure_to_second_hand_smoke,
        NEW.alcohol_consumption,
        NEW.type_of_alcohol,
        NEW.alcohol_since_when,
        NEW.substance_use,
        NEW.route,
        NEW.frequency,
        NEW.physical_limitation,
        NEW.diagnosed_eating_disorders,
        NEW.patient_is_free,
        NEW.free_text,
        NEW.status,
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

DROP TRIGGER IF EXISTS trg_claim_encounter_copy_social_history_log
ON public.claim_encounter_copy_social_histories;

CREATE TRIGGER trg_claim_encounter_copy_social_history_log
  AFTER INSERT OR UPDATE
                    ON public.claim_encounter_copy_social_histories
                    FOR EACH ROW
                    EXECUTE FUNCTION public.trg_claim_encounter_copy_social_history_log_fn();
