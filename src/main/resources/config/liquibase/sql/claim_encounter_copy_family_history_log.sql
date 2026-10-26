CREATE OR REPLACE FUNCTION log_claim_encounter_copy_family_history()
RETURNS TRIGGER AS
$$
BEGIN

    IF TG_OP = 'INSERT' THEN

        INSERT INTO claim_encounter_copy_family_history_logs (
            claim_encounter_copy_family_history_id,
            operation_type,
            log_date,
            log_by,
            condition,
            relation,
            inherited_diseases,
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
            NEW.condition,
            NEW.relation,
            NEW.inherited_diseases,
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

    ELSIF TG_OP = 'UPDATE'
        AND ROW(OLD.*) IS DISTINCT FROM ROW(NEW.*) THEN

        INSERT INTO claim_encounter_copy_family_history_logs (
            claim_encounter_copy_family_history_id,
            operation_type,
            log_date,
            log_by,
            condition,
            relation,
            inherited_diseases,
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
            NEW.condition,
            NEW.relation,
            NEW.inherited_diseases,
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

END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;


DROP TRIGGER IF EXISTS trg_claim_encounter_copy_family_history
ON claim_encounter_copy_family_histories;


CREATE TRIGGER trg_claim_encounter_copy_family_history
  AFTER INSERT OR UPDATE
                    ON claim_encounter_copy_family_histories
                    FOR EACH ROW
                    EXECUTE FUNCTION log_claim_encounter_copy_family_history();
