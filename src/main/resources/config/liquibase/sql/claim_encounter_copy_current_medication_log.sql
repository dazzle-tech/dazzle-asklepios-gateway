CREATE OR REPLACE FUNCTION log_claim_encounter_copy_current_medication()
RETURNS TRIGGER AS $$
BEGIN

    IF TG_OP = 'INSERT' THEN

        INSERT INTO claim_encounter_copy_current_medication_logs (
            claim_encounter_copy_current_medication_id,
            operation_type,
            log_date,
            log_by,
            claim_encounter_copy_id,
            current_medication_id,
            active_ingredient_id,
            dosage,
            unit,
            frequency,
            start_date,
            patient_is_free,
            free_text,
            status,
            cancelled_by,
            cancelled_date,
            cancellation_reason,
            created_by,
            created_date,
            last_modified_by,
            last_modified_date
        )
        VALUES (
            NEW.id,
            'INSERT',
            NOW(),
            NEW.created_by,
            NEW.claim_encounter_copy_id,
            NEW.current_medication_id,
            NEW.active_ingredient_id,
            NEW.dosage,
            NEW.unit,
            NEW.frequency,
            NEW.start_date,
            NEW.patient_is_free,
            NEW.free_text,
            NEW.status,
            NEW.cancelled_by,
            NEW.cancelled_date,
            NEW.cancellation_reason,
            NEW.created_by,
            NEW.created_date,
            NEW.last_modified_by,
            NEW.last_modified_date
        );

RETURN NEW;

ELSIF TG_OP = 'UPDATE'
        AND ROW(OLD.*) IS DISTINCT FROM ROW(NEW.*) THEN

        INSERT INTO claim_encounter_copy_current_medication_logs (
            claim_encounter_copy_current_medication_id,
            operation_type,
            log_date,
            log_by,
            claim_encounter_copy_id,
            current_medication_id,
            active_ingredient_id,
            dosage,
            unit,
            frequency,
            start_date,
            patient_is_free,
            free_text,
            status,
            cancelled_by,
            cancelled_date,
            cancellation_reason,
            created_by,
            created_date,
            last_modified_by,
            last_modified_date
        )
        VALUES (
            NEW.id,
            'UPDATE',
            NOW(),
            NEW.last_modified_by,
            NEW.claim_encounter_copy_id,
            NEW.current_medication_id,
            NEW.active_ingredient_id,
            NEW.dosage,
            NEW.unit,
            NEW.frequency,
            NEW.start_date,
            NEW.patient_is_free,
            NEW.free_text,
            NEW.status,
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

RETURN NEW;

END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_claim_encounter_copy_current_medication_audit
  AFTER INSERT OR UPDATE
                    ON claim_encounter_copy_current_medications
                    FOR EACH ROW
                    EXECUTE FUNCTION log_claim_encounter_copy_current_medication();
