CREATE OR REPLACE FUNCTION fn_patient_charge_audit()
RETURNS TRIGGER
LANGUAGE plpgsql
AS
$$
BEGIN

    IF TG_OP = 'INSERT' THEN

        INSERT INTO patient_charges_log (
            patient_charge_id,
            patient_id,
            encounter_id,
            plan_id,
            action,
            due_amount,
            remaining,
            currency,
            facility_default_currency,
            created_date,
            last_modified_date,
            payload
        )
        VALUES (
            NEW.id,
            NEW.patient_id,
            NEW.encounter_id,
            NEW.plan_id,
            'CREATE',
            NEW.due_amount,
            NEW.remaining,
            NEW.currency,
            NEW.facility_default_currency,
            NEW.created_date,
            NEW.last_modified_date,
            to_jsonb(NEW)
        );

RETURN NEW;

ELSIF TG_OP = 'UPDATE' THEN

        INSERT INTO patient_charges_log (
            patient_charge_id,
            patient_id,
            encounter_id,
            plan_id,
            action,
            due_amount,
            remaining,
            currency,
            facility_default_currency,
            created_date,
            last_modified_date,
            payload
        )
        VALUES (
            NEW.id,
            NEW.patient_id,
            NEW.encounter_id,
            NEW.plan_id,
            'UPDATE',
            NEW.due_amount,
            NEW.remaining,
            NEW.currency,
            NEW.facility_default_currency,
            NEW.created_date,
            NEW.last_modified_date,
            jsonb_build_object(
                'old', to_jsonb(OLD),
                'new', to_jsonb(NEW)
            )
        );

RETURN NEW;

ELSIF TG_OP = 'DELETE' THEN

        INSERT INTO patient_charges_log (
            patient_charge_id,
            patient_id,
            encounter_id,
            plan_id,
            action,
            due_amount,
            remaining,
            currency,
            facility_default_currency,
            created_date,
            last_modified_date,
            payload
        )
        VALUES (
            OLD.id,
            OLD.patient_id,
            OLD.encounter_id,
            OLD.plan_id,
            'DELETE',
            OLD.due_amount,
            OLD.remaining,
            OLD.currency,
            OLD.facility_default_currency,
            OLD.created_date,
            OLD.last_modified_date,
            to_jsonb(OLD)
        );

RETURN OLD;

END IF;

RETURN NULL;

END;
$$;


DROP TRIGGER IF EXISTS trg_patient_charge_audit
    ON patient_charges;

CREATE TRIGGER trg_patient_charge_audit
  AFTER INSERT OR UPDATE OR DELETE
                  ON patient_charges
                    FOR EACH ROW
                    EXECUTE FUNCTION fn_patient_charge_audit();
