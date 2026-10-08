CREATE OR REPLACE FUNCTION log_claim_encounter_copy_hospitalization()
RETURNS TRIGGER AS
$$
BEGIN

    IF TG_OP = 'INSERT' THEN

        INSERT INTO claim_encounter_copy_hospitalization_logs (
            claim_encounter_copy_hospitalization_id,
            operation_type,
            log_date,
            log_by,
            facility,
            reason,
            admission_type,
            date_of_admission,
            length_of_stay_days,
            outcomes,
            medical_interventions_performed,
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
            NEW.facility,
            NEW.reason,
            NEW.admission_type,
            NEW.date_of_admission,
            NEW.length_of_stay_days,
            NEW.outcomes,
            NEW.medical_interventions_performed,
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

        INSERT INTO claim_encounter_copy_hospitalization_logs (
            claim_encounter_copy_hospitalization_id,
            operation_type,
            log_date,
            log_by,
            facility,
            reason,
            admission_type,
            date_of_admission,
            length_of_stay_days,
            outcomes,
            medical_interventions_performed,
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
            NEW.facility,
            NEW.reason,
            NEW.admission_type,
            NEW.date_of_admission,
            NEW.length_of_stay_days,
            NEW.outcomes,
            NEW.medical_interventions_performed,
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

DROP TRIGGER IF EXISTS trg_claim_encounter_copy_hospitalization
ON claim_encounter_copy_hospitalizations;

CREATE TRIGGER trg_claim_encounter_copy_hospitalization
  AFTER INSERT OR UPDATE
                    ON claim_encounter_copy_hospitalizations
                    FOR EACH ROW
                    EXECUTE FUNCTION log_claim_encounter_copy_hospitalization();
