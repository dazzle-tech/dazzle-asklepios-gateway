CREATE OR REPLACE FUNCTION fn_patient_payment_audit()
RETURNS TRIGGER
LANGUAGE plpgsql
AS
$$
BEGIN

    IF TG_OP = 'INSERT' THEN

        INSERT INTO patient_payments_log (
            patient_payment_id,
            patient_id,
            encounter_id,
            plan_id,
            action,
            status,
            payment_method,
            payment_type,
            amount,
            remaining,
            refunds,
            currency,
            facility_default_currency,
            amount_in_facility_currency,
            created_by,
            created_date,
            last_modified_by,
            last_modified_date,
            payload
        )
        VALUES (
            NEW.id,
            NEW.patient_id,
            NEW.encounter_id,
            NEW.plan_id,
            'CREATE',
            NEW.status,
            NEW.payment_method,
            NEW.payment_type,
            NEW.amount,
            NEW.remaining,
            NEW.refunds,
            NEW.currency,
            NEW.facility_default_currency,
            NEW.amount_in_facility_currency,
            NEW.created_by,
            NEW.created_date,
            NEW.last_modified_by,
            NEW.last_modified_date,
            to_jsonb(NEW)
        );

RETURN NEW;

ELSIF TG_OP = 'UPDATE' THEN

        INSERT INTO patient_payments_log (
            patient_payment_id,
            patient_id,
            encounter_id,
            plan_id,
            action,
            status,
            payment_method,
            payment_type,
            amount,
            remaining,
            refunds,
            currency,
            facility_default_currency,
            amount_in_facility_currency,
            created_by,
            created_date,
            last_modified_by,
            last_modified_date,
            payload
        )
        VALUES (
            NEW.id,
            NEW.patient_id,
            NEW.encounter_id,
            NEW.plan_id,
            'UPDATE',
            NEW.status,
            NEW.payment_method,
            NEW.payment_type,
            NEW.amount,
            NEW.remaining,
            NEW.refunds,
            NEW.currency,
            NEW.facility_default_currency,
            NEW.amount_in_facility_currency,
            NEW.created_by,
            NEW.created_date,
            NEW.last_modified_by,
            NEW.last_modified_date,
            jsonb_build_object(
                'old', to_jsonb(OLD),
                'new', to_jsonb(NEW)
            )
        );

RETURN NEW;

ELSIF TG_OP = 'DELETE' THEN

        INSERT INTO patient_payments_log (
            patient_payment_id,
            patient_id,
            encounter_id,
            plan_id,
            action,
            status,
            payment_method,
            payment_type,
            amount,
            remaining,
            refunds,
            currency,
            facility_default_currency,
            amount_in_facility_currency,
            created_by,
            created_date,
            last_modified_by,
            last_modified_date,
            payload
        )
        VALUES (
            OLD.id,
            OLD.patient_id,
            OLD.encounter_id,
            OLD.plan_id,
            'DELETE',
            OLD.status,
            OLD.payment_method,
            OLD.payment_type,
            OLD.amount,
            OLD.remaining,
            OLD.refunds,
            OLD.currency,
            OLD.facility_default_currency,
            OLD.amount_in_facility_currency,
            OLD.created_by,
            OLD.created_date,
            OLD.last_modified_by,
            OLD.last_modified_date,
            to_jsonb(OLD)
        );

RETURN OLD;

END IF;

RETURN NULL;

END;
$$;


DROP TRIGGER IF EXISTS trg_patient_payment_audit
    ON patient_payments;

CREATE TRIGGER trg_patient_payment_audit
  AFTER INSERT OR UPDATE OR DELETE
                  ON patient_payments
                    FOR EACH ROW
                    EXECUTE FUNCTION fn_patient_payment_audit();
