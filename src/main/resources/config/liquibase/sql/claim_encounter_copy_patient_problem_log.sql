CREATE OR REPLACE FUNCTION log_claim_encounter_copy_patient_problem()
RETURNS TRIGGER AS
$$
BEGIN

    IF TG_OP = 'INSERT' THEN

        INSERT INTO claim_encounter_copy_patient_problem_logs (
            claim_encounter_copy_patient_problem_id,
            operation_type,
            log_date,
            log_by,
            condition,
            date_of_diagnosis,
            condition_status,
            type,
            date_of_resolution,
            by_patient,
            source_of_information,
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
            NEW.date_of_diagnosis,
            NEW.condition_status,
            NEW.type,
            NEW.date_of_resolution,
            NEW.by_patient,
            NEW.source_of_information,
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

        INSERT INTO claim_encounter_copy_patient_problem_logs (
            claim_encounter_copy_patient_problem_id,
            operation_type,
            log_date,
            log_by,
            condition,
            date_of_diagnosis,
            condition_status,
            type,
            date_of_resolution,
            by_patient,
            source_of_information,
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
            NEW.date_of_diagnosis,
            NEW.condition_status,
            NEW.type,
            NEW.date_of_resolution,
            NEW.by_patient,
            NEW.source_of_information,
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


DROP TRIGGER IF EXISTS trg_claim_encounter_copy_patient_problem
ON claim_encounter_copy_patient_problems;


CREATE TRIGGER trg_claim_encounter_copy_patient_problem
  AFTER INSERT OR UPDATE
                    ON claim_encounter_copy_patient_problems
                    FOR EACH ROW
                    EXECUTE FUNCTION log_claim_encounter_copy_patient_problem();
