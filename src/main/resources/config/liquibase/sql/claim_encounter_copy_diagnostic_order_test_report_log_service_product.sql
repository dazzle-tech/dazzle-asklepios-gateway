CREATE OR REPLACE FUNCTION log_claim_encounter_copy_diagnostic_order_test_report()
RETURNS TRIGGER AS $$
BEGIN

    IF TG_OP = 'INSERT' THEN

        INSERT INTO claim_encounter_copy_diagnostic_order_test_report_logs (
            claim_encounter_copy_diagnostic_order_test_report_id,
            operation_type,
            log_date,
            log_by,
            claim_encounter_copy_id,
            diagnostic_order_test_report_id,
            order_test_id,
            patient_service_product_id,
            report,
            radiologist_information,
            critical_findings,
            radiologist_comments,
            severity,
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
            NEW.diagnostic_order_test_report_id,
            NEW.order_test_id,
            NEW.patient_service_product_id,
            NEW.report,
            NEW.radiologist_information,
            NEW.critical_findings,
            NEW.radiologist_comments,
            NEW.severity,
            NEW.created_by,
            NEW.created_date,
            NEW.last_modified_by,
            NEW.last_modified_date
        );

RETURN NEW;

ELSIF TG_OP = 'UPDATE'
        AND ROW(OLD.*) IS DISTINCT FROM ROW(NEW.*) THEN

        INSERT INTO claim_encounter_copy_diagnostic_order_test_report_logs (
            claim_encounter_copy_diagnostic_order_test_report_id,
            operation_type,
            log_date,
            log_by,
            claim_encounter_copy_id,
            diagnostic_order_test_report_id,
            order_test_id,
            patient_service_product_id,
            report,
            radiologist_information,
            critical_findings,
            radiologist_comments,
            severity,
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
            NEW.diagnostic_order_test_report_id,
            NEW.order_test_id,
            NEW.patient_service_product_id,
            NEW.report,
            NEW.radiologist_information,
            NEW.critical_findings,
            NEW.radiologist_comments,
            NEW.severity,
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
