CREATE OR REPLACE FUNCTION fn_patient_payment_allocation_audit()
RETURNS TRIGGER
LANGUAGE plpgsql
AS
$$
BEGIN

    IF TG_OP = 'INSERT' THEN

        INSERT INTO patient_payment_allocations_log (
            allocation_id,
            payment_id,
            charge_id,
            action,
            paid_from_amount,
            paid_from_balance,
            created_date,
            last_modified_date,
            payload
        )
        VALUES (
            NEW.id,
            NEW.payment_id,
            NEW.charge_id,
            'CREATE',
            NEW.paid_from_amount,
            NEW.paid_from_balance,
            NEW.created_date,
            NEW.last_modified_date,
            to_jsonb(NEW)
        );

RETURN NEW;

ELSIF TG_OP = 'UPDATE' THEN

        INSERT INTO patient_payment_allocations_log (
            allocation_id,
            payment_id,
            charge_id,
            action,
            paid_from_amount,
            paid_from_balance,
            created_date,
            last_modified_date,
            payload
        )
        VALUES (
            NEW.id,
            NEW.payment_id,
            NEW.charge_id,
            'UPDATE',
            NEW.paid_from_amount,
            NEW.paid_from_balance,
            NEW.created_date,
            NEW.last_modified_date,
            jsonb_build_object(
                'old', to_jsonb(OLD),
                'new', to_jsonb(NEW)
            )
        );

RETURN NEW;

ELSIF TG_OP = 'DELETE' THEN

        INSERT INTO patient_payment_allocations_log (
            allocation_id,
            payment_id,
            charge_id,
            action,
            paid_from_amount,
            paid_from_balance,
            created_date,
            last_modified_date,
            payload
        )
        VALUES (
            OLD.id,
            OLD.payment_id,
            OLD.charge_id,
            'DELETE',
            OLD.paid_from_amount,
            OLD.paid_from_balance,
            OLD.created_date,
            OLD.last_modified_date,
            to_jsonb(OLD)
        );

RETURN OLD;

END IF;

RETURN NULL;

END;
$$;


DROP TRIGGER IF EXISTS trg_patient_payment_allocation_audit
    ON patient_payment_allocations;

CREATE TRIGGER trg_patient_payment_allocation_audit
  AFTER INSERT OR UPDATE OR DELETE
                  ON patient_payment_allocations
                    FOR EACH ROW
                    EXECUTE FUNCTION fn_patient_payment_allocation_audit();
