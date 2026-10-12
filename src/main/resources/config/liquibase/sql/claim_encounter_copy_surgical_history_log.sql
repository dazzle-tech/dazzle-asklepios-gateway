CREATE OR REPLACE FUNCTION log_claim_encounter_copy_surgical_history()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN

    IF TG_OP = 'INSERT' THEN

        INSERT INTO claim_encounter_copy_surgical_history_logs (
            claim_encounter_copy_surgical_history_id,
            operation_type,
            log_date,
            log_by,
            surgery,
            date_of_surgery,
            facility,
            anesthesia_type,
            complications,
            adverse_reactions_to_anesthesia,
            has_implants_or_devices,
            implants_or_devices_description,
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
            NOW(),
            NEW.created_by,
            NEW.surgery,
            NEW.date_of_surgery,
            NEW.facility,
            NEW.anesthesia_type,
            NEW.complications,
            NEW.adverse_reactions_to_anesthesia,
            NEW.has_implants_or_devices,
            NEW.implants_or_devices_description,
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

            INSERT INTO claim_encounter_copy_surgical_history_logs (
                claim_encounter_copy_surgical_history_id,
                operation_type,
                log_date,
                log_by,
                surgery,
                date_of_surgery,
                facility,
                anesthesia_type,
                complications,
                adverse_reactions_to_anesthesia,
                has_implants_or_devices,
                implants_or_devices_description,
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
                NOW(),
                NEW.last_modified_by,
                NEW.surgery,
                NEW.date_of_surgery,
                NEW.facility,
                NEW.anesthesia_type,
                NEW.complications,
                NEW.adverse_reactions_to_anesthesia,
                NEW.has_implants_or_devices,
                NEW.implants_or_devices_description,
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
$$;

DROP TRIGGER IF EXISTS trg_claim_encounter_copy_surgical_history_log
ON claim_encounter_copy_surgical_histories;

CREATE TRIGGER trg_claim_encounter_copy_surgical_history_log
  AFTER INSERT OR UPDATE
                    ON claim_encounter_copy_surgical_histories
                    FOR EACH ROW
                    EXECUTE FUNCTION log_claim_encounter_copy_surgical_history();
