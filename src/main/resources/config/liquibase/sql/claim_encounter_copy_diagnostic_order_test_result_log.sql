CREATE OR REPLACE FUNCTION log_claim_encounter_copy_diagnostic_order_test_result()
RETURNS TRIGGER AS $$
BEGIN

    IF TG_OP = 'INSERT' THEN

        INSERT INTO claim_encounter_copy_diagnostic_order_test_result_logs (
            claim_encounter_copy_diagnostic_order_test_result_id,
            operation_type,
            log_date,
            log_by,
            claim_encounter_copy_id,
            diagnostic_order_test_result_id,
            order_test_id,
            profile_test_id,
            result_value_number,
            result_value_text,
            marker,
            normal_range_value,
            result_type_at_entry,
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
            NEW.diagnostic_order_test_result_id,
            NEW.order_test_id,
            NEW.profile_test_id,
            NEW.result_value_number,
            NEW.result_value_text,
            NEW.marker,
            NEW.normal_range_value,
            NEW.result_type_at_entry,
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

        INSERT INTO claim_encounter_copy_diagnostic_order_test_result_logs (
            claim_encounter_copy_diagnostic_order_test_result_id,
            operation_type,
            log_date,
            log_by,
            claim_encounter_copy_id,
            diagnostic_order_test_result_id,
            order_test_id,
            profile_test_id,
            result_value_number,
            result_value_text,
            marker,
            normal_range_value,
            result_type_at_entry,
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
            NEW.diagnostic_order_test_result_id,
            NEW.order_test_id,
            NEW.profile_test_id,
            NEW.result_value_number,
            NEW.result_value_text,
            NEW.marker,
            NEW.normal_range_value,
            NEW.result_type_at_entry,
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

CREATE TRIGGER trg_claim_encounter_copy_diagnostic_order_test_result_audit
  AFTER INSERT OR UPDATE
                    ON claim_encounter_copy_diagnostic_order_test_results
                    FOR EACH ROW
                    EXECUTE FUNCTION log_claim_encounter_copy_diagnostic_order_test_result();
