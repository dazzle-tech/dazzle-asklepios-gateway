CREATE OR REPLACE FUNCTION trg_encounter_plans_log_fn()
RETURNS TRIGGER AS $$
BEGIN

    IF TG_OP = 'INSERT' THEN

        IF NEW.goals IS NOT NULL THEN
            INSERT INTO patient_encounter_plan_field_audit
            (
                encounter_plan_id,
                field_name,
                operation_type,
                old_value,
                new_value,
                log_date,
                log_by
            )
            VALUES
            (
                NEW.id,
                'goals',
                'INSERT',
                NULL,
                NEW.goals,
                NEW.created_date,
                NEW.created_by
            );
END IF;

        IF NEW.treatment_plan IS NOT NULL THEN
            INSERT INTO patient_encounter_plan_field_audit
            (
                encounter_plan_id,
                field_name,
                operation_type,
                old_value,
                new_value,
                log_date,
                log_by
            )
            VALUES
            (
                NEW.id,
                'treatmentPlan',
                'INSERT',
                NULL,
                NEW.treatment_plan,
                NEW.created_date,
                NEW.created_by
            );
END IF;

    ELSIF TG_OP = 'UPDATE' THEN

        IF OLD.goals IS DISTINCT FROM NEW.goals THEN
            INSERT INTO patient_encounter_plan_field_audit
            (
                encounter_plan_id,
                field_name,
                operation_type,
                old_value,
                new_value,
                log_date,
                log_by
            )
            VALUES
            (
                NEW.id,
                'goals',
                'UPDATE',
                OLD.goals,
                NEW.goals,
                NEW.last_modified_date,
                NEW.last_modified_by
            );
END IF;

        IF OLD.treatment_plan IS DISTINCT FROM NEW.treatment_plan THEN
            INSERT INTO patient_encounter_plan_field_audit
            (
                encounter_plan_id,
                field_name,
                operation_type,
                old_value,
                new_value,
                log_date,
                log_by
            )
            VALUES
            (
                NEW.id,
                'treatmentPlan',
                'UPDATE',
                OLD.treatment_plan,
                NEW.treatment_plan,
                NEW.last_modified_date,
                NEW.last_modified_by
            );
END IF;

END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;


DROP TRIGGER IF EXISTS trg_encounter_plans_log
ON encounter_plans;


CREATE TRIGGER trg_encounter_plans_log
  AFTER INSERT OR UPDATE
                    ON encounter_plans
                    FOR EACH ROW
                    EXECUTE FUNCTION trg_encounter_plans_log_fn();
